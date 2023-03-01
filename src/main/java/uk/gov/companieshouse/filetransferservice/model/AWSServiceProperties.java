package uk.gov.companieshouse.filetransferservice.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;

@Component
@ConfigurationProperties(prefix = "aws")
public class AWSServiceProperties {

    @NotBlank
    private String secretAccessKey;

    @NotBlank
    private String accessKeyId;

    @NotBlank
    private String bucketName;

    @NotBlank
    private String protocol;

    @NotBlank
    private String s3PathPrefix;

    private String proxyHost;

    private Integer proxyPort;

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
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

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }
}
