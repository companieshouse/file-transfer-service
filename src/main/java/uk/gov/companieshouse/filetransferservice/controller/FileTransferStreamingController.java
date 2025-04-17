package uk.gov.companieshouse.filetransferservice.controller;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.api.model.filetransfer.IdApi;
import uk.gov.companieshouse.filetransferservice.converter.MultipartFileToFileUploadApiConverter;
import uk.gov.companieshouse.filetransferservice.exception.FileNotCleanException;
import uk.gov.companieshouse.filetransferservice.exception.FileNotFoundException;
import uk.gov.companieshouse.filetransferservice.exception.InvalidMimeTypeException;
import uk.gov.companieshouse.filetransferservice.model.FileDownloadApi;
import uk.gov.companieshouse.filetransferservice.model.FileUploadApi;
import uk.gov.companieshouse.filetransferservice.service.storage.FileStorageStrategy;
import uk.gov.companieshouse.filetransferservice.validation.MimeTypeValidator;
import uk.gov.companieshouse.logging.Logger;

@Controller
@RequestMapping(path = "${service.path.prefix}")
public class FileTransferStreamingController {

    private final FileStorageStrategy fileStorageStrategy;
    private final MultipartFileToFileUploadApiConverter fileUploadConverter;
    private final MimeTypeValidator mimeTypeValidator;
    private final Logger logger;

    @Autowired
    public FileTransferStreamingController(FileStorageStrategy fileStorageStrategy,
                                           MultipartFileToFileUploadApiConverter fileUploadConverter,
                                           MimeTypeValidator mimeTypeValidator,
                                           Logger logger) {
        this.fileStorageStrategy = fileStorageStrategy;
        this.fileUploadConverter = fileUploadConverter;
        this.mimeTypeValidator = mimeTypeValidator;
        this.logger = logger;
    }

    @PostMapping(value = "/save", consumes = "multipart/form-data")
    public ResponseEntity<IdApi> save(@RequestParam(value = "file") MultipartFile uploadedFile) throws InvalidMimeTypeException {
        logger.trace("save(file) method called.");

        mimeTypeValidator.validate(uploadedFile.getContentType());

        FileUploadApi file = fileUploadConverter.convert(uploadedFile);
        String fileId = fileStorageStrategy.save(file);

        return ResponseEntity.ok(new IdApi(fileId));
    }

    @PostMapping(value = "/save", consumes = "application/json")
    public ResponseEntity<IdApi> save(@RequestBody FileUploadApi uploadedData) throws InvalidMimeTypeException {
        logger.trace("save(data) method called.");

        mimeTypeValidator.validate(uploadedData.getMimeType());

        String fileId = fileStorageStrategy.save(uploadedData);

        return ResponseEntity.ok(new IdApi(fileId));
    }

    @GetMapping(path = "/{fileId}/fetch", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<FileDownloadApi> fetch(@PathVariable String fileId) throws FileNotFoundException, FileNotCleanException {
        logger.trace(format("fetch(fileId=%s) method called.", fileId));

        FileDetailsApi fileDetailsApi = fileStorageStrategy.getFileDetails(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        FileDownloadApi fileDownload = fileStorageStrategy.fetch(fileId, fileDetailsApi)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        return ResponseEntity.ok(fileDownload);
    }

    @GetMapping(path = "/{fileId}/fetch-raw")
    public ResponseEntity<InputStreamResource> fetchRaw(@PathVariable String fileId) throws FileNotFoundException, FileNotCleanException {
        logger.trace(format("fetchRaw(fileId=%s) method called.", fileId));

        FileDetailsApi fileDetailsApi = fileStorageStrategy.getFileDetails(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        FileDownloadApi fileDownload = fileStorageStrategy.fetch(fileId, fileDetailsApi)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(fileDownload.getMimeType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileDownload.getFileName()).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(fileDownload.getBody()));
    }

}
