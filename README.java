package com.edp.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Sets JVM proxy system properties at the earliest possible
 * point in application startup — before any AWS SDK clients
 * are initialised.
 *
 * This replicates exactly what AWS CLI does when
 * HTTP_PROXY=http://username:password@proxyhost:port is set.
 */
@Slf4j
@Component
public class ProxyInitializer
        implements ApplicationListener<ApplicationStartingEvent> {

    @Value("${proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${proxy.host:}")
    private String proxyHost;

    @Value("${proxy.port:8080}")
    private int proxyPort;

    @Value("${proxy.username:}")
    private String proxyUsername;

    @Value("${proxy.password:}")
    private String proxyPassword;

    @Override
    public void onApplicationEvent(
            ApplicationStartingEvent event) {

        if (!proxyEnabled
                || proxyHost == null
                || proxyHost.isBlank()) {
            log.info("[Proxy] Proxy disabled or not configured.");
            return;
        }

        log.info("[Proxy] Setting JVM proxy: {}:{}",
                proxyHost, proxyPort);

        // Set proxy system properties — same mechanism
        // AWS CLI uses internally
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort",
                String.valueOf(proxyPort));
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort",
                String.valueOf(proxyPort));
        System.setProperty("http.nonProxyHosts",
                "localhost|127.0.0.1");

        if (proxyUsername != null && !proxyUsername.isBlank()) {
            System.setProperty("https.proxyUser", proxyUsername);
            System.setProperty("https.proxyPassword", proxyPassword);
            System.setProperty("http.proxyUser", proxyUsername);
            System.setProperty("http.proxyPassword", proxyPassword);
            // This is the key line — enables basic auth
            // for Java's built-in proxy handling
            System.setProperty(
                    "jdk.http.auth.tunneling.disabledSchemes",
                    "");
            System.setProperty(
                    "jdk.http.auth.proxying.disabledSchemes",
                    "");
        }

        log.info("[Proxy] JVM proxy configured successfully.");
    }
        }
