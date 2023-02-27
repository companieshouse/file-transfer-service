package uk.gov.companieshouse.filetransferservice.service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.Tag;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface AmazonFileTransfer {
    BasicAWSCredentials getAWSCredentials();

    void uploadFile(String fileId, Map<String, String> metaData, InputStream inputStream);

    String downloadFile(String fileId);

    S3Object getFileObject(String fileId);

    List<Tag> getFileTags(String fileId);

    void deleteFile(String fileId);
}
