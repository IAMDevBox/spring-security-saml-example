package com.example.saml.config;

import com.example.saml.auth.SamlAttributeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

/**
 * Main SAML security configuration.
 *
 * Full tutorial: https://iamdevbox.com/posts/configuring-saml-login-with-spring-security/
 */
@Configuration
@EnableWebSecurity
public class SamlSecurityConfig {

    @Autowired
    private SamlAttributeMapper samlAttributeMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/error", "/webjars/**", "/css/**").permitAll()
                .anyRequest().authenticated()
            )
            .saml2Login(saml2 -> saml2
                .loginPage("/login")
                .authenticationManager(authenticationManager())
                .successHandler(samlSuccessHandler())
                .failureHandler(samlFailureHandler())
            )
            .saml2Logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
            );

        return http.build();
    }

    /**
     * Custom authentication manager with attribute mapping.
     * Uses OpenSaml4AuthenticationProvider to support SAML 2.0 assertions.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        OpenSaml4AuthenticationProvider provider = new OpenSaml4AuthenticationProvider();
        // Wire in our custom attribute mapper to extract email, roles etc.
        provider.setResponseAuthenticationConverter(samlAttributeMapper.converter());
        return new ProviderManager(provider);
    }

    @Bean
    public SavedRequestAwareAuthenticationSuccessHandler samlSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler handler =
            new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl("/dashboard");
        return handler;
    }

    @Bean
    public SimpleUrlAuthenticationFailureHandler samlFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler("/login?error=true");
    }
}
