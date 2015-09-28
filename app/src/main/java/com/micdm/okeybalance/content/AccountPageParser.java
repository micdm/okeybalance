package com.micdm.okeybalance.content;

import com.micdm.okeybalance.exceptions.WrongCredentialsException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccountPageParser {

    protected static final Pattern pattern = Pattern.compile("<td>Баланс: </td><td>([\\d\\. ]+) р.</td>");

    public static String parse(String content) throws WrongCredentialsException {
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            throw new WrongCredentialsException();
        }
        String result = matcher.group(1);
        if (result == null) {
            throw new WrongCredentialsException();
        }
        return result.replace(" ", "");
    }
}
