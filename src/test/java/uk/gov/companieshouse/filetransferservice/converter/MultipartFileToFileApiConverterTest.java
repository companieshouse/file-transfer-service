package uk.gov.companieshouse.filetransferservice.converter;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.filetransferservice.model.FileUploadApi;

@ExtendWith(MockitoExtension.class)
class MultipartFileToFileApiConverterTest {

    private MultipartFileToFileUploadApiConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new MultipartFileToFileUploadApiConverter();
    }

    @Test
    void testConvertMultipartFileWithContent() throws IOException {
        // Given
        byte[] fileContent = "Hello, World!".getBytes();

        InputStream content = new ByteArrayInputStream(fileContent);
        String filename = "example.txt";
        String contentType = "text/plain";
        MultipartFile multipartFile = new MockMultipartFile(filename, filename, contentType, content);

        // When
        FileUploadApi result = underTest.convert(multipartFile);

        // Then
        assertEquals(filename, result.getFileName());
        assertEquals(contentType, result.getMimeType());
        assertEquals(fileContent.length, result.getBody().readAllBytes().length);
        assertEquals(0, result.getSize());
        assertEquals("txt", result.getExtension());
    }

    @Test
    void testConvertMultipartFileWithNoContent() throws IOException {
        // Given
        byte[] fileContent = {};

        InputStream content = new ByteArrayInputStream(fileContent);
        String filename = "example.txt";
        String contentType = "text/plain";
        MultipartFile multipartFile = new MockMultipartFile(filename, filename, contentType, content);

        // When
        FileUploadApi result = underTest.convert(multipartFile);

        // Then
        assertEquals(filename, result.getFileName());
        assertEquals(contentType, result.getMimeType());
        Assertions.assertNotNull(result.getBody());
        assertEquals(fileContent.length, result.getBody().readAllBytes().length);
        assertEquals(0, result.getSize());
        assertEquals("txt", result.getExtension());
    }

}