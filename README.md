# spring-security-saml-example

Production-ready Spring Boot 3 application demonstrating SAML 2.0 SSO integration with Spring Security. Includes multi-IdP support, attribute mapping, SLO (Single Logout), and Docker Compose for quick local testing with a mock IdP.

> **Full tutorial:** [Configuring SAML Login with Spring Security: metadata-location and Relying Party Setup](https://iamdevbox.com/posts/configuring-saml-login-with-spring-security/?utm_source=github&utm_medium=companion-repo&utm_campaign=spring-security-saml-example)

---

## Features

- SAML 2.0 SP-initiated and IdP-initiated login
- Multi-IdP support (Okta, Azure AD, Ping Identity, mock IdP)
- Dynamic metadata from URL (no manual certificate copy-paste)
- Custom attribute mapping (email, roles from SAML assertions)
- Single Logout (SLO) — both SP-initiated and IdP-initiated
- Role-based authorization via SAML `groups` attribute
- Docker Compose with [mocksaml.com](https://mocksaml.com) / local Keycloak IdP for local testing
- Spring Boot 3.2 + Spring Security 6.x

---

## Quick Start

```bash
git clone https://github.com/IAMDevBox/spring-security-saml-example.git
cd spring-security-saml-example

# Start with Docker Compose (includes Keycloak as IdP)
docker compose up -d

# App runs at https://localhost:8443
# Keycloak admin at http://localhost:8080 (admin/admin)
```

Open https://localhost:8443 → click Login → redirected to Keycloak → login with `user@example.com` / `password` → authenticated.

---

## Project Structure

```
spring-security-saml-example/
├── src/
│   └── main/
│       ├── java/com/example/saml/
│       │   ├── SamlApplication.java
│       │   ├── config/
│       │   │   ├── SamlSecurityConfig.java      # Main SAML security config
│       │   │   └── MultiIdpConfig.java           # Multi-IdP registration
│       │   ├── auth/
│       │   │   ├── SamlAttributeMapper.java      # Custom attribute mapping
│       │   │   └── SamlAuthenticationHandler.java
│       │   └── web/
│       │       ├── DashboardController.java
│       │       └── LoginController.java
│       └── resources/
│           ├── application.yml
│           ├── application-local.yml             # Local/dev overrides
│           ├── saml/
│           │   ├── sp-private-key.pem            # SP signing key (generated)
│           │   └── sp-certificate.pem            # SP certificate (generated)
│           └── templates/
│               ├── login.html
│               ├── select-idp.html
│               └── dashboard.html
├── docker/
│   └── keycloak/
│       └── realm-export.json                     # Pre-configured Keycloak realm
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## Configuration

### application.yml

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12

saml:
  sp:
    entity-id: https://app.example.com:8443/saml2/service-provider-metadata/keycloak

spring:
  security:
    saml2:
      relyingparty:
        registration:
          keycloak:
            assertingparty:
              metadata-uri: http://localhost:8080/realms/demo/protocol/saml/descriptor
            signing:
              credentials:
                - private-key-location: classpath:saml/sp-private-key.pem
                  certificate-location: classpath:saml/sp-certificate.pem
```

### Multi-IdP Setup

For multiple IdPs (employees on Okta, contractors on Azure AD):

```yaml
spring:
  security:
    saml2:
      relyingparty:
        registration:
          okta:
            assertingparty:
              metadata-uri: https://dev-12345.okta.com/app/exk123456/sso/saml/metadata
          azure:
            assertingparty:
              metadata-uri: https://login.microsoftonline.com/{tenant-id}/federationmetadata/2007-06/federationmetadata.xml
          keycloak:
            assertingparty:
              metadata-uri: http://localhost:8080/realms/demo/protocol/saml/descriptor
```

---

## Generating SP Certificates

```bash
# Generate SP private key and self-signed certificate
openssl req -newkey rsa:2048 -nodes \
  -keyout src/main/resources/saml/sp-private-key.pem \
  -x509 -days 3650 \
  -out src/main/resources/saml/sp-certificate.pem \
  -subj "/CN=saml-sp/OU=IAMDevBox/O=Example/L=City/C=US"

# Generate PKCS12 keystore for HTTPS
keytool -genkeypair -alias spring -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore src/main/resources/keystore.p12 \
  -storepass changeit -dname "CN=localhost, OU=IAMDevBox, O=Example, L=City, C=US"
```

---

## Common Errors & Fixes

### "SAML signature validation failed"

```bash
# Check IdP certificate matches what's in metadata
curl http://localhost:8080/realms/demo/protocol/saml/descriptor | grep X509Certificate

# Check certificate expiry
openssl x509 -in src/main/resources/saml/idp-cert.pem -text -noout | grep "Not After"

# Sync system time (clock skew > 5min causes this)
sudo ntpdate pool.ntp.org
```

### "Destination mismatch"

Your ACS URL in IdP must exactly match:
```
https://localhost:8443/saml2/login/sso/{registrationId}
```

Get your SP metadata endpoint:
```bash
curl -k https://localhost:8443/saml2/service-provider-metadata/keycloak
```
Upload the entire XML to your IdP.

### Attribute not mapped

```java
// Add this to debug all SAML attributes on login
principal.getAttributes().forEach((key, values) ->
    System.out.println("SAML Attribute: " + key + " = " + values));
```

---

## Running Tests

```bash
./mvnw test
```

Integration tests use Spring's `MockMvc` with a mocked SAML response — no live IdP required.

---

## Related Resources

- **Full tutorial**: [Configuring SAML Login with Spring Security](https://iamdevbox.com/posts/configuring-saml-login-with-spring-security/?utm_source=github&utm_medium=companion-repo&utm_campaign=spring-security-saml-example)
- **SAML vs OIDC**: [When to Use Which Protocol](https://iamdevbox.com/posts/saml-vs-oidc-when-to-use-which-protocol-in-2025/?utm_source=github&utm_medium=companion-repo&utm_campaign=spring-security-saml-example)
- **SAML Decoder tool**: [Debug your SAML assertions](https://iamdevbox.com/tools/saml-decoder/?utm_source=github&utm_medium=companion-repo&utm_campaign=spring-security-saml-example)
- **Understanding SSO**: [SSO and SAML Simplified](https://iamdevbox.com/posts/understanding-single-sign-on-sso-and-saml-simplified/?utm_source=github&utm_medium=companion-repo&utm_campaign=spring-security-saml-example)
- **Testing SAML with Postman**: [SAML & OIDC Testing Guide](https://iamdevbox.com/posts/testing-saml-and-oidc-authorization-flows-with-postman/?utm_source=github&utm_medium=companion-repo&utm_campaign=spring-security-saml-example)
- [Spring Security SAML2 Reference](https://docs.spring.io/spring-security/reference/servlet/saml2/index.html)
- [SAML 2.0 Specification](https://docs.oasis-open.org/security/saml/Post2.0/sstc-saml-tech-overview-2.0.html)

---

## License

MIT License — see [LICENSE](LICENSE).

Built with ❤️ by [IAMDevBox.com](https://iamdevbox.com/?utm_source=github&utm_medium=companion-repo&utm_campaign=spring-security-saml-example) — Identity & Access Management for developers.
