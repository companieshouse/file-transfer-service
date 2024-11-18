package uk.gov.companieshouse.filetransferservice.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.error.ApiErrorResponse;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.api.model.filetransfer.IdApi;
import uk.gov.companieshouse.filetransferservice.converter.MultipartFileToFileApiConverter;
import uk.gov.companieshouse.filetransferservice.errors.ErrorResponseBuilder;
import uk.gov.companieshouse.filetransferservice.exception.FileNotCleanException;
import uk.gov.companieshouse.filetransferservice.exception.FileNotFoundException;
import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.filetransferservice.service.file.transfer.FileStorageStrategy;
import uk.gov.companieshouse.filetransferservice.validation.UploadedFileValidator;
import uk.gov.companieshouse.logging.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

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
        // Get maximum size of heap in bytes. The heap cannot grow beyond this size.// Any attempt will result in an OutOfMemoryException.
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        long heapSize = Runtime.getRuntime().totalMemory();

        // Get amount of free memory within the heap in bytes. This size will increase // after garbage collection and decrease as new objects are created.
        long heapFreeSize = Runtime.getRuntime().freeMemory();

        logger.error("heap size:."+ formatSize(heapSize));
        logger.error("heap max size: " + formatSize(heapMaxSize));
        logger.error("heap free size: " + formatSize(heapFreeSize));
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
    public ResponseEntity<IdApi> upload(
            @RequestParam(value = "file") MultipartFile uploadedFile) throws IOException, InvalidMimeTypeException {

        FileApi file = fileConverter.convert(uploadedFile);

        return uploadJson(file);
    }

    @ExceptionHandler({IOException.class})
    ResponseEntity<ApiErrorResponse> handleIOException(IOException e) {
        logger.error("Error uploading file IOException when reading file contents.", e);

        return ErrorResponseBuilder
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .withError("Unable to upload file",
                        "getBytes",
                        "method",
                        "upload").build();
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
    public ResponseEntity<IdApi> uploadJson(
            @RequestBody FileApi file) throws InvalidMimeTypeException {

        fileValidator.validate(file);
        String fileId = fileStorageStrategy.save(file);
        return ResponseEntity.ok(new IdApi(fileId));
    }

    @ExceptionHandler({InvalidMimeTypeException.class})
    ResponseEntity<ApiErrorResponse> handleInvalidMimeType(InvalidMimeTypeException e) {
        logger.error("File was uploaded with an invalid mime type", e);
        return ErrorResponseBuilder
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .withError("Invalid MIME type",
                        "file",
                        "body_parameter",
                        "validation"
                )
                .build();
    }

    /**
     * Handles the request to retrieve a file from S3 in Json format
     *
     * @param fileId of remote file
     * @return file data
     */
    @GetMapping(path = "/{fileId}/download", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<FileApi> downloadAsJson(@PathVariable String fileId) throws FileNotFoundException, FileNotCleanException {
        return ResponseEntity.ok(getFileApi(fileId, false));
    }

    /**
     * Handles the request to retrieve a file from S3
     *
     * @param fileId of remote file
     * @return file data
     */
    @GetMapping(path = "/{fileId}/download")
    public ResponseEntity<byte[]> download(@PathVariable String fileId, @RequestParam(defaultValue = "false") boolean bypassAv) throws FileNotFoundException, FileNotCleanException {

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

    @ExceptionHandler({FileNotCleanException.class})
    ResponseEntity<ApiErrorResponse> handleFileNotCleanException(FileNotCleanException e) {
        String fileId = e.getFileId();
        Map<String, Object> loggedVars = new HashMap<>();
        loggedVars.put(FILE_ID_KEY, fileId);
        logger.infoContext(fileId,
                "Request for file denied as AV status is not clean",
                loggedVars);
        return ErrorResponseBuilder
                .status(HttpStatus.FORBIDDEN)
                .withError("File retrieval denied due to unclean antivirus status",
                        fileId,
                        FILE_ID_KEY,
                        "retrieval")
                .build();
    }

    @ExceptionHandler({FileNotFoundException.class})
    ResponseEntity<ApiErrorResponse> handleFileNotFoundException(FileNotFoundException e) {
        String fileId = e.getFileId();

        Map<String, Object> loggedVars = new HashMap<>();
        loggedVars.put(FILE_ID_KEY, fileId);
        logger.errorContext(fileId, "Unable to find file with ID", e, loggedVars);
        return ErrorResponseBuilder
                .status(HttpStatus.NOT_FOUND)
                .withError(String.format("Unable to find file with id [%s]", fileId),
                        fileId,
                        "jsonPath",
                        "retrieval")
                .build();
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
        Supplier<FileNotFoundException> notFoundException = () ->
                new FileNotFoundException(fileId);
        FileDetailsApi fileDetails = fileStorageStrategy
                .getFileDetails(fileId)
                .orElseThrow(notFoundException);

        if (!bypassAv && fileDetails.getAvStatusApi() != AvStatusApi.CLEAN) {
            throw new FileNotCleanException(fileDetails.getAvStatusApi(), fileId);
        }

        return fileStorageStrategy.load(fileId, fileDetails).orElseThrow(notFoundException);
    }

    public static String formatSize(long v) {
        if (v < 1024) return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.1f %sB", (double)v / (1L << (z*10)), " KMGTPE".charAt(z));
    }
}