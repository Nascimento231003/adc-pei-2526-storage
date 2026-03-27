package pt.unl.fct.di.adc.firstwebapp.results;

public final class CreateAccountResult {
    public String username;
    public String role;

    public CreateAccountResult() {}

    public CreateAccountResult(String username, String role) {
        this.username = username;
        this.role = role;
    }
}