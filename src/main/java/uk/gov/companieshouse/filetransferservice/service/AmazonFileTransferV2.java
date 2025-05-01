package uk.gov.companieshouse.filetransferservice.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;

public interface AmazonFileTransferV2 {

    void uploadFile(String fileId, Map<String, String> metaData, InputStream inputStream);

    Optional<byte[]> downloadFile(String fileId);

    Optional<InputStream> downloadStream(String fileId);

    Optional<S3Object> getFileObject(String fileId);

    Optional<List<Tag>> getFileTags(String fileId);

    void deleteFile(String fileId);
}
