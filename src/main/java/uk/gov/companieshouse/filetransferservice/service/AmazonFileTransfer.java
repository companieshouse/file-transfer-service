package uk.gov.companieshouse.filetransferservice.service;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.Tag;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AmazonFileTransfer {
    void uploadFile(String fileId, Map<String, String> metaData, InputStream inputStream);

    Optional<byte[]> downloadFile(String fileId);

    Optional<S3Object> getFileObject(String fileId);

    Optional<List<Tag>> getFileTags(String fileId);

    void deleteFile(String fileId);
}
