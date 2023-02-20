package uk.gov.companieshouse.filetransferservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.filetransferservice.service.file.transfer.FileStorageStrategy;
import uk.gov.companieshouse.logging.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping(path = "/files")
public class FileTransferController {
    public static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "text/plain",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "application/pdf",
            "text/csv",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "image/gif",
            "application/x-rar-compressed",
            "application/x-tar",
            "application/x-7z-compressed",
            "application/xhtml+xml",
            "application/zip"
    );

    private final FileStorageStrategy fileStorageStrategy;
    private final Logger logger;

    @Autowired
    public FileTransferController(FileStorageStrategy fileStorageStrategy, Logger logger) {
        this.fileStorageStrategy = fileStorageStrategy;
        this.logger = logger;
    }

    /**
     * Uploads the specified file to the file transfer service. The file is checked for valid MIME type and size
     * limits, and an appropriate response is returned containing the ID of the uploaded file or an error message.
     *
     * @param file the file to upload
     * @return a ResponseEntity containing the ID of the uploaded file or an error message
     */
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            byte[] data = file.getBytes();
            String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("");
            String mimeType = file.getContentType();
            int size = (int) file.getSize();
            String extension = getFileExtension(fileName);
            if (ALLOWED_MIME_TYPES.contains(mimeType)) {
                FileApi fileApi = new FileApi(fileName, data, mimeType, size, extension);
                String fileId = fileStorageStrategy.save(fileApi);
                logger.infoContext(fileId, "Created file", Map.of("id", fileId));
                return ResponseEntity.status(HttpStatus.CREATED).body(fileId);
            } else {
                logger.error("Unable to upload file as it has an invalid mime type",
                        Map.of("mime type",
                                mimeType != null ? mimeType : "No Mime type",
                                "file name",
                                fileName));
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Unsupported file type");
            }
        } catch (IOException e) {
            logger.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to upload file");
        }
    }

    /**
     * Gets the file extension of the specified file name. For example, if the file name is "document.pdf",
     * the file extension returned is "pdf". If the file name does not contain a file extension, an empty string
     * is returned.
     *
     * @param fileName the name of the file
     * @return the file extension
     */
    private String getFileExtension(String fileName) {
        String[] parts = fileName.split("\\.");
        if (parts.length > 1) {
            return parts[parts.length - 1];
        } else {
            return "";
        }
    }


    /**
     * Handles the request to retrieve a file from S3
     *
     * @param fileId of remote file
     * @return file data
     */
    @GetMapping(path = "/{fileId}/download")
    public ResponseEntity<byte[]> download(@PathVariable String fileId) {
        Optional<FileDetailsApi> fileDetailsOptional = fileStorageStrategy.getFileDetails(fileId);

        if (fileDetailsOptional.isEmpty()) {
            logger.errorContext(fileId,
                    "No file with id found",
                    null,
                    Map.of("fileId", fileId));
            return ResponseEntity.notFound().build();
        }

        FileDetailsApi fileDetails = fileDetailsOptional.get();

        if (fileDetails.getAvStatusApi() != AvStatusApi.CLEAN) {
            logger.infoContext(fileId,
                    "Request for file denied as AV status is not clean",
                    Map.of("fileId", fileId));
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var maybeFile = fileStorageStrategy.load(fileId, fileDetails);
        if (maybeFile.isEmpty()) {
            // This should be impossible as file details must be present to reach this point.
            // It's just here for completeness
            return ResponseEntity.notFound().build();
        }

        var file = maybeFile.get();
        var data = file.getBody();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getMimeType()));
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(file.getFileName())
                .build());
        headers.setContentLength(data.length);

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    /**
     * Handles the request to retrieve a file's details from S3
     *
     * @param fileId of remote file
     * @return file details data or 404 if the file is not found
     */
    @GetMapping(path = "/{fileId}")
    public ResponseEntity<FileDetailsApi> getFileDetails(@PathVariable String fileId) {
        Optional<FileDetailsApi> fileDetails = fileStorageStrategy.getFileDetails(fileId);

        // Multiline is more readable than single line
        //noinspection OptionalIsPresent
        if (fileDetails.isPresent()) {
            return ResponseEntity.ok(fileDetails.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    /**
     * Handles the request to delete a file from S3
     *
     * @param fileId of remote file
     */
    @DeleteMapping(path = "/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable String fileId) {
        fileStorageStrategy.delete(fileId);
//todo        logger.infoContext(fileId, "Deleted file", Map.of("fileId", fileId));
        return ResponseEntity.ok().build();
    }

}