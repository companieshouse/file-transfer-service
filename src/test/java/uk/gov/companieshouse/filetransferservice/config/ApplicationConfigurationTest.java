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
        Logger actual = undertest.logger();
        assertTrue(actual != null && undertest.logger() instanceof Logger);
    }

    @Test
    @DisplayName("Test AmazonS3ClientBuilder Bean creates correct type")
    void testAmazonS3ClientBuilderCreation() {
        AmazonS3ClientBuilder actual = undertest.amazonS3ClientBuilder();
        assertTrue(actual != null && undertest.amazonS3ClientBuilder() instanceof AmazonS3ClientBuilder);
    }

    @Test
    @DisplayName("Test interceptor Bean creates correct type")
    void testClientConfigurationCreation() {
        ClientConfiguration actual = undertest.clientConfiguration();
        assertTrue(actual != null && undertest.clientConfiguration() instanceof ClientConfiguration);
    }

    @Test
    @DisplayName("Test interceptor Bean creates correct type")
    void testInterceptorCreation() {
        InternalUserInterceptor actual = undertest.userInterceptor();
        assertTrue(actual != null && undertest.userInterceptor() instanceof InternalUserInterceptor);
    }

    @Test
    @DisplayName("Test AmazonS3 bean creates correct type without proxy properties set")
    void testAmazonS3ClientIsSuccessfulNoProxy() {
        createAmazonS3ClientMocks();
        createPropertyValueMocks(null, null);

        AmazonS3 actual = undertest.getAmazonS3Client(properties, clientConfiguration, builder);

        assertTrue(actual != null && actual instanceof AmazonS3);
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
        createPropertyValueMocks("anything", 9999);

        AmazonS3 actual = undertest.getAmazonS3Client(properties, clientConfiguration, builder);

        assertTrue(actual != null && actual instanceof AmazonS3);
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

    private void createPropertyValueMocks(String proxyHost, Integer proxyPort) {
        when(properties.getAccessKeyId()).thenReturn("anything");
        when(properties.getSecretAccessKey()).thenReturn("anything");
        when(properties.getProxyHost()).thenReturn(proxyHost);
        when(properties.getProxyPort()).thenReturn(proxyPort);
        when(properties.getRegion()).thenReturn("anything");
    }
}