package uk.gov.companieshouse.filetransferservice.model.legacy;

import java.util.Objects;

@Deprecated(since = "4.0.305")
public class IdApi {
    private String id;

    public IdApi() {
    }

    public IdApi(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdApi idApi = (IdApi) o;
        return Objects.equals(id, idApi.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "IdApi{" +
                "id='" + id + '\'' +
                '}';
    }
}
