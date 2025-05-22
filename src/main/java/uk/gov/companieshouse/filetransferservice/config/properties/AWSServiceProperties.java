package uk.gov.companieshouse.filetransferservice.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotBlank;

@Component
@ConfigurationProperties(prefix = "aws")
public class AWSServiceProperties {

    @NotBlank
    private String region;

    @NotBlank
    private String bucketName;

    @NotBlank
    private String protocol;

    @NotBlank
    private String s3PathPrefix;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getS3PathPrefix() {
        return s3PathPrefix;
    }

    public void setS3PathPrefix(String s3PathPrefix) {
        this.s3PathPrefix = s3PathPrefix;
    }
}
