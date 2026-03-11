private ApacheHttpClient.Builder buildApacheHttpClient(
        RedshiftProperties props) {

    RedshiftProperties.ProxyProperties proxy =
            props.getProxy();

    return ApacheHttpClient.builder()
            .proxyConfiguration(
                software.amazon.awssdk.http.apache
                    .ProxyConfiguration.builder()
                        .endpoint(URI.create(
                                "http://" +
                                proxy.getHost() +
                                ":" +
                                proxy.getPort()))
                        .username(proxy.getUsername())
                        .password(proxy.getPassword())
                        .addNonProxyHost("localhost")
                        .addNonProxyHost("127.0.0.1")
                        .build());
}
