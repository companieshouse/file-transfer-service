package uk.gov.companieshouse.filetransferservice.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class FileUploadValidatorTest {

    @Mock
    private Logger logger;

    @InjectMocks
    private FileUploadValidator underTest;

    @Test
    void testFileUploadValid() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        underTest.validate(file);

        verify(logger, times(1)).debug(anyString());
    }

    @Test
    void testFileUploadEmpty() {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "".getBytes());

        IOException raisedException = assertThrows(IOException.class, () -> underTest.validate(file));

        verify(logger, times(1)).debug(anyString());

        assertThat(raisedException.getMessage(), is("Uploaded file has no content: test.txt"));
    }
}
