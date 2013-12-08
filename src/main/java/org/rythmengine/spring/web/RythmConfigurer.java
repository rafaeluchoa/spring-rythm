package org.rythmengine.spring.web;

import org.rythmengine.RythmEngine;
import org.rythmengine.conf.RythmConfigurationKey;
import org.rythmengine.exception.RythmException;
import org.rythmengine.extension.ISourceCodeEnhancer;
import org.rythmengine.spring.RythmEngineFactory;
import org.rythmengine.template.ITemplate;
import org.rythmengine.utils.S;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.*;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: luog
 * Date: 2/12/13
 * Time: 7:58 PM
 * To change this template use File | Settings | File Templates.
 */
@Configuration
@EnableWebMvc
@ComponentScan("org.rythmengine.spring.web")
public class RythmConfigurer extends RythmEngineFactory implements
    RythmHolder, InitializingBean, DisposableBean,
    ResourceLoaderAware, ServletContextAware,
    WebMvcConfigurer {

    /**
     * so that app developer can retrieve the servlet context with
     * {@code @servletContext}
     */
    public static final String CTX_SERVLET_CONTEXT = "servletContext";

    //public static final String CTX_

    public static final String CONF_USER_CONTEXT = "userContext";
    /**
     * Do we allow add all http request parameters into the render args?
     */
    public static final String CONF_OUTOUT_REQ_PARAMS = "outputRequestParameters";

    public static final String CONF_UNDERSCORE_IMPLICIT_VAR_NAME = "underscoreImplicityVarName";

    public static final String CONF_CSRF_PARAM_NAME = "csrfParameterName";

    public static final String CONF_CSRF_HEADER_NAME = "csrfHeaderName";

    private static RythmEngine engine;

    private ServletContext ctx;

    private boolean outputRequestParameters = false;

    private boolean underscoreImplicitVariableName = false;

    private String sessionCookiePrefix = null;

    private String sessionCookieExpire = null;

    private boolean autoCsrfCheck = true;

    private boolean enableSessionManager = false;

    private boolean enableCacheFor = false;

    private static String secretKey;

    private String csrfParamName = null;

    private String csrfHeaderName = null;

    private static RythmConfigurer inst;

    public static RythmConfigurer getInstance() {
        return inst;
    }

    public void setRythmEngine(RythmEngine engine) {
        this.engine = engine;
    }

    @Override
    public RythmEngine getRythmEngine() {
        return engine;
    }

    public static RythmEngine engine() {
        return engine;
    }

    public void setOutputRequestParameters(boolean outputRequestParameters) {
        this.outputRequestParameters = outputRequestParameters;
    }

    public void setUnderscoreImplicitVariableName(boolean underscoreImplicitVariableName) {
        this.underscoreImplicitVariableName = underscoreImplicitVariableName;
    }

    public void setAutoCsrfCheck(boolean autoCsrfCheck) {
        this.autoCsrfCheck = autoCsrfCheck;
    }

    public void setEnableCacheFor(boolean enableCacheFor) {
        this.enableCacheFor = enableCacheFor;
    }

    public void setEnableSessionManager(boolean enableSessionManager) {
        this.enableSessionManager = enableSessionManager;
    }

    public boolean sessionManagerEnabled() {
        return enableSessionManager;
    }

    public void setSecretKey(String secretKey) {
        Assert.hasText(secretKey);
        int len = secretKey.length();
        int delta = 16 - len;
        if (delta > 0) {
            StringBuilder sb = new StringBuilder(secretKey);
            for (int i = 0; i < delta; ++i) {
                sb.append("\u0000");
            }
            secretKey = sb.toString();
        }
        RythmConfigurer.secretKey = secretKey;
    }

    public static String getSecretKey() {
        return secretKey;
    }

    public void setSessionCookiePrefix(String sessionCookiePrefix) {
        Assert.notNull(sessionCookiePrefix);
        this.sessionCookiePrefix = sessionCookiePrefix;
    }

    public void setConfSessionCookieExpire(String sessionCookieExpire) {
        Assert.notNull(sessionCookieExpire);
        this.sessionCookieExpire = sessionCookieExpire;
    }

    public void setCsrfParamName(String csrfParamName) {
        Assert.notNull(csrfParamName);
        this.csrfParamName = csrfParamName;
    }

    public void setCsrfHeaderName(String csrfHeaderName) {
        Assert.notNull(csrfHeaderName);
        this.csrfHeaderName = csrfHeaderName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (null == engine) {
            engine = createRythmEngine();
        }
        inst = this;
    }

    @Override
    public void destroy() throws Exception {
        if (null != engine) {
            engine.shutdown();
        }
    }

    private Map<String, Object> userContext = new HashMap<String, Object>();

    private void setUserContext(String key, Object v) {
        userContext.put(key, v);
    }

    @Override
    protected void configRythm(Map<String, Object> config) {
//        WebApplicationContext ctx = (WebApplicationContext)getApplicationContext();
//        if (!config.containsKey(RythmConfigurationKey.HOME_TMP.getKey())) {
//            File tmpdir = (File)ctx.getServletContext().getAttribute("javax.servlet.context.tempdir");
//            if (null != tmpdir) {
//                config.put(RythmConfigurationKey.HOME_TMP.getKey(), new File(tmpdir, "__rythm"));
//            }
//        }
        config.put(RythmConfigurationKey.CODEGEN_SOURCE_CODE_ENHANCER.getKey(), new ISourceCodeEnhancer() {
            ImplicitVariables implicitVariables = new ImplicitVariables(underscoreImplicitVariableName);

            @Override
            public List<String> imports() {
                List<String> l = new ArrayList<String>();
                return l;
            }

            @Override
            public String sourceCode() {
                return "";
            }

            @Override
            public Map<String, ?> getRenderArgDescriptions() {
                Map<String, Object> m = new HashMap<String, Object>();
                for (ImplicitVariables.Var var : implicitVariables.vars) {
                    m.put(var.name(), var.type);
                }
                return m;
            }

            @Override
            public void setRenderArgs(ITemplate template) {
                Map<String, Object> m = new HashMap<String, Object>();
                for (ImplicitVariables.Var var : implicitVariables.vars) {
                    m.put(var.name(), var.evaluate());
                }
                template.__setRenderArgs(m);
            }
        });
    }

    @Override
    protected void postProcessRythmEngine(RythmEngine engine) throws IOException, RythmException {
        engine.setProperty(CONF_USER_CONTEXT, userContext);
        setUserContext(CTX_SERVLET_CONTEXT, ctx);
        setUserContext(ServletContext.class.getName(), ctx);
        engine.setProperty(CONF_OUTOUT_REQ_PARAMS, outputRequestParameters);
        engine.setProperty(CONF_UNDERSCORE_IMPLICIT_VAR_NAME, underscoreImplicitVariableName);
        if (null != csrfHeaderName) engine.setProperty(CONF_CSRF_HEADER_NAME, csrfHeaderName);
        if (null != csrfParamName) engine.setProperty(CONF_CSRF_PARAM_NAME, csrfParamName);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        ctx = servletContext;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {

    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {

    }

    @Override
    public Validator getValidator() {
        return null;
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {

    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {

    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {

    }

    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {

    }

    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {

    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (autoCsrfCheck) {
            CsrfManager csrfManager = new CsrfManager();
            csrfManager.setParameterName(csrfParamName);
            csrfManager.setHeaderName(csrfHeaderName);
            registry.addInterceptor(csrfManager);
        }
        if (enableSessionManager) {
            if (S.empty(secretKey)) {
                throw new RuntimeException("No secure salt configured while session manager is enabled");
            }
            SessionManager sm = new SessionManager();
            sm.setSessionExpire(sessionCookieExpire);
            registry.addInterceptor(sm);
        }
        if (enableCacheFor && enableCache) {
            CacheInterceptor ci = new CacheInterceptor();
            ci.setEngine(engine);
            registry.addInterceptor(ci);
        }
    }

    @Override
    public MessageCodesResolver getMessageCodesResolver() {
        return null;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {

    }
}