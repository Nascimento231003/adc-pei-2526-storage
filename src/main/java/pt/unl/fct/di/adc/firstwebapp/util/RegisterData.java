package pt.unl.fct.di.adc.firstwebapp.util;
import pt.unl.fct.di.adc.firstwebapp.model.Role;

public class RegisterData {
	
	public String username, password, phone, address, confirmation;
    public Role role;
	
	public RegisterData() { }
	
	public RegisterData(String username, String password, String confirmation, String phone, String address) {
		this.username = username;
		this.password = password;
        this.confirmation = confirmation;
        this.phone = phone;
        this.address = address;
		this.role = Role.USER;
	}

    public boolean validRegistration(){
        return password.equals(confirmation);
    }
	
} 

