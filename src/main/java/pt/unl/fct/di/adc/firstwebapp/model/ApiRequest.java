package pt.unl.fct.di.adc.firstwebapp.model;

import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
public class ApiRequest<T> {
    public T input;
    public AuthToken token;

    public ApiRequest() {}
}