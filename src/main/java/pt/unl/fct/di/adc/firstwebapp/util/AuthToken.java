package pt.unl.fct.di.adc.firstwebapp.util;

import pt.unl.fct.di.adc.firstwebapp.model.Role;
import java.util.UUID;

public class AuthToken {

	public static final long EXPIRATION_TIME = 1000*60*15; // 15m
	
	public String username;
	public String tokenId;
	public Role role;
	public long issuedAt;
	public long expiresAt;
	
	public AuthToken() { }
	
	public AuthToken(String username, Role role) {
		this.username = username;
		this.tokenId = UUID.randomUUID().toString();
		this.issuedAt = System.currentTimeMillis();
		this.expiresAt = this.issuedAt + EXPIRATION_TIME;
		this.role = role;
	}

	public boolean isValid() {
		return issuedAt <= System.currentTimeMillis() && System.currentTimeMillis() <= expiresAt;
	}
	
}
