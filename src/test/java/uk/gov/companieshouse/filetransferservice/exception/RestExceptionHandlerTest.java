package uk.gov.companieshouse.filetransferservice.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import uk.gov.companieshouse.api.error.ApiError;
import uk.gov.companieshouse.api.error.ApiErrorResponse;
import uk.gov.companieshouse.api.filetransfer.AvStatus;

@ExtendWith(MockitoExtension.class)
class RestExceptionHandlerTest {

    @InjectMocks
    private RestExceptionHandler underTest;

    @Test
    void testHandleIOException() {
        ResponseEntity<ApiErrorResponse> response = underTest.handleIOException(new IOException());

        ApiError apiError = new ApiError("Unable to upload file",
                "getBytes",
                "method",
                "upload"
        );

        assertThat(response.getStatusCode(), is(HttpStatusCode.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody().getErrors(), is(List.of(apiError)));
    }

    @Test
    void testHandleInvalidMimeTypeException() {
        ResponseEntity<ApiErrorResponse> response = underTest.handleInvalidMimeType(
                new InvalidMimeTypeException("application/octet-stream"));

        ApiError apiError = new ApiError("Invalid MIME type",
                "file",
                "body_parameter",
                "validation"
        );

        assertThat(response.getStatusCode(), is(HttpStatusCode.valueOf(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody().getErrors(), is(List.of(apiError)));
    }

    @Test
    void testHandleFileNotCleanException() {
        ResponseEntity<ApiErrorResponse> response = underTest.handleFileNotCleanException(
                new FileNotCleanException(AvStatus.CLEAN, "fileId"));

        ApiError apiError = new ApiError("File retrieval denied due to unclean antivirus status",
                "fileId",
                "fileId",
                "retrieval"
        );

        assertThat(response.getStatusCode(), is(HttpStatusCode.valueOf(HttpStatus.FORBIDDEN.value())));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody().getErrors(), is(List.of(apiError)));
    }

    @Test
    void testHandleFileNotFoundException() {
        ResponseEntity<ApiErrorResponse> response = underTest.handleFileNotFoundException(
                new FileNotFoundException("fileId"));

        ApiError apiError = new ApiError("Unable to find file with id [fileId]",
                "fileId",
                "jsonPath",
                "retrieval"
        );

        assertThat(response.getStatusCode(), is(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value())));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody().getErrors(), is(List.of(apiError)));
    }

    @Test
    void testHandleMaxUploadSizeExceededException() {
        ResponseEntity<?> response = underTest.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(1000L));

        assertThat(response.getStatusCode(), is(HttpStatusCode.valueOf(HttpStatus.PAYLOAD_TOO_LARGE.value())));
        assertThat(response.getBody(), nullValue());
    }
}
