@Bean
@ConditionalOnMissingBean
@ConditionalOnProperty(
        prefix = "redshift",
        name = "connection-strategy",
        havingValue = "DATA_API",
        matchIfMissing = true)
public RedshiftDataClient redshiftDataClient(
        RedshiftProperties props,
        AwsCredentialsProvider credentialsProvider) {

    log.info("[Redshift] Initialising Data API client, " +
            "region: {}", props.getRegion());

    return RedshiftDataClient.builder()
            .region(Region.of(props.getRegion()))
            .credentialsProvider(credentialsProvider)
            .httpClientBuilder(
                ApacheHttpClient.builder()
                    .proxyConfiguration(
                        ProxyConfiguration.builder()
                            .endpoint(URI.create(
                                "http://proxy.yourcompany.com:8080"))
                            .username("your-username")  // remove if no auth
                            .password("your-password")  // remove if no auth
                            .build()))
            .build();
}
