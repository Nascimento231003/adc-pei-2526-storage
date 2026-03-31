package pt.unl.fct.di.adc.firstwebapp.util;

import pt.unl.fct.di.adc.firstwebapp.model.Role;

public class ChangeRoleData {
    public String username; 
    public Role newRole;

    public ChangeRoleData () {}
    
    public ChangeRoleData (String username, Role newRole) {
        this.username = username;
        this.newRole = newRole;
    }
}
