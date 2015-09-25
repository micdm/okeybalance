package com.micdm.okeybalance.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class CredentialStore {

    public static class Credentials {

        public final String cardNumber;
        public final String password;

        public Credentials(String cardNumber, String password) {
            this.cardNumber = cardNumber;
            this.password = password;
        }
    }

    private static final String PREF_NAME = "credentials";
    private static final String CARD_NUMBER_PREF_KEY = "cardNumber";
    private static final String PASSWORD_PREF_KEY = "password";

    public static Credentials get(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String cardNumber = prefs.getString(CARD_NUMBER_PREF_KEY, null);
        String password = prefs.getString(PASSWORD_PREF_KEY, null);
        return (cardNumber == null || password == null) ? null : new Credentials(cardNumber, password);
    }

    public static void put(Context context, Credentials credentials) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(CARD_NUMBER_PREF_KEY, credentials.cardNumber);
        editor.putString(PASSWORD_PREF_KEY, credentials.password);
        editor.apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}