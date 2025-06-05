package uk.gov.companieshouse.filetransferservice.model.legacy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@Deprecated(since = "4.0.305")
public enum AvStatusApi {
    INFECTED("infected"),
    CLEAN("clean"),
    NOT_SCANNED("not-scanned");

    final String statusString;

    AvStatusApi(final String statusString) {
        this.statusString = statusString;
    }

    @JsonCreator
    public static AvStatusApi create(final String val) {
        AvStatusApi[] units = AvStatusApi.values();
        for (AvStatusApi unit : units) {
            if (unit.getValue().equals(val)) {
                return unit;
            }
        }

        throw new IllegalArgumentException();
    }

    @JsonValue
    public String getValue() {
        return statusString;
    }
}

