package com.craftbid.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "craftbid.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private Auth auth = new Auth();
    private PublicConfig publicConfig = new PublicConfig();
    private Authenticated authenticated = new Authenticated();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public PublicConfig getPublic() {
        return publicConfig;
    }

    public void setPublic(PublicConfig publicConfig) {
        this.publicConfig = publicConfig;
    }

    public Authenticated getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(Authenticated authenticated) {
        this.authenticated = authenticated;
    }

    // =========================================================
    // TIER 1: AUTH ROUTES
    // =========================================================
    public static class Auth {
        private int ipLimit = 10;
        private int ipWindowSeconds = 60;
        private int accountLimit = 5;
        private int accountWindowSeconds = 60;
        private int baseBackoffSeconds = 15;
        private int maxBackoffSeconds = 900;
        private int backoffResetSeconds = 900;

        public int getIpLimit() {
            return ipLimit;
        }

        public void setIpLimit(int ipLimit) {
            this.ipLimit = ipLimit;
        }

        public int getIpWindowSeconds() {
            return ipWindowSeconds;
        }

        public void setIpWindowSeconds(int ipWindowSeconds) {
            this.ipWindowSeconds = ipWindowSeconds;
        }

        public int getAccountLimit() {
            return accountLimit;
        }

        public void setAccountLimit(int accountLimit) {
            this.accountLimit = accountLimit;
        }

        public int getAccountWindowSeconds() {
            return accountWindowSeconds;
        }

        public void setAccountWindowSeconds(int accountWindowSeconds) {
            this.accountWindowSeconds = accountWindowSeconds;
        }

        public int getBaseBackoffSeconds() {
            return baseBackoffSeconds;
        }

        public void setBaseBackoffSeconds(int baseBackoffSeconds) {
            this.baseBackoffSeconds = baseBackoffSeconds;
        }

        public int getMaxBackoffSeconds() {
            return maxBackoffSeconds;
        }

        public void setMaxBackoffSeconds(int maxBackoffSeconds) {
            this.maxBackoffSeconds = maxBackoffSeconds;
        }

        public int getBackoffResetSeconds() {
            return backoffResetSeconds;
        }

        public void setBackoffResetSeconds(int backoffResetSeconds) {
            this.backoffResetSeconds = backoffResetSeconds;
        }
    }

    // =========================================================
    // TIER 2: PUBLIC BROWSING ROUTES
    // =========================================================
    public static class PublicConfig {
        private int ipLimit = 60;
        private int ipWindowSeconds = 60;

        public int getIpLimit() {
            return ipLimit;
        }

        public void setIpLimit(int ipLimit) {
            this.ipLimit = ipLimit;
        }

        public int getIpWindowSeconds() {
            return ipWindowSeconds;
        }

        public void setIpWindowSeconds(int ipWindowSeconds) {
            this.ipWindowSeconds = ipWindowSeconds;
        }
    }

    // =========================================================
    // TIER 3: AUTHENTICATED USER ACTIONS
    // =========================================================
    public static class Authenticated {
        private int userLimit = 300;
        private int userWindowSeconds = 60;

        public int getUserLimit() {
            return userLimit;
        }

        public void setUserLimit(int userLimit) {
            this.userLimit = userLimit;
        }

        public int getUserWindowSeconds() {
            return userWindowSeconds;
        }

        public void setUserWindowSeconds(int userWindowSeconds) {
            this.userWindowSeconds = userWindowSeconds;
        }
    }
}
