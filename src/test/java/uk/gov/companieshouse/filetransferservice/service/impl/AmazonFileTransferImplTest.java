package uk.gov.companieshouse.filetransferservice.service.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.filetransferservice.service.file.transfer.S3FileStorage.FILENAME_METADATA_KEY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAclRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAclResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import uk.gov.companieshouse.filetransferservice.model.AWSServiceProperties;
import uk.gov.companieshouse.filetransferservice.model.S3FileMetadata;
import uk.gov.companieshouse.logging.Logger;

class AmazonFileTransferImplTest {
    private static final String TEST_FILE_NAME = "file.pdf";
    private AmazonFileTransferImpl amazonFileTransfer;
    private AWSServiceProperties properties;
    private S3Client client;
    private S3Object s3Object;
    private PutObjectRequest putObjectRequest;
    private DeleteObjectRequest deleteObjectRequest;
    private static final String S3_PATH = "s3://s3av-cidev/ade";
    private static final String VALID_S3_PATH_PREFIX = "s3://";
    private static final String INVALID_S3_PATH_PREFIX = "anything";
    private static final String BUCKET_NAME = "s3av-cidev";
    private static final String PATH_DIRECTORY = "ade/";

    @BeforeEach
    public void setUp() {
        properties = mock(AWSServiceProperties.class);
        putObjectRequest = mock(PutObjectRequest.class);
        deleteObjectRequest = mock(DeleteObjectRequest.class);
        client = mock(S3Client.class);

        s3Object = createTestS3Object();
        properties = mock(AWSServiceProperties.class);
        amazonFileTransfer = new AmazonFileTransferImpl(client, properties, mock(Logger.class));
    }

    private void mockConfigurationDetails() {
        when(properties.getProtocol()).thenReturn("");
        when(properties.getBucketName()).thenReturn(S3_PATH);
        when(client.getBucketAcl(r -> r.bucket(any()))).thenReturn(GetBucketAclResponse.builder().build());
    }

