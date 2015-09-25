package com.micdm.okeybalance.content;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class InformationRetriever {

    public static String getBalance(String cardNumber, String password) {
        String response = HttpTransport.send(getUrl(), getParams(cardNumber, password));
        return AccountPageParser.parse(response);
    }

    private static URL getUrl() {
        try {
            return new URL("https://m.okeycity.ru/site/login");
        } catch (MalformedURLException e) {
            throw new RuntimeException("invalid login URL", e);
        }
    }

    private static Map<String, String> getParams(String cardNumber, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("LoginForm[login]", cardNumber);
        params.put("LoginForm[password]", password);
        params.put("yt0", "Вход");
        params.put("LoginForm[rememberMe]", "0");
        return params;
    }
}
