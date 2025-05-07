package uk.gov.companieshouse.filetransferservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.InputStream;
import java.util.Objects;

public class FileDownloadApi {

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("body")
    private InputStream body;

    @JsonProperty("mime_type")
    private String mimeType;

    @JsonProperty("size")
    private int size;

    @JsonProperty("extension")
    private String extension;

    public FileDownloadApi() {
    }

    public FileDownloadApi(String fileName, InputStream body, String mimeType, int size, String extension) {
        this.fileName = fileName;
        this.body = body;
        this.mimeType = mimeType;
        this.size = size;
        this.extension = extension;
    }

    public String getFileName() {
        return this.fileName;
    }

    public InputStream getBody() {
        return this.body;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public int getSize() {
        return this.size;
    }

    public String getExtension() {
        return this.extension;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            FileDownloadApi that = (FileDownloadApi)o;
            return this.size == that.size && this.fileName.equals(that.fileName) && this.mimeType.equals(that.mimeType) && Objects.equals(this.extension, that.extension);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(this.fileName, this.mimeType, this.size, this.extension);
    }

    public String toString() {
        return "FileDownloadApi{fileName='" + this.fileName + '\'' + ", mimeType='" + this.mimeType + '\'' + ", size=" + this.size + ", extension='" + this.extension + '\'' + '}';
    }
}
