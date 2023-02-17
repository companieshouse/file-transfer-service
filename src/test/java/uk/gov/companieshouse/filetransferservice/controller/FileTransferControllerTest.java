package uk.gov.companieshouse.filetransferservice.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.filetransferservice.service.file.transfer.FileStorageStrategy;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

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
    @DisplayName("Test uploading a file with allowed MIME type")
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
    @DisplayName("Test uploading a file with unsupported MIME type")
    public void testUploadFileWithUnsupportedMimeType() {
        MultipartFile mockFile = new MockMultipartFile("test.txt", "test".getBytes());

        ResponseEntity<String> response = fileTransferController.upload(mockFile);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertEquals("Unsupported file type", response.getBody());
        verify(fileStorageStrategy, times(0)).save(any());
    }

    @Test
    @DisplayName("Test uploading a file with IOException")
    public void testUploadFileWithIOException() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getBytes()).thenThrow(new IOException());

        ResponseEntity<String> response = fileTransferController.upload(mockFile);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unable to upload file", response.getBody());
        verify(fileStorageStrategy, times(0)).save(any());
    }

    @Test
    @DisplayName("Test successful file deletion")
    public void testDeleteFileSuccess() {
        String fileId = "123";

        ResponseEntity<Void> response = fileTransferController.delete(fileId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(fileStorageStrategy, times(1)).delete(fileId);
    }

    @Test
    @DisplayName("Test successful retrieval of file details")
    public void testGetFileDetailsSuccess() {
        String fileId = "123";
        FileDetailsApi expectedFileDetails = new FileDetailsApi();
        when(fileStorageStrategy.getFileDetails(fileId)).thenReturn(Optional.of(expectedFileDetails));

        ResponseEntity<FileDetailsApi> response = fileTransferController.getFileDetails(fileId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedFileDetails, response.getBody());
        verify(fileStorageStrategy, times(1)).getFileDetails(fileId);
    }

    @Test
    @DisplayName("Test retrieval of non-existent file details")
    public void testGetFileDetailsNotFound() {
        String fileId = "123";
        when(fileStorageStrategy.getFileDetails(fileId)).thenReturn(Optional.empty());

        ResponseEntity<FileDetailsApi> response = fileTransferController.getFileDetails(fileId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(fileStorageStrategy, times(1)).getFileDetails(fileId);
    }

    @Test
    @DisplayName("Test successful file download")
    public void testDownloadSuccess() {
        String fileId = "123";
        byte[] content = {0x01, 0x02, 0x03};
        String mimeType = "text/plain";
        String fileName = "file.txt";

        var fileDetails = new FileDetailsApi();
        ReflectionTestUtils.setField(fileDetails, "contentType", mimeType);
        ReflectionTestUtils.setField(fileDetails, "avStatus", AvStatusApi.CLEAN);

        when(fileStorageStrategy.getFileDetails(fileId))
                .thenReturn(Optional.of(fileDetails));

        var file = new FileApi(fileName, content, mimeType, content.length, "txt");

        when(fileStorageStrategy.load(fileId))
                .thenReturn(Optional.of(file));

        ResponseEntity<byte[]> response = fileTransferController.download(fileId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(content.length, Objects.requireNonNull(response.getBody()).length);
        assertArrayEquals(content, response.getBody());
        assertEquals(mimeType, Objects.requireNonNull(response.getHeaders().getContentType()).toString());
        assertEquals("attachment; filename=\"file.txt\"", response.getHeaders().getContentDisposition().toString());
        assertEquals(content.length, response.getHeaders().getContentLength());
    }

    @Test
    @DisplayName("Test unsuccessful file download due to missing file")
    public void testDownloadFileNotFound() {
        String fileId = "123";

        when(fileStorageStrategy.getFileDetails(fileId))
                .thenReturn(Optional.empty());

        ResponseEntity<byte[]> response = fileTransferController.download(fileId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Test unsuccessful file download due to non-clean file status")
    public void testDownloadFileNotClean() {
        String fileId = "123";

        var fileDetailsApi = new FileDetailsApi();
        ReflectionTestUtils.setField(fileDetailsApi, "avStatus", AvStatusApi.INFECTED);

        when(fileStorageStrategy.getFileDetails(fileId))
                .thenReturn(Optional.of(fileDetailsApi));

        ResponseEntity<byte[]> response = fileTransferController.download(fileId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
