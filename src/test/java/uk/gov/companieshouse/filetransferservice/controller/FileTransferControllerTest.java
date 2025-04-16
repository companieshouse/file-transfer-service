package uk.gov.companieshouse.filetransferservice.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import uk.gov.companieshouse.api.error.ApiErrorResponse;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.api.model.filetransfer.IdApi;
import uk.gov.companieshouse.filetransferservice.converter.MultipartFileToFileApiConverter;
import uk.gov.companieshouse.filetransferservice.exception.FileNotCleanException;
import uk.gov.companieshouse.filetransferservice.exception.FileNotFoundException;
import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.filetransferservice.service.storage.FileStorageStrategy;
import uk.gov.companieshouse.filetransferservice.validation.UploadedFileValidator;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
public class FileTransferControllerTest {
    @Mock
    private Logger logger;

    @Mock
    private UploadedFileValidator uploadedFileValidator;

    @Spy
    private MultipartFileToFileApiConverter converter;

    @Mock
    private FileStorageStrategy fileStorageStrategy;

    private FileTransferController fileTransferController;

    @BeforeEach
    void beforeEach() {
        fileTransferController = new FileTransferController(fileStorageStrategy, logger, converter, uploadedFileValidator);
    }

    @Test
    @DisplayName("Test uploading a file with allowed MIME type")
    public void testUploadFileWithAllowedMimeType() throws IOException, InvalidMimeTypeException {
        MultipartFile mockFile = new MockMultipartFile("test.pdf",
                "test.pdf",
                "application/pdf",
                "test".getBytes());

        when(fileStorageStrategy.save(any(FileApi.class))).thenReturn("123");

        ResponseEntity<?> response = fileTransferController.upload(mockFile);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new IdApi("123"), response.getBody());
        verify(fileStorageStrategy, times(1)).save(any(FileApi.class));
    }

    @Test
    @DisplayName("Test uploading a file with unsupported MIME type")
    public void testUploadFileWithUnsupportedMimeType() throws InvalidMimeTypeException {
        doThrow(new InvalidMimeTypeException("invalid")).when(uploadedFileValidator).validate(any(FileApi.class));
        MultipartFile mockFile = new MockMultipartFile("file",
                "test.txt",
                "invalid",
                "test".getBytes());

        assertThrows(InvalidMimeTypeException.class, () -> fileTransferController.upload(mockFile));
    }

    @Test
    @DisplayName("Test uploading a file with IOException")
    public void testUploadFileWithIOException() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getBytes()).thenThrow(new IOException());

        assertThrows(IOException.class, () -> fileTransferController.upload(mockFile));
    }

    @Test
    @DisplayName("Test successful file deletion")
    public void testDeleteFileSuccess() {
        String fileId = "123";

        ResponseEntity<Void> response = fileTransferController.delete(fileId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(fileStorageStrategy, times(1)).delete(fileId);
    }

    @Test
    @DisplayName("Test successful retrieval of file details")
    public void testGetFileDetailsSuccess() throws FileNotFoundException {
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

        assertThrows(FileNotFoundException.class, () -> fileTransferController.getFileDetails(fileId));
    }

    @Test
    @DisplayName("Test successful file download")
    public void testDownloadSuccess() throws FileNotFoundException, FileNotCleanException {
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

        when(fileStorageStrategy.load(fileId, fileDetails))
                .thenReturn(Optional.of(file));

        ResponseEntity<byte[]> response = fileTransferController.downloadBinary(fileId, false);

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

        assertThrows(FileNotFoundException.class, () -> fileTransferController.downloadBinary(fileId, false));
    }

    @Test
    @DisplayName("Test unsuccessful file download due to non-clean file status")
    public void testDownloadFileNotClean() {
        String fileId = "123";

        var fileDetailsApi = new FileDetailsApi();
        ReflectionTestUtils.setField(fileDetailsApi, "avStatus", AvStatusApi.INFECTED);

        when(fileStorageStrategy.getFileDetails(fileId))
                .thenReturn(Optional.of(fileDetailsApi));

        assertThrows(FileNotCleanException.class, () -> fileTransferController.downloadBinary(fileId, false));
    }

    /*
    @Test
    @DisplayName("IOException should result in an internal server error")
    void testIOException() {
        // Given
        IOException e = new IOException();

        // When
        ResponseEntity<ApiErrorResponse> response = fileTransferController.handleIOException(e);

        // Then
        assertThat(response.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    @DisplayName("FileNotFound exception should result in a not found response")
    void testFileNotFoundException() {
        // Given
        String fileId = "fileId";
        FileNotFoundException e = new FileNotFoundException(fileId);

        // When
        ResponseEntity<ApiErrorResponse> response = fileTransferController
                .handleFileNotFoundException(e);

        // Then
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("A FileNotCleanException should result in a forbidden response")
    void handleFileNotCleanException() {
        // Given
        String fileId = "fileId";
        FileNotCleanException e = new FileNotCleanException(AvStatusApi.INFECTED, fileId);

        // When
        ResponseEntity<ApiErrorResponse> response = fileTransferController
                .handleFileNotCleanException(e);

        // Then
        assertThat(response.getStatusCode(), equalTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("InvalidMimeType exception should result an unsupported media type exception")
    void testInvalidMimeTypeExceptionHandler() {
        // Given
        String mimeType = "mimeType";
        InvalidMimeTypeException e = new InvalidMimeTypeException(mimeType);

        // When
        ResponseEntity<ApiErrorResponse> response = fileTransferController.handleInvalidMimeType(e);

        // Then
        assertThat(response.getStatusCode(), equalTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }
    */
}
