@Valid
private ProxyProperties proxy = new ProxyProperties();

@Getter
@Setter
public static class ProxyProperties {

    private boolean enabled = false;
    private String host;

    @Min(1) @Max(65535)
    private int port = 8080;

    private String username;
    private String password;
}
