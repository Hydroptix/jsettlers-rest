package com.samuelfrazee.jsettlersrest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import soc.baseclient.ServerConnectInfo;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class JsettlersrestApplication {

    public static JSettlersClient jsettlersClient;

    public static void main(String[] args)
    {
        SpringApplication.run(JsettlersrestApplication.class, args);
    }

    @PostConstruct
    public void init() {
        jsettlersClient = new JSettlersClient(new ServerConnectInfo("", 8880, "1234"));
        jsettlersClient.init();
    }

}
