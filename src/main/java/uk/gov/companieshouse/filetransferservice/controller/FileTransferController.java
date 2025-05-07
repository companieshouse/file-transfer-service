package uk.gov.companieshouse.filetransferservice.controller;

import static java.lang.String.format;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.error.ApiErrorResponse;
import uk.gov.companieshouse.api.model.filetransfer.AvStatusApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.api.model.filetransfer.IdApi;
import uk.gov.companieshouse.filetransferservice.converter.MultipartFileToFileUploadApiConverter;
import uk.gov.companieshouse.filetransferservice.errors.ErrorResponseBuilder;
import uk.gov.companieshouse.filetransferservice.exception.FileNotCleanException;
import uk.gov.companieshouse.filetransferservice.exception.FileNotFoundException;
import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.filetransferservice.model.FileDownloadApi;
import uk.gov.companieshouse.filetransferservice.model.FileUploadApi;
import uk.gov.companieshouse.filetransferservice.service.storage.FileStorageStrategy;
import uk.gov.companieshouse.filetransferservice.validation.FileUploadValidator;
import uk.gov.companieshouse.filetransferservice.validation.MimeTypeValidator;
import uk.gov.companieshouse.logging.Logger;

@Controller
@RequestMapping(path = "${service.path.prefix}")
public class FileTransferController {

    public static final String FILE_ID_KEY = "fileId";

    private final FileStorageStrategy fileStorageStrategy;
    private final MultipartFileToFileUploadApiConverter fileUploadConverter;
    private final MimeTypeValidator mimeTypeValidator;
    private final FileUploadValidator fileUploadValidator;
    private final Logger logger;

    @Autowired
    public FileTransferController(FileStorageStrategy fileStorageStrategy,
            MultipartFileToFileUploadApiConverter fileUploadConverter,
            MimeTypeValidator mimeTypeValidator,
            FileUploadValidator fileUploadValidator,
            Logger logger) {
        this.fileStorageStrategy = fileStorageStrategy;
        this.fileUploadConverter = fileUploadConverter;
        this.mimeTypeValidator = mimeTypeValidator;
        this.fileUploadValidator = fileUploadValidator;
        this.logger = logger;
    }

    /**
     * Uploads the specified file to the file transfer service. The uploaded file must be of a valid MIME type and
     * within size limits. If the upload is successful, the ID of the uploaded file is returned in a ResponseEntity.
     * Otherwise, an error message is returned.
     *
     * @param uploadedFile the file to upload
     * @return a ResponseEntity containing the ID of the uploaded file or an error message
     */
    @PostMapping(value = "/", consumes = "multipart/form-data")
    public ResponseEntity<IdApi> upload(@RequestParam(value = "file") MultipartFile uploadedFile)
            throws InvalidMimeTypeException, IOException {
        logger.trace("upload(file) method called.");

        mimeTypeValidator.validate(uploadedFile.getContentType());
        fileUploadValidator.validate(uploadedFile);

        FileUploadApi file = fileUploadConverter.convert(uploadedFile);
        String fileId = fileStorageStrategy.save(file);

        return ResponseEntity.ok(new IdApi(fileId));
    }

    /**
     * Get the file details for this object, so we can inspect the contents only.
     *
     * @param fileId The fileId of the resource to be retrieved.
     * @return The details of the file resource that was retrieved.
     */
    @GetMapping(path = "/{fileId}")
    public ResponseEntity<FileDetailsApi> getFileDetails(@PathVariable String fileId)
            throws FileNotFoundException, FileNotCleanException {

        logger.trace(format("getFileDetails(fileId=%s) method called.", fileId));

        FileDetailsApi fileDetails = fileStorageStrategy.getFileDetails(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        checkAntiVirusStatus(fileDetails, false);

        return ResponseEntity.ok(fileDetails);
    }

    @GetMapping(path = "/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable String fileId, @RequestParam(defaultValue = "false") boolean bypassAv)
            throws FileNotFoundException, FileNotCleanException {

        logger.trace(format("download(fileId=%s) method called.", fileId));

        FileDetailsApi fileDetailsApi = fileStorageStrategy.getFileDetails(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        checkAntiVirusStatus(fileDetailsApi, bypassAv);

        FileDownloadApi fileDownload = fileStorageStrategy.load(fileDetailsApi)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(fileDownload.getMimeType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileDownload.getFileName()).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(new BufferedInputStream(fileDownload.getBody())));
    }

    /**
     * Handles the request to delete a file from S3
     *
     * @param fileId of remote file
     */
    @DeleteMapping(path = "/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable String fileId) throws FileNotFoundException{
        logger.trace(format("deletedFile(fileId=%s) method called.", fileId));

        FileDetailsApi fileDetails = fileStorageStrategy.getFileDetails(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        fileStorageStrategy.delete(fileDetails.getId());

        logger.infoContext(fileId, "Deleted file", new HashMap<>(Map.of(FILE_ID_KEY, fileId)));

        return ResponseEntity.noContent().build();
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

    private void checkAntiVirusStatus(final FileDetailsApi fileDetails,  boolean bypassAv) throws FileNotCleanException {
        logger.trace(format("getFileApi(fileId=%s, bypassAv=%s) method called.", fileDetails.getId(), bypassAv));

        if (!bypassAv && fileDetails.getAvStatusApi() != AvStatusApi.CLEAN) {
            throw new FileNotCleanException(fileDetails.getAvStatusApi(), fileDetails.getId());
        }
    }
}