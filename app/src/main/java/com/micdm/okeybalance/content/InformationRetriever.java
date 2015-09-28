package com.micdm.okeybalance.content;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class InformationRetriever {

    protected static final String LOGIN_PAGE_URL = "https://m.okeycity.ru/site/login";
    protected static final String CARD_NUMBER_FIELD_NAME = "LoginForm[login]";
    protected static final String PASSWORD_FIELD_NAME = "LoginForm[password]";

    public static String getBalance(String cardNumber, String password) {
        String response = HttpTransport.send(getUrl(), getParams(cardNumber, password));
        return AccountPageParser.parse(response);
    }

    protected static URL getUrl() {
        try {
            return new URL(LOGIN_PAGE_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException("invalid login URL", e);
        }
    }

    protected static Map<String, String> getParams(String cardNumber, String password) {
        Map<String, String> params = new HashMap<>();
        params.put(CARD_NUMBER_FIELD_NAME, cardNumber);
        params.put(PASSWORD_FIELD_NAME, password);
        return params;
    }
}
