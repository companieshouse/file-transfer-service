package uk.gov.companieshouse.filetransferservice.model;

import java.io.InputStream;

public class S3File {

    private String fileName;
    private InputStream content;
    private String mimeType;
    private int size;
    private String extension;

}
