package uk.gov.companieshouse.filetransferservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.filetransferservice.config.properties.AWSServiceProperties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigurationTest {

    private ApplicationConfiguration applicationConfiguration;

    @Mock
    private AWSServiceProperties properties;

    @BeforeEach
    public void setUp() {
        applicationConfiguration = new ApplicationConfiguration();
    }

    @Test
    @DisplayName("Test logging Bean creates correct type")
    void testLoggerCreation() {
        assertNotNull(applicationConfiguration.logger());
    }

//    @Test
//    @DisplayName("Test AmazonS3ClientBuilder Bean creates correct type")
//    void testAmazonS3ClientBuilderCreation() {
//        assertNotNull(applicationConfiguration.amazonS3ClientBuilder());
//    }

//    @Test
//    @DisplayName("Test interceptor Bean creates correct type")
//    void testClientConfigurationCreation() {
//        assertNotNull(applicationConfiguration.clientConfiguration());
//    }

    @Test
    @DisplayName("Test interceptor Bean creates correct type")
    void testInterceptorCreation() {
        assertNotNull(applicationConfiguration.userInterceptor());
    }

//    @Test
//    @DisplayName("Test AmazonS3 bean creates correct type without proxy properties set")
//    void testAmazonS3ClientIsSuccessfulNoProxy() {
//        createAmazonS3ClientMocks();
//        createPropertyValueMocks(null, null);
//
//        AmazonS3 actual = applicationConfiguration.getAmazonS3Client(properties, clientConfiguration, builder);
//
//        assertNotNull(actual);
//        verify(properties).getProxyPort();
//        verify(properties).getRegion();
//        verify(clientConfiguration, times(0)).setProxyHost(anyString());
//        verify(clientConfiguration, times(0)).setProxyPort(anyInt());
//        verifyNoMoreInteractions(properties);
//    }

//    @Test
//    @DisplayName("Test AmazonS3 bean creates correct type with proxy properties set")
//    void testAmazonS3ClientIsSuccessfulWithProxy() {
//        createAmazonS3ClientMocks();
//        createPropertyValueMocks("anything", 9999);
//
//        AmazonS3 actual = applicationConfiguration.getAmazonS3Client(properties, clientConfiguration, builder);
//
//        assertNotNull(actual);
//        verify(properties).getProxyPort();
//        verify(properties, atLeastOnce()).getRegion();
//        verify(clientConfiguration).setProxyHost(anyString());
//        verify(clientConfiguration).setProxyPort(anyInt());
//        verifyNoMoreInteractions(properties);
//    }

//    private void createAmazonS3ClientMocks() {
//        properties = mock(AWSServiceProperties.class);
//        clientConfiguration = mock(ClientConfiguration.class);
//        builder = mock(AmazonS3ClientBuilder.class, RETURNS_DEEP_STUBS);
//    }

    private void createPropertyValueMocks(String proxyHost, Integer proxyPort) {
        when(properties.getProxyHost()).thenReturn(proxyHost);
        when(properties.getProxyPort()).thenReturn(proxyPort);
        when(properties.getRegion()).thenReturn("anything");
    }
}