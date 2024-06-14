<img align="right" width="250" height="47" src="docs/img/gematik_logo.png"/> <br/>    

# Release Notes ISIK Sicherheit Stufe 3 Test Suite

## Release 0.2.0 (2024-06)

### added
- support for TLS endpoints

### changed
- enabled any 40x HTTP response codes on unauthorized access
- using asymmetric client authentication for all test cases

### fixed
- usage of aud-parameter in test cases
- requested scopes and HTTP requests didn't match in some test cases

## Release 0.1.1 (2024-05)

### fixed

- image links in README and ReleaseNotes

## Release 0.1.0 (2024-05)

### added

- test cases for [mandatory requirements](https://simplifier.net/guide/isik-sicherheit-v3/ImplementationGuide-markdown-Conformance?version=current) in the specification 