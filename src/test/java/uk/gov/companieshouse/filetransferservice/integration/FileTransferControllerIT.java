package uk.gov.companieshouse.filetransferservice.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.api.model.filetransfer.IdApi;
import uk.gov.companieshouse.filetransferservice.config.TestContainersConfiguration;

@Import(TestContainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FileTransferControllerIT {

    private static final String SERVICE_PATH = "/file-transfer-service";

    private static final String BUCKET_NAME = "fts-bucket";
    private static final Pattern FILENAME_PATTERN = Pattern.compile(".*filename=\"(.+?)\".*");

    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3") )
            .withServices(LocalStackContainer.Service.S3)
            .withEnv("DEFAULT_REGION", "eu-west-2")
            .withEnv("AWS_ACCESS_KEY_ID", "noop")
            .withEnv("AWS_SECRET_ACCESS_KEY", "noop");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        localStack.start();
        localStack.execInContainer("awslocal", "s3api", "create-bucket", "--bucket", BUCKET_NAME);
    }

    @DynamicPropertySource
    static void overrideProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.s3.endpoint", () -> localStack.getEndpointOverride(S3).toString());

        registry.add("aws.region.static", localStack::getRegion);
        registry.add("aws.accessKeyId", localStack::getAccessKey);
        registry.add("aws.secretAccessKey", localStack::getSecretKey);
        registry.add("aws.bucketName", () -> BUCKET_NAME);
    }

    @Test
    void shouldUpLoadAndDownloadTestFileToS3() throws Exception {
        try (FileInputStream is = new FileInputStream(ResourceUtils.getFile("classpath:large-file.pdf"))) {

            // Upload
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "large-file.pdf",
                    "application/pdf",
                    is.readAllBytes());

            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
            HttpHeaders headers = new HttpHeaders();
            headers.add("ERIC-Identity", "someone");
            headers.add("ERIC-Identity-Type", "key");
            headers.add("ERIC-Authorised-Key-Roles", "*");

            MockHttpServletResponse uploadResult = mockMvc.perform(multipart("%s/".formatted(SERVICE_PATH))
                            .file(file)
                            .headers(headers))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse();

            IdApi fileId = objectMapper.readValue(uploadResult.getContentAsString(), IdApi.class);

            MockHttpServletResponse downloadResult = mockMvc.perform(get("%s/%s/download".formatted(SERVICE_PATH, fileId.getId()))
                            .param("bypassAv", "true")
                            .headers(headers))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse();

            byte[] content = downloadResult.getContentAsByteArray();
            String mimeType = downloadResult.getContentType();
            Matcher matcher = FILENAME_PATTERN.matcher(downloadResult.getHeader(HttpHeaders.CONTENT_DISPOSITION));
            String filename = matcher.find() ? matcher.group(1) : "unknown";
            File downloadedFile = new File(filename);

            try (FileOutputStream os = new FileOutputStream(downloadedFile)) {
                os.write(content);
            }

            System.out.printf("Download complete for %s%n", downloadedFile.getCanonicalPath());
            System.out.printf("Bytes: %d%n", downloadResult.getContentLengthLong());
            System.out.printf("Mime-Type %s%n", mimeType);
        }
    }
}
