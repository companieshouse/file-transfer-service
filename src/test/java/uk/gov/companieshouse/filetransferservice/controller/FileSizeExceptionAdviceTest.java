package uk.gov.companieshouse.filetransferservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import uk.gov.companieshouse.logging.Logger;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

public class FileSizeExceptionAdviceTest {

    @Mock
    private Logger mockLogger;

    private FileSizeExceptionAdvice advice;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        advice = new FileSizeExceptionAdvice(mockLogger);
    }

    @Test
    void testHandleFileTooLarge() {
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(1024L);
        ResponseEntity<?> response = advice.handleFileTooLarge(exception);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());

        verify(mockLogger).error(
                "Uploaded file was too large",
                exception,
                new HashMap<String, Object>() {{
                    put("maxFileSize", 1024L);
                }}
        );
    }
}
