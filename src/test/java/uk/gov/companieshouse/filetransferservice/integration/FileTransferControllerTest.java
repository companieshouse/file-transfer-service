package uk.gov.companieshouse.filetransferservice.integration;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import uk.gov.companieshouse.api.model.filetransfer.FileApi;
import uk.gov.companieshouse.api.model.filetransfer.FileDetailsApi;
import uk.gov.companieshouse.filetransferservice.config.WebMvcConfig;
import uk.gov.companieshouse.filetransferservice.service.file.transfer.FileStorageStrategy;

@SpringBootTest(properties = { "service.path.prefix=/file-transfer-service" })
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class FileTransferControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    WebMvcConfig webMvcConfig;

    @MockitoBean
    FileStorageStrategy fileStorageStrategy;

    @Test
    void fileTransferControllerTest() throws Exception {
        doNothing().when(webMvcConfig).addInterceptors( any() );

        FileDetailsApi api = new FileDetailsApi();
        FileApi api2 =  new FileApi();
        api2.setBody(new byte[]{1,1,1,1,1,1,1,1});
        api2.setMimeType("application/pdf");
        when(fileStorageStrategy.getFileDetails("12345")).thenReturn(Optional.of(api));
        when(fileStorageStrategy.load("12345", api)).thenReturn(Optional.of(api2));

        mvc.perform(get("/file-transfer-service/12345/downloadbinary?bypassAv=true"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(content().bytes(new byte[]{1,1,1,1,1,1,1,1}));
    }
}