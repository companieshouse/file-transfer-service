package uk.gov.companieshouse.filetransferservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.filetransferservice.model.AWSServiceProperties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigurationTest {

    private ApplicationConfiguration undertest;

    @BeforeEach
    public void setUp() {
        undertest = new ApplicationConfiguration();
    }

    @Test
    @DisplayName("Get the bean for Amazon File Transfer")
    void testAmazonFileTransferCreation() {
        assertNotNull(undertest.amazonFileTransfer(new AWSServiceProperties()));
    }

    @Test
    @DisplayName("Get the bean for logging")
    void testLoggerCreation() {
        assertNotNull(undertest.logger());
    }
}