package uk.gov.companieshouse.filetransferservice.service;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.Tag;

public interface AmazonFileTransfer {

    void uploadFile(String fileId, Map<String, String> metaData, InputStream inputStream);
    Optional<InputStream> downloadStream(String fileId);
    Optional<ResponseInputStream<GetObjectResponse>> getFileObject(String fileId);
    Optional<List<Tag>> getFileTags(String fileId);
    URL createPresignedGetUrl(String fileId);
    void deleteFile(String fileId);
}
