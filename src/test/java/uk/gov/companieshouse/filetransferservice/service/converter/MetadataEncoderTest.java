package uk.gov.companieshouse.filetransferservice.service.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MetadataEncoderTest {

    private MetadataEncoder underTest;

    @BeforeEach
    public void setUp() {
        underTest = new MetadataEncoder();
    }

    @Test
    @DisplayName("Test conversion of non-encoded filename to URL encoded format")
    void givenNonEncodedFilename_whenConvertCalled_thenFilenameEncoded() {
        String input = "test file.txt";
        String expectedOutput = "test%20file.txt";

        String result = underTest.convert(input);

        assertThat(result, is(expectedOutput));
    }

    @Test
    @DisplayName("Test conversion of pre-encoded filename to URL encoded format")
    void givenPreEncodedFilename_whenConvertCalled_thenFilenameEncoded() {
        String input = "test%20file.txt";
        String expectedOutput = "test%20file.txt";

        String result = underTest.convert(input);

        assertThat(result, is(expectedOutput));
    }

    @Test
    @DisplayName("Test conversion of irregular filename to URL encoded format")
    void givenIrregularFilename_whenConvertCalled_thenFilenameEncoded() {
        String input = "CIC_D’Artagnan House C.I.C._31102024.zip";
        String expectedOutput = "CIC_D%E2%80%99Artagnan%20House%20C.I.C._31102024.zip";

        String result = underTest.convert(input);

        assertThat(result, is(expectedOutput));
    }

    @Test
    @DisplayName("Test conversion of malformed filename to URL encoded format")
    void givenMalformedFilename_whenConvertCalled_thenFilenameEncoded() {
        String input = "abc%2.zip";
        String expectedOutput = "abc%2.zip";

        String result = underTest.convert(input);

        assertThat(result, is(expectedOutput));
    }

}
