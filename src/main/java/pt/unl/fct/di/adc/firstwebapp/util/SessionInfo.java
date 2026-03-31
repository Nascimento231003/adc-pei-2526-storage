package pt.unl.fct.di.adc.firstwebapp.util;

public class SessionInfo {
    public String tokenId;
    public String username;
    public String role;
    public long expiresAt;

    public SessionInfo() {}

    public SessionInfo(String tokenId, String username, String role, long expiresAt) {
        this.tokenId = tokenId;
        this.username = username;
        this.role = role;
        this.expiresAt = expiresAt;
    }
}
