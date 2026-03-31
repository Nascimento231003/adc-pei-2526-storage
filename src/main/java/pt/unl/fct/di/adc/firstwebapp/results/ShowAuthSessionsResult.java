package pt.unl.fct.di.adc.firstwebapp.results;

import java.util.List;

import pt.unl.fct.di.adc.firstwebapp.util.SessionInfo;

public class ShowAuthSessionsResult {
    public List<SessionInfo> sessions;

    public ShowAuthSessionsResult() {}

    public ShowAuthSessionsResult(List<SessionInfo> sessions) {
        this.sessions = sessions;
    }
}