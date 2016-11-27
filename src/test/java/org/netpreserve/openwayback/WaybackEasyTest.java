package org.netpreserve.openwayback;

import org.apache.commons.io.IOUtils;
import org.archive.wayback.webapp.AccessPoint;
import org.archive.wayback.webapp.WaybackCollection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WaybackEasyTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void test() throws IOException {
        copyTestFiles();

        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getContextPath()).thenReturn("/wayback");

        WaybackEasy waybackEasy = new WaybackEasy(loadTestConfig(), servletContext, null);

        assertEquals(new HashSet(Arrays.asList("testcoll", "coll2", "remotecoll", "locationdb")), waybackEasy.accessPoints.keySet());

        AccessPoint ap = waybackEasy.accessPoints.get("testcoll");

        assertEquals("/wayback/testcoll/", ap.getQueryPrefix());
        assertEquals("/wayback/testcoll/", ap.getReplayPrefix());
        assertEquals("/wayback/", ap.getStaticPrefix());

        WaybackCollection collection = ap.getCollection();

    }

    private void copyTestFiles() throws IOException {
        File cdxDir = folder.newFolder("cdx");
        try (InputStream stream = getClass().getResourceAsStream("example.cdx")) {
            Files.copy(stream, new File(cdxDir, "example.cdx").toPath());
        }
    }

    private Map<String,Object> loadTestConfig() throws IOException {
        String text;
        try (InputStream stream = getClass().getResourceAsStream("config.yaml")) {
            text = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        text = text.replace("${TESTDIR}", folder.getRoot().toString());
        return new Yaml().loadAs(text, Map.class);
    }
}
