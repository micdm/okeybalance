package com.micdm.okeybalance.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.math.BigDecimal;

public class BalanceStore {

    protected static final String PREF_NAME = "balance";
    protected static final String VALUE_PREF_KEY = "value";

    public static BigDecimal get(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String value = prefs.getString(VALUE_PREF_KEY, null);
        return (value == null) ? null : new BigDecimal(value);
    }

    public static void put(Context context, BigDecimal value) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(VALUE_PREF_KEY, value.toString());
        editor.apply();
    }

    protected static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
