package uk.gov.companieshouse.filetransferservice.converter;

import com.amazonaws.services.s3.model.S3Object;
import org.springframework.core.convert.converter.Converter;
import uk.gov.companieshouse.filetransferservice.model.S3File;

public class S3FileConverter implements Converter<S3Object, S3File> {

    @Override
    public S3File convert(final S3Object source) {
        S3File target = new S3File();
        return target;
    }

}
