package pt.unl.fct.di.adc.firstwebapp.results;

import java.util.List;

import pt.unl.fct.di.adc.firstwebapp.util.UserInfo;

public class ShowUsersResult {
    List<UserInfo> users;

    public ShowUsersResult() {}

    public ShowUsersResult(List<UserInfo> users) {
        this.users = users;
    }
}
