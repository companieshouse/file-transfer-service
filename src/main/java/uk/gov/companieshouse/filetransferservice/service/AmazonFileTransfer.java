package uk.gov.companieshouse.filetransferservice.service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.Tag;

import java.io.InputStream;
import java.util.List;

public interface AmazonFileTransfer {
    BasicAWSCredentials getAWSCredentials();

    void uploadFile(String fileId, InputStream inputStream);

    String downloadFile(String fileId);

    S3Object getFileObject(String fileId);

    List<Tag> getFileTags(String fileId);

    void deleteFile(String fileId);
}
