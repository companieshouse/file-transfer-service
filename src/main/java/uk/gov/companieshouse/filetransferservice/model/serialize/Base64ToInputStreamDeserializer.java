package uk.gov.companieshouse.filetransferservice.model.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

public class Base64ToInputStreamDeserializer extends JsonDeserializer<InputStream> {

    @Override
    public InputStream deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        // Get the base64 encoded string from the JSON field
        String base64String = parser.getText();

        // Decode the base64 string to a byte array
        byte[] decodedBytes = Base64.getDecoder().decode(base64String);

        // Convert the byte array to an InputStream
        return new ByteArrayInputStream(decodedBytes);
    }

}