package uk.gov.companieshouse.filetransferservice.config;

import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
public class DefaultContentTypeFilterTest {

    @Mock
    private Logger logger;

    private DefaultContentTypeFilter underTest;

    @BeforeEach
    void setUp() {
        underTest = new DefaultContentTypeFilter(logger);
    }

    @Test
    @DisplayName("Test HttpServletRequest POST with a valid content-type and valid accept header")
    void testFilterWithPostAndContentTypeAndAcceptHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setMethod("POST");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/file-transfer-service/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        underTest.doFilter(request, response, chain);

        verify(logger, times(2)).trace(anyString());
        verify(logger, times(1)).debug(format("*** INCOMING REQUEST: Method=%s, Path=%s, ContentType=%s, Accept=%s",
                request.getMethod(), request.getRequestURI(), request.getContentType(), request.getHeader("Accept")));

        verify(logger, times(0)).info(anyString());
    }

    @Test
    @DisplayName("Test HttpServletRequest POST with invalid content-type and valid header")
    void testFilterWithPostInvalidContentTypeAndValidAcceptHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(null);
        request.setMethod("POST");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/file-transfer-service/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        underTest.doFilter(request, response, chain);

        verify(logger, times(2)).trace(anyString());
        verify(logger, times(1)).debug(format("*** INCOMING REQUEST: Method=%s, Path=%s, ContentType=%s, Accept=%s",
                request.getMethod(), request.getRequestURI(), request.getContentType(), request.getHeader("Accept")));

        verify(logger, times(1)).info(format("WARNING: Content-Type NOT supplied for %s request: %s",
                request.getMethod(), request.getRequestURI()));
    }

    @Test
    @DisplayName("Test HttpServletRequest POST with a valid content-type and invalid accept header")
    void testFilterWithPostValidContentTypeAndInvalidAcceptHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setMethod("POST");
        request.addHeader("Accept", "");
        request.setRequestURI("/file-transfer-service/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        underTest.doFilter(request, response, chain);

        verify(logger, times(2)).trace(anyString());
        verify(logger, times(1)).debug(format("*** INCOMING REQUEST: Method=%s, Path=%s, ContentType=%s, Accept=%s",
                request.getMethod(), request.getRequestURI(), request.getContentType(), request.getHeader("Accept")));

        verify(logger, times(1)).info(format("WARNING: Accept header NOT supplied for %s request: %s",
                request.getMethod(), request.getRequestURI()));
    }

    @Test
    @DisplayName("Test HttpServletRequest POST with an invalid content-type and invalid header")
    void testFilterWithPostInvalidContentTypeAndInvalidAcceptHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(null);
        request.setMethod("POST");
        request.addHeader("Accept", "");
        request.setRequestURI("/file-transfer-service/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        underTest.doFilter(request, response, chain);

        verify(logger, times(2)).trace(anyString());
        verify(logger, times(1)).debug(format("*** INCOMING REQUEST: Method=%s, Path=%s, ContentType=%s, Accept=%s",
                request.getMethod(), request.getRequestURI(), request.getContentType(), request.getHeader("Accept")));

        verify(logger, times(2)).info(anyString());
    }


    @Test
    @DisplayName("Test HttpServletRequest GET with a valid content-type and valid accept header")
    void testFilterWithGetAndContentTypeAndAcceptHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setMethod("GET");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/file-transfer-service/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        underTest.doFilter(request, response, chain);

        verify(logger, times(2)).trace(anyString());
        verify(logger, times(1)).debug(format("*** INCOMING REQUEST: Method=%s, Path=%s, ContentType=%s, Accept=%s",
                request.getMethod(), request.getRequestURI(), request.getContentType(), request.getHeader("Accept")));

        verify(logger, times(0)).info(anyString());
    }

    @Test
    @DisplayName("Test HttpServletRequest GET with invalid content-type and valid header")
    void testFilterWithGetInvalidContentTypeAndValidAcceptHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(null);
        request.setMethod("GET");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/file-transfer-service/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        underTest.doFilter(request, response, chain);

        verify(logger, times(2)).trace(anyString());
        verify(logger, times(1)).debug(format("*** INCOMING REQUEST: Method=%s, Path=%s, ContentType=%s, Accept=%s",
                request.getMethod(), request.getRequestURI(), request.getContentType(), request.getHeader("Accept")));

        verify(logger, times(0)).info(anyString());
    }

    @Test
    @DisplayName("Test HttpServletRequest GET with a valid content-type and invalid accept header")
    void testFilterWithGetValidContentTypeAndInvalidAcceptHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setMethod("GET");
        request.addHeader("Accept", "");
        request.setRequestURI("/file-transfer-service/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        underTest.doFilter(request, response, chain);

        verify(logger, times(2)).trace(anyString());
        verify(logger, times(1)).debug(format("*** INCOMING REQUEST: Method=%s, Path=%s, ContentType=%s, Accept=%s",
                request.getMethod(), request.getRequestURI(), request.getContentType(), request.getHeader("Accept")));

        verify(logger, times(1)).info(format("WARNING: Accept header NOT supplied for %s request: %s",
                request.getMethod(), request.getRequestURI()));
    }

    @Test
    @DisplayName("Test HttpServletRequest GET with an invalid content-type and invalid header")
    void testFilterWithGetInvalidContentTypeAndInvalidAcceptHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(null);
        request.setMethod("GET");
        request.addHeader("Accept", "");
        request.setRequestURI("/file-transfer-service/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        underTest.doFilter(request, response, chain);

        verify(logger, times(2)).trace(anyString());
        verify(logger, times(1)).debug(format("*** INCOMING REQUEST: Method=%s, Path=%s, ContentType=%s, Accept=%s",
                request.getMethod(), request.getRequestURI(), request.getContentType(), request.getHeader("Accept")));

        verify(logger, times(1)).info(format("WARNING: Accept header NOT supplied for %s request: %s",
                request.getMethod(), request.getRequestURI()));
    }

}
