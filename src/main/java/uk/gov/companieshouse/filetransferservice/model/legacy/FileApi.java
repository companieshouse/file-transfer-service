package uk.gov.companieshouse.filetransferservice.model.legacy;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Objects;

@Deprecated(since = "4.0.305")
public class FileApi {
    @JsonProperty("file_name")
    private String fileName;

    private byte[] body;

    @JsonProperty("mime_type")
    private String mimeType;

    private int size;

    private String extension;

    public FileApi() {
    }

    public FileApi(final String fileName,
                   final byte[] body,
                   final String mimeType,
                   final int size,
                   final String extension) {
        this.fileName = fileName;
        this.body = body;
        this.mimeType = mimeType;
        this.size = size;
        this.extension = extension;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileApi that = (FileApi) o;
        return size == that.size && fileName.equals(that.fileName) && Arrays.equals(body, that.body) && mimeType.equals(that.mimeType) && Objects.equals(extension, that.extension);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fileName, mimeType, size, extension);
        result = 31 * result + Arrays.hashCode(body);
        return result;
    }

    @Override
    public String toString() {
        return "NewFileApi{" +
                "fileName='" + fileName + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", size=" + size +
                ", extension='" + extension + '\'' +
                '}';
    }
}