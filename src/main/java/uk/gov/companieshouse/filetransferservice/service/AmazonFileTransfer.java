package uk.gov.companieshouse.filetransferservice.service;

import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.InputStream;
import java.util.Map;

public interface AmazonFileTransfer {
    void uploadFile(String fileId, InputStream inputStream, ObjectMetadata omd);

    String downloadFile(String fileId);

    ObjectMetadata getFileMetaData(String fileId);

    Map<String, String> getFileTags(String fileId);

    void deleteFile(String fileId);
}
