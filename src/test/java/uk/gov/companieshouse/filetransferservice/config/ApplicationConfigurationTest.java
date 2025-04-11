package uk.gov.companieshouse.filetransferservice.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import software.amazon.awssdk.services.s3.S3Client;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigurationTest {

    @Mock
    private Logger mockLogger;

    @Test
    void testRestTemplateBean() {
        ApplicationConfiguration config = new ApplicationConfiguration();
        RestTemplate restTemplate = config.restTemplate();
        assertNotNull(restTemplate, "RestTemplate bean should not be null");
    }

    @Test
    void testLoggerBean() {
        ApplicationConfiguration config = new ApplicationConfiguration();
        ReflectionTestUtils.setField(config, "applicationNameSpace", "test-namespace");
        Logger logger = config.logger();
        assertNotNull(logger, "Logger bean should not be null");
    }

    @Test
    void testUserInterceptorBean() {
        ApplicationConfiguration config = new ApplicationConfiguration();
        ReflectionTestUtils.setField(config, "applicationNameSpace", "test-namespace");
        InternalUserInterceptor interceptor = config.userInterceptor();
        assertNotNull(interceptor, "InternalUserInterceptor bean should not be null");
    }

    @Test
    void testS3ClientBean() {
        ApplicationConfiguration config = new ApplicationConfiguration();
        S3Client s3Client = config.s3Client();
        assertNotNull(s3Client, "S3Client bean should not be null");
    }

    @Test
    void testS3ClientLocalBean() {
        ApplicationConfiguration config = new ApplicationConfiguration();
        ReflectionTestUtils.setField(config, "region", "eu-west-2");
        ReflectionTestUtils.setField(config, "s3endpoint", "http://localhost:4566");
        S3Client s3ClientLocal = config.s3ClientLocal();
        assertNotNull(s3ClientLocal, "S3Client local bean should not be null");
    }
}