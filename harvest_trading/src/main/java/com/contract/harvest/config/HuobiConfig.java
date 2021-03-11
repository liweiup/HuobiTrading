package com.contract.harvest.config;

import com.contract.harvest.Aspects.HuobiServiceAspects;
import com.huobi.api.service.account.AccountAPIServiceImpl;
import com.huobi.api.service.market.MarketAPIServiceImpl;
import com.huobi.api.service.trade.TradeAPIServiceImpl;
import com.huobi.api.service.transfer.TransferApiServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;


@EnableAspectJAutoProxy
@Configuration
//@PropertySource(value = {"classpath:exchange.properties"})

public class HuobiConfig {

    @Value("${huobi.api_key}")
    private String huobiApiKey;

    @Value("${huobi.secret_key}")
    private String huobiSecretKey;

    @Value("${huobi.huobi_api_url}")
    private String huobiApiUrl;

	//合约账户信息api
    @Bean("accountApi")
    public AccountAPIServiceImpl accountApi() {
        return new AccountAPIServiceImpl(huobiApiKey, huobiSecretKey);
    }
    //合约市场信息api
    @Bean("marketApi")
    public MarketAPIServiceImpl marketApi() {
        return new MarketAPIServiceImpl();
    }
    //订单交易api
    @Bean("tradeApi")
    public TradeAPIServiceImpl tradeApi() {
        return new TradeAPIServiceImpl(huobiApiKey, huobiSecretKey);
    }
    //账户划转api
    @Bean("transferApi")
    public TransferApiServiceImpl transferApi() {
        return new TransferApiServiceImpl(huobiApiKey, huobiSecretKey);
    }
    /***永续合约****/
	@Bean
    public AccountAPIServiceImpl swapGet() {
        return new AccountAPIServiceImpl(huobiApiKey, huobiSecretKey);
    }


    @Bean
    public HuobiServiceAspects huobiServiceAspects(){
        return new HuobiServiceAspects();
    }

}
