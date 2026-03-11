private ApacheHttpClient.Builder buildApacheHttpClient(
        RedshiftProperties props) {

    RedshiftProperties.ProxyProperties proxy =
            props.getProxy();

    log.info("[Proxy-Debug] host={}, port={}, user={}",
            proxy.getHost(),
            proxy.getPort(),
            proxy.getUsername());

    URI proxyEndpoint = URI.create(
            "http://" +
            proxy.getHost() +
            ":" +
            proxy.getPort());

    return ApacheHttpClient.builder()
            .connectionTimeout(
                    java.time.Duration.ofSeconds(30))
            .socketTimeout(
                    java.time.Duration.ofSeconds(60))
            .proxyConfiguration(
                software.amazon.awssdk.http.apache
                    .ProxyConfiguration.builder()
                        .endpoint(proxyEndpoint)
                        .username(proxy.getUsername())
                        .password(proxy.getPassword())
                        .preemptiveBasicAuthenticationEnabled(
                                Boolean.TRUE)
                        .useSystemPropertyValues(Boolean.FALSE)
                        .addNonProxyHost("localhost")
                        .addNonProxyHost("127.0.0.1")
                        .build());
}
