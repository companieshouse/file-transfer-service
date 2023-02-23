package uk.gov.companieshouse.filetransferservice.service.file.transfer;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
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
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class S3FileStorageTest {

    @Mock
    private AmazonFileTransfer amazonFileTransfer;

    @InjectMocks
    private S3FileStorage underTest;

    @Test
    @DisplayName("Test file Id returned on successful file save")
    void testSaveFileSuccess() {
        doNothing().when(amazonFileTransfer).uploadFile(any(String.class), any(InputStream.class));

        String actual = underTest.save(createTestFileApi());

        assertEquals("test.pdf", actual);
        verify(amazonFileTransfer).uploadFile(any(String.class), any(InputStream.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when unsuccessful file save")
    void testSdkClientExceptionThrownFromFileSaveFailure() {
        doThrow(SdkClientException.class).when(amazonFileTransfer).uploadFile(any(String.class), any(InputStream.class));

        assertThrows(SdkClientException.class, () -> {
            underTest.save(createTestFileApi());
        });
    }

    @Test
    @DisplayName("Test FileApi object returned on successful file load")
    void testLoadFileSuccess() {
        when(amazonFileTransfer.downloadFile(any(String.class))).thenReturn("sdf");

        Optional<FileApi> actual = underTest.load("test.pdf", createTestFileDetailsApi());

        assertTrue(actual.isPresent());
        verify(amazonFileTransfer).downloadFile(any(String.class));
    }

    @Test
    @DisplayName("Test no FileApi object returned on failed file load")
    void testLoadFileFailureReturnsEmptyObject() {
        when(amazonFileTransfer.downloadFile(any(String.class))).thenReturn(null);

        Optional<FileApi> actual = underTest.load("test.pdf", createTestFileDetailsApi());

        assertTrue(actual.isEmpty());
        verify(amazonFileTransfer).downloadFile(any(String.class));
    }

    @Test
    @DisplayName("Test FileDetailsApi object returned with both AV tags on successful get file details")
    void testGetFileDetailsSuccessWithAvTags() {
        when(amazonFileTransfer.getS3Object(any(String.class))).thenReturn(createTestS3ObjectTags(4));
        when(amazonFileTransfer.getFileTaggingResult(any(String.class))).thenReturn(createAvTags());

        Optional<FileDetailsApi> actual = underTest.getFileDetails("test.pdf");

        assertTrue(actual.isPresent());
        assertNotNull(actual.get().getAvTimestamp());
        assertNotNull(actual.get().getAvStatusApi());
        verify(amazonFileTransfer).getS3Object(any(String.class));
        verify(amazonFileTransfer).getFileTaggingResult(any(String.class));
    }

    @Test
    @DisplayName("Test FileDetailsApi object returned without any tags on successful get file details")
    void testGetFileDetailsSuccessWithNullTags() {
        when(amazonFileTransfer.getS3Object(any(String.class))).thenReturn(createTestS3ObjectBasic());

        Optional<FileDetailsApi> actual = underTest.getFileDetails("test.pdf");

        assertTrue(actual.isPresent());
        verify(amazonFileTransfer).getS3Object(any(String.class));
        verify(amazonFileTransfer, times(0)).getFileTaggingResult(any(String.class));
    }

    @Test
    @DisplayName("Test FileDetailsApi object returned with zero tags on successful get file details")
    void testGetFileDetailsSuccessWithZeroTags() {
        when(amazonFileTransfer.getS3Object(any(String.class))).thenReturn(createTestS3ObjectWithZeroTags());

        Optional<FileDetailsApi> actual = underTest.getFileDetails("test.pdf");

        assertTrue(actual.isPresent());
        verify(amazonFileTransfer).getS3Object(any(String.class));
        verify(amazonFileTransfer, times(0)).getFileTaggingResult(any(String.class));
    }

    @Test
    @DisplayName("Test no FileDetailsApi object returned when s3 object not found")
    void testGetFileDetailsFailsWhenS3ObjectNotFound() {
        when(amazonFileTransfer.getS3Object(any(String.class))).thenReturn(null);

        Optional<FileDetailsApi> actual = underTest.getFileDetails("test.pdf");

        assertTrue(actual.isEmpty());
        verify(amazonFileTransfer).getS3Object(any(String.class));
    }

    @Test
    @DisplayName("Test no FileDetailsApi object returned when retrieving Tags fails during getting file details")
    void testGetFileDetailsFailsOnRetrievingTags() {
        when(amazonFileTransfer.getS3Object(any(String.class))).thenReturn(createTestS3ObjectBasic());
        when(amazonFileTransfer.getFileTaggingResult(any(String.class))).thenReturn(null);

        Optional<FileDetailsApi> actual = underTest.getFileDetails("test.pdf");

        assertTrue(actual.isEmpty());
        verify(amazonFileTransfer).getS3Object(any(String.class));
        verify(amazonFileTransfer).getFileTaggingResult(any(String.class));
        verify(amazonFileTransfer, times(0)).getFileTaggingResult(any(String.class));
    }

    @Test
    @DisplayName("Test successful file delete")
    void testDeleteFileSuccess() {
        doNothing().when(amazonFileTransfer).deleteFile(any(String.class));

        underTest.delete("test.pdf");

        verify(amazonFileTransfer).deleteFile(any(String.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when unsuccessful file delete")
    void testSdkClientExceptionThrownFromFileDeleteFailure() {
        doThrow(SdkClientException.class).when(amazonFileTransfer).deleteFile(any(String.class));

        assertThrows(SdkClientException.class, () -> {
            underTest.delete("test.pdf");
        });
    }

    private FileApi createTestFileApi() {
        return new FileApi("test.pdf",
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

    private S3Object createTestS3ObjectBasic() {
        S3Object s3Object = new S3Object();
        ObjectMetadata objectMetadata = new ObjectMetadata();

        objectMetadata.setContentType("anything");
        objectMetadata.setContentLength(objectMetadata.getContentType().length());
        objectMetadata.setLastModified(new Date());

        s3Object.setObjectMetadata(objectMetadata);

        return s3Object;
    }

    private S3Object createTestS3ObjectWithZeroTags() {
        S3Object s3Object = createTestS3ObjectBasic();

        s3Object.setTaggingCount(0);

        return s3Object;
    }

    private S3Object createTestS3ObjectTags(int tagCount) {
        S3Object s3Object = createTestS3ObjectBasic();

        s3Object.setTaggingCount(tagCount);

        return s3Object;
    }

    private GetObjectTaggingResult createAvTags() {
        GetObjectTaggingResult taggingResult = createNonAvTags();

        taggingResult.getTagSet().add(new Tag("av-timestamp", new Date().toString()));
        taggingResult.getTagSet().add(new Tag("av-status", "clean"));

        return taggingResult;
    }

    private GetObjectTaggingResult createNonAvTags() {
        List<Tag> tags = new ArrayList<>() {{
            add(new Tag("anything", "anything"));
            add(new Tag("anything2", "anything2"));
        }};

        return new GetObjectTaggingResult(tags);
    }
}