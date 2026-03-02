package com.example.saml.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Dashboard shown after successful SAML SSO authentication.
 * Displays authenticated user's attributes from the SAML assertion.
 */
@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal Saml2AuthenticatedPrincipal principal,
                            Model model) {
        model.addAttribute("username", principal.getName());
        model.addAttribute("email", principal.getFirstAttribute("email"));
        model.addAttribute("givenName", principal.getFirstAttribute("given_name"));
        model.addAttribute("roles", principal.getAuthorities());
        model.addAttribute("relyingPartyId", principal.getRelyingPartyRegistrationId());

        // Debug: expose all attributes in development
        model.addAttribute("allAttributes", principal.getAttributes());

        return "dashboard";
    }
}
