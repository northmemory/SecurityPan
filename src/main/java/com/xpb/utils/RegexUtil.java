package com.xpb.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtil {
    public static boolean isEmail(String email){
        String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public static boolean verify(String target,String regex){
        Pattern pattern = Pattern.compile(target);
        Matcher matcher = pattern.matcher(regex);
        return matcher.matches();
    }
}
