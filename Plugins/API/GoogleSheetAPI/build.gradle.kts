plugins {
    id("xs-plugin-shadow")
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.21.0")
    api("com.google.api-client:google-api-client:2.8.1")
    api("com.google.oauth-client:google-oauth-client-jetty:1.39.0")
    api("com.google.apis:google-api-services-sheets:v4-rev20251110-2.0.0")
}
