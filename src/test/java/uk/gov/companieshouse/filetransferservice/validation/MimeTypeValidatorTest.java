package uk.gov.companieshouse.filetransferservice.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.tika.mime.MimeTypeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.filetransferservice.exception.MismatchFileExtensionException;
import uk.gov.companieshouse.filetransferservice.service.identity.FileTypeIdentityService;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class MimeTypeValidatorTest {


        private static final List<List<String>> MIME_TYPE_AND_VALID_FILE_NAME = Arrays.asList(
            List.of("text/plain", "file.txt"),
            List.of("image/png", "file.png"),
            List.of("image/jpeg", "file.jpg"),
            List.of("application/pdf", "file.pdf"),
            List.of("text/csv", "file.csv"),
            List.of("text/html", "file.html"),
            List.of("text/xml", "file.xml"),
            List.of("application/msword", "file.doc"),
            List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "file.docx"),
            List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "file.xlsx"),
            List.of("application/vnd.ms-excel", "file.xls"),
            List.of("image/gif", "file.gif"),
            List.of("application/x-rar-compressed", "file.rar"),
            List.of("application/x-tar", "file.tar"),
            List.of("application/vnd.rar", "file.rar"),
            List.of("application/x-7z-compressed", "file.7z"),
            List.of("application/xhtml+xml", "file.xhtml"),
            List.of("application/zip", "file.zip"),
            List.of("application/xml", "file.xml"),
            List.of("multipart/x-zip", "file.zip"),
            List.of("application/zip-compressed", "file.zip"),
            List.of("application/x-zip-compressed", "file.zip"),
            List.of("application/atom+xml", "file.xml"),
            List.of("application/rss+xml", "file.xml"),
            List.of("text/css", "file.css"),
            List.of("text/calendar", "file.ics"),
            List.of("text/markdown", "file.md"),
            List.of("image/bmp", "file.bmp"),
            List.of("image/svg+xml", "file.svg"),
            List.of("image/tiff", "file.tiff"),
            List.of("image/webp", "file.webp"),
            List.of("audio/midi", "file.midi"),
            List.of("audio/mpeg", "file.mp3"),
            List.of("audio/webm", "file.webm"),
            List.of("audio/ogg", "file.ogg"),
            List.of("audio/wav", "file.wav"),
            List.of("video/mp4", "file.mp4"),
            List.of("video/mpeg", "file.mpeg"),
            List.of("video/ogg", "file.ogg"),
            List.of("video/quicktime", "file.mov"),
            List.of("video/webm", "file.webm"),
            List.of("application/javascript", "file.js"),
            List.of("application/json", "file.json"),
            List.of("application/ld+json", "file.json"),
            List.of("application/octet-stream", "file.bin"),
            List.of("application/vnd.ms-powerpoint", "file.ppt"),
            List.of("application/vnd.openxmlformats-officedocument.presentationml.presentation", "file.pptx"),
            List.of("application/x-bzip2", "file.bz2")
        );


    @InjectMocks
    private MimeTypeValidator validator;

    @Mock
    private Logger logger;

    @Mock
    private FileTypeIdentityService fileTypeIdentityService;

    public static Stream<Arguments> getAllowedMimeTypes() {
        return MimeTypeValidator.ALLOWED_MIME_TYPES.stream().map(Arguments::of);
    }

    public static Stream<Arguments> getAllowedMimeTypesAndValidFileNames() {
        return MIME_TYPE_AND_VALID_FILE_NAME.stream()
                .filter(t -> MimeTypeValidator.ALLOWED_MIME_TYPES.contains(t.get(0)))
                .map(t -> Arguments.of(t.get(0), t.get(1)));
    }

    public static Stream<Arguments> getDisallowedMimeTypesAndValidFileNames() {
        return MIME_TYPE_AND_VALID_FILE_NAME.stream()
                .filter(t -> !MimeTypeValidator.ALLOWED_MIME_TYPES.contains(t.get(0)))
                .map(t -> Arguments.of(t.get(0), t.get(1)));
    }

    public static Stream<Arguments> getMultipartFileInvalidCases() {
        return Stream.of(
                Arguments.of("text/plain", "file.pdf", "application/pdf", MismatchFileExtensionException.class),
                Arguments.of("text/plain", "file.txt", null, InvalidMimeTypeException.class),
                Arguments.of("text/plain", "file.txt", "video/mp4", InvalidMimeTypeException.class)
        );
    }

    private static MockMultipartFile createMockMultipartFile(final String mimeType, final String fileName) {
        return new org.springframework.mock.web.MockMultipartFile("file", fileName, mimeType, "file content".getBytes());
    }

    @ParameterizedTest(name = "{index} {0} {1}")
    @MethodSource("getAllowedMimeTypesAndValidFileNames")
    @DisplayName("Given a MultipartFile with a valid mime type, when validated by the validator, then no exception should be thrown")
    void testAllowedMimeTypePassesValidation(String mimeType, String fileName) throws InvalidMimeTypeException, IOException, MimeTypeException {
        // Create a mock MultipartFile object with the given mime type
        MultipartFile file = createMockMultipartFile(mimeType, fileName);

        when(fileTypeIdentityService.detectMimeType(any(MultipartFile.class))).thenReturn(mimeType);
        when(fileTypeIdentityService.getAllowedExtensions(mimeType)).thenReturn(List.of("." + fileName.substring(fileName.lastIndexOf('.') + 1)));

        validator.validate(file);
    }

    @ParameterizedTest(name = "{index} {0} {1}")
    @MethodSource("getDisallowedMimeTypesAndValidFileNames")
    @DisplayName("Given a MultipartFile with an in-valid mime type, when validated by the validator, an exception should be thrown")
    void testDisallowedMimeTypeThrowsException(String mimeType, String fileName) throws IOException {
        // Create a mock MultipartFile object with the given mime type
        MultipartFile file = createMockMultipartFile(mimeType, fileName);

        when(fileTypeIdentityService.detectMimeType(any(MultipartFile.class))).thenReturn(mimeType);

        // Verify that an exception is thrown when the validator is used to validate the file
        assertThrows(InvalidMimeTypeException.class, () -> validator.validate(file));
    }

    @Test
    @DisplayName("Given a MultipartFile with is null type, then an exception should be thrown")
    void testNullMimeTypeThrowsException() throws IOException {
        // Create a mock MultipartFile object with a null mime type
        MultipartFile file = createMockMultipartFile(null, "file.txt");

        when(fileTypeIdentityService.detectMimeType(any(MultipartFile.class))).thenReturn("text/plain");

        // Verify that an exception is thrown when the validator is used to validate the file
        assertThrows(InvalidMimeTypeException.class, () -> validator.validate(file));
    }

    @Test
    @DisplayName("Given a MultipartFile with an empty mime type, then an exception should be thrown")
    void testEmptyMimeTypeThrowsException() throws IOException {
        // Create a mock MultipartFile object with an empty mime type
        MultipartFile file = createMockMultipartFile("", "file.txt");
        
        when(fileTypeIdentityService.detectMimeType(any(MultipartFile.class))).thenReturn("text/plain");

        // Verify that an exception is thrown when the validator is used to validate the file
        assertThrows(InvalidMimeTypeException.class, () -> validator.validate(file));
    }

    @ParameterizedTest(name = "{index} {0} {1} {2}")
    @MethodSource("getMultipartFileInvalidCases")
    @DisplayName("Given a MultipartFile with declared mime, filename and detected mime, then the expected exception is thrown")
    void testMultipartFileInvalidCases(String declaredMime, String fileName, String detectedMime,
                                       Class<? extends Exception> expectedException) throws IOException {
        MultipartFile file = createMockMultipartFile(declaredMime, fileName);
        when(fileTypeIdentityService.detectMimeType(any(MultipartFile.class))).thenReturn(detectedMime);
        assertThrows(expectedException, () -> validator.validate(file));
    }
}
