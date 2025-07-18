package uk.gov.companieshouse.filetransferservice.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.logging.Logger;

@Component
public class DefaultContentTypeFilter implements Filter {

    private final Logger logger;

    public DefaultContentTypeFilter(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        logger.trace("doFilter(req, res, chain) method called.");

        HttpServletRequest request = (HttpServletRequest) req;

        // If Content-Type is missing and it's a POST wrap it (for legacy/deprecated client
        if (request.getContentType() == null && ("POST".equalsIgnoreCase(request.getMethod()))) {

            HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {

                @Override
                public String getContentType() {
                    return MediaType.APPLICATION_JSON_VALUE;
                }

                @Override
                public String getHeader(String name) {
                    if ("Content-Type".equalsIgnoreCase(name)) {
                        return MediaType.APPLICATION_JSON_VALUE;
                    }
                    return super.getHeader(name);
                }
            };

            chain.doFilter(wrapper, res);

            return;
        }

        chain.doFilter(req, res);
    }
}
