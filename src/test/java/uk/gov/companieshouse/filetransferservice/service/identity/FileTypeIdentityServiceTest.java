package uk.gov.companieshouse.filetransferservice.service.identity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.apache.tika.Tika;

import uk.gov.companieshouse.logging.Logger;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileTypeIdentityServiceTest {

    @Mock
    private Tika tika;

    @Mock
    private Logger logger;

    @InjectMocks
    private FileTypeIdentityService service;

    @Test
    @DisplayName("detectMimeType(byte[]) throws when byte content is null")
    void detectMimeTypeBytesNullThrows() {
        byte[] content = null;
        assertThrows(IOException.class, () -> service.detectMimeType(content));
    }


    @Test
    @DisplayName("detectMimeType(byte[]) returns detected mime type")
    void detectMimeTypeBytesReturnsDetected() throws IOException {
        byte[] content = "hello".getBytes();
        when(tika.detect(content)).thenReturn("text/plain");

        String detected = service.detectMimeType(content);
        assertEquals("text/plain", detected);
        verify(tika).detect(content);
    }

    @Test
    @DisplayName("detectMimeType(MultipartFile) throws when multipart is null")
    void detectMimeTypeMultipartNullThrows() {
        assertThrows(IOException.class, () -> service.detectMimeType((MultipartFile) null));
    }



    @Test
    @DisplayName("detectMimeType(MultipartFile) returns detected mime type")
    void detectMimeTypeMultipartReturnsDetected() throws IOException {
        byte[] content = "abc".getBytes();
        String fileName = "abc.txt";
        MockMultipartFile multipart = new MockMultipartFile("file", fileName, "text/plain", content);
        when(tika.detect(any(InputStream.class))).thenReturn("text/plain");

        String detected = service.detectMimeType(multipart);
        assertEquals("text/plain", detected);
        verify(tika).detect(any(InputStream.class));
    }
}
