package uk.gov.companieshouse.filetransferservice.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigurationTest {

    private ApplicationConfiguration applicationConfiguration;

    @BeforeEach
    void setUp() {
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
}