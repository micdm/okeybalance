package com.micdm.okeybalance.content;

import com.micdm.okeybalance.exceptions.WrongCredentialsException;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccountPageParser {

    protected static final Pattern pattern = Pattern.compile("<span class=\"lkuibalance\">([\\d\\. ]+) р.</span>");

    public static BigDecimal parse(String content) throws WrongCredentialsException {
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            throw new WrongCredentialsException();
        }
        String result = matcher.group(1);
        if (result == null) {
            throw new WrongCredentialsException();
        }
        return new BigDecimal(result.replace(" ", ""));
    }
}
