package com.gmail.dev.zhilin.analizator.util;

import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    public static Date plusDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        calendar.add(Calendar.DATE, days);

        return calendar.getTime();
    }

    public static Date minusDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        calendar.add(Calendar.DATE, (0 - days));

        return calendar.getTime();
    }

}
