package uk.gov.companieshouse.filetransferservice.config;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
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
    private final List<String> requestsWithBodyMethods;

    public DefaultContentTypeFilter(final Logger logger) {
        this.logger = logger;
        this.requestsWithBodyMethods = List.of("POST", "PUT", "PATCH");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        logger.trace("doFilter(req, res, chain) method called.");

        HttpServletRequest request = (HttpServletRequest) req;
        logRequestDetails(request);

        // Check if we are expecting a payload in the request, for legacy clients backwards compatibility.
        if(requestsWithBodyMethods.contains(request.getMethod())) {
            handleContent(req, res, chain);
        } else {
            handleNoContent(req, res, chain);
        }
    }

    private void logRequestDetails(final HttpServletRequest request) {
        logger.trace("logRequestDetails() method called.");

        logger.debug(format("*** INCOMING REQUEST: Method=%s, Path=%s, ContentType=%s, Accept=%s",
                request.getMethod(),
                request.getRequestURI(),
                request.getContentType(),
                request.getHeader("Accept")));
    }

    private void handleContent(ServletRequest req, ServletResponse res, FilterChain chain)  throws IOException, ServletException {
        logger.trace("handleContent(req, res, chain) method called.");

        HttpServletRequest request = (HttpServletRequest) req;

        // Check that we have a content type set
        if(isNotBlank(req.getContentType())) {
            logger.debug(format("Content-Type detected within %s request, no extra processing required...", request.getMethod()));
            return;
        }

        // No content-type was detected, so we will wrap the request to set a default content-type.
        final HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper((HttpServletRequest) req) {

            @Override
            public String getContentType() {
                return APPLICATION_JSON_VALUE;
            }

            @Override
            public String getHeader(final String name) {
                if ("Content-Type".equalsIgnoreCase(name)) {
                    return APPLICATION_JSON_VALUE;
                }
                return super.getHeader(name);
            }
        };

        chain.doFilter(wrapper, res);
    }

    private void handleNoContent(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        logger.trace("handleNoContent(req, res, chain) method called.");

        HttpServletRequest request = (HttpServletRequest) req;

        // If the accept header is supplied, we do not need to wrap the request with our own defaults.
        if(isNotBlank(request.getHeader("Accept"))) {
            logger.debug(format("Accept header was detected within %s request, no extra processing required...", request.getMethod()));
            return;
        }

        // No Accept header was detected, so we will wrap the request to set a default Accept header.
        final HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public String getHeader(final String name) {
                if ("Accept".equalsIgnoreCase(name)) {
                    return MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }
                return super.getHeader(name);
            }

        };

        logger.debug(format("Accept header was not detected within GET request, to preserve legacy client functionality we "
                + "are wrapping request to set default Accept header to: %s", wrapper.getHeader("Accept")));

        chain.doFilter(wrapper, res);
    }
}
