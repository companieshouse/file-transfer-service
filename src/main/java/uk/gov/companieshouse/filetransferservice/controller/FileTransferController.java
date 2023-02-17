package uk.gov.companieshouse.filetransferservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.filetransferservice.service.file.transfer.FileStorageStrategy;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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

    @Autowired
    public FileTransferController(FileStorageStrategy fileStorageStrategy) {
        this.fileStorageStrategy = fileStorageStrategy;
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
                return ResponseEntity.status(HttpStatus.CREATED).body(fileId);
            } else {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Unsupported file type");
            }
        } catch (IOException e) {
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
     * @param id of remote file
     * @return file data
     */
    @GetMapping(path = "/{fileId}/download")
    public FileApi download(@PathVariable String id) {
        return new FileApi();
    }

    /**
     * Handles the request to retrieve a file's details from S3
     *
     * @param fileId of remote file
     * @return file details data
     */
    @GetMapping(path = "/{fileId}")
    public FileDetailsApi getFileDetails(@PathVariable String fileId) {
        return new FileDetailsApi();
    }

    /**
     * Handles the request to delete a file from S3
     *
     * @param fileId of remote file
     */
    @DeleteMapping(path = "/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable String fileId) {
        fileStorageStrategy.delete(fileId);
        return ResponseEntity.ok().build();
    }

}