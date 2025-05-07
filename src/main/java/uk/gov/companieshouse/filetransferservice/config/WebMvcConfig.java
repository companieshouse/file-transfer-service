package uk.gov.companieshouse.filetransferservice.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.filetransferservice.logging.LoggingInterceptor;

@Component
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String HEALTH_CHECK_PATH = "/file-transfer-service/healthcheck";

    private final LoggingInterceptor loggingInterceptor;
    private final InternalUserInterceptor internalUserInterceptor;

    public WebMvcConfig(LoggingInterceptor loggingInterceptor, InternalUserInterceptor internalUserInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
        this.internalUserInterceptor = internalUserInterceptor;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor).excludePathPatterns(HEALTH_CHECK_PATH);
        registry.addInterceptor(internalUserInterceptor).excludePathPatterns(HEALTH_CHECK_PATH);
    }
}
