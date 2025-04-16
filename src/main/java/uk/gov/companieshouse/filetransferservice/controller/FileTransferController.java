package uk.gov.companieshouse.filetransferservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.api.model.filetransfer.IdApi;
import uk.gov.companieshouse.filetransferservice.converter.MultipartFileToFileApiConverter;
import uk.gov.companieshouse.filetransferservice.exception.FileNotCleanException;
import uk.gov.companieshouse.filetransferservice.exception.FileNotFoundException;
import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.filetransferservice.service.storage.FileStorageStrategy;
import uk.gov.companieshouse.filetransferservice.validation.UploadedFileValidator;
import uk.gov.companieshouse.logging.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
@RequestMapping(path = "${service.path.prefix}")
public class FileTransferController {

    public static final String FILE_ID_KEY = "fileId";

    private final FileStorageStrategy fileStorageStrategy;
    private final Logger logger;
    private final MultipartFileToFileApiConverter fileConverter;
    private final UploadedFileValidator fileValidator;

    @Autowired
    public FileTransferController(FileStorageStrategy fileStorageStrategy,
                                  Logger logger,
                                  MultipartFileToFileApiConverter fileConverter,
                                  UploadedFileValidator fileValidator) {

        this.fileStorageStrategy = fileStorageStrategy;
        this.logger = logger;
        this.fileConverter = fileConverter;
        this.fileValidator = fileValidator;
    }

    /**
     * Uploads the specified file to the file transfer service. The uploaded file must be of a valid MIME type and
     * within size limits. If the upload is successful, the ID of the uploaded file is returned in a ResponseEntity.
     * Otherwise, an error message is returned.
     *
     * @param uploadedFile the file to upload
     * @return a ResponseEntity containing the ID of the uploaded file or an error message
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<IdApi> upload(@RequestParam(value = "file") MultipartFile uploadedFile) throws IOException, InvalidMimeTypeException {
        logger.trace("upload(file) method called.");

        FileApi file = fileConverter.convert(uploadedFile);

        return uploadJson(file);
    }

    /**
     * Uploads a file represented as a JSON object to the file transfer service. The uploaded file must be of a valid
     * MIME type and within size limits. If the upload is successful, the ID of the uploaded file is returned in a
     * ResponseEntity. Otherwise, an error message is returned.
     *
     * @param file a JSON object representing the file to upload
     * @return a ResponseEntity containing the ID of the uploaded file or an error message
     */
    @PostMapping(value = "/upload", consumes = "application/json")
    public ResponseEntity<IdApi> uploadJson(@RequestBody FileApi file) throws InvalidMimeTypeException {
        logger.debug("upload(json) method called.");

        fileValidator.validate(file);
        String fileId = fileStorageStrategy.save(file);

        return ResponseEntity.ok(new IdApi(fileId));
    }

    /**
     * Handles the request to retrieve a file from S3 in Json format
     *
     * @param fileId of remote file
     * @return file data
     */
    @GetMapping(path = "/{fileId}/download", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<FileApi> downloadAsJson(@PathVariable String fileId) throws FileNotFoundException, FileNotCleanException {
        FileApi fileApi = getFileApi(fileId, false);
        return ResponseEntity.ok(fileApi);
    }

    /**
     * Handles the request to retrieve a file from S3
     *
     * @param fileId of remote file
     * @return file data
     */
    @GetMapping(path = "/{fileId}/downloadbinary")
    public ResponseEntity<byte[]> downloadBinary(@PathVariable String fileId, @RequestParam(defaultValue = "false") boolean bypassAv) throws FileNotFoundException, FileNotCleanException {
        FileApi file = getFileApi(fileId, bypassAv);

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
    }

    /**
     * Handles the request to retrieve a file's details from S3
     *
     * @param fileId of remote file
     * @return file details data or 404 if the file is not found
     */
    @GetMapping(path = "/{fileId}")
    public ResponseEntity<FileDetailsApi> getFileDetails(@PathVariable String fileId) throws FileNotFoundException {
        Optional<FileDetailsApi> fileDetails = fileStorageStrategy.getFileDetails(fileId);

        if (fileDetails.isPresent()) {
            return ResponseEntity.ok(fileDetails.get());
        } else {
            throw new FileNotFoundException(fileId);
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
        logger.infoContext(fileId, "Deleted file", new HashMap<>(Map.of(FILE_ID_KEY, fileId)));
        return ResponseEntity.noContent().build();
    }

    private FileApi getFileApi(String fileId, boolean bypassAv) throws FileNotFoundException, FileNotCleanException {
        Supplier<FileNotFoundException> notFoundException = () -> new FileNotFoundException(fileId);
        FileDetailsApi fileDetails = fileStorageStrategy
                .getFileDetails(fileId)
                .orElseThrow(notFoundException);

        logger.debug(String.format("The retrieved details for file %s are: %s", fileId, fileDetails.toString()));

        if (!bypassAv && fileDetails.getAvStatusApi() != AvStatusApi.CLEAN) {
            throw new FileNotCleanException(fileDetails.getAvStatusApi(), fileId);
        }

        return fileStorageStrategy.load(fileId, fileDetails).orElseThrow(notFoundException);
    }

}