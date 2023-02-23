package uk.gov.companieshouse.filetransferservice.service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.S3Object;

import java.io.InputStream;

public interface AmazonFileTransfer {
    BasicAWSCredentials getAWSCredentials();

    void uploadFile(String fileId, InputStream inputStream);

    String downloadFile(String fileId);

    S3Object getS3Object(String fileId);

    GetObjectTaggingResult getFileTaggingResult(String fileId);

    void deleteFile(String fileId);
}
