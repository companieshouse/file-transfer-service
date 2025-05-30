package uk.gov.companieshouse.filetransferservice.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.filetransferservice.service.storage.S3FileStorage.FILENAME_METADATA_KEY;

import java.io.ByteArrayInputStream;
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
    void testDownloadIsSuccessful() {
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

    private InputStream getInputStream() {
        return new ByteArrayInputStream("anything".getBytes());
    }

    private Map<String, String> createValidMetaData() {
        return Map.of(
            "Content-Type", "application/pdf",
            FILENAME_METADATA_KEY, TEST_FILE_NAME);
    }
}
