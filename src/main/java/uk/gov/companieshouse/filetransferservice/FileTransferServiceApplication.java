package uk.gov.companieshouse.filetransferservice;

import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.logging.Logger;

@SpringBootApplication
public class FileTransferServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(FileTransferServiceApplication.class, args);
    }

}

