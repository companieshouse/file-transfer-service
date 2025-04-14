package uk.gov.companieshouse.filetransferservice.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.filetransferservice.model.AWSServiceProperties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigurationTest {

    private ApplicationConfiguration undertest;

    private AWSCredentials credentials;
    private AWSServiceProperties properties;

    @BeforeEach
    public void setUp() {
        undertest = new ApplicationConfiguration();
    }

    @Test
    @DisplayName("Test logging Bean creates correct type")
    void testLoggerCreation() {
        assertNotNull(undertest.logger());
    }

    @Test
    @DisplayName("Test AwsCredentials Bean creates correct type")
    void testAwsCredentialsCreation() {
        assertNotNull(undertest.awsCredentials("accessKey", "secretKey"));
    }

    @Test
    @DisplayName("Test interceptor Bean creates correct type")
    void testInterceptorCreation() {
        assertNotNull(undertest.userInterceptor());
    }

    @Test
    @DisplayName("Test AmazonS3 bean creates correct type without proxy properties set")
    void testAmazonS3ClientIsSuccessfulNoProxy() {
        createAmazonS3ClientMocks();
        createPropertyValueMocks(null, null);

        AmazonS3 actual = undertest.getAmazonS3Client(credentials, properties);

        assertNotNull(actual);
        verify(properties).getProxyPort();
        verify(properties).getRegion();
        verifyNoMoreInteractions(properties);
    }

    @Test
    @DisplayName("Test AmazonS3 bean creates correct type with proxy properties set")
    void testAmazonS3ClientIsSuccessfulWithProxy() {
        createAmazonS3ClientMocks();
        createPropertyValueMocks("anything", 9999);

        AmazonS3 actual = undertest.getAmazonS3Client(credentials, properties);

        assertNotNull(actual);
        verify(properties).getProxyPort();
        verify(properties, atLeastOnce()).getRegion();
        verifyNoMoreInteractions(properties);
    }

    private void createAmazonS3ClientMocks() {
        credentials = new BasicAWSCredentials("accessKey", "secretKey");
        properties = mock(AWSServiceProperties.class);
    }

    private void createPropertyValueMocks(String proxyHost, Integer proxyPort) {
        when(properties.getProxyHost()).thenReturn(proxyHost);
        when(properties.getProxyPort()).thenReturn(proxyPort);
        when(properties.getRegion()).thenReturn("anything");
    }
}