package com.atguigu.core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhonecodeUtil {

    public static boolean isMobile(String phoneNum) {
        String regex = "^[1](([3|5|8][\\d])|([4][4,5,6,7,8,9])|([6][2,5,6,7])|([7][^9])|([9][1,8,9]))[\\d]{8}$";
        if (phoneNum.length() != 11) {
            return false;
        } else {
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(phoneNum);
            boolean b = m.matches();
            return b;
        }
    }
}
