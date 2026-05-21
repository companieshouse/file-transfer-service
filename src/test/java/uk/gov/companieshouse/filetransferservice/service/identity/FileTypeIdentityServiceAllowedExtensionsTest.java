package uk.gov.companieshouse.filetransferservice.service.identity;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.companieshouse.logging.Logger;

class FileTypeIdentityServiceAllowedExtensionsTest {

    private final Logger logger = Mockito.mock(Logger.class);
    private final FileTypeIdentityService service = new FileTypeIdentityService(new org.apache.tika.Tika(), logger);

    @Test
    @DisplayName("Known MIME returns extensions (application/pdf)")
    void knownMimeReturnsExtensions() throws Exception {
        List<String> exts = service.getAllowedExtensions("application/pdf");
        assertNotNull(exts);
        assertTrue(exts.contains(".pdf"), "Expected .pdf in allowed extensions");
    }

    @Test
    @DisplayName("Null or blank MIME returns empty list")
    void nullOrBlankReturnsEmpty() throws Exception {
        assertTrue(service.getAllowedExtensions(null).isEmpty());
        assertTrue(service.getAllowedExtensions("").isEmpty());
        assertTrue(service.getAllowedExtensions("   ").isEmpty());
    }

    @Test
    @DisplayName("Unknown/invalid MIME returns empty list (no exception)")
    void unknownMimeReturnsEmpty() throws Exception {
        List<String> exts = service.getAllowedExtensions("invalid/mime");
        assertNotNull(exts);
        assertTrue(exts.isEmpty());
    }
}
