package uk.gov.companieshouse.filetransferservice.service.impl;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
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
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.filetransferservice.model.AWSServiceProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AmazonFileTransferImplTest {
    private AmazonFileTransferImpl amazonFileTransfer;
    private AWSServiceProperties configuration;
    private GetObjectTaggingResult taggingResult;
    private AmazonS3Client client;
    private S3Object s3Object;
    private S3ObjectInputStream s3ObjectInputStream;
    private PutObjectRequest putObjectRequest;
    private PutObjectResult putObjectResult;
    private DeleteObjectRequest deleteObjectRequest;
    private static final String S3_PATH = "s3://s3av-cidev/ade";
    private static final String INVALID_PATH = "anything";
    private static final String S3_PATH_WITHOUT_BUCKET_NAME = "s3://";
    private static final String BUCKET_NAME = "s3av-cidev";
    private static final String PATH_DIRECTORY = "ade/";
    private static final String S3_LOCATION = "s3://s3av-cidev/files/test.pdf";

    @BeforeEach
    public void setUp() {
        configuration = mock(AWSServiceProperties.class);
        putObjectRequest = mock(PutObjectRequest.class);
        deleteObjectRequest = mock(DeleteObjectRequest.class);
        putObjectResult = mock(PutObjectResult.class);
        client = mock(AmazonS3Client.class);
        taggingResult = mock(GetObjectTaggingResult.class);

        s3Object = createTestS3Object();
        configuration = mock(AWSServiceProperties.class);
        s3ObjectInputStream = mock(S3ObjectInputStream.class);

        amazonFileTransfer = new AmazonFileTransferImpl(configuration) {
            @Override
            protected AmazonS3 getAmazonS3Client() {
                return client;
            }
        };
    }

    private void mockConfigurationDetails() {
        when(configuration.getAccessKeyId()).thenReturn("");
        when(configuration.getSecretAccessKey()).thenReturn("");
        when(configuration.getProtocol()).thenReturn("");
        when(configuration.getS3Path()).thenReturn(S3_PATH);
        when(client.doesBucketExist(BUCKET_NAME)).thenReturn(true);
        when(client.doesObjectExist(BUCKET_NAME, PATH_DIRECTORY)).thenReturn(true);
    }

    @Test
    @DisplayName("Test successful AWS credentials returned")
    void testGetAWSCredentials() {
        mockConfigurationDetails();
        BasicAWSCredentials awsCredentials = amazonFileTransfer.getAWSCredentials();
        assertNotNull(awsCredentials);

        verify(configuration).getAccessKeyId();
        verify(configuration).getSecretAccessKey();
    }

    @Test
    @DisplayName("Test successful File Upload")
    void testUploadFileIsSuccessful() {
        mockConfigurationDetails();
        when(client.putObject(putObjectRequest)).thenReturn(putObjectResult);

        amazonFileTransfer.uploadFile("123", getInputStream());

        verify(client).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on File Upload")
    void testUploadFileWhenInvalidS3Path() {
        when(configuration.getS3Path()).thenReturn(INVALID_PATH);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.uploadFile("123", getInputStream()));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on File Upload")
    void testUploadFileWhenEmptyBucketName() {
        when(configuration.getS3Path()).thenReturn(S3_PATH_WITHOUT_BUCKET_NAME);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.uploadFile("123", getInputStream()));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on File Upload")
    void testUploadFileWhenBucketDoesNotExist() {
        mockConfigurationDetails();
        when(client.doesBucketExist(BUCKET_NAME)).thenReturn(false);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.uploadFile("123", getInputStream()));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 location not found on File Upload")
    void testUploadFileWhenLocationNotFound() {
        mockConfigurationDetails();
        when(client.doesObjectExist(BUCKET_NAME, PATH_DIRECTORY)).thenReturn(false);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.uploadFile("123", getInputStream()));
    }

    @Test
    @DisplayName("Test successful File Download")
    void testDownloadIsSuccessful() throws IOException {
        S3Object s3Object = mock(S3Object.class);

        mockConfigurationDetails();
        when(client.doesObjectExist(anyString(), anyString())).thenReturn(true);
        when(client.getObject(any())).thenReturn(s3Object);
        when(s3Object.getObjectContent()).thenReturn(s3ObjectInputStream);
        when(s3ObjectInputStream.read(any())).thenReturn(-1);

        String actual = amazonFileTransfer.downloadFile("123");

        assertEquals("", actual);
        verify(client).doesObjectExist(anyString(), anyString());
        verify(client).getObject(any());
        verify(s3Object).getObjectContent();
        verify(s3ObjectInputStream).read(any());
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on File Download")
    void testDownloadFileWhenInvalidS3Path() {
        when(configuration.getS3Path()).thenReturn(INVALID_PATH);

        String actual = amazonFileTransfer.downloadFile("123");

        assertNull(actual);
        verify(configuration, atLeastOnce()).getS3Path();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on File Download")
    void testDownloadFileWhenEmptyBucketName() {
        when(configuration.getS3Path()).thenReturn(S3_PATH_WITHOUT_BUCKET_NAME);

        String actual = amazonFileTransfer.downloadFile("123");

        assertNull(actual);
        verify(configuration, atLeastOnce()).getS3Path();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on File Download")
    void testDownloadFileWhenBucketDoesNotExist() {
        mockConfigurationDetails();
        when(client.doesBucketExist(BUCKET_NAME)).thenReturn(false);

        String actual = amazonFileTransfer.downloadFile("123");

        assertNull(actual);
        verify(client, atLeastOnce()).doesBucketExist(BUCKET_NAME);
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 location not found on File Download")
    void testDownloadWhenLocationNotFound() {
        mockConfigurationDetails();
        when(client.doesObjectExist(anyString(), anyString())).thenReturn(false);

        String actual = amazonFileTransfer.downloadFile("123");

        assertNull(actual);
        verify(client).doesObjectExist(anyString(), anyString());
    }

    @Test
    @DisplayName("Test successful Get File Object")
    void testGetFileObjectIsSuccessful() {
        mockConfigurationDetails();
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(s3Object);

        S3Object actual = amazonFileTransfer.getFileObject("123");

        Assert.assertNotNull(actual);
        verify(client).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on Get File Object")
    void testGetFileObjectWhenInvalidS3Path() {
        when(configuration.getS3Path()).thenReturn(INVALID_PATH);

        S3Object actual = amazonFileTransfer.getFileObject("123");

        assertNull(actual);
        verify(configuration, atLeastOnce()).getS3Path();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on Get File Object")
    void testGetFileObjectWhenEmptyBucketName() {
        when(configuration.getS3Path()).thenReturn(S3_PATH_WITHOUT_BUCKET_NAME);

        S3Object actual = amazonFileTransfer.getFileObject("123");

        assertNull(actual);
        verify(configuration, atLeastOnce()).getS3Path();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on Get File Object")
    void testGetFileObjectWhenBucketDoesNotExist() {
        mockConfigurationDetails();
        when(client.doesBucketExist(BUCKET_NAME)).thenReturn(false);

        S3Object actual = amazonFileTransfer.getFileObject("123");

        assertNull(actual);
        verify(client, atLeastOnce()).doesBucketExist(BUCKET_NAME);
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 location not found on Get File Object")
    void testGetFileObjectWhenLocationNotFound() {
        mockConfigurationDetails();
        when(client.getObject(any(GetObjectRequest.class))).thenThrow(SdkClientException.class);

        S3Object actual = amazonFileTransfer.getFileObject("123");

        assertNull(actual);
        verify(client).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Test successful Get File Tags")
    void getGetFileTagsIsSuccessful() {
        mockConfigurationDetails();
        when(client.getObjectTagging(any(GetObjectTaggingRequest.class))).thenReturn(taggingResult);

        List<Tag> actual = amazonFileTransfer.getFileTags("123");

        Assert.assertNotNull(actual);
        verify(client).getObjectTagging(any(GetObjectTaggingRequest.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on Get File Tags")
    void testGetFileTagsWhenInvalidS3Path() {
        when(configuration.getS3Path()).thenReturn(INVALID_PATH);

        List<Tag> actual = amazonFileTransfer.getFileTags("123");

        assertNull(actual);
        verify(configuration, atLeastOnce()).getS3Path();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on Get File Tags")
    void testGetFileTagsWhenEmptyBucketName() {
        when(configuration.getS3Path()).thenReturn(S3_PATH_WITHOUT_BUCKET_NAME);

        List<Tag> actual = amazonFileTransfer.getFileTags("123");

        assertNull(actual);
        verify(configuration, atLeastOnce()).getS3Path();
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on Get File Tags")
    void testGetFileTagsWhenBucketDoesNotExist() {
        mockConfigurationDetails();
        when(client.doesBucketExist(BUCKET_NAME)).thenReturn(false);

        List<Tag> actual = amazonFileTransfer.getFileTags("123");

        assertNull(actual);
        verify(client, atLeastOnce()).doesBucketExist(BUCKET_NAME);
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 location not found on Get File Tags")
    void testGetFileTagsWhenLocationNotFound() {
        mockConfigurationDetails();
        when(client.doesObjectExist(anyString(), anyString())).thenReturn(false);
        when(client.getObjectTagging(any(GetObjectTaggingRequest.class))).thenThrow(SdkClientException.class);

        List<Tag> actual = amazonFileTransfer.getFileTags("123");

        assertNull(actual);
        verify(client).getObjectTagging(any(GetObjectTaggingRequest.class));
    }

    @Test
    @DisplayName("Test successful Delete File")
    void testDeleteFileIsSuccessful() {
        mockConfigurationDetails();
        doNothing().when(client).deleteObject(deleteObjectRequest);

        amazonFileTransfer.deleteFile("123");

        verify(client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 Path is invalid on Delete File")
    void testDeleteFileWhenInvalidS3Path() {
        when(configuration.getS3Path()).thenReturn(INVALID_PATH);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.deleteFile("123"));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when empty Bucket name on Delete File")
    void testDeleteFileWhenEmptyBucketName() {
        when(configuration.getS3Path()).thenReturn(S3_PATH_WITHOUT_BUCKET_NAME);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.deleteFile("123"));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when Bucket does not exist on Delete File")
    void testDeleteFileWhenBucketDoesNotExist() {
        mockConfigurationDetails();
        when(client.doesBucketExist(BUCKET_NAME)).thenReturn(false);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.deleteFile("123"));
    }

    @Test
    @DisplayName("Test SdkClientException thrown when S3 location not found on Delete File")
    void testDeleteFileWhenLocationNotFound() {
        mockConfigurationDetails();
        when(client.doesObjectExist(anyString(), anyString())).thenReturn(false);

        assertThrows(SdkClientException.class, () -> amazonFileTransfer.deleteFile("123"));
    }

    @Test
    void getAmazonS3Client() {
        AmazonFileTransferImpl testAmazonFileTransfer = new AmazonFileTransferImpl(configuration);

        when(configuration.getAccessKeyId()).thenReturn("");
        when(configuration.getSecretAccessKey()).thenReturn("");

        AmazonS3 testS3Client = testAmazonFileTransfer.getAmazonS3Client();

        verify(configuration, times(1)).getAccessKeyId();
        verify(configuration, times(1)).getSecretAccessKey();
        verify(configuration, times(1)).getProtocol();

        assertNotNull(testS3Client);
    }

    @Test
    void getBucketFromS3Location() {
        String bucket = amazonFileTransfer.getBucketFromS3Location(S3_LOCATION);

        assertEquals("s3av-cidev", bucket);
    }

    @Test
    void getKeyFromS3Location() {
        String key = amazonFileTransfer.getKeyFromS3Location(S3_LOCATION);

        assertEquals("files/test.pdf", key);
    }

    @Test
    void getClientConfigurationWithProxy() {
        ClientConfiguration clientConfiguration = amazonFileTransfer.getClientConfiguration("proxyhost", 123, "http");

        assertEquals(Protocol.HTTP, clientConfiguration.getProtocol());
        assertEquals("proxyhost", clientConfiguration.getProxyHost());
        assertEquals(123, clientConfiguration.getProxyPort());
    }

    @Test
    void getClientConfigurationNoProxy() {
        ClientConfiguration clientConfiguration = amazonFileTransfer.getClientConfiguration(null, null, null);

        assertEquals(Protocol.HTTPS, clientConfiguration.getProtocol());
        assertEquals("websenseproxy.internal.ch", clientConfiguration.getProxyHost());
        assertEquals(8080, clientConfiguration.getProxyPort());
    }

    private InputStream getInputStream() {
        return new ByteArrayInputStream("anything".getBytes());
    }

    private S3Object createTestS3Object() {
        return new S3Object();
    }
}