    @Test
    @DisplayName("Test successful File Upload")
    void testUploadFileIsSuccessful() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.getBucketAcl(r -> r.bucket(any()))).thenReturn(GetBucketAclResponse.builder().build());
        amazonFileTransfer.uploadFile("123", createValidMetaData(), "anything".getBytes());

        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when invalid meta tags on File Upload")
    void testUploadWhenInvalidMetaTags() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.getBucketAcl(any(GetBucketAclRequest.class))).thenReturn(GetBucketAclResponse.builder().build());

        PutObjectRequest objectRequest = PutObjectRequest.builder().build();
        AmazonFileTransferImpl amazonFileTransfer1 = new AmazonFileTransferImpl(client, properties, mock(Logger.class));
        assertThrows(SdkClientException.class, () -> amazonFileTransfer1.uploadFile("123", createInvalidMetaData(), "anything".getBytes()));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on File Upload")
    void testUploadFileWhenInvalidS3Path() {
        when(properties.getS3PathPrefix()).thenReturn(INVALID_S3_PATH_PREFIX);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.uploadFile("123", createValidMetaData(), "anything".getBytes()));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on File Upload")
    void testUploadFileWhenEmptyBucketName() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("");

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.uploadFile("123", createValidMetaData(), "anything".getBytes()));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on File Upload")
    void testUploadFileWhenBucketDoesNotExist() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.getBucketAcl(r -> r.bucket(any()))).thenThrow(AwsServiceException.builder().statusCode(404).build());

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.uploadFile("123", createValidMetaData(), "anything".getBytes()));
    }

    @Test
    @DisplayName("Test successful File Download")
    void testDownloadIsSuccessful() throws IOException {
        S3Object s3Object = mock(S3Object.class);
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.getBucketAcl(r -> r.bucket(any()))).thenReturn(GetBucketAclResponse.builder().build());
        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<GetObjectResponse>(GetObjectResponse.builder().build(), getInputStream());

        when(client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);
        byte[] testData = "test_data".getBytes();

        Optional<byte[]> actual = amazonFileTransfer.downloadFile("123");

        assertTrue(actual.isPresent());
        assertArrayEquals(testData,actual.get());
        verify(client).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Object not found")
    void testDownloadFileWhenS3ObjectNotFound() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.getBucketAcl(r -> r.bucket(any()))).thenReturn(GetBucketAclResponse.builder().build());
        Optional<byte[]> actual = amazonFileTransfer.downloadFile("123");

        assertTrue(actual.isEmpty());
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on File Download")
    void testDownloadFileWhenInvalidS3Path() {
        when(properties.getS3PathPrefix()).thenReturn(INVALID_S3_PATH_PREFIX);

        Optional<byte[]> actual = amazonFileTransfer.downloadFile("123");

        assertTrue(actual.isEmpty());
        verify(properties, atLeastOnce()).getBucketName();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on File Download")
    void testDownloadFileWhenEmptyBucketName() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("");

        Optional<byte[]> actual = amazonFileTransfer.downloadFile("123");

        assertTrue(actual.isEmpty());
        verify(properties, atLeastOnce()).getBucketName();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on File Download")
    void testDownloadFileWhenBucketDoesNotExist() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.getBucketAcl(r -> r.bucket(any()))).thenThrow(AwsServiceException.builder().statusCode(404).build());

        Optional<byte[]> actual = amazonFileTransfer.downloadFile("123");

        assertTrue(actual.isEmpty());
        verify(client, atLeastOnce()).getBucketAcl(r -> r.bucket(any()));
    }

    @Test
    @DisplayName("Test successful Get File Object")
    void testGetFileObjectIsSuccessful() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.getBucketAcl(r -> r.bucket(any()))).thenReturn(GetBucketAclResponse.builder().build());
        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<GetObjectResponse>(GetObjectResponse.builder().build(), getInputStream());

        when(client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        Optional<S3FileMetadata> actual = amazonFileTransfer.getFileMetadata("123");

        assertTrue(actual.isPresent());
        verify(client).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on Get File Object")
    void testGetFileObjectWhenInvalidS3Path() {
        when(properties.getS3PathPrefix()).thenReturn(INVALID_S3_PATH_PREFIX);

        Optional<S3FileMetadata> actual = amazonFileTransfer.getFileMetadata("123");

        assertTrue(actual.isEmpty());
        verify(properties, atLeastOnce()).getBucketName();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on Get File Object")
    void testGetFileObjectWhenEmptyBucketName() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("");

        Optional<S3FileMetadata> actual = amazonFileTransfer.getFileMetadata("123");

        assertTrue(actual.isEmpty());
        verify(properties, atLeastOnce()).getBucketName();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on Get File Object")
    void testGetFileObjectWhenBucketDoesNotExist() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.getBucketAcl(r -> r.bucket(any()))).thenThrow(AwsServiceException.builder().statusCode(404).build());

        Optional<S3FileMetadata> actual = amazonFileTransfer.getFileMetadata("123");

        assertTrue(actual.isEmpty());
        verify(client, atLeastOnce()).getBucketAcl(r -> r.bucket(any()));
    }

    @Test
    @DisplayName("Test successful Get File Tags")
    void getGetFileTagsIsSuccessful() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.getBucketAcl(r -> r.bucket(any()))).thenReturn(GetBucketAclResponse.builder().build());

        GetObjectTaggingResponse getObjectTaggingResponse = GetObjectTaggingResponse.builder().build();

        when(client.getObjectTagging(any(GetObjectTaggingRequest.class))).thenReturn(getObjectTaggingResponse);
        Optional<S3FileMetadata> actual = amazonFileTransfer.getFileMetadata("123");

        assertTrue(actual.isPresent());
        verify(client).getObjectTagging(any(GetObjectTaggingRequest.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on Get File Tags")
    void testGetFileTagsWhenInvalidS3Path() {
        when(properties.getS3PathPrefix()).thenReturn(INVALID_S3_PATH_PREFIX);

        Optional<S3FileMetadata> actual = amazonFileTransfer.getFileMetadata("123");

        assertTrue(actual.isEmpty());
        verify(properties, atLeastOnce()).getBucketName();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on Get File Tags")
    void testGetFileTagsWhenEmptyBucketName() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("");

        Optional<S3FileMetadata> actual = amazonFileTransfer.getFileMetadata("123");

        assertTrue(actual.isEmpty());
        verify(properties, atLeastOnce()).getBucketName();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on Get File Tags")
    void testGetFileTagsWhenBucketDoesNotExist() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");


        Optional<S3FileMetadata> actual = amazonFileTransfer.getFileMetadata("123");

        assertTrue(actual.isEmpty());
        verify(client).getBucketAcl(r -> r.bucket(any()));
    }

    @Test
    @DisplayName("Test successful Delete File")
    void testDeleteFileIsSuccessful() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.getBucketAcl(r -> r.bucket(any()))).thenReturn(GetBucketAclResponse.builder().build());
        doNothing().when(client).deleteObject(deleteObjectRequest);

        amazonFileTransfer.deleteFile("123");

        verify(client).getBucketAcl(r -> r.bucket(any()));
        verify(client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on Delete File")
    void testDeleteFileWhenInvalidS3Path() {
        when(properties.getS3PathPrefix()).thenReturn(INVALID_S3_PATH_PREFIX);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.deleteFile("123"));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on Delete File")
    void testDeleteFileWhenEmptyBucketName() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("");

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.deleteFile("123"));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on Delete File")
    void testDeleteFileWhenBucketDoesNotExist() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.getBucketAcl(r -> r.bucket(any()))).thenThrow(AwsServiceException.builder().statusCode(404).build());

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.deleteFile("123"));
    }

    private InputStream getInputStream() {
        return new ByteArrayInputStream("anything".getBytes());
    }

    private S3Object createTestS3Object() {
        return  S3Object.builder()
                        .key("file.txt")
                        .build();
    }

    private Map<String, String> createValidMetaData() {
        return new HashMap<>() {{
            put("Content-Type", "application/pdf");
            put(FILENAME_METADATA_KEY, TEST_FILE_NAME);
        }};
    }


    private Map<String, String> createInvalidMetaData() {
        return new HashMap<>() {{
            put("anything", "anything");
        }};
    }
}
