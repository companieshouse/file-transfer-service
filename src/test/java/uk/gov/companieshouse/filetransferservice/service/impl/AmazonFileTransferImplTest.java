package uk.gov.companieshouse.filetransferservice.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.filetransferservice.service.storage.S3FileStorage.FILENAME_METADATA_KEY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import uk.gov.companieshouse.filetransferservice.config.properties.AWSServiceProperties;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class AmazonFileTransferImplTest {

    private static final String TEST_FILE_NAME = "file.pdf";
    private static final String S3_PATH = "s3://s3av-cidev/ade";
    private static final String VALID_S3_PATH_PREFIX = "s3://";
    private static final String INVALID_S3_PATH_PREFIX = "anything";
    private static final String BUCKET_NAME = "s3av-cidev";

    @Mock
    private AWSServiceProperties properties;
    @Mock
    private GetObjectTaggingResponse taggingResult;
    @Mock
    private S3Client client;
    @Mock
    private PutObjectRequest putObjectRequest;
    @Mock
    private PutObjectResponse putObjectResult;
    @Mock
    private DeleteObjectRequest deleteObjectRequest;
    @Mock
    private Logger logger;
    @Mock
    private HeadObjectResponse headObjectResponse;
    @Mock
    private SdkHttpResponse sdkHttpResponse;
    @Mock
    private ResponseInputStream<GetObjectResponse> responseInputStream;

    @Test
    @DisplayName("Test successful File Upload")
    void testUploadFileIsSuccessful() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn(S3_PATH + BUCKET_NAME);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(putObjectResult);

        AmazonFileTransferImpl amazonFileTransfer = new AmazonFileTransferImpl(client, properties, logger);
        amazonFileTransfer.uploadFile("123", createValidMetaData(), getInputStream());

        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when invalid meta tags on File Upload")
    void testUploadWhenInvalidMetaTags() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn(BUCKET_NAME);

        AmazonFileTransferImpl amazonFileTransfer = new AmazonFileTransferImpl(client, properties, logger);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.uploadFile("123", Map.of(), getInputStream()));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid")
    void testUploadFileWhenInvalidS3Path() {
        when(properties.getS3PathPrefix()).thenReturn(INVALID_S3_PATH_PREFIX);

        assertThrows(SdkClientException.class, () -> new AmazonFileTransferImpl(client, properties, logger));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on File Upload")
    void testUploadFileWhenEmptyBucketName() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("");

        assertThrows(SdkClientException.class, () -> new AmazonFileTransferImpl(client, properties, logger));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on File Upload")
    void testUploadFileWhenBucketDoesNotExist() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn(BUCKET_NAME);
        when(client.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder().build());

        assertThrows(SdkClientException.class, () ->  new AmazonFileTransferImpl(client, properties, logger));
    }

    @Test
    @DisplayName("Test successful File Download")
    void testDownloadIsSuccessful() throws IOException {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn(BUCKET_NAME);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        AmazonFileTransferImpl amazonFileTransfer = new AmazonFileTransferImpl(client, properties, logger);

        Optional<InputStream> actual = amazonFileTransfer.downloadStream("123");

        verify(client, times(1)).getObject(any(GetObjectRequest.class));
        verify(client, times(0)).headObject(any(HeadObjectRequest.class));

        assertTrue(actual.isPresent());
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Object not found")
    void testDownloadFileWhenS3ObjectNotFound() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn(BUCKET_NAME);

        AmazonFileTransferImpl amazonFileTransfer = new AmazonFileTransferImpl(client, properties, logger);

        Optional<InputStream> actual = amazonFileTransfer.downloadStream("123");

        verify(client, times(1)).getObject(any(GetObjectRequest.class));
        verify(client, times(0)).headObject(any(HeadObjectRequest.class));

        assertTrue(actual.isEmpty());
    }

