package uk.gov.companieshouse.filetransferservice.integration;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HealthCheckControllerTest  {
    @Autowired
    private MockMvc mvc;

    @Test
    void HealthCheckEndpointTest() throws Exception {
        this.mvc.perform(get("/file-transfer-service/healthcheck"))
                .andExpect(status().isOk());
    }

}