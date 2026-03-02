#!/bin/bash
# Generate SP signing certificates and HTTPS keystore for local development.
# Run this script once before starting the application.
#
# Full tutorial: https://iamdevbox.com/posts/configuring-saml-login-with-spring-security/

set -e

SAML_DIR="src/main/resources/saml"
RESOURCES_DIR="src/main/resources"

mkdir -p "$SAML_DIR"

echo "Generating SP SAML signing key and certificate..."
openssl req -newkey rsa:2048 -nodes \
  -keyout "$SAML_DIR/sp-private-key.pem" \
  -x509 -days 3650 \
  -out "$SAML_DIR/sp-certificate.pem" \
  -subj "/CN=saml-sp/OU=IAMDevBox/O=Example/L=City/C=US"

echo "Generating HTTPS keystore..."
keytool -genkeypair -alias spring -keyalg RSA -keysize 2048 \
  -storetype PKCS12 \
  -keystore "$RESOURCES_DIR/keystore.p12" \
  -storepass changeit \
  -dname "CN=localhost, OU=IAMDevBox, O=Example, L=City, C=US" \
  -validity 3650

echo ""
echo "Done! Certificates generated:"
echo "  - $SAML_DIR/sp-private-key.pem"
echo "  - $SAML_DIR/sp-certificate.pem"
echo "  - $RESOURCES_DIR/keystore.p12"
echo ""
echo "Next: Register SP metadata with your IdP:"
echo "  curl -k https://localhost:8443/saml2/service-provider-metadata/keycloak"