//    @Test
//    @DisplayName("Test SdkClientException thrown when empty Bucket name on File Download")
//    void testDownloadFileWhenEmptyBucketName() {
//        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
//        when(properties.getBucketName()).thenReturn("");
//
//        Optional<byte[]> actual = amazonFileTransfer.downloadFile("123");
//
//        assertTrue(actual.isEmpty());
//        verify(properties, atLeastOnce()).getBucketName();
//    }
//
//    @Test
//    @DisplayName("Test SdkClientException thrown when Bucket does not exist on File Download")
//    void testDownloadFileWhenBucketDoesNotExist() {
//        mockConfigurationDetails();
//        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
//        when(properties.getBucketName()).thenReturn("anything");
////        when(client.doesBucketExistV2(anyString())).thenReturn(false);
//
//        Optional<byte[]> actual = amazonFileTransfer.downloadFile("123");
//
//        assertTrue(actual.isEmpty());
////        verify(client, atLeastOnce()).doesBucketExistV2(anyString());
//    }
//
//    @Test
//    @DisplayName("Test successful Get File Object")
//    void testGetFileObjectIsSuccessful() {
//        mockConfigurationDetails();
//        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
//        when(properties.getBucketName()).thenReturn("anything");
////        when(client.doesBucketExistV2(anyString())).thenReturn(true);
//        when(client.getObject(any(GetObjectRequest.class))).thenReturn(s3Object);
//
//        Optional<S3Object> actual = amazonFileTransfer.getFileObject("123");
//
//        assertTrue(actual.isPresent());
//        verify(client).getObject(any(GetObjectRequest.class));
//    }
//
//    @Test
//    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on Get File Object")
//    void testGetFileObjectWhenInvalidS3Path() {
//        when(properties.getS3PathPrefix()).thenReturn(INVALID_S3_PATH_PREFIX);
//
//        Optional<S3Object> actual = amazonFileTransfer.getFileObject("123");
//
//        assertTrue(actual.isEmpty());
//        verify(properties, atLeastOnce()).getBucketName();
//    }
//
//    @Test
//    @DisplayName("Test SdkClientException thrown when empty Bucket name on Get File Object")
//    void testGetFileObjectWhenEmptyBucketName() {
//        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
//        when(properties.getBucketName()).thenReturn("");
//
//        Optional<S3Object> actual = amazonFileTransfer.getFileObject("123");
//
//        assertTrue(actual.isEmpty());
//        verify(properties, atLeastOnce()).getBucketName();
//    }
//
//    @Test
//    @DisplayName("Test SdkClientException thrown when Bucket does not exist on Get File Object")
//    void testGetFileObjectWhenBucketDoesNotExist() {
//        mockConfigurationDetails();
//        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
//        when(properties.getBucketName()).thenReturn("anything");
////        when(client.doesBucketExistV2(anyString())).thenReturn(false);
//
//        Optional<S3Object> actual = amazonFileTransfer.getFileObject("123");
//
//        assertTrue(actual.isEmpty());
////        verify(client, atLeastOnce()).doesBucketExistV2(anyString());
//    }
//
//    @Test
//    @DisplayName("Test successful Get File Tags")
//    void getGetFileTagsIsSuccessful() {
//        mockConfigurationDetails();
//        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
//        when(properties.getBucketName()).thenReturn("anything");
////        when(client.doesBucketExistV2(anyString())).thenReturn(true);
//        when(client.getObjectTagging(any(GetObjectTaggingRequest.class))).thenReturn(taggingResult);
//
//        Optional<List<Tag>> actual = amazonFileTransfer.getFileTags("123");
//
//        assertTrue(actual.isPresent());
//        verify(client).getObjectTagging(any(GetObjectTaggingRequest.class));
//    }
//
//    @Test
//    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on Get File Tags")
//    void testGetFileTagsWhenInvalidS3Path() {
//        when(properties.getS3PathPrefix()).thenReturn(INVALID_S3_PATH_PREFIX);
//
//        Optional<List<Tag>> actual = amazonFileTransfer.getFileTags("123");
//
//        assertTrue(actual.isEmpty());
//        verify(properties, atLeastOnce()).getBucketName();
//    }
//
//    @Test
//    @DisplayName("Test SdkClientException thrown when empty Bucket name on Get File Tags")
//    void testGetFileTagsWhenEmptyBucketName() {
//        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
//        when(properties.getBucketName()).thenReturn("");
//
//        Optional<List<Tag>> actual = amazonFileTransfer.getFileTags("123");
//
//        assertTrue(actual.isEmpty());
//        verify(properties, atLeastOnce()).getBucketName();
//    }
//
//    @Test
//    @DisplayName("Test SdkClientException thrown when Bucket does not exist on Get File Tags")
//    void testGetFileTagsWhenBucketDoesNotExist() {
//        mockConfigurationDetails();
//        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
//        when(properties.getBucketName()).thenReturn("anything");
////        when(client.doesBucketExistV2(anyString())).thenReturn(false);
//
//        Optional<List<Tag>> actual = amazonFileTransfer.getFileTags("123");
//
//        assertTrue(actual.isEmpty());
////        verify(client, atLeastOnce()).doesBucketExistV2(anyString());
//    }
//
//    @Test
//    @DisplayName("Test successful Delete File")
//    void testDeleteFileIsSuccessful() {
//        mockConfigurationDetails();
//        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
//        when(properties.getBucketName()).thenReturn("anything");
////        when(client.doesBucketExistV2(anyString())).thenReturn(true);
//        doNothing().when(client).deleteObject(deleteObjectRequest);
//
//        amazonFileTransfer.deleteFile("123");
//
////        verify(client).doesBucketExistV2(anyString());
//        verify(client).deleteObject(any(DeleteObjectRequest.class));
//    }
//
//    @Test
//    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on Delete File")
//    void testDeleteFileWhenInvalidS3Path() {
//        when(properties.getS3PathPrefix()).thenReturn(INVALID_S3_PATH_PREFIX);
//
//        assertThrows(SdkClientException.class, () -> amazonFileTransfer.deleteFile("123"));
//    }
//
//    @Test
//    @DisplayName("Test SdkClientException thrown when empty Bucket name on Delete File")
//    void testDeleteFileWhenEmptyBucketName() {
//        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
//        when(properties.getBucketName()).thenReturn("");
//
//        assertThrows(SdkClientException.class, () -> amazonFileTransfer.deleteFile("123"));
//    }
//
//    @Test
//    @DisplayName("Test SdkClientException thrown when Bucket does not exist on Delete File")
//    void testDeleteFileWhenBucketDoesNotExist() {
//        mockConfigurationDetails();
//        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
//        when(properties.getBucketName()).thenReturn("anything");
////        when(client.doesBucketExistV2(anyString())).thenReturn(false);
//
//        assertThrows(SdkClientException.class, () -> amazonFileTransfer.deleteFile("123"));
//    }
//
    private InputStream getInputStream() {
        return new ByteArrayInputStream("anything".getBytes());
    }

    private Map<String, String> createValidMetaData() {
        return Map.of(
            "Content-Type", "application/pdf",
            FILENAME_METADATA_KEY, TEST_FILE_NAME);
    }
}
