package uk.gov.companieshouse.filetransferservice.integration;

import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.shaded.com.google.common.net.HttpHeaders.CONTENT_TYPE;

import com.google.api.client.http.HttpHeaders;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ResourceUtils;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.api.filetransfer.AvStatus;
import uk.gov.companieshouse.api.filetransfer.FileApi;
import uk.gov.companieshouse.api.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.api.filetransfer.IdApi;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.filetransfer.FileTransferHttpClient;
import uk.gov.companieshouse.api.handler.filetransfer.InternalFileTransferClient;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.filetransferservice.config.TestContainersConfiguration;

@Import(TestContainersConfiguration.class)
@TestPropertySource(properties = "filetransfer.bypass_av_check:true")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InternalFileTransferClientIT {

    private static final String BUCKET_NAME = "fts-bucket";
    private static final String API_KEY = "chsApiKey";
    private static final Pattern FILENAME_PATTERN = Pattern.compile(".*filename=\"(.+?)\".*");

    @LocalServerPort
    private Integer port;

    private InternalFileTransferClient internalFileTransferClient;
    private IdApi idApi;

    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3"))
            .withServices(LocalStackContainer.Service.S3)
            .withEnv("DEFAULT_REGION", "eu-west-2")
            .withEnv("AWS_ACCESS_KEY_ID", "noop")
            .withEnv("AWS_SECRET_ACCESS_KEY", "noop");

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        localStack.start();
        localStack.execInContainer("awslocal", "s3api", "create-bucket", "--bucket", BUCKET_NAME);
    }
    
    @BeforeEach
    void setUp() throws IOException, URIValidationException {
        internalFileTransferClient = buildInternalFileTransferClient();
        idApi = uploadFile();
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
    void shouldUploadFileAndDownloadAsStream() throws Exception {
        ApiResponse<FileApi> downloadResponse = internalFileTransferClient.privateFileTransferHandler()
                .downloadAsStream(idApi.getId())
                .execute();
        assertEquals(200, downloadResponse.getStatusCode());

        FileApi content = downloadResponse.getData();
        assertEquals("application/pdf", content.getMimeType());
        assertEquals("large-file.pdf", content.getFileName());

        Map<String, Object> metadata = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        metadata.putAll(downloadResponse.getHeaders());

        String mimeType = ((List)metadata.get(CONTENT_TYPE)).getFirst().toString();
        String contentDisposition = Optional.ofNullable(metadata.get(CONTENT_DISPOSITION))
                .orElseThrow(() -> new IllegalArgumentException("Missing Content-Disposition header")).toString();

        Matcher matcher = FILENAME_PATTERN.matcher(contentDisposition);
        String filename = matcher.find() ? matcher.group(1) : "unknown";
        assertEquals("application/pdf", mimeType);
        assertEquals("large-file.pdf", filename);
    }

    @Test
    void shouldUploadFileAndDeleteFileFromS3() throws Exception {
        ApiResponse<Void> deleteResponse = internalFileTransferClient.privateFileTransferHandler()
                .delete(idApi.getId())
                .execute();
        assertEquals(204, deleteResponse.getStatusCode());

        ApiResponse<FileApi> downloadResponse = internalFileTransferClient.privateFileTransferHandler()
                .download(idApi.getId())
                .execute();
        assertEquals(404, downloadResponse.getStatusCode());
    }

    @Test
    void shouldUploadFileAndGetFileDetails() throws Exception {
        ApiResponse<FileDetailsApi> detailsResponse = internalFileTransferClient.privateFileTransferHandler()
                .details(idApi.getId())
                .execute();
        assertEquals(200, detailsResponse.getStatusCode());

        FileDetailsApi fileDetailsApi = detailsResponse.getData();
        assertEquals(AvStatus.NOT_SCANNED, fileDetailsApi.getAvStatus());
        assertEquals("application/pdf", fileDetailsApi.getContentType());
        assertEquals(idApi.getId(), fileDetailsApi.getId());
        assertEquals("large-file.pdf", fileDetailsApi.getName());
        assertNotNull( fileDetailsApi.getLinks().getSelf());
        assertNotNull( fileDetailsApi.getLinks().getDownload());
    }

    @Test
    void shouldDeprecatedUploadAndDownloadAsStream() throws Exception {
        ApiResponse<FileDetailsApi> detailsResponse = internalFileTransferClient.privateFileTransferHandler()
                .details(idApi.getId())
                .execute();
        assertEquals(200, detailsResponse.getStatusCode());

        FileDetailsApi fileDetailsApi = detailsResponse.getData();
        assertEquals(AvStatus.NOT_SCANNED, fileDetailsApi.getAvStatus());
        assertEquals("application/pdf", fileDetailsApi.getContentType());
        assertEquals(idApi.getId(), fileDetailsApi.getId());
        assertEquals("large-file.pdf", fileDetailsApi.getName());
        assertNotNull( fileDetailsApi.getLinks().getSelf());
        assertNotNull( fileDetailsApi.getLinks().getDownload());

        ApiResponse<FileApi> downloadResponse = internalFileTransferClient.privateFileTransferHandler()
                .downloadAsStream(idApi.getId())
                .execute();

        assertEquals(200, downloadResponse.getStatusCode());
        assertEquals(13, downloadResponse.getHeaders().size());

        assertEquals("application/pdf", ((List<?>)downloadResponse.getHeaders().get("Content-Type")).getFirst());
        assertEquals("attachment; filename=\"large-file.pdf\"", ((List<?>)downloadResponse.getHeaders().get("Content-Disposition")).getFirst());
        assertNull(downloadResponse.getHeaders().get("Content-Length"));

        FileApi responseBody = downloadResponse.getData();

        assertEquals("large-file.pdf", responseBody.getFileName());
        assertEquals(19510707, responseBody.getBody().length);
        assertEquals("application/pdf", responseBody.getMimeType());
        assertEquals(19510707, responseBody.getSize());
        assertEquals("pdf", responseBody.getExtension());
    }

    @Test
    void shouldDeprecatedUploadAndDownloadAsModel() throws Exception {
        ApiResponse<FileDetailsApi> detailsResponse = internalFileTransferClient.privateFileTransferHandler()
                .details(idApi.getId())
                .execute();
        assertEquals(200, detailsResponse.getStatusCode());

        FileDetailsApi fileDetailsApi = detailsResponse.getData();
        assertEquals(AvStatus.NOT_SCANNED, fileDetailsApi.getAvStatus());
        assertEquals("application/pdf", fileDetailsApi.getContentType());
        assertEquals(idApi.getId(), fileDetailsApi.getId());
        assertEquals("large-file.pdf", fileDetailsApi.getName());
        assertNotNull( fileDetailsApi.getLinks().getSelf());
        assertNotNull( fileDetailsApi.getLinks().getDownload());

        ApiResponse<FileApi> downloadResponse = internalFileTransferClient.privateFileTransferHandler()
                .download(idApi.getId())
                .execute();

        assertEquals(200, downloadResponse.getStatusCode());
        assertEquals(12, downloadResponse.getHeaders().size());
        assertEquals("application/json", ((List<?>)downloadResponse.getHeaders().get("Content-Type")).getFirst());
        assertNull(downloadResponse.getHeaders().get("Content-Disposition"));

        FileApi responseBody = downloadResponse.getData();

        assertEquals("large-file.pdf", responseBody.getFileName());
        assertEquals(19510707, responseBody.getBody().length);
        assertEquals("application/pdf", responseBody.getMimeType());
        assertEquals(19510707, responseBody.getSize());
        assertEquals("pdf", responseBody.getExtension());
    }

    private IdApi uploadFile() throws IOException, URIValidationException {
        try (FileInputStream is = new FileInputStream(ResourceUtils.getFile("classpath:large-file.pdf"))) {
            ApiResponse<IdApi> uploadResponse = internalFileTransferClient.privateFileTransferHandler()
                    .upload(is, "application/pdf", "large-file.pdf")
                    .execute();
            assertEquals(200, uploadResponse.getStatusCode());
            return uploadResponse.getData();
        }
    }

    private @NotNull InternalFileTransferClient buildInternalFileTransferClient() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("ERIC-Identity", "someone");
        headers.set("ERIC-Identity-Type", "key");
        headers.set("ERIC-Authorised-Key-Roles", "*");

        FileTransferHttpClient fileTransferHttpClient = new FileTransferHttpClient(API_KEY, headers);
        InternalFileTransferClient apiClient = new InternalFileTransferClient(fileTransferHttpClient);
        apiClient.setBasePath("http://localhost:%d".formatted(port));
        return apiClient;
    }
}
