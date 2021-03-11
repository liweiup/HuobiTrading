package com.contract.harvest.config;
import com.huobiswap.api.service.account.AccountAPIServiceImpl;
import com.huobiswap.api.service.market.MarketAPIServiceImpl;
import com.huobiswap.api.service.trade.TradeAPIServiceImpl;
import com.huobiswap.api.service.transfer.TransferApiServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;

@EnableAspectJAutoProxy
@Configuration
@PropertySource(value = {"classpath:exchange.properties"})
public class HuobiSwapConfig {
    @Value("${huobi.api_key}")
    private String huobiApiKey;

    @Value("${huobi.secret_key}")
    private String huobiSecretKey;

    @Value("${huobi.huobi_api_url}")
    private String huobiApiUrl;

    //合约账户信息api
    @Bean("swapAccountApi")
    public AccountAPIServiceImpl accountApi() {
        return new AccountAPIServiceImpl(huobiApiKey, huobiSecretKey);
    }
    //合约市场信息api
    @Bean("swapMarketApi")
    public MarketAPIServiceImpl marketApi() {
        return new MarketAPIServiceImpl();
    }
    //订单交易api
    @Bean("swapTradeApi")
    public TradeAPIServiceImpl tradeApi() {
        return new TradeAPIServiceImpl(huobiApiKey, huobiSecretKey);
    }
    //账户划转api
    @Bean("swapTransferApi")
    public TransferApiServiceImpl transferApi() {
        return new TransferApiServiceImpl(huobiApiKey, huobiSecretKey);
    }
}
