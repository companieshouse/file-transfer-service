package uk.gov.companieshouse.filetransferservice.service.impl;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.filetransferservice.model.AWSServiceProperties;
import uk.gov.companieshouse.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AmazonFileTransferImplTest {
    private AmazonFileTransferImpl amazonFileTransfer;
    private AWSServiceProperties properties;
    private GetObjectTaggingResult taggingResult;
    private AmazonS3Client client;
    private S3Object s3Object;
    private S3ObjectInputStream s3ObjectInputStream;
    private PutObjectRequest putObjectRequest;
    private PutObjectResult putObjectResult;
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
        putObjectResult = mock(PutObjectResult.class);
        client = mock(AmazonS3Client.class);
        taggingResult = mock(GetObjectTaggingResult.class);

        s3Object = createTestS3Object();
        properties = mock(AWSServiceProperties.class);
        s3ObjectInputStream = mock(S3ObjectInputStream.class);

        amazonFileTransfer = new AmazonFileTransferImpl(client, properties, mock(Logger.class));
    }

    private void mockConfigurationDetails() {
        when(properties.getAccessKeyId()).thenReturn("");
        when(properties.getSecretAccessKey()).thenReturn("");
        when(properties.getProtocol()).thenReturn("");
        when(properties.getBucketName()).thenReturn(S3_PATH);
        when(client.doesBucketExistV2(anyString())).thenReturn(true);
        when(client.doesObjectExist(BUCKET_NAME, PATH_DIRECTORY)).thenReturn(true);
    }

    @Test
    @DisplayName("Test successful File Upload")
    void testUploadFileIsSuccessful() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.doesBucketExistV2(anyString())).thenReturn(true);
        when(client.putObject(putObjectRequest)).thenReturn(putObjectResult);

        amazonFileTransfer.uploadFile("123", createValidMetaData(), getInputStream());

        verify(client).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when invalid meta tags on File Upload")
    void testUploadWhenInvalidMetaTags() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.doesBucketExistV2(anyString())).thenReturn(true);
        when(client.putObject(putObjectRequest)).thenReturn(putObjectResult);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.uploadFile("123", createInvalidMetaData(), getInputStream()));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on File Upload")
    void testUploadFileWhenInvalidS3Path() {
        when(properties.getS3PathPrefix()).thenReturn(INVALID_S3_PATH_PREFIX);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.uploadFile("123", createValidMetaData(), getInputStream()));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on File Upload")
    void testUploadFileWhenEmptyBucketName() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("");

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.uploadFile("123", createValidMetaData(), getInputStream()));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on File Upload")
    void testUploadFileWhenBucketDoesNotExist() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.doesBucketExistV2(anyString())).thenReturn(false);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.uploadFile("123", createValidMetaData(), getInputStream()));
    }

    @Test
    @DisplayName("Test successful File Download")
    void testDownloadIsSuccessful() throws IOException {
        S3Object s3Object = mock(S3Object.class);
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.doesBucketExistV2(anyString())).thenReturn(true);
        when(client.doesObjectExist(anyString(), anyString())).thenReturn(true);
        when(client.getObject(any())).thenReturn(s3Object);
        when(s3Object.getObjectContent()).thenReturn(s3ObjectInputStream);
        when(s3ObjectInputStream.read(any())).thenReturn(-1);

        Optional<byte[]> actual = amazonFileTransfer.downloadFile("123");

        assertTrue(actual.isPresent());
        verify(client).doesObjectExist(anyString(), anyString());
        verify(client).getObject(any());
        verify(s3Object).getObjectContent();
        verify(s3ObjectInputStream).read(any());
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Object not found")
    void testDownloadFileWhenS3ObjectNotFound() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.doesBucketExistV2(anyString())).thenReturn(true);
        when(client.doesObjectExist(anyString(), anyString())).thenReturn(false);

        Optional<byte[]> actual = amazonFileTransfer.downloadFile("123");

        assertTrue(actual.isEmpty());
        verify(client).doesObjectExist(anyString(), anyString());
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
        when(client.doesBucketExistV2(anyString())).thenReturn(false);

        Optional<byte[]> actual = amazonFileTransfer.downloadFile("123");

        assertTrue(actual.isEmpty());
        verify(client, atLeastOnce()).doesBucketExistV2(anyString());
    }

    @Test
    @DisplayName("Test successful Get File Object")
    void testGetFileObjectIsSuccessful() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.doesBucketExistV2(anyString())).thenReturn(true);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(s3Object);

        Optional<S3Object> actual = amazonFileTransfer.getFileObject("123");

        assertTrue(actual.isPresent());
        verify(client).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on Get File Object")
    void testGetFileObjectWhenInvalidS3Path() {
        when(properties.getS3PathPrefix()).thenReturn(INVALID_S3_PATH_PREFIX);

        Optional<S3Object> actual = amazonFileTransfer.getFileObject("123");

        assertTrue(actual.isEmpty());
        verify(properties, atLeastOnce()).getBucketName();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on Get File Object")
    void testGetFileObjectWhenEmptyBucketName() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("");

        Optional<S3Object> actual = amazonFileTransfer.getFileObject("123");

        assertTrue(actual.isEmpty());
        verify(properties, atLeastOnce()).getBucketName();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on Get File Object")
    void testGetFileObjectWhenBucketDoesNotExist() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.doesBucketExistV2(anyString())).thenReturn(false);

        Optional<S3Object> actual = amazonFileTransfer.getFileObject("123");

        assertTrue(actual.isEmpty());
        verify(client, atLeastOnce()).doesBucketExistV2(anyString());
    }

    @Test
    @DisplayName("Test successful Get File Tags")
    void getGetFileTagsIsSuccessful() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.doesBucketExistV2(anyString())).thenReturn(true);
        when(client.getObjectTagging(any(GetObjectTaggingRequest.class))).thenReturn(taggingResult);

        Optional<List<Tag>> actual = amazonFileTransfer.getFileTags("123");

        assertTrue(actual.isPresent());
        verify(client).getObjectTagging(any(GetObjectTaggingRequest.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on Get File Tags")
    void testGetFileTagsWhenInvalidS3Path() {
        when(properties.getS3PathPrefix()).thenReturn(INVALID_S3_PATH_PREFIX);

        Optional<List<Tag>> actual = amazonFileTransfer.getFileTags("123");

        assertTrue(actual.isEmpty());
        verify(properties, atLeastOnce()).getBucketName();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on Get File Tags")
    void testGetFileTagsWhenEmptyBucketName() {
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("");

        Optional<List<Tag>> actual = amazonFileTransfer.getFileTags("123");

        assertTrue(actual.isEmpty());
        verify(properties, atLeastOnce()).getBucketName();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on Get File Tags")
    void testGetFileTagsWhenBucketDoesNotExist() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.doesBucketExistV2(anyString())).thenReturn(false);

        Optional<List<Tag>> actual = amazonFileTransfer.getFileTags("123");

        assertTrue(actual.isEmpty());
        verify(client, atLeastOnce()).doesBucketExistV2(anyString());
    }

    @Test
    @DisplayName("Test successful Delete File")
    void testDeleteFileIsSuccessful() {
        mockConfigurationDetails();
        when(properties.getS3PathPrefix()).thenReturn(VALID_S3_PATH_PREFIX);
        when(properties.getBucketName()).thenReturn("anything");
        when(client.doesBucketExistV2(anyString())).thenReturn(true);
        doNothing().when(client).deleteObject(deleteObjectRequest);

        amazonFileTransfer.deleteFile("123");

        verify(client).doesBucketExistV2(anyString());
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
        when(client.doesBucketExistV2(anyString())).thenReturn(false);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.deleteFile("123"));
    }

    private InputStream getInputStream() {
        return new ByteArrayInputStream("anything".getBytes());
    }

    private S3Object createTestS3Object() {
        return new S3Object();
    }

    private Map<String, String> createValidMetaData() {
        return new HashMap<>() {{
            put("Content-Type", "application/pdf");
        }};
    }


    private Map<String, String> createInvalidMetaData() {
        return new HashMap<>() {{
            put("anything", "anything");
        }};
    }
}

