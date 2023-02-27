package uk.gov.companieshouse.filetransferservice.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class MultipartFileToFileApiConverterTest {
    private MultipartFileToFileApiConverter fileApiConverter;

    @BeforeEach
    void setUp() {
        fileApiConverter = new MultipartFileToFileApiConverter();
    }

    @Test
    void testConvertMultipartFile() throws IOException {
        // Given
        byte[] bytes = "Hello, World!".getBytes();
        String filename = "example.txt";
        String contentType = "text/plain";
        MultipartFile multipartFile = new MockMultipartFile(filename, filename, contentType, bytes);

        // When
        FileApi result = fileApiConverter.convert(multipartFile);

        // Then
        assertEquals(filename, result.getFileName());
        assertEquals(bytes, result.getBody());
        assertEquals(contentType, result.getMimeType());
        assertEquals(bytes.length, result.getSize());
        assertEquals("txt", result.getExtension());
    }

}