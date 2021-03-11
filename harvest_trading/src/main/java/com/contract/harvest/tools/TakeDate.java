package com.contract.harvest.tools;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TakeDate {
    public static List<Long> getDateList(int offset) {
        Date start = dayStartDate(new Date());//转换为天的起始date
        Date nextDayDate = endDay(start);//下一天的date
        List<Long> result = new ArrayList<Long>();
        while (start.compareTo(nextDayDate) < 0) {
            result.add(start.getTime() / 1000);
            start = addFiveMin(start, offset);
        }
        return result;
    }

    private static Date addFiveMin(Date start, int offset) {
        Calendar c = Calendar.getInstance();
        c.setTime(start);
        c.add(Calendar.MINUTE, offset);
        return c.getTime();
    }

    private static Date endDay(Date start) {
        Calendar c = Calendar.getInstance();
//        c.setTime(start);
//        c.add(Calendar.DATE, 1);
        return c.getTime();
    }

    private static Date dayStartDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
