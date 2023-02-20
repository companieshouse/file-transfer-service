package uk.gov.companieshouse.filetransferservice.service;

import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.InputStream;
import java.util.Map;

public interface AmazonFileTransfer {
    void uploadFile(String key, InputStream inputStream, ObjectMetadata omd);

    String downloadFile(String s3Location);

    ObjectMetadata getFileMetaData(String s3Location);

    Map<String, String> getFileTags(String s3Location);
}
