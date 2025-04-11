package uk.gov.companieshouse.filetransferservice.service;


import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;

import uk.gov.companieshouse.filetransferservice.model.S3FileMetadata;

public interface AmazonFileTransfer {
    void uploadFile(String fileId, Map<String, String> metaData, byte[] data);

    Optional<byte[]> downloadFile(String fileId);

    OutputStream downloadFileAStream(String fileId);

    Optional <S3FileMetadata> getFileMetadata(String fileId);

    void deleteFile(String fileId);
}
