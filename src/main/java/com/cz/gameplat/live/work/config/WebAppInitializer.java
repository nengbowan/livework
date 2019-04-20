package com.cz.gameplat.live.work.config;

import org.springframework.web.*;
import javax.servlet.*;
import org.springframework.web.context.support.*;
import org.springframework.web.context.*;
import java.util.*;

public class WebAppInitializer implements WebApplicationInitializer
{
    public void onStartup(final ServletContext container) {
        final AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(new Class[] { RedisConfig.class });
        container.addListener((EventListener)new ContextLoaderListener((WebApplicationContext)rootContext));
    }
}
