package uk.gov.companieshouse.filetransferservice.model;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.filetransferservice.model.serialize.InputStreamToBase64Serializer;

@ExtendWith(MockitoExtension.class)
public class InputStreamToBase64SerializerTest {

    @InjectMocks
    private InputStreamToBase64Serializer underTest;

    @Test
    void testSerializeInputStreamToBase64() throws IOException {
        JsonGenerator generator = mock(JsonGenerator.class);
        doNothing().when(generator).writeString(anyString());

        underTest.serialize(new ByteArrayInputStream("Hello, World!".getBytes()), generator, null);

        verify(generator, times(1)).writeString(anyString());
    }
}
