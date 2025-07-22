package uk.gov.companieshouse.filetransferservice.config;

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
    @DisplayName("Test HttpServletRequest POST with a valid content-type header")
    void testDoFilterWithPostAndContentType() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setMethod("POST");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        underTest.doFilter(request, response, chain);

        verify(logger, times(0)).debug(anyString());
    }

    @Test
    @DisplayName("Test HttpServletRequest POST without a valid content-type header")
    void testDoFilterWithPostWithoutContentType() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(null);
        request.setMethod("POST");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        underTest.doFilter(request, response, chain);

        verify(logger, times(1)).debug("Content-Type not detected within POST request, to "
                + "preserve legacy client functionality we are wrapping request to set default Content-Type to application/json");
    }

    @Test
    @DisplayName("Test HttpServletRequest GET with a valid content-type header")
    void testDoFilterWithGetAndContentType() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setMethod("GET");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        underTest.doFilter(request, response, chain);

        verify(logger, times(0)).debug(anyString());
    }


    @Test
    @DisplayName("Test HttpServletRequest GET without a valid content-type header")
    void testDoFilterWithGetWithoutContentType() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(null);
        request.setMethod("GET");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        underTest.doFilter(request, response, chain);

        verify(logger, times(0)).debug(anyString());
    }

}
