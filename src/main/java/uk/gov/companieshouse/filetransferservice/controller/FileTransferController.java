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
import uk.gov.companieshouse.filetransferservice.converter.MultipartFileToFileApiConverter;
import uk.gov.companieshouse.filetransferservice.exception.FileNotCleanException;
import uk.gov.companieshouse.filetransferservice.exception.FileNotFoundException;
import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.filetransferservice.service.file.transfer.FileStorageStrategy;
import uk.gov.companieshouse.logging.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Controller
@RequestMapping(path = "/files")
public class FileTransferController {
    private final FileStorageStrategy fileStorageStrategy;
    private final Logger logger;
    private final MultipartFileToFileApiConverter fileConverter;

    @Autowired
    public FileTransferController(FileStorageStrategy fileStorageStrategy,
                                  Logger logger,
                                  MultipartFileToFileApiConverter fileConverter) {
        this.fileStorageStrategy = fileStorageStrategy;
        this.logger = logger;
        this.fileConverter = fileConverter;
    }

    /**
     * Uploads the specified file to the file transfer service. The file is checked for valid MIME type and size
     * limits, and an appropriate response is returned containing the ID of the uploaded file or an error message.
     *
     * @param uploadedFile the file to upload
     * @return a ResponseEntity containing the ID of the uploaded file or an error message
     */
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile uploadedFile) {
        try {
            FileApi file = fileConverter.convert(uploadedFile);
            String fileId = fileStorageStrategy.save(file);
            return ResponseEntity.ok(fileId);
        } catch (IOException e) {
            logger.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to upload file");
        } catch (InvalidMimeTypeException e) {
            logger.error("File was uploaded with an invalid mime type", e);
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(e.getMessage());
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
        try {
            Supplier<FileNotFoundException> notFoundException = () ->
                    new FileNotFoundException(fileId);
            FileDetailsApi fileDetails = fileStorageStrategy
                    .getFileDetails(fileId)
                    .orElseThrow(notFoundException);

            if (fileDetails.getAvStatusApi() != AvStatusApi.CLEAN) {
                throw new FileNotCleanException(fileDetails.getAvStatusApi());
            }

            var file = fileStorageStrategy.load(fileId).orElseThrow(notFoundException);
            var data = file.getBody();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(file.getMimeType()));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(file.getFileName())
                    .build());
            headers.setContentLength(data.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(data);
        } catch (FileNotFoundException exception) {
            Map<String, Object> loggedVars = new HashMap<>();
            loggedVars.put("fileId", fileId);
            logger.errorContext(fileId, "Unable to find file with ID", exception, loggedVars);
            return ResponseEntity.notFound().build();
        } catch (FileNotCleanException exception) {
            Map<String, Object> loggedVars = new HashMap<>();
            loggedVars.put("fileId", fileId);
            logger.infoContext(fileId,
                    "Request for file denied as AV status is not clean",
                    loggedVars);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
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
        logger.infoContext(fileId, "Deleted file", new HashMap<>(Map.of("fileId", fileId)));
        return ResponseEntity.ok().build();
    }

}