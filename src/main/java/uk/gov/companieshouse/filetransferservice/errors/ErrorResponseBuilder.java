package uk.gov.companieshouse.filetransferservice.errors;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.api.error.ApiError;
import uk.gov.companieshouse.api.error.ApiErrorResponse;

public class ErrorResponseBuilder {
    private final List<ApiError> errors;
    private HttpStatus status;

    public ErrorResponseBuilder() {
        this.errors = new ArrayList<>();
    }

    public static ErrorResponseBuilder status(HttpStatus status) {
        var builder = new ErrorResponseBuilder();

        if (!status.isError()) {
            throw new IllegalArgumentException(String.format(
                    "Status to ApiErrorResponse must be an error status. %d (%s) is not.",
                    status.value(),
                    status));
        }

        builder.status = status;
        return builder;
    }

    public ErrorResponseBuilder withError(ApiError error) {
        this.errors.add(error);
        return this;
    }

    public ErrorResponseBuilder withError(String error,
                                          String location,
                                          String locationType,
                                          String type) {
        return withError(new ApiError(error, location, locationType, type));
    }

    public ResponseEntity<ApiErrorResponse> build() {
        var errResponse = new ApiErrorResponse();
        errResponse.setErrors(errors);
        return ResponseEntity.status(this.status).body(errResponse);
    }
}
