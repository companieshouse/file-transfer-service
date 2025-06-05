package uk.gov.companieshouse.filetransferservice.model.legacy;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

@Deprecated(since = "4.0.305")
public class FileDetailsApi {
    private String id;

    @JsonProperty("av_timestamp")
    private String avTimestamp;

    @JsonProperty("av_status")
    private AvStatusApi avStatus;

    @JsonProperty("content_type")
    private String contentType;

    private long size;

    private String name;

    @JsonProperty("created_on")
    private String createdOn;

    private FileLinksApi links;

    public FileDetailsApi() {
    }

    public FileDetailsApi(final String id,
                          final String avTimestamp,
                          final AvStatusApi avStatus,
                          final String contentType,
                          final long size,
                          final String name,
                          final String createdOn,
                          final FileLinksApi links) {
        this.id = id;
        this.avTimestamp = avTimestamp;
        this.avStatus = avStatus;
        this.contentType = contentType;
        this.size = size;
        this.name = name;
        this.createdOn = createdOn;
        this.links = links;
    }

    public String getId() {
        return id;
    }

    public String getAvTimestamp() {
        return avTimestamp;
    }

    public AvStatusApi getAvStatusApi() {
        return avStatus;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public FileLinksApi getLinks() {
        return links;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileDetailsApi that = (FileDetailsApi) o;
        return size == that.size && Objects.equals(id, that.id) && Objects.equals(avTimestamp, that.avTimestamp) && avStatus == that.avStatus && Objects.equals(contentType, that.contentType) && Objects.equals(name, that.name) && Objects.equals(createdOn, that.createdOn) && Objects.equals(links, that.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, avTimestamp, avStatus, contentType, size, name, createdOn, links);
    }

    @Override
    public String toString() {
        return "FileDetails{" +
                "id='" + id + '\'' +
                ", avTimestamp='" + avTimestamp + '\'' +
                ", avStatus=" + avStatus +
                ", contentType='" + contentType + '\'' +
                ", size=" + size +
                ", name='" + name + '\'' +
                ", createdOn='" + createdOn + '\'' +
                ", links=" + links +
                '}';
    }
}
