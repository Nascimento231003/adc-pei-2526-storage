package pt.unl.fct.di.adc.firstwebapp.results;

import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;

public class LoginResult {
    public AuthToken token;

    public LoginResult() {}

    public LoginResult(AuthToken token) {
        this.token = token;
    }
}