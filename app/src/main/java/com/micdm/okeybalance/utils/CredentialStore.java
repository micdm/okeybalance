package com.micdm.okeybalance.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class CredentialStore {

    protected static final String PREF_NAME = "credentials";
    protected static final String CARD_NUMBER_PREF_KEY = "cardNumber";
    protected static final String PASSWORD_PREF_KEY = "password";

    public static String getCardNumber(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(CARD_NUMBER_PREF_KEY, null);
    }

    public static String getPassword(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(PASSWORD_PREF_KEY, null);
    }

    public static void put(Context context, String cardNumber, String password) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(CARD_NUMBER_PREF_KEY, cardNumber);
        editor.putString(PASSWORD_PREF_KEY, password);
        editor.apply();
    }

    public static boolean hasPassword(Context context) {
        return getPassword(context) != null;
    }

    public static void clearPassword(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.remove(PASSWORD_PREF_KEY);
        editor.apply();
    }

    protected static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
