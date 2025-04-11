package uk.gov.companieshouse.filetransferservice.model;

import java.util.List;

import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.Tag;

public class S3FileMetadata {

    private List<Tag> tags;
    private HeadObjectResponse metadata;


    public S3FileMetadata() {

    }

    public S3FileMetadata(List<Tag> tags, HeadObjectResponse metadata) {
        this.tags = tags;
        this.metadata = metadata;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public HeadObjectResponse getMetadata() {
        return metadata;
    }

    public void setMetadata(HeadObjectResponse metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "S3FileMetadata{" +
                "tags=" + tags +
                ", metadata=" + metadata +
                '}';
    }
}
