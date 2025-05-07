package uk.gov.companieshouse.filetransferservice.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

@ExtendWith(MockitoExtension.class)
public class WebSecurityConfigTest {

    @Mock
    private HttpSecurity httpSecurity;

    @InjectMocks
    private WebSecurityConfig underTest;

    @Test
    void testFilterChain() throws Exception {
        when(httpSecurity.httpBasic(any())).thenReturn(httpSecurity);
        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.formLogin(any())).thenReturn(httpSecurity);
        when(httpSecurity.logout(any())).thenReturn(httpSecurity);
        when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
        when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
        when(httpSecurity.build()).thenReturn(new DefaultSecurityFilterChain(null, List.of()));

        SecurityFilterChain securityFilterChain = underTest.filterChain(httpSecurity);

        assertNotNull(securityFilterChain);
    }

    @Test
    void testWebSecurityCustomizer() {
        WebSecurityCustomizer webSecurityCustomizer = underTest.webSecurityCustomizer("/file-transfer-service");

        assertNotNull(webSecurityCustomizer);
    }
}
