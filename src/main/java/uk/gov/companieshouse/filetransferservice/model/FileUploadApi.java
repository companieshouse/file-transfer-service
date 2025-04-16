package uk.gov.companieshouse.filetransferservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.InputStream;
import java.util.Objects;
import uk.gov.companieshouse.filetransferservice.model.format.Base64ToInputStreamDeserializer;

public class FileUploadApi {

    @JsonProperty("file_name")
    private String fileName;

    @JsonDeserialize(using = Base64ToInputStreamDeserializer.class)
    @JsonProperty("body")
    private InputStream body;

    @JsonProperty("mime_type")
    private String mimeType;

    @JsonProperty("size")
    private int size;

    @JsonProperty("extension")
    private String extension;

    public FileUploadApi() {
        super();
    }

    public FileUploadApi(String fileName, InputStream body, String mimeType, int size, String extension) {
        this.fileName = fileName;
        this.body = body;
        this.mimeType = mimeType;
        this.size = size;
        this.extension = extension;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public InputStream getBody() {
        return this.body;
    }

    public void setBody(InputStream body) {
        this.body = body;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getExtension() {
        return this.extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            FileUploadApi that = (FileUploadApi)o;
            return this.size == that.size && this.fileName.equals(that.fileName) && this.mimeType.equals(that.mimeType) && Objects.equals(this.extension, that.extension);
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = Objects.hash(this.fileName, this.mimeType, this.size, this.extension);
        return result;
    }

    public String toString() {
        return "FileUploadApi{fileName='" + this.fileName + '\'' + ", mimeType='" + this.mimeType + '\'' + ", size=" + this.size + ", extension='" + this.extension + '\'' + '}';
    }

}
