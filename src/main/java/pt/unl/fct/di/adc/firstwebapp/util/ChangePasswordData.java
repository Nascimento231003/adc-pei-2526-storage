package pt.unl.fct.di.adc.firstwebapp.util;

public class ChangePasswordData {
    public String username, oldPassword, newPassword;

    public ChangePasswordData () {}
    
    public ChangePasswordData (String username, String oldPassword, String newPassword) {
        this.username = username;
        this.newPassword = newPassword;
        this.oldPassword = oldPassword;
    }
}
