package uk.gov.companieshouse.filetransferservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.filetransferservice.service.file.transfer.FileStorageStrategy;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
public class FileTransferControllerTest {

    @Mock
    private FileStorageStrategy fileStorageStrategy;

    private FileTransferController fileTransferController;

    @BeforeEach
    public void setup() {
        fileTransferController = new FileTransferController(fileStorageStrategy);
    }

    @Test
    public void testUploadFileWithAllowedMimeType() {
        MultipartFile mockFile = new MockMultipartFile("test.pdf",
                "test.pdf",
                "application/pdf",
                "test".getBytes());
        FileApi mockFileApi = new FileApi("test.pdf",
                "test".getBytes(),
                "application/pdf",
                4,
                "pdf");
        when(fileStorageStrategy.save(mockFileApi)).thenReturn("123");

        ResponseEntity<String> response = fileTransferController.upload(mockFile);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("123", response.getBody());
        verify(fileStorageStrategy, times(1)).save(mockFileApi);
    }

    @Test
    public void testUploadFileWithUnsupportedMimeType() {
        MultipartFile mockFile = new MockMultipartFile("test.txt", "test".getBytes());

        ResponseEntity<String> response = fileTransferController.upload(mockFile);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertEquals("Unsupported file type", response.getBody());
        verify(fileStorageStrategy, times(0)).save(any());
    }

    @Test
    public void testUploadFileWithIOException() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getBytes()).thenThrow(new IOException());

        ResponseEntity<String> response = fileTransferController.upload(mockFile);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unable to upload file", response.getBody());
        verify(fileStorageStrategy, times(0)).save(any());
    }

}
