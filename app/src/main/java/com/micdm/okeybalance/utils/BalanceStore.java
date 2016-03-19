package com.micdm.okeybalance.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.math.BigDecimal;

public class BalanceStore {

    protected static final String PREF_NAME = "balance";
    protected static final String PREF_KEY_CARD_NUMBER = "card_number";
    protected static final String PREF_KEY_BALANCE = "balance";

    public static String getCardNumber(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(PREF_KEY_CARD_NUMBER, null);
    }

    public static BigDecimal getBalance(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String value = prefs.getString(PREF_KEY_BALANCE, null);
        return (value == null) ? null : new BigDecimal(value);
    }

    public static void put(Context context, String cardNumber, BigDecimal balance) {
        getPrefs(context)
            .edit()
            .putString(PREF_KEY_CARD_NUMBER, cardNumber)
            .putString(PREF_KEY_BALANCE, balance.toString())
            .apply();
    }

    public static void clear(Context context) {
        getPrefs(context)
            .edit()
            .remove(PREF_KEY_CARD_NUMBER)
            .remove(PREF_KEY_BALANCE)
            .apply();
    }

    protected static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
