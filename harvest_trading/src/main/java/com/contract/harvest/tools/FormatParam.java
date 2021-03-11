package com.contract.harvest.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.contract.harvest.entity.Candlestick;
import com.xiaoleilu.hutool.date.DateTime;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

public class FormatParam<T>  {
    /**
     * json转为List Candlestick
     */
    public static List<Candlestick> getCandlestickList(String candleJson) {
        JSONArray candleJsonArr = JSON.parseObject(candleJson).getJSONArray("data");
        List<Candlestick> candlestickList = new ArrayList<>();
        for (int i = 0; i < candleJsonArr.size(); i++) {
            Candlestick candlestick = JSON.parseObject(candleJsonArr.getString(i),Candlestick.class);
            candlestickList.add(candlestick);
        }
        return candlestickList;
    }
    /**
     * 获取json一列值
     */
    public static double[] getArrColumn(JSONArray arr, String key,Boolean reverseFlag) {
        if (arr.size() <= 0 ) {
            return null;
        }
        if (reverseFlag) {
            Collections.reverse(arr);
        }
        double[] out = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            JSONObject jobj = arr.getJSONObject(i);
            double value = jobj.getDoubleValue(key);
            out[i] = value;
        }
        return out;
    }
    /*
     * 将时间转换为时间戳
     */
    public static String dateToStamp(String s) throws ParseException {
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse(s);
        long ts = date.getTime();
        res = String.valueOf(ts);
        return res;
    }
    /*
     * 将时间戳转换为时间
     */
    public static String stampToDate(long timestamp){
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(timestamp*1000);
        res = simpleDateFormat.format(date);
        return res;
    }
    /**
     * 获取当前秒
     */
    public static int getSecond(){
        return LocalDateTime.now().getSecond();
    }
    public static int get(){
        return LocalDateTime.now().getSecond();
    }
    /**
     * 获取精确到秒的时间戳
     */
    public static Long getSecondTimestamp(){
        Date date = new DateTime();
        String timestamp = String.valueOf(date.getTime()/1000);
        return Long.parseLong(timestamp);
    }

}
