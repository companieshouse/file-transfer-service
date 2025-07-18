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

/**
 * A filter that sets a default Content-Type of application/json for POST requests
 * that do not have a Content-Type header set. This is intended for legacy clients
 * that may not set the Content-Type header.
 *
 * This can be removed in future versions as it is a workaround for legacy clients.
 * It is recommended that clients set the Content-Type header explicitly, and
 * updating the private-api-sdk-java dependence > 4.0.315 should resolve this issue.
 *
 * @deprecated This filter is deprecated and will be removed in future versions.
 */
@Component
@Deprecated(since = "0.2.16", forRemoval = true)
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
