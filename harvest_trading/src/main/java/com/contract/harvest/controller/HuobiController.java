package com.contract.harvest.controller;

import com.contract.harvest.view.ContractInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;

@Api(value = "HuobiController",tags = "火币")
@RestController
@ResponseBody
@RequestMapping("/huobi")
public class HuobiController {

    @Resource
    private ContractInfo contractInfo;

    @ApiOperation(value = "获取成交记录")
    @RequestMapping(value = "/getContractList", method = {
            RequestMethod.GET, RequestMethod.POST})
    @ApiImplicitParam(name="keys",
            value="redis-key",
            dataType="String",
            paramType = "query",
            defaultValue = "HB:DELIVERY:CONTRACT:ORDER_DEAL_OID:BSV,HB:DELIVERY:CONTRACT:ORDER_LOSS:BSV,HB:DELIVERY:CONTRACT:ORDER_WIN:BSV",
            required = true)
    public Object getContractList(@RequestParam String[] keys) {
        return contractInfo.getContractList(keys);
    }

    @ApiOperation(value = "获取持仓信息记录")
    @RequestMapping(value = "/getSpaceInfo", method = {
            RequestMethod.GET, RequestMethod.POST})
    @ApiImplicitParam(name="keys",
            value="symbol",
            dataType="String",
            paramType = "query",
            defaultValue = "BSV,BTC",
            required = true)
    public Object getSpaceInfo(@RequestParam String[] keys) {
        return contractInfo.getSpaceInfo(keys);
    }

    @ApiOperation(value = "获取监控的币")
    @RequestMapping(value = "/getSymbols", method = {
            RequestMethod.GET, RequestMethod.POST})
    public Object getSymbols() {
        return contractInfo.getSymbols();
    }


}
