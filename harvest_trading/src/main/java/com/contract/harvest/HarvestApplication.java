package com.contract.harvest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

//@SpringBootApplication(exclude= {DataSourceAutoConfiguration.class})
//@EnableAsync
//@EnableScheduling
//@EnableCaching

public class HarvestApplication {

	//设置时区 相差8小时
	@PostConstruct
	void started() {
		TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
	}


	public static void main(String[] args) {

//		SpringApplication.run(HarvestApplication.class, args);
		List<String> list = new ArrayList<String>();
		list.add("AA");
		list.add("BBB");
		list.add("CCCC");
		list.add("DDDD");
		list.add("EEE");
		//删除元素后必须break跳出，否则报出异常
		for (String s : list) {
			if (s.length() == 4) {
				list.remove(s);
				break;
			}
		}
			System.out.println(list);
	}

}
