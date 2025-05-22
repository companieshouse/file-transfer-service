package uk.gov.companieshouse.filetransferservice;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;

@ExtendWith(MockitoExtension.class)
class FileTransferServiceApplicationTest {

    @Test
    void testMainRunsSpringApplication() {
        try (var mockedSpringApp = mockStatic(SpringApplication.class)) {
            FileTransferServiceApplication.main(new String[]{});
            mockedSpringApp.verify(() -> SpringApplication.run(FileTransferServiceApplication.class, new String[]{}));
        }
    }
}