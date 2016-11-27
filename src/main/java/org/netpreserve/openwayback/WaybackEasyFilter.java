package org.netpreserve.openwayback;

import org.archive.wayback.ReplayDispatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class WaybackEasyFilter implements Filter {
    WaybackEasy waybackEasy;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();

        Yaml yaml = new Yaml();
        Map<String,Object> config = yaml.loadAs(getClass().getResourceAsStream("/config.yaml"), Map.class);

        ApplicationContext springXml = new FileSystemXmlApplicationContext("file:/WEB-INF/ArchivalUrlReplay.xml");
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
