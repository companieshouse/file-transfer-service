package uk.gov.companieshouse.filetransferservice.controller;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.filetransfer.AvStatus;
import uk.gov.companieshouse.api.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.api.filetransfer.IdApi;
import uk.gov.companieshouse.filetransferservice.converter.MultipartFileToFileUploadApiConverter;
import uk.gov.companieshouse.filetransferservice.exception.FileNotCleanException;
import uk.gov.companieshouse.filetransferservice.exception.FileNotFoundException;
import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.filetransferservice.model.FileDownloadApi;
import uk.gov.companieshouse.filetransferservice.model.FileUploadApi;
import uk.gov.companieshouse.filetransferservice.model.legacy.FileApi;
import uk.gov.companieshouse.filetransferservice.service.storage.FileStorageStrategy;
import uk.gov.companieshouse.filetransferservice.validation.FileUploadValidator;
import uk.gov.companieshouse.filetransferservice.validation.MimeTypeValidator;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class FileTransferControllerTest {

    @Mock
    private FileStorageStrategy fileStorageStrategy;

    @Spy
    private MultipartFileToFileUploadApiConverter converter;

    @Mock
    private MimeTypeValidator mimeTypeValidator;

    @Mock
    private FileUploadValidator fileUploadValidator;

    @Mock
    private Logger logger;

    private FileTransferController fileTransferController;

    @BeforeEach
    void beforeEach() {
        fileTransferController = new FileTransferController(
                fileStorageStrategy, converter, mimeTypeValidator, fileUploadValidator, logger, false);
    }

    @Test
    @DisplayName("Test deprecated uploading a FileAPI model with allowed MIME type")
    @Deprecated
    void testDeprecatedUploadFileWithAllowedMimeType() throws IOException, InvalidMimeTypeException {
        FileApi fileApi = new FileApi();
        fileApi.setFileName("test.txt");
        fileApi.setMimeType("text/plain");
        fileApi.setBody("test content".getBytes());
        fileApi.setSize(12);
        fileApi.setExtension("txt");

        when(fileStorageStrategy.save(any(FileUploadApi.class))).thenReturn("123");

        ResponseEntity<?> response = fileTransferController.uploadJson(fileApi);

        verify(fileStorageStrategy, times(1)).save(any(FileUploadApi.class));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new IdApi("123"), response.getBody());
    }

    @Test
    @DisplayName("Test uploading a file with allowed MIME type")
    void testUploadFileWithAllowedMimeType() throws IOException, InvalidMimeTypeException {
        MultipartFile mockFile = new MockMultipartFile("test.pdf",
                "test.pdf",
                "application/pdf",
                "test".getBytes());

        when(fileStorageStrategy.save(any(FileUploadApi.class))).thenReturn("123");

        ResponseEntity<?> response = fileTransferController.upload(mockFile);

        verify(fileStorageStrategy, times(1)).save(any(FileUploadApi.class));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new IdApi("123"), response.getBody());
    }

    @Test
    @DisplayName("Test uploading a file with unsupported MIME type")
    void testUploadFileWithUnsupportedMimeType() throws InvalidMimeTypeException {
        doThrow(new InvalidMimeTypeException("invalid")).when(mimeTypeValidator).validate(anyString());

        MultipartFile mockFile = new MockMultipartFile("file",
                "test.txt",
                "invalid",
                "test".getBytes());

        assertThrows(InvalidMimeTypeException.class, () -> fileTransferController.upload(mockFile));
    }

    @Test
    @DisplayName("Test uploading a file with IOException")
    void testUploadEmptyFileWithIOException() throws InvalidMimeTypeException, IOException {
        MultipartFile mockFile = new MockMultipartFile("file.pdf",
                "test.txt",
                "application/pdf",
                "".getBytes());

        doThrow(new IOException("Empty file!")).when(fileUploadValidator).validate(mockFile);

        IOException expectedException = assertThrows(IOException.class, () -> fileTransferController.upload(mockFile));

        verify(fileUploadValidator, times(1)).validate(mockFile);

        assertEquals("Empty file!", expectedException.getMessage());
    }

    @Test
    @DisplayName("Test successful file deletion")
    void testDeleteFileSuccess() throws FileNotFoundException {
        String fileId = "123";

        FileDetailsApi expectedFileDetails = new FileDetailsApi(fileId, null, null, null, 0L, null, null, null);
        when(fileStorageStrategy.getFileDetails(fileId)).thenReturn(Optional.of(expectedFileDetails));

        doNothing().when(fileStorageStrategy).delete(fileId);

        ResponseEntity<Void> response = fileTransferController.delete(fileId);

        verify(fileStorageStrategy, times(1)).getFileDetails(fileId);
        verify(fileStorageStrategy, times(1)).delete(fileId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    @DisplayName("Test successful retrieval of file details")
    void testGetFileDetailsSuccess() throws FileNotFoundException, FileNotCleanException {
        String fileId = "123";

        FileDetailsApi expectedFileDetails = new FileDetailsApi(fileId, null, AvStatus.CLEAN, null, 0L, null, null, null);
        when(fileStorageStrategy.getFileDetails(fileId)).thenReturn(Optional.of(expectedFileDetails));

        ResponseEntity<FileDetailsApi> response = fileTransferController.get(fileId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedFileDetails, response.getBody());
        verify(fileStorageStrategy, times(1)).getFileDetails(fileId);
    }

    @Test
    @DisplayName("Test retrieval of non-existent file details")
    void testGetFileDetailsNotFound() {
        String fileId = "123";
        when(fileStorageStrategy.getFileDetails(fileId)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> fileTransferController.get(fileId));
    }

    @Test
    @DisplayName("Test deprecated successful file download")
    void testDeprecatedDownloadSuccess() throws FileNotFoundException, FileNotCleanException, IOException {
        String fileId = "123";
        byte[] content = "test content".getBytes();
        String mimeType = "text/plain";
        String fileName = "file.txt";

        FileDetailsApi fileDetails = new FileDetailsApi()
                .id(fileId)
                .name(fileName)
                .size((long) content.length)
                .contentType(mimeType)
                .avStatus(AvStatus.CLEAN);

        when(fileStorageStrategy.getFileDetails(fileId)).thenReturn(Optional.of(fileDetails));

        var file = new FileDownloadApi(fileName, new ByteArrayInputStream(content), mimeType, content.length, "txt");

        when(fileStorageStrategy.load(fileDetails)).thenReturn(Optional.of(file));

        ResponseEntity<uk.gov.companieshouse.filetransferservice.model.legacy.FileApi> response = fileTransferController.downloadAsJson(fileId);

        FileApi fileApi = requireNonNull(response.getBody());
        byte[] responseContent = fileApi.getBody();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(content.length, responseContent.length);
        assertArrayEquals(content, responseContent);
        assertEquals(mimeType, fileApi.getMimeType());
        assertEquals(0, response.getHeaders().size());
    }

    @Test
    @DisplayName("Test deprecated successful binary file download")
    void testDeprecatedDownloadBinarySuccess() throws FileNotFoundException, FileNotCleanException, IOException {
        String fileId = "123";
        byte[] content = "test content".getBytes();
        MediaType mimeType = MediaType.TEXT_PLAIN;
        String fileName = "file.txt";

        FileDetailsApi fileDetails = new FileDetailsApi()
                .id(fileId)
                .name(fileName)
                .size((long) content.length)
                .contentType(mimeType.toString())
                .avStatus(AvStatus.CLEAN);

        when(fileStorageStrategy.getFileDetails(fileId)).thenReturn(Optional.of(fileDetails));

        var file = new FileDownloadApi(fileName, new ByteArrayInputStream(content), mimeType.toString(), content.length, "txt");

        when(fileStorageStrategy.load(fileDetails)).thenReturn(Optional.of(file));

        ResponseEntity<byte[]> response = fileTransferController.downloadAsBinary(fileId, true);
        byte[] responseContent = requireNonNull(response.getBody());
        HttpHeaders responseHeaders = response.getHeaders();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(content.length, responseContent.length);
        assertArrayEquals(content, responseContent);

        assertEquals(3, response.getHeaders().size());
        assertEquals(mimeType, responseHeaders.getContentType());
        assertEquals("attachment; filename=\"file.txt\"", responseHeaders.getContentDisposition().toString());
        assertEquals(12L, responseHeaders.getContentLength());
    }

    @Test
    @DisplayName("Test successful file download")
    void testDownloadSuccess() throws FileNotFoundException, FileNotCleanException, IOException {
        String fileId = "123";
        byte[] content = {0x01, 0x02, 0x03};
        String mimeType = "text/plain";
        String fileName = "file.txt";

        FileDetailsApi fileDetails = new FileDetailsApi()
                .id(fileId)
                .contentType(mimeType)
                .avStatus(AvStatus.CLEAN);

        when(fileStorageStrategy.getFileDetails(fileId)).thenReturn(Optional.of(fileDetails));

        var file = new FileDownloadApi(fileName, new ByteArrayInputStream(content), mimeType, content.length, "txt");

        when(fileStorageStrategy.load(fileDetails)).thenReturn(Optional.of(file));

        ResponseEntity<Resource> response = fileTransferController.download(fileId);

        requireNonNull(response.getBody());
        byte[] responseContent = response.getBody().getContentAsByteArray();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(content.length, responseContent.length);
        assertArrayEquals(content, responseContent);
        assertEquals(mimeType, requireNonNull(response.getHeaders().getContentType()).toString());
        assertEquals("attachment; filename=\"file.txt\"", response.getHeaders().getContentDisposition().toString());
        assertEquals(2, response.getHeaders().size());
    }

    @Test
    @DisplayName("Test unsuccessful file download due to missing file")
    void testDownloadFileNotFound() {
        String fileId = "123";

        when(fileStorageStrategy.getFileDetails(fileId)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> fileTransferController.download(fileId));
    }

    @Test
    @DisplayName("Test unsuccessful file download due to non-clean file status")
    void testDownloadFileNotClean() {
        String fileId = "123";

        var fileDetailsApi = new FileDetailsApi();
        ReflectionTestUtils.setField(fileDetailsApi, "avStatus", AvStatus.INFECTED);

        when(fileStorageStrategy.getFileDetails(fileId)).thenReturn(Optional.of(fileDetailsApi));

        assertThrows(FileNotCleanException.class, () -> fileTransferController.download(fileId));
    }

}
