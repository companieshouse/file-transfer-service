package uk.gov.companieshouse.filetransferservice.service.file.transfer;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class S3FileStorageTest {

    private static final String TEST_FILE_NAME = "test.pdf";

    @Mock
    private AmazonFileTransfer amazonFileTransfer;

    @InjectMocks
    private S3FileStorage underTest;

    @Test
    @DisplayName("Test successful File Save")
    void testSaveFileSuccess() {
        doNothing().when(amazonFileTransfer).uploadFile(anyString(), any(Map.class), any(InputStream.class));

        String actual = underTest.save(createTestFileApi());

        assertNotNull(UUID.fromString(actual));
        verify(amazonFileTransfer).uploadFile(anyString(), any(Map.class), any(InputStream.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown on unsuccessful File Save")
    void testSdkClientExceptionThrownFromFileSaveFailure() {
        doThrow(SdkClientException.class).when(amazonFileTransfer).uploadFile(anyString(), any(Map.class), any(InputStream.class));

        assertThrows(SdkClientException.class, () -> {
            underTest.save(createTestFileApi());
        });
    }

    @Test
    @DisplayName("Test successful File Load")
    void testLoadFileSuccess() {
        when(amazonFileTransfer.downloadFile(anyString())).thenReturn(Optional.of("sdf".getBytes()));

        Optional<FileApi> actual = underTest.load(TEST_FILE_NAME, createTestFileDetailsApi());

        assertTrue(actual.isPresent());
        verify(amazonFileTransfer).downloadFile(anyString());
    }

    @Test
    @DisplayName("Test SdkClientException thrown on unsuccessful File Load")
    void testLoadFileFailureReturnsEmptyObject() {
        when(amazonFileTransfer.downloadFile(anyString())).thenReturn(Optional.empty());

        Optional<FileApi> actual = underTest.load(TEST_FILE_NAME, createTestFileDetailsApi());

        assertTrue(actual.isEmpty());
        verify(amazonFileTransfer).downloadFile(anyString());
    }

    @Test
    @DisplayName("Test successful Get File Details with AV tags")
    void testGetFileDetailsSuccessWithAvTags() {
        when(amazonFileTransfer.getFileObject(anyString())).thenReturn(createTestS3ObjectTags(4));
        when(amazonFileTransfer.getFileTags(anyString())).thenReturn(createMixedTags());

        Optional<FileDetailsApi> actual = underTest.getFileDetails(TEST_FILE_NAME);

        assertTrue(actual.isPresent());
        assertNotNull(actual.get().getAvTimestamp());
        assertNotNull(actual.get().getAvStatusApi());
        verify(amazonFileTransfer).getFileObject(anyString());
        verify(amazonFileTransfer).getFileTags(anyString());
    }

    @Test
    @DisplayName("Test successful Get File Details with empty tags")
    void testGetFileDetailsSuccessWithNullTags() {
        when(amazonFileTransfer.getFileObject(anyString())).thenReturn(createTestS3ObjectWithNullTags());

        Optional<FileDetailsApi> actual = underTest.getFileDetails(TEST_FILE_NAME);

        assertTrue(actual.isPresent());
        verify(amazonFileTransfer).getFileObject(anyString());
        verify(amazonFileTransfer, times(0)).getFileTags(anyString());
    }

    @Test
    @DisplayName("Test successful Get File Details with zero tags")
    void testGetFileDetailsSuccessWithZeroTags() {
        when(amazonFileTransfer.getFileObject(anyString())).thenReturn(createTestS3ObjectTags(0));

        Optional<FileDetailsApi> actual = underTest.getFileDetails(TEST_FILE_NAME);

        assertTrue(actual.isPresent());
        verify(amazonFileTransfer).getFileObject(anyString());

        verify(amazonFileTransfer, times(0)).getFileTags(anyString());
    }

    @Test
    @DisplayName("Test failure Get File Details with no AV tags")
    void testGetFileDetailsSuccessWithNoAVTags() {
        when(amazonFileTransfer.getFileObject(anyString())).thenReturn(createTestS3ObjectTags(2));
        when(amazonFileTransfer.getFileTags(anyString())).thenReturn(createNonAvTags());

        Optional<FileDetailsApi> actual = underTest.getFileDetails(TEST_FILE_NAME);

        assertTrue(actual.isEmpty());
        verify(amazonFileTransfer).getFileObject(anyString());
        verify(amazonFileTransfer).getFileTags(anyString());
    }

    @Test
    @DisplayName("Test failure Get File Details when s3 object not found")
    void testGetFileDetailsFailsWhenS3ObjectNotFound() {
        when(amazonFileTransfer.getFileObject(anyString())).thenReturn(Optional.empty());

        Optional<FileDetailsApi> actual = underTest.getFileDetails(TEST_FILE_NAME);

        assertTrue(actual.isEmpty());
        verify(amazonFileTransfer).getFileObject(anyString());
        verify(amazonFileTransfer, times(0)).getFileTags(anyString());
    }

    @Test
    @DisplayName("Test failure no Get File Details when Get File Tags fails")
    void testGetFileDetailsFailsOnRetrievingTags() {
        when(amazonFileTransfer.getFileObject(anyString())).thenReturn(createTestS3ObjectTags(1));
        when(amazonFileTransfer.getFileTags(anyString())).thenReturn(Optional.empty());

        Optional<FileDetailsApi> actual = underTest.getFileDetails(TEST_FILE_NAME);

        assertTrue(actual.isEmpty());
        verify(amazonFileTransfer).getFileObject(anyString());
        verify(amazonFileTransfer).getFileTags(anyString());
    }

    @Test
    @DisplayName("Test successful File Delete")
    void testDeleteFileSuccess() {
        doNothing().when(amazonFileTransfer).deleteFile(anyString());

        underTest.delete(TEST_FILE_NAME);

        verify(amazonFileTransfer).deleteFile(anyString());
    }

    @Test
    @DisplayName("Test SdkClientException thrown on unsuccessful File Delete")
    void testSdkClientExceptionThrownFromFileDeleteFailure() {
        doThrow(SdkClientException.class).when(amazonFileTransfer).deleteFile(anyString());

        assertThrows(SdkClientException.class, () -> {
            underTest.delete(TEST_FILE_NAME);
        });
    }

    private FileApi createTestFileApi() {
        return new FileApi(TEST_FILE_NAME,
                "test".getBytes(),
                "application/pdf",
                4,
                "pdf");
    }

    private FileDetailsApi createTestFileDetailsApi() {
        return new FileDetailsApi(
                "id",
                "avTimestamp",
                AvStatusApi.CLEAN,
                "contentType",
                123L,
                "name",
                "createdOn",
                null);
    }

    private Optional<S3Object> createTestS3ObjectWithNullTags() {
        S3Object s3Object = new S3Object();
        ObjectMetadata objectMetadata = new ObjectMetadata();

        objectMetadata.setContentType("anything");
        objectMetadata.setContentLength(objectMetadata.getContentType().length());
        objectMetadata.setLastModified(new Date());

        s3Object.setObjectMetadata(objectMetadata);

        return Optional.of(s3Object);
    }

    private Optional<S3Object> createTestS3ObjectTags(int tagCount) {
        Optional<S3Object> s3Object = createTestS3ObjectWithNullTags();

        s3Object.get().setTaggingCount(tagCount);

        return s3Object;
    }

    private Optional<List<Tag>> createAvTags() {
        return Optional.of(new ArrayList<>() {{
            add(new Tag("av-timestamp", new Date().toString()));
            add(new Tag("av-status", "clean"));
        }});
    }

    private Optional<List<Tag>> createNonAvTags() {
        return Optional.of(new ArrayList<>() {{
            add(new Tag("anything", "anything"));
            add(new Tag("anything2", "anything2"));
        }});
    }

    private Optional<List<Tag>> createMixedTags() {
        return Optional.of(Stream.concat(createAvTags().get().stream(), createNonAvTags().get().stream())
                .collect(Collectors.toList()));
    }
}