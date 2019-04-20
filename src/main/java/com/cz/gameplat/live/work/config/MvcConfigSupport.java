package com.cz.gameplat.live.work.config;

import org.springframework.stereotype.*;
import com.cz.gameplat.esports.service.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.web.accept.*;
import com.github.theborakompanioni.spring.useragentutils.*;
import org.springframework.http.*;
import org.springframework.web.servlet.mvc.method.annotation.*;
import org.springframework.web.method.support.*;
import java.nio.charset.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.support.*;
import org.springframework.http.converter.xml.*;
import org.springframework.http.converter.json.*;
import org.springframework.web.bind.support.*;
import com.cz.rest.config.*;
import org.springframework.beans.*;
import org.springframework.web.servlet.config.annotation.*;
import com.cz.framework.web.*;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.handler.*;
import org.springframework.web.servlet.i18n.*;
import org.springframework.context.support.*;
import org.springframework.validation.*;
import org.springframework.validation.beanvalidation.*;
import org.hibernate.validator.*;
import org.springframework.context.*;
import com.cz.gameplat.sports.manager.*;
import java.util.*;
import com.cz.framework.*;
import java.io.*;
import org.springframework.core.io.*;
import org.springframework.util.*;
import org.slf4j.*;

@Configuration
@ImportResource({ "classpath:spring/admin-mvc.xml" })
@EnableWebMvc
@ComponentScan(basePackages = { "com.cz.gameplat" }, useDefaultFilters = false, includeFilters = { @ComponentScan.Filter(type = FilterType.ANNOTATION, value = { Controller.class }) })
public class MvcConfigSupport extends WebMvcConfigurationSupport
{
    @Autowired
    private SyncEsportsDataService syncEsportsDataService;
    private static final boolean jaxb2Present;
    private static final boolean jackson2Present;
    private static final Logger logger;
    
    @Bean
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        return super.requestMappingHandlerMapping();
    }
    
    @Bean
    public ContentNegotiationManager mvcContentNegotiationManager() {
        return super.mvcContentNegotiationManager();
    }
    
    @Bean
    public UserAgentResolverHandlerInterceptor userAgentResolverHandlerInterceptor() {
        return new UserAgentResolverHandlerInterceptor();
    }
    
    @Bean
    public UserAgentHandlerMethodArgumentResolver userAgentHandlerMethodArgumentResolver() {
        return new UserAgentHandlerMethodArgumentResolver();
    }
    
    public void configureContentNegotiation(final ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(false).favorParameter(false);
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }
    
    @Bean
    public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
        return super.requestMappingHandlerAdapter();
    }
    
    protected void addArgumentResolvers(final List<HandlerMethodArgumentResolver> argumentResolvers) {
        System.out.println("---------===addArgumentResolvers start");
        argumentResolvers.add((HandlerMethodArgumentResolver)this.userAgentHandlerMethodArgumentResolver());
    }
    
    public void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
        final StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        stringConverter.setWriteAcceptCharset(false);
        converters.add((HttpMessageConverter<?>)new ByteArrayHttpMessageConverter());
        converters.add((HttpMessageConverter<?>)stringConverter);
        converters.add((HttpMessageConverter<?>)new ResourceHttpMessageConverter());
        converters.add((HttpMessageConverter<?>)new SourceHttpMessageConverter());
        converters.add((HttpMessageConverter<?>)new AllEncompassingFormHttpMessageConverter());
        if (MvcConfigSupport.jaxb2Present) {
            converters.add((HttpMessageConverter<?>)new Jaxb2RootElementHttpMessageConverter());
        }
        if (MvcConfigSupport.jackson2Present) {
            final MappingJackson2HttpMessageConverter convert = new MappingJackson2HttpMessageConverter();
            convert.setObjectMapper(WafJsonMapper.getMapper());
            final List<MediaType> supportedMediaTypes = new ArrayList<MediaType>();
            supportedMediaTypes.add(MediaType.APPLICATION_JSON);
            convert.setSupportedMediaTypes((List)supportedMediaTypes);
            converters.add((HttpMessageConverter<?>)convert);
        }
    }
    
    protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer() {
        MvcConfigSupport.logger.info("---------===ConfigurableWebBindingInitializer");
        final ConfigurableWebBindingInitializer initializer = super.getConfigurableWebBindingInitializer();
        final DatePropertyEditorRegistrar register = new DatePropertyEditorRegistrar();
        register.setFormat("yyyy-MM-dd");
        initializer.setPropertyEditorRegistrar((PropertyEditorRegistrar)register);
        return initializer;
    }
    
    protected void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor((HandlerInterceptor)this.requestLogInterceptor()).addPathPatterns(new String[] { "/**" });
        registry.addInterceptor((HandlerInterceptor)this.userAgentResolverHandlerInterceptor());
    }
    
    @Bean
    public RequestLogInterceptor requestLogInterceptor() {
        return new RequestLogInterceptor();
    }
    
    @Bean
    public WebExceptionResolver webExceptionResolver() {
        return new WebExceptionResolver();
    }
    
    @Bean
    public HandlerExceptionResolver handlerExceptionResolver() {
        return (HandlerExceptionResolver)new HandlerExceptionResolverComposite();
    }
    
    @Bean
    public CookieLocaleResolver localeResolver() {
        return new CookieLocaleResolver();
    }
    
    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        final ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:bundle/messages");
        source.setDefaultEncoding("UTF-8");
        source.setCacheSeconds(0);
        source.setUseCodeAsDefaultMessage(false);
        return source;
    }
    
    @Bean
    protected Validator getValidator() {
        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setProviderClass((Class)HibernateValidator.class);
        validator.setValidationMessageSource((MessageSource)this.messageSource());
        return (Validator)validator;
    }
    
    public void setApplicationContext(final ApplicationContext applicationContext) {
        super.setApplicationContext(applicationContext);
        SportManager.setApplicationContext(applicationContext);
        final Resource resource = applicationContext.getResource("classpath:config.properties");
        final Properties properties = new Properties();
        try {
            properties.load(resource.getInputStream());
            final String api = properties.getProperty("collect.sport.api");
            SportManager.setApiBaseUrl(api);
            final String apiSlaves = properties.getProperty("collect.sport.url.bak");
            if (apiSlaves != null && apiSlaves.length() > 0) {
                if (apiSlaves.indexOf(",") > 0) {
                    Arrays.asList(apiSlaves.split(",")).forEach(e -> SportManager.setApiSlaves(e));
                }
                else {
                    SportManager.setApiSlaves(apiSlaves);
                }
                this.syncEsportsDataService.startSyncResultThread();
            }
            else {
                LogUtil.error("\u6ca1\u6709\u914d\u7f6e\u4ece\u91c7\u96c6\u670d\u52a1\u5668,\u4e3b\u91c7\u96c6\u6302\u540e\u65e0\u6cd5\u4f7f\u7528\u8d1f\u8f7d\u81ea\u52a8\u8c03\u5ea6\u5230\u4ece\u91c7\u96c6.");
            }
        }
        catch (IOException e2) {
            e2.printStackTrace();
        }
    }
    
    static {
        jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", MvcConfigSupport.class.getClassLoader());
        jackson2Present = (ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", MvcConfigSupport.class.getClassLoader()) && ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", MvcConfigSupport.class.getClassLoader()));
        logger = LoggerFactory.getLogger((Class)MvcConfigSupport.class);
    }
}
