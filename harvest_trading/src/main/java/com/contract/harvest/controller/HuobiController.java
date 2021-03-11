package com.contract.harvest.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseBody
@RequestMapping("/huobi")
public class HuobiController {

    @RequestMapping(value = "/test", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = {
            RequestMethod.GET, RequestMethod.POST})
    public Object test() {
        return "1212";
    }
}
