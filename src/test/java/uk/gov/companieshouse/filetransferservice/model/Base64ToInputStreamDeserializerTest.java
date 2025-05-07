package uk.gov.companieshouse.filetransferservice.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.filetransferservice.model.serialize.Base64ToInputStreamDeserializer;

@ExtendWith(MockitoExtension.class)
public class Base64ToInputStreamDeserializerTest {

    @InjectMocks
    private Base64ToInputStreamDeserializer underTest;

    @Test
    public void testDeserializeBase64ToInputStream() throws IOException {
        String originalContent = "File content text!";

        JsonParser parser = mock(JsonParser.class);
        when(parser.getText()).thenReturn(new String(Base64.getEncoder().encode(originalContent.getBytes())));

        DeserializationContext context = mock(DeserializationContext.class);

        InputStream inputStream = underTest.deserialize(parser, context);
        assertNotNull(inputStream);

        verify(parser, times(1)).getText();
        verifyNoInteractions(context);

        String responseContent = new String(inputStream.readAllBytes());
        assertThat(responseContent, is(originalContent));
    }
}
