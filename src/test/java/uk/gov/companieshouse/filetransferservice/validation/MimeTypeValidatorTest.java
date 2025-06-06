package uk.gov.companieshouse.filetransferservice.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.filetransferservice.model.FileUploadApi;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class MimeTypeValidatorTest {

    private static final List<String> ALL_MIME_TYPES = Arrays.asList(
            "text/plain",
            "text/html",
            "text/xml",
            "text/css",
            "text/csv",
            "text/calendar",
            "text/markdown",
            "image/gif",
            "image/jpeg",
            "image/png",
            "image/bmp",
            "image/svg+xml",
            "image/tiff",
            "image/webp",
            "audio/midi",
            "audio/mpeg",
            "audio/webm",
            "audio/ogg",
            "audio/wav",
            "video/mp4",
            "video/mpeg",
            "video/ogg",
            "video/quicktime",
            "video/webm",
            "application/javascript",
            "application/json",
            "application/ld+json",
            "application/msword",
            "application/octet-stream",
            "application/pdf",
            "application/vnd.ms-powerpoint",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/x-rar-compressed",
            "application/x-tar",
            "application/x-bzip2",
            "application/x-7z-compressed",
            "application/zip",
            "application/x-zip-compressed",
            "application/xml",
            "application/atom+xml",
            "application/rss+xml",
            "multipart/x-zip",
            "application/zip-compressed"
    );


    @InjectMocks
    private MimeTypeValidator validator;

    @Mock
    private Logger logger;

    public static Stream<Arguments> getAllowedMimeTypes() {
        return MimeTypeValidator.ALLOWED_MIME_TYPES.stream().map(Arguments::of);
    }

    public static Stream<Arguments> getDisallowedMimeTypes() {
        return ALL_MIME_TYPES.stream()
                .filter(t -> !MimeTypeValidator.ALLOWED_MIME_TYPES.contains(t))
                .map(Arguments::of);
    }

    private static FileUploadApi createMockMultipartFile(final String mimeType) {
        byte[] bytes = "file content".getBytes();
        InputStream content = new ByteArrayInputStream(bytes);
        return new FileUploadApi("filename.jpg", content, mimeType, bytes.length, "jpg");
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("getAllowedMimeTypes")
    @DisplayName("Given a MultipartFile with a valid mime type, when validated by the validator, then no exception should be thrown")
    void testAllowedMimeTypePassesValidation(String mimeType) throws InvalidMimeTypeException {
        // Create a mock MultipartFile object with the given mime type
        FileUploadApi file = createMockMultipartFile(mimeType);

        validator.validate(file.getMimeType());
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("getDisallowedMimeTypes")
    @DisplayName("Given a MultipartFile with an in-valid mime type, when validated by the validator, an exception should be thrown")
    void testDisallowedMimeTypeThrowsException(String mimeType) {
        // Create a mock MultipartFile object with the given mime type
        FileUploadApi file = createMockMultipartFile(mimeType);

        // Verify that an exception is thrown when the validator is used to validate the file
        assertThrows(InvalidMimeTypeException.class, () -> validator.validate(file.getMimeType()));
    }
}