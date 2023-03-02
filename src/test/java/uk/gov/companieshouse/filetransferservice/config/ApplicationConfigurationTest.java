package uk.gov.companieshouse.filetransferservice.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.filetransferservice.model.AWSServiceProperties;
import uk.gov.companieshouse.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigurationTest {

    private ApplicationConfiguration undertest;

    private AWSServiceProperties properties;
    private ClientConfiguration clientConfiguration;
    private AmazonS3ClientBuilder builder;

    @BeforeEach
    public void setUp() {
        undertest = new ApplicationConfiguration();
    }

    @Test
    @DisplayName("Test logging Bean creates correct type")
    void testLoggerCreation() {
        assertTrue(undertest.logger() instanceof Logger);
    }

    @Test
    @DisplayName("Test AmazonS3ClientBuilder Bean creates correct type")
    void testAmazonS3ClientBuilderCreation() {
        assertTrue(undertest.amazonS3ClientBuilder() instanceof AmazonS3ClientBuilder);
    }

    @Test
    @DisplayName("Test interceptor Bean creates correct type")
    void testClientConfigurationCreation() {
        assertTrue(undertest.clientConfiguration() instanceof ClientConfiguration);
    }

    @Test
    @DisplayName("Test interceptor Bean creates correct type")
    void testInterceptorCreation() {
        assertNotNull(undertest.userInterceptor() instanceof InternalUserInterceptor);
    }

    @Test
    @DisplayName("Test AmazonS3 bean creates correct type without proxy properties set")
    void testAmazonS3ClientIsSuccessfulNoProxy() {
        createAmazonS3ClientMocks();
        createPropertyReturns("anything", "anything", null, null, "anything");

        AmazonS3 actual = undertest.amazonS3Client(properties, clientConfiguration, builder);

        assertNotNull(actual instanceof AmazonS3);
        verify(properties).getAccessKeyId();
        verify(properties).getSecretAccessKey();
        verify(properties).getProxyPort();
        verify(properties).getRegion();
        verify(clientConfiguration, times(0)).setProxyHost(anyString());
        verify(clientConfiguration, times(0)).setProxyPort(anyInt());
        verifyNoMoreInteractions(properties);
    }

    @Test
    @DisplayName("Test AmazonS3 bean creates correct type with proxy properties set")
    void testAmazonS3ClientIsSuccessfulWithProxy() {
        createAmazonS3ClientMocks();
        createPropertyReturns("anything", "anything", "anything", 9999, "anything");

        AmazonS3 actual = undertest.amazonS3Client(properties, clientConfiguration, builder);

        assertNotNull(actual instanceof AmazonS3);
        verify(properties).getAccessKeyId();
        verify(properties).getSecretAccessKey();
        verify(properties).getProxyPort();
        verify(properties, atLeastOnce()).getRegion();
        verify(clientConfiguration).setProxyHost(anyString());
        verify(clientConfiguration).setProxyPort(anyInt());
        verifyNoMoreInteractions(properties);
    }

    private void createAmazonS3ClientMocks() {
        properties = mock(AWSServiceProperties.class);
        clientConfiguration = mock(ClientConfiguration.class);
        builder = mock(AmazonS3ClientBuilder.class, RETURNS_DEEP_STUBS);
    }

    private void createPropertyReturns(String accessKeyId, String secretAccessKey, String proxyHost, Integer proxyPort, String region) {
        when(properties.getAccessKeyId()).thenReturn(accessKeyId);
        when(properties.getSecretAccessKey()).thenReturn(secretAccessKey);
        when(properties.getProxyHost()).thenReturn(proxyHost);
        when(properties.getProxyPort()).thenReturn(proxyPort);
        when(properties.getRegion()).thenReturn(region);
    }
}