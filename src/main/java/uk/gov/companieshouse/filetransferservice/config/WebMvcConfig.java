package uk.gov.companieshouse.filetransferservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.filetransferservice.security.LoggingInterceptor;


@Component
public class WebMvcConfig implements WebMvcConfigurer {
    private final LoggingInterceptor loggingInterceptor;
    private final InternalUserInterceptor internalUserInterceptor;

    private final String HEALTHCHECK_PATH="/file-transfer-service/healthcheck";


    @Autowired
    public WebMvcConfig(LoggingInterceptor loggingInterceptor,
                        InternalUserInterceptor internalUserInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
        this.internalUserInterceptor = internalUserInterceptor;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor).excludePathPatterns(HEALTHCHECK_PATH);
        registry.addInterceptor(internalUserInterceptor).excludePathPatterns(HEALTHCHECK_PATH);
    }
}
