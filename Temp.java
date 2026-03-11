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

    RedshiftDataClient.Builder builder =
            RedshiftDataClient.builder()
                    .region(Region.of(props.getRegion()))
                    .credentialsProvider(credentialsProvider);

    // Apply proxy config if enabled
    if (props.getProxy().isEnabled()) {
        log.info("[Redshift] Proxy enabled: {}:{}",
                props.getProxy().getHost(),
                props.getProxy().getPort());

        ProxyConfiguration.Builder proxyBuilder =
                ProxyConfiguration.builder()
                        .endpoint(URI.create(
                                "http://" +
                                props.getProxy().getHost() +
                                ":" +
                                props.getProxy().getPort()));

        // Only set credentials if proxy requires auth
        if (props.getProxy().getUsername() != null
                && !props.getProxy().getUsername().isBlank()) {
            proxyBuilder
                    .username(props.getProxy().getUsername())
                    .password(props.getProxy().getPassword());
        }

        builder.httpClientBuilder(
                ApacheHttpClient.builder()
                        .proxyConfiguration(proxyBuilder.build()));
    }

    return builder.build();
                            }
