package uk.gov.companieshouse.filetransferservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
class AwsSdkV2Config {

    @Bean
    S3Client s3Client() {
        return S3Client.create();
    }
}
