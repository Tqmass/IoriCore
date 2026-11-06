package us.iorilabs.proxy.config;

public final class RedisConfig {

    private final YamlConfig yaml;

    public RedisConfig(YamlConfig yaml) {
        this.yaml = yaml;
    }

    public boolean enabled() {
        return yaml.getBoolean("enabled", false);
    }

    public String host() {
        return yaml.getString("host", "127.0.0.1");
    }

    public int port() {
        return yaml.getInt("port", 6379);
    }

    public int database() {
        return yaml.getInt("database", 0);
    }

    public int timeoutMs() {
        return yaml.getInt("timeout_ms", 2000);
    }

    public boolean ssl() {
        return yaml.getBoolean("ssl", false);
    }

    public String keyPrefix() {
        return yaml.getString("key_prefix", "ioriproxy");
    }

    public String channel() {
        return yaml.getString("channel", "ioriproxy");
    }

    public boolean authEnabled() {
        return yaml.getBoolean("authentication.enabled", false);
    }

    public String username() {
        return yaml.getString("authentication.username", "");
    }

    public String password() {
        return yaml.getString("authentication.password", "");
    }

    public void validate(ConfigValidation validation) {
        if (host().isBlank()) {
            validation.error(yaml.fileName(), "HOST", "Redis host cannot be blank");
        }

        if (port() <= 0 || port() > 65535) {
            validation.error(yaml.fileName(), "PORT", "Redis port must be 1-65535");
        }
    }
}
