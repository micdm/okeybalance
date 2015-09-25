package com.micdm.okeybalance.content;

import com.micdm.okeybalance.exceptions.AuthRequiredException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccountPageParser {

    private static final Pattern pattern = Pattern.compile("<td>Баланс: </td><td>([\\d\\. ]+) р.</td>");

    public static String parse(String content) throws AuthRequiredException {
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            throw new AuthRequiredException();
        }
        String result = matcher.group(1);
        if (result == null) {
            throw new AuthRequiredException();
        }
        return result.replace(" ", "");
    }
}
