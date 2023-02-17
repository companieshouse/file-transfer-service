package uk.gov.companieshouse.filetransferservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.api.model.efs.submissions.FileDetailApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;

@Controller
@RequestMapping(path = "/files")
public class FileTransferController {
    /**
     * Handles the request to save a file to S3
     *
     * @param file the file data
     * @return id of remote file
     */
    @PostMapping
    public String save(@RequestParam("file") MultipartFile file) {
        return "123";
    }

    /**
     * Handles the request to retrieve a file from S3
     *
     * @param id of remote file
     * @return file data
     */
    @GetMapping(path = "/{fileId}")
    public FileApi load(@PathVariable String id) {
        return new FileApi();
    }

    /**
     * Handles the request to retrieve a file's details from S3
     *
     * @param fileId of remote file
     * @return file details data
     */
    @GetMapping(path = "/check/{fileId}")
    public FileDetailApi getFileDetails(@PathVariable String fileId) {
        return new FileDetailApi();
    }

    /**
     * Handles the request to delete a file from S3
     *
     * @param fileId of remote file
     */
    @DeleteMapping(path = "/{fileId}")
    public void delete(@PathVariable String fileId) {
    }
}
