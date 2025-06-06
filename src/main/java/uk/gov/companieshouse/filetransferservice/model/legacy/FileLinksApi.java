package uk.gov.companieshouse.filetransferservice.model.legacy;

import java.util.Objects;

@Deprecated(since = "4.0.305")
public class FileLinksApi {
    private String download;
    private String self;

    public FileLinksApi() {
    }

    public FileLinksApi(final String download, final String self) {
        this.download = download;
        this.self = self;
    }

    public String getDownload() {
        return download;
    }

    public String getSelf() {
        return self;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        FileLinksApi that = (FileLinksApi) obj;
        return Objects.equals(this.download, that.download) &&
                Objects.equals(this.self, that.self);
    }

    @Override
    public int hashCode() {
        return Objects.hash(download, self);
    }

    @Override
    public String toString() {
        return "FileLinks[" +
                "download=" + download + ", " +
                "self=" + self + ']';
    }
}
