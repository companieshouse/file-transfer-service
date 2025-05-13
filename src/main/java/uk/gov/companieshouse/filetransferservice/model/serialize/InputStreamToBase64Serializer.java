package uk.gov.companieshouse.filetransferservice.model.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

public class InputStreamToBase64Serializer extends JsonSerializer<InputStream> {

    @Override
    public void serialize(InputStream input, JsonGenerator generator, SerializerProvider provider) throws IOException {
        // Read the InputStream into a byte array
        byte[] byteArray = input.readAllBytes();

        // Encode the byte array to a base64 string
        String base64String = Base64.getEncoder().encodeToString(byteArray);

        // Write the base64 string to the JSON output
        generator.writeString(base64String);
    }

}
