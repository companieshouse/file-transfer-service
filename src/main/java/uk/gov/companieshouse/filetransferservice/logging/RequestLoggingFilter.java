package uk.gov.companieshouse.filetransferservice.logging;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.util.RequestLogger;

@Component
@Order(value = HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter implements RequestLogger {

    private final Logger LOGGER;

    public RequestLoggingFilter(Logger logger) {
    LOGGER = logger;
    }
    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain) throws ServletException, IOException {
            logStartRequestProcessing(request, LOGGER);

        try {
            filterChain.doFilter(request, response);
        } finally {
            logEndRequestProcessing(request, response, LOGGER);
        }
    }
}