package org.netpreserve.openwayback;

import org.archive.wayback.ReplayDispatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class WaybackEasyFilter implements Filter {
    WaybackEasy waybackEasy;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();

        String configFile = filterConfig.getInitParameter("wayback.config");
        if (configFile == null) {
            configFile = filterConfig.getServletContext().getInitParameter("wayback.config");
        }
        if (configFile == null) {
            configFile = System.getProperty("wayback.config");
        }
        if (configFile == null) {
            configFile = System.getenv("WAYBACK_CONFIG");
        }
        if (configFile == null) {
            throw new ServletException("A Wayback configuration file must be specied using servlet/system propery 'wayback.config' or environment variable WAYBACK_CONFIG");
        }

        Yaml yaml = new Yaml();
        Map<String,Object> config;

        try {
            try (InputStream stream = new FileInputStream(configFile)) {
                config = yaml.loadAs(stream, Map.class);
            }
        } catch (IOException e) {
            throw new ServletException("unable to read wayback configuration file: " + configFile, e);
        }

        String xmlPath = servletContext.getRealPath("/WEB-INF/ArchivalUrlReplay.xml");
        ApplicationContext springXml = new FileSystemXmlApplicationContext("file:" + xmlPath);
        ReplayDispatcher replay = springXml.getBean("archivalurlreplay", ReplayDispatcher.class);

        waybackEasy = new WaybackEasy(config, servletContext, replay);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        boolean finished = false;
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            finished = waybackEasy.handle((HttpServletRequest) request, (HttpServletResponse) response);
        }
        if (!finished) {
            chain.doFilter(request, response);
        }
    }


    @Override
    public void destroy() {

    }
}
