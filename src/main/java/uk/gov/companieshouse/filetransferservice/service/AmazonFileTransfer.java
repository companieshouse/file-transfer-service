package uk.gov.companieshouse.filetransferservice.service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.InputStream;

public interface AmazonFileTransfer {
    BasicAWSCredentials getAWSCredentials();

    void uploadFile(String fileId, InputStream inputStream);

    String downloadFile(String fileId);

    ObjectMetadata getFileMetaData(String fileId);

    GetObjectTaggingResult getFileTaggingResult(String fileId);

    void deleteFile(String fileId);
}
