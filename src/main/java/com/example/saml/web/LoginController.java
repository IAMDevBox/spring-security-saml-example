package com.example.saml.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles IdP selection for multi-IdP SAML login.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Authentication failed. Please try again.");
        }
        // Model includes available IdPs for selection UI
        return "select-idp";
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }
}
