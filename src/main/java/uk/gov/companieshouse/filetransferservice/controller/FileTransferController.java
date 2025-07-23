package uk.gov.companieshouse.filetransferservice.controller;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.filetransfer.AvStatus;
import uk.gov.companieshouse.api.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.api.filetransfer.IdApi;
import uk.gov.companieshouse.filetransferservice.converter.MultipartFileToFileUploadApiConverter;
import uk.gov.companieshouse.filetransferservice.exception.FileNotCleanException;
import uk.gov.companieshouse.filetransferservice.exception.FileNotFoundException;
import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.filetransferservice.model.FileDownloadApi;
import uk.gov.companieshouse.filetransferservice.model.FileUploadApi;
import uk.gov.companieshouse.filetransferservice.model.legacy.FileApi;
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
    private final boolean antiVirusCheckingEnabled;

    public FileTransferController(FileStorageStrategy fileStorageStrategy,
            MultipartFileToFileUploadApiConverter fileUploadConverter,
            MimeTypeValidator mimeTypeValidator,
            FileUploadValidator fileUploadValidator,
            Logger logger,
            @Value("${antivirus.checking.enabled:true}") boolean antiVirusCheckEnabled) {
        this.fileStorageStrategy = fileStorageStrategy;
        this.fileUploadConverter = fileUploadConverter;
        this.mimeTypeValidator = mimeTypeValidator;
        this.fileUploadValidator = fileUploadValidator;
        this.logger = logger;
        this.antiVirusCheckingEnabled = antiVirusCheckEnabled;
    }

    /**
     * Uploads the specified data (JSON payload) to the file transfer service. The uploaded file must be of a valid
     * MIME type and this end-point is only available for legacy clients. This endpoint is deprecated and only used for
     * earlier version of the private-api-sdk-java which did not originally support multipart/form-data.
     *
     * @param file the data to upload, represented as a JSON payload
     * @return a ResponseEntity containing the ID of the uploaded file or an error message
     * @throws InvalidMimeTypeException if the MIME type of the uploaded file is unsupported
     * @throws IOException if an I/O error occurs during the upload process
     */
    @PostMapping(value = {"/", "/upload"}, consumes = "application/json", produces = "application/json")
    @Deprecated(since = "0.2.16", forRemoval = true)
    public ResponseEntity<IdApi> upload(@RequestBody uk.gov.companieshouse.filetransferservice.model.legacy.FileApi file)
            throws InvalidMimeTypeException, IOException {
        logger.trace("upload(json) method called.");

        mimeTypeValidator.validate(file.getMimeType());

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(file.getBody())) {
            FileUploadApi fileUploadApi = new FileUploadApi(file.getFileName(),
                    inputStream, file.getMimeType(), file.getSize(), file.getExtension());

            String fileId = fileStorageStrategy.save(fileUploadApi);
            logger.infoContext(fileId, "File uploaded successfully", new HashMap<>(Map.of(FILE_ID_KEY, fileId)));

            return ResponseEntity.ok(new IdApi(fileId));
        }
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
    public ResponseEntity<FileDetailsApi> get(@PathVariable String fileId)
            throws FileNotFoundException, FileNotCleanException {
        logger.trace(format("getFileDetails(fileId=%s) method called.", fileId));

        FileDetailsApi fileDetails = fileStorageStrategy.getFileDetails(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        return ResponseEntity.ok(fileDetails);
    }

    @GetMapping(path = "/{fileId}/download", produces = APPLICATION_JSON_VALUE)
    @Deprecated(since = "0.2.16", forRemoval = true)
    public ResponseEntity<uk.gov.companieshouse.filetransferservice.model.legacy.FileApi> downloadAsJson(
            @PathVariable String fileId, @RequestParam(defaultValue = "false") boolean bypassAv)
            throws FileNotFoundException, FileNotCleanException, IOException {
        logger.trace(format("downloadAsJson(fileId=%s, bypassAv=%s) method called.", fileId, bypassAv));

        FileDetailsApi fileDetailsApi = get(fileId).getBody();
        Resource fileResource = download(fileId, bypassAv).getBody();

        if (fileDetailsApi == null || fileResource == null) {
            throw new FileNotFoundException(fileId);
        }

        FileApi fileApi = getFileApi(fileDetailsApi, fileResource);

        return ResponseEntity.ok(fileApi);
    }

    @GetMapping(path = "/{fileId}/downloadbinary")
    @Deprecated(since = "0.2.16", forRemoval = true)
    public ResponseEntity<byte[]> downloadAsBinary(@PathVariable String fileId, @RequestParam(defaultValue = "false") boolean bypassAv)
            throws FileNotFoundException, FileNotCleanException, IOException {
        logger.trace(format("downloadAsBinary(fileId=%s, bypassAv=%s) method called.", fileId, bypassAv));

        FileDetailsApi fileDetailsApi = get(fileId).getBody();
        logger.info(format("Download binary file with details: %s", fileDetailsApi));

        checkAntiVirusStatus(fileDetailsApi, bypassAv);

        var file = downloadAsJson(fileId, bypassAv).getBody();
        var data = file.getBody();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getMimeType()));
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(file.getFileName())
                .build());
        headers.setContentLength(data.length);

        logger.info(format("Generating response for file download with headers: %s", headers));

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
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

    @SuppressWarnings("deprecation")
    private FileApi getFileApi(final FileDetailsApi fileDetailsApi, final Resource fileResource) throws IOException {
        logger.trace(format("getFileApi(fileId=%s) method called.", fileDetailsApi.getId()));

        String originalFilename = fileDetailsApi.getName();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);

        return new FileApi(originalFilename, fileResource.getContentAsByteArray(), fileDetailsApi.getContentType(),
                fileDetailsApi.getSize().intValue(), fileExtension);
    }

    private void checkAntiVirusStatus(final FileDetailsApi fileDetails, boolean bypassAV) throws FileNotCleanException {
        logger.trace(format("checkAntiVirusStatus(fileId=%s, bypassAv=%s) method called.", fileDetails.getId(),
                antiVirusCheckingEnabled));

        logger.info(format("AV Checking Enabled: %s, AV Status: %s", antiVirusCheckingEnabled, fileDetails.getAvStatus()));

        // If AV checking is disabled (for integration testing), or the file is being bypassed, skip the AV check
        if(!antiVirusCheckingEnabled || bypassAV) {
            logger.info(format("> Bypassing AV check for fileId: %s", fileDetails.getId()));
            return;
        }

        // If the file is not clean, throw an exception
        if(fileDetails.getAvStatus() != AvStatus.CLEAN) {
            throw new FileNotCleanException(fileDetails.getAvStatus(), fileDetails.getId());
        }
    }
}