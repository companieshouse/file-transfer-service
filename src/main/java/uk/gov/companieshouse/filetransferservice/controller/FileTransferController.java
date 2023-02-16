package uk.gov.companieshouse.filetransferservice.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.companieshouse.api.model.efs.submissions.FileDetailApi;
import uk.gov.companieshouse.api.model.filetransfer.FileApi;

@Controller
@RequestMapping("/files")
public class FileTransferController {
    /**
     * Handles the request to save a file to S3
     *
     * @param fileApi the file data
     * @return id of remote file
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public String save(@ModelAttribute FileApi fileApi) {
        return null;
    }

    /**
     * Handles the request to retrieve a file from S3
     *
     * @param id of remote file
     * @return file data
     */
    @GetMapping("/{id}")
    public FileApi load(@PathVariable String id) {
        return null;
    }

    /**
     * Handles the request to retrieve file details from S3
     *
     * @param id of remote file
     * @return file details data
     */
    @GetMapping("/{id}")
    public FileDetailApi getFileDetails(@PathVariable String id) {
        return null;
    }

    /**
     * Handles the request to delete a file from S3
     *
     * @param id of remote file
     */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable String id) {
        return null;
    }
}
