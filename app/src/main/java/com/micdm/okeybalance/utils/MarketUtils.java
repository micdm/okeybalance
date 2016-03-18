package com.micdm.okeybalance.utils;

public class MarketUtils {

    public static String getMarketUri(String packageName) {
        return String.format("https://play.google.com/store/apps/details?id=%s", packageName);
    }
}
