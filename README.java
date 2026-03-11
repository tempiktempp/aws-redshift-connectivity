package com.edp.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Registers a JVM-level Authenticator for corporate proxy
 * basic auth — handles the 407 challenge automatically.
 *
 * This works alongside the JVM proxy system properties
 * set in pom.xml jvmArguments.
 */
@Slf4j
@Component
public class ProxyInitializer {

    @Value("${proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${proxy.username:}")
    private String proxyUsername;

    @Value("${proxy.password:}")
    private String proxyPassword;

    @EventListener(ContextRefreshedEvent.class)
    public void init() {

        if (!proxyEnabled
                || proxyUsername == null
                || proxyUsername.isBlank()) {
            log.info("[Proxy] Proxy auth not configured.");
            return;
        }

        log.info("[Proxy] Registering proxy Authenticator " +
                "for user: {}", proxyUsername);

        // Register JVM-level authenticator —
        // responds automatically to proxy 407 challenges
        // with correct credentials
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication
                    getPasswordAuthentication() {
                if (getRequestorType() ==
                        RequestorType.PROXY) {
                    log.debug("[Proxy] Providing credentials " +
                            "for proxy challenge.");
                    return new PasswordAuthentication(
                            proxyUsername,
                            proxyPassword.toCharArray());
                }
                return null;
            }
        });

        // Re-enable Basic auth for HTTPS proxy tunneling
        // Disabled by default since Java 8u111
        System.setProperty(
                "jdk.http.auth.tunneling.disabledSchemes",
                "");
        System.setProperty(
                "jdk.http.auth.proxying.disabledSchemes",
                "");

        log.info("[Proxy] Proxy Authenticator registered.");
    }
}
