package com.contract.harvest.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class ElesService {

    @Value("${logging.file.path}")
    private String log_path;

    public StringBuffer get_harvest_log(String dateStr) throws IOException,NullPointerException {
        if (dateStr.equals("")) {
            Date date = new Date();
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd");
            dateStr = simpleDate.format(date);
        }
        String path = log_path+"harvest-"+dateStr+".log";
        String line = "";
        StringBuffer buffer = new StringBuffer();
        InputStream logFileInputStream = new FileInputStream(path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(logFileInputStream));
        line = reader.readLine(); // 读取第一行
        while (line != null) { // 如果 line 为空说明读完了
            buffer.append(line).append("\n"); // 将读到的内容添加到 buffer 中
            line = reader.readLine(); // 读取下一行
        }
        logFileInputStream.close();
        reader.close();
        return buffer;
    }

}
