package com.contract.harvest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailService {

    @Value("${spring.mail.username}")
    private String fromMail;

    @Autowired
    private JavaMailSenderImpl mailSender;

    private static final String MY_RECEIVE_MAIL = "321327476@qq.com";

    public void sendMail(String topic,String context,String receiveAddress) {
        String toMail = "";
        if ("".equals(receiveAddress)) {
            toMail = MY_RECEIVE_MAIL;
        }
        SimpleMailMessage simpleMailMessage =  new SimpleMailMessage();
        simpleMailMessage.setFrom(fromMail);
        simpleMailMessage.setSubject(topic);
        simpleMailMessage.setText(context);
        simpleMailMessage.setTo(toMail);
        try {
            mailSender.send(simpleMailMessage);
        }catch (Exception e) {
            log.info(e.getMessage());
        }
    }
}
