package org.hzz.rpc.client.config;

import org.hzz.rpc.client.rpc.RpcClientFrame;
import org.hzz.rpc.remote.SendSms;
import org.hzz.rpc.remote.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 类说明：
 */
@Configuration
public class BeanConfig {

    @Autowired
    private RpcClientFrame rpcClientFrame;


    @Bean
    public SendSms getSmsService() throws Exception{
        return rpcClientFrame.getRemoteProxyObject(SendSms.class);
    }

    @Bean
    public StockService getStockService() throws Exception{
        return rpcClientFrame.getRemoteProxyObject(StockService.class);
    }
}
