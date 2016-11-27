package org.netpreserve.openwayback;

import org.archive.wayback.ReplayDispatcher;
import org.archive.wayback.ResourceIndex;
import org.archive.wayback.ResourceStore;
import org.archive.wayback.archivalurl.ArchivalUrlRequestParser;
import org.archive.wayback.archivalurl.ArchivalUrlResultURIConverter;
import org.archive.wayback.query.Renderer;
import org.archive.wayback.resourceindex.LocalResourceIndex;
import org.archive.wayback.resourceindex.WatchedCDXSource;
import org.archive.wayback.resourceindex.cdx.CDXIndex;
import org.archive.wayback.resourceindex.cdxserver.EmbeddedCDXServerIndex;
import org.archive.wayback.resourcestore.SimpleResourceStore;
import org.archive.wayback.util.url.KeyMakerUrlCanonicalizer;
import org.archive.wayback.util.webapp.RequestMapper;
import org.archive.wayback.webapp.AccessPoint;
import org.archive.wayback.webapp.WaybackCollection;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class WaybackEasy implements Closeable {
    Map<String, AccessPoint> accessPoints = new HashMap<>();

    WaybackEasy(Map<String, Object> config, ServletContext servletContext, ReplayDispatcher replay) {

        Map<String, Object> collections = (Map<String, Object>) config.get("collections");

        for (Map.Entry<String, Object> entry : collections.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            String prefix = servletContext.getContextPath() + "/" + name + "/";

            AccessPoint accessPoint = new AccessPoint();
            accessPoint.setServletContext(servletContext);
            accessPoint.setReplay(replay);

            Renderer query = new Renderer();
            query.setCaptureJsp("/WEB-INF/query/CalendarResults.jsp");
            accessPoint.setQuery(query);

            ArchivalUrlRequestParser parser = new ArchivalUrlRequestParser();
            parser.init();
            accessPoint.setParser(parser);

            ArchivalUrlResultURIConverter uriConverter = new ArchivalUrlResultURIConverter();
            uriConverter.setReplayURIPrefix(prefix);
            accessPoint.setUriConverter(uriConverter);

            accessPoint.setStaticPrefix(servletContext.getContextPath().isEmpty() ? "/" : servletContext.getContextPath() + "/");
            accessPoint.setReplayPrefix(prefix);
            accessPoint.setQueryPrefix(prefix);

            WaybackCollection collection = new WaybackCollection();

            if (value instanceof String) {
                collection.setResourceIndex(configureResourceIndex(config, value));
                collection.setResourceStore(configureResourceStore(value));
            } else if (value instanceof Map) {
                Map<String,String> map = (Map<String,String>) value;
                collection.setResourceIndex(configureResourceIndex(config, map.get("index")));
                collection.setResourceStore(configureResourceStore(map.get("resource")));
            } else {
                throw new IllegalArgumentException("collection " + name + " must be a string or map");
            }

            accessPoint.setCollection(collection);

            accessPoints.put(name, accessPoint);
        }
    }

    private ResourceStore configureResourceStore(Object resource) {
        if (resource instanceof ResourceStore) {
            return (ResourceStore) resource;
        } else if (resource instanceof String) {
            SimpleResourceStore resourceStore = new SimpleResourceStore();
            resourceStore.setPrefix((String) resource);
            return resourceStore;
        } else {
            throw new IllegalArgumentException("must be ResourceStore, path or URL");
        }
    }

    private ResourceIndex configureResourceIndex(Map<String, Object> config, Object index) {
        boolean surtOrdered = (Boolean) config.getOrDefault("surt_ordered", true);
        KeyMakerUrlCanonicalizer canonicalizer = new KeyMakerUrlCanonicalizer(surtOrdered);

        if (index instanceof ResourceIndex) {
            return (ResourceIndex) index;
        } else if (index instanceof String) {
            String url = (String) index;
            if (url.startsWith("http:") || url.startsWith("https:")) {
                EmbeddedCDXServerIndex resourceIndex = new EmbeddedCDXServerIndex();
                resourceIndex.setRemoteCdxPath(url);
                resourceIndex.setCanonicalizer(canonicalizer);
                return resourceIndex;
            } else {
                Path path = Paths.get(url);
                if (Files.isDirectory(path)) {
                    LocalResourceIndex resourceIndex = new LocalResourceIndex();
                    WatchedCDXSource cdxSource = new WatchedCDXSource();
                    cdxSource.setPath(path.toString());
                    resourceIndex.setSource(cdxSource);
                    resourceIndex.setCanonicalizer(canonicalizer);
                    return resourceIndex;
                } else if (Files.isRegularFile(path)) {
                    LocalResourceIndex resourceIndex = new LocalResourceIndex();
                    CDXIndex cdxIndex = new CDXIndex();
                    cdxIndex.setPath(path.toString());
                    resourceIndex.setSource(cdxIndex);
                    resourceIndex.setCanonicalizer(canonicalizer);
                    return resourceIndex;
                } else {
                    throw new IllegalArgumentException("no such CDX file or directory: " + path);
                }
            }
        } else {
            throw new IllegalArgumentException("must be a ResourceIndex, path or URL");
        }
    }

    boolean handle(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith("/")) {
            return false;
        }
        int i = path.indexOf('/', 1);
        if (i == -1) {
            i = path.length();
        }
        AccessPoint accessPoint = accessPoints.get(path.substring(1, i));
        if (accessPoint == null) {
            return false;
        }
        request.setAttribute(RequestMapper.REQUEST_CONTEXT_PREFIX, request.getRequestURI().substring(0, i + request.getContextPath().length() + 1));
        accessPoint.handleRequest(request, response);
        return true;
    }

    @Override
    public void close() {
        for (AccessPoint accessPoint : accessPoints.values()) {
            accessPoint.shutdown();
        }
    }
}
