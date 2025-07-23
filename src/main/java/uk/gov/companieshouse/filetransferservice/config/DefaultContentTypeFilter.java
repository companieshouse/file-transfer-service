package uk.gov.companieshouse.filetransferservice.config;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.logging.Logger;

/**
 * A filter that sets a default Content-Type of application/json for POST requests
 * that do not have a Content-Type header set. This is intended for legacy clients
 * that may not set the Content-Type header.
 *
 * <p>
 * This can be removed in future versions as it is a workaround for legacy clients.
 * It is recommended that clients set the Content-Type header explicitly, and
 * updating the private-api-sdk-java dependence > 4.0.315 should resolve this issue.
 * </p>
 *
 * @deprecated This filter is deprecated and will be removed in future versions.
 */
@Component
@Deprecated(since = "0.2.16", forRemoval = true)
public class DefaultContentTypeFilter implements Filter {

    private final Logger logger;
    private final List<String> contentRequiredMethods;

    public DefaultContentTypeFilter(final Logger logger) {
        this.logger = logger;
        this.contentRequiredMethods = List.of("POST", "PUT", "PATCH");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        logger.trace("doFilter(request, response, chain) method called.");

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        logRequestDetails(httpRequest);

        // Issue warning if the request method is POST, PUT, or PATCH and the Content-Type header is not supplied.
        if(contentRequiredMethods.contains(httpRequest.getMethod()) && isBlank(httpRequest.getContentType())) {
            logger.info(format("WARNING: Content-Type NOT supplied for %s request: %s", httpRequest.getMethod(), httpRequest.getRequestURI()));
        }

        // Issue warning if the Accept header is not supplied for all requests.
        if(isBlank(httpRequest.getHeader("Accept"))) {
            logger.info(format("WARNING: Accept header NOT supplied for %s request: %s", httpRequest.getMethod(), httpRequest.getRequestURI()));
        }

        chain.doFilter(request, response);
    }

    private void logRequestDetails(final HttpServletRequest request) {
        logger.trace("logRequestDetails() method called.");

        logger.debug(format("*** INCOMING REQUEST: Method=%s, Path=%s, ContentType=%s, Accept=%s",
                request.getMethod(),
                request.getRequestURI(),
                request.getContentType(),
                request.getHeader("Accept")));
    }
}
