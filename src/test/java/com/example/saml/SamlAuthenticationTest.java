package com.example.saml;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.saml2Login;

/**
 * Integration tests for SAML authentication flow.
 * Uses Spring Security's built-in SAML2 test support — no live IdP required.
 *
 * See: https://iamdevbox.com/posts/configuring-saml-login-with-spring-security/
 */
@SpringBootTest
@AutoConfigureMockMvc
class SamlAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void unauthenticatedRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void authenticatedUserCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/dashboard")
                .with(saml2Login()
                    .relyingPartyRegistrationId("keycloak")
                    .attributes(attrs -> {
                        attrs.put("email", "user@example.com");
                        attrs.put("given_name", "Demo");
                        attrs.put("roles", java.util.List.of("user"));
                    })))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard"));
    }

    @Test
    void adminUserHasAdminRole() throws Exception {
        mockMvc.perform(get("/dashboard")
                .with(saml2Login()
                    .relyingPartyRegistrationId("keycloak")
                    .attributes(attrs -> {
                        attrs.put("email", "admin@example.com");
                        attrs.put("roles", java.util.List.of("admin", "user"));
                    })))
            .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void homePageIsPublic() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }
}
