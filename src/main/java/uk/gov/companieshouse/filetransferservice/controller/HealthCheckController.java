package uk.gov.companieshouse.filetransferservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/file-transfer-service/healthcheck")
    public ResponseEntity<String> healthcheck() {
        return new ResponseEntity<>(HttpStatus.OK);
    }
}