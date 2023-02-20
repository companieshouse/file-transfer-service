package uk.gov.companieshouse.filetransferservice.service;

import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.InputStream;

public interface AmazonFileTransfer {
    void uploadFileInS3(String key, InputStream inputStream, ObjectMetadata omd);
}
