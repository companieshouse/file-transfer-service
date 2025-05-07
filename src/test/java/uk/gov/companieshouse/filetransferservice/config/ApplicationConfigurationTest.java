package uk.gov.companieshouse.filetransferservice.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.filetransferservice.config.properties.AWSServiceProperties;

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

    @Test
    @DisplayName("Test interceptor Bean creates correct type")
    void testInterceptorCreation() {
        assertNotNull(applicationConfiguration.userInterceptor());
    }

    private void createPropertyValueMocks(String proxyHost, Integer proxyPort) {
        when(properties.getProxyHost()).thenReturn(proxyHost);
        when(properties.getProxyPort()).thenReturn(proxyPort);
        when(properties.getRegion()).thenReturn("anything");
    }
}