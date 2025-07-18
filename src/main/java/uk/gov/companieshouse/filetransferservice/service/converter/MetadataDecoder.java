package uk.gov.companieshouse.filetransferservice.service.converter;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class MetadataDecoder implements Converter<String, String> {

    @Override
    public String convert(final String source) {
        try {
            // Check if the input is empty, or already URL encoded?
            if(isEmpty(source) || !isUrlEncoded(source)) {
                return source;
            }

            // Input is NOT URL encoded, so encode it.
            return URLDecoder.decode(source, StandardCharsets.UTF_8);

        } catch (Exception e) {
            // Something went wrong during encoding, return the original source.
            return source;
        }
    }

    private boolean isUrlEncoded(final String input) {
        // Decode and re-encode, then compare
        String decoded = URLDecoder.decode(input, StandardCharsets.UTF_8);
        String encoded = URLEncoder.encode(decoded, StandardCharsets.UTF_8).replace("+", "%20");

        // Normalize input for comparison
        String normalizedInput = input.replace("+", "%20");

        return normalizedInput.equals(encoded);
    }

}
