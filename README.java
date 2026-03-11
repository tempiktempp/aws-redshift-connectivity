package com.edp.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Sets JVM proxy system properties at startup.
 * Reads directly from environment variables via System.getenv()
 * so it works regardless of Spring context loading order.
 */
@Slf4j
@Configuration
public class ProxyInitializer {

    @PostConstruct
    public void init() {
        String proxyEnabled = System.getenv("PROXY_ENABLED");
        String proxyHost    = System.getenv("PROXY_HOST");
        String proxyPort    = System.getenv("PROXY_PORT");
        String proxyUser    = System.getenv("PROXY_USERNAME");
        String proxyPass    = System.getenv("PROXY_PASSWORD");

        // Debug — verify env vars are being read
        log.info("[Proxy] PROXY_ENABLED  = {}", proxyEnabled);
        log.info("[Proxy] PROXY_HOST     = {}", proxyHost);
        log.info("[Proxy] PROXY_PORT     = {}", proxyPort);
        log.info("[Proxy] PROXY_USERNAME = {}", proxyUser);
        log.info("[Proxy] PROXY_PASSWORD length = {}",
                proxyPass != null
                        ? proxyPass.length() : "NULL");

        if (proxyHost == null || proxyHost.isBlank()) {
            log.info("[Proxy] PROXY_HOST not set — skipping.");
            return;
        }

        String port = (proxyPort != null
                && !proxyPort.isBlank())
                ? proxyPort : "8080";

        // Set JVM system properties for proxy
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", port);
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", port);
        System.setProperty("http.nonProxyHosts",
                "localhost|127.0.0.1");

        log.info("[Proxy] Proxy host/port set: {}:{}",
                proxyHost, port);

        if (proxyUser != null && !proxyUser.isBlank()) {
            System.setProperty("https.proxyUser", proxyUser);
            System.setProperty("https.proxyPassword",
                    proxyPass != null ? proxyPass : "");
            System.setProperty("http.proxyUser", proxyUser);
            System.setProperty("http.proxyPassword",
                    proxyPass != null ? proxyPass : "");

            // Critical — allows basic auth through
            // HTTPS CONNECT tunnel
            // Without this Java blocks proxy credentials
            // for HTTPS by default
            System.setProperty(
                    "jdk.http.auth.tunneling.disabledSchemes",
                    "");
            System.setProperty(
                    "jdk.http.auth.proxying.disabledSchemes",
                    "");

            log.info("[Proxy] Proxy credentials configured " +
                    "for user: {}", proxyUser);
        }

        log.info("[Proxy] JVM proxy setup complete.");
    }
}
