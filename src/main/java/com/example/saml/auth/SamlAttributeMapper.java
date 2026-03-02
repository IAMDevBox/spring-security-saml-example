package com.example.saml.auth;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.core.xml.XMLObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps SAML assertion attributes to Spring Security principal and authorities.
 *
 * Different IdPs (Okta, Azure AD, Keycloak, Ping) use different attribute names
 * for the same information. This mapper normalizes them into a consistent model.
 *
 * See: https://iamdevbox.com/posts/configuring-saml-login-with-spring-security/
 */
@Component
public class SamlAttributeMapper {

    // Common attribute name variations across IdPs
    private static final Map<String, String> ATTRIBUTE_ALIASES = Map.of(
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress", "email",
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname", "given_name",
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname", "family_name",
        "http://schemas.microsoft.com/ws/2008/06/identity/claims/role", "roles",
        "http://schemas.microsoft.com/ws/2008/06/identity/claims/groups", "groups",
        "urn:oid:1.2.840.113549.1.9.1", "email",           // LDAP emailAddress OID
        "urn:oid:2.16.840.1.113730.3.1.241", "displayName" // LDAP displayName OID
    );

    /**
     * Returns a Converter for OpenSaml4AuthenticationProvider that:
     * 1. Extracts all assertion attributes with alias normalization
     * 2. Maps group membership to Spring GrantedAuthority
     * 3. Builds a Saml2Authentication with the full principal
     */
    public Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2Authentication> converter() {
        return responseToken -> {
            Saml2AuthenticationToken token = responseToken.getToken();
            Assertion assertion = responseToken.getResponse().getAssertions().get(0);

            Map<String, List<Object>> attributes = extractAttributes(assertion);
            Collection<GrantedAuthority> authorities = extractAuthorities(attributes);

            String username = resolveUsername(assertion, attributes);
            DefaultSaml2AuthenticatedPrincipal principal =
                new DefaultSaml2AuthenticatedPrincipal(username, attributes);

            return new Saml2Authentication(
                principal,
                token.getSaml2Response(),
                authorities
            );
        };
    }

    private Map<String, List<Object>> extractAttributes(Assertion assertion) {
        Map<String, List<Object>> attributes = new LinkedHashMap<>();

        assertion.getAttributeStatements().forEach(statement ->
            statement.getAttributes().forEach(attr -> {
                String name = normalizeAttributeName(attr.getName());
                List<Object> values = extractValues(attr);
                // If multiple statements define the same attribute, merge the values
                attributes.merge(name, values, (existing, newVals) -> {
                    existing.addAll(newVals);
                    return existing;
                });
            })
        );

        return attributes;
    }

    private String normalizeAttributeName(String attributeName) {
        return ATTRIBUTE_ALIASES.getOrDefault(attributeName, attributeName);
    }

    private List<Object> extractValues(Attribute attr) {
        return attr.getAttributeValues().stream()
            .map(XMLObject::getDOM)
            .filter(Objects::nonNull)
            .map(Element::getTextContent)
            .collect(Collectors.toList());
    }

    private String resolveUsername(Assertion assertion, Map<String, List<Object>> attributes) {
        // Prefer email as username; fall back to NameID
        List<Object> emails = attributes.get("email");
        if (emails != null && !emails.isEmpty()) {
            return String.valueOf(emails.get(0));
        }
        return assertion.getSubject().getNameID().getValue();
    }

    private Collection<GrantedAuthority> extractAuthorities(Map<String, List<Object>> attributes) {
        // Check both "roles" and "groups" attributes (different IdPs use different names)
        List<Object> roleValues = new ArrayList<>();
        Optional.ofNullable(attributes.get("roles")).ifPresent(roleValues::addAll);
        Optional.ofNullable(attributes.get("groups")).ifPresent(roleValues::addAll);

        if (roleValues.isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER")); // default role
        }

        return roleValues.stream()
            .map(String::valueOf)
            .map(role -> mapGroupToAuthority(role))
            .collect(Collectors.toList());
    }

    private GrantedAuthority mapGroupToAuthority(String group) {
        // Map IdP group names to Spring Security roles
        String role = switch (group.toUpperCase()) {
            case "ADMIN", "ADMINISTRATORS", "APP_ADMIN" -> "ROLE_ADMIN";
            case "MANAGER", "MANAGERS", "APP_MANAGER" -> "ROLE_MANAGER";
            case "USER", "USERS", "APP_USER" -> "ROLE_USER";
            default -> "ROLE_" + group.toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        };
        return new SimpleGrantedAuthority(role);
    }
}
