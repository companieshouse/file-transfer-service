package uk.gov.companieshouse.filetransferservice.service.storage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.filetransferservice.service.storage.S3FileStorage.FILENAME_METADATA_KEY;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.Tag;
import uk.gov.companieshouse.api.filetransfer.AvStatus;
import uk.gov.companieshouse.api.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.filetransferservice.model.FileDownloadApi;
import uk.gov.companieshouse.filetransferservice.model.FileUploadApi;
import uk.gov.companieshouse.filetransferservice.service.AmazonFileTransfer;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(MockitoExtension.class)
class S3FileStorageTest {

    private static final String TEST_FILE_NAME = "test.pdf";
    private static final String SOME_CONTENT = "anything";

    @Mock
    private AmazonFileTransfer amazonFileTransfer;

    @Mock
    private ByteArrayInputStream mockInputStream;

    @InjectMocks
    private S3FileStorage underTest;

    @Test
    @DisplayName("Test successful File Save")
    void testSaveFileSuccess() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> metaDataCapture = ArgumentCaptor.forClass(Map.class);
        doNothing().when(amazonFileTransfer).uploadFile(anyString(), metaDataCapture.capture(), any(InputStream.class));

        String actual = underTest.save(createTestFileUploadApi());

        assertNotNull(UUID.fromString(actual));
        verify(amazonFileTransfer).uploadFile(anyString(), anyMap(), any(InputStream.class));
        assertThat(metaDataCapture.getValue(), hasEntry(FILENAME_METADATA_KEY, TEST_FILE_NAME));
    }

    @Test
    @DisplayName("Test SdkClientException thrown on unsuccessful File Save")
    void testSdkClientExceptionThrownFromFileSaveFailure() {
        doThrow(SdkClientException.class).when(amazonFileTransfer).uploadFile(anyString(), anyMap(), any(InputStream.class));

        assertThrows(SdkClientException.class, () -> underTest.save(createTestFileUploadApi()));
    }

    @Test
    @DisplayName("Test successful File Load")
    void testLoadFileSuccess() {
        when(amazonFileTransfer.downloadStream(anyString()))
                .thenReturn(Optional.of(new ByteArrayInputStream("sdf".getBytes())));

        Optional<FileDownloadApi> actual = underTest.load(createTestFileDetailsApi());
        verify(amazonFileTransfer).downloadStream(anyString());

        assertTrue(actual.isPresent());
    }

    @Test
    @DisplayName("Test SdkClientException thrown on unsuccessful File Load")
    void testLoadFileFailureReturnsEmptyObject() {
        when(amazonFileTransfer.downloadStream(anyString())).thenReturn(Optional.empty());

        Optional<FileDownloadApi> actual = underTest.load(createTestFileDetailsApi());

        verify(amazonFileTransfer).downloadStream(anyString());

        assertTrue(actual.isEmpty());
    }

    @Test
    @DisplayName("Test successful Get File Details with AV tags")
    void testGetFileDetailsSuccessWithAvTags() {
        when(amazonFileTransfer.getFileObject(anyString())).thenReturn(createTestS3ObjectTags(4));
        when(amazonFileTransfer.getFileTags(anyString())).thenReturn(createMixedTags());

        Optional<FileDetailsApi> actual = underTest.getFileDetails(TEST_FILE_NAME);

        assertTrue(actual.isPresent());
        assertNotNull(actual.get().getAvTimestamp());
        assertNotNull(actual.get().getAvStatus());
        verify(amazonFileTransfer).getFileObject(anyString());
        verify(amazonFileTransfer).getFileTags(anyString());
    }

    @Test
    @DisplayName("Test successful Get File Details with empty tags")
    void testGetFileDetailsSuccessWithNullTags() {
        when(amazonFileTransfer.getFileObject(anyString())).thenReturn(Optional.of(
                new ResponseInputStream<>(createTestS3ObjectWithNullTags().build(), mockInputStream)
        ));

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

        assertThrows(SdkClientException.class, () -> underTest.delete(TEST_FILE_NAME));
    }

    private FileUploadApi createTestFileUploadApi() {
        return new FileUploadApi(TEST_FILE_NAME,
                new ByteArrayInputStream("test".getBytes()),
                "application/pdf",
                4,
                "pdf");
    }

    private FileDetailsApi createTestFileDetailsApi() {
        return new FileDetailsApi(
                "id",
                "avTimestamp",
                AvStatus.CLEAN,
                "contentType",
                123L,
                "name",
                "createdOn",
                null);
    }

    private GetObjectResponse.Builder createTestS3ObjectWithNullTags() {
        return GetObjectResponse.builder()
                .contentType(SOME_CONTENT)
                .contentLength((long) SOME_CONTENT.length())
                .lastModified(Instant.now().minusSeconds(10));
    }

    private Optional<ResponseInputStream<GetObjectResponse>>  createTestS3ObjectTags(int tagCount) {
        GetObjectResponse.Builder  objectResponseBuilder = createTestS3ObjectWithNullTags();
        objectResponseBuilder.tagCount(tagCount);

        return Optional.of(new ResponseInputStream<>(objectResponseBuilder.build(), mockInputStream));
    }

    private Optional<List<Tag>> createAvTags() {
        return Optional.of(List.of(
            Tag.builder()
                    .key("av-timestamp")
                    .value(new Date().toString())
                    .build(),
            Tag.builder()
                    .key("av-status")
                    .value("clean")
                    .build()));
    }

    private Optional<List<Tag>> createNonAvTags() {
        return Optional.of(List.of(
                Tag.builder()
                        .key("anything")
                        .value("anything")
                        .build(),
                Tag.builder()
                        .key("anything2")
                        .value("anything2")
                        .build()));
    }

    private Optional<List<Tag>> createMixedTags() {
        return Optional.of(Stream.concat(
                createAvTags().get().stream(),
                createNonAvTags().get().stream())
                .toList());
    }
}