package com.samuelfrazee.jsettlersrest;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import soc.baseclient.ServerConnectInfo;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class JsettlersrestApplication {

    public static JSettlersClient jsettlersClient;

    public static void main(String[] args) {
        SpringApplication.run(JsettlersrestApplication.class, args);
    }

    @PostConstruct
    public void init() {
        jsettlersClient = new JSettlersClient(new ServerConnectInfo("", 8880, "1234"));
        jsettlersClient.init();
    }
}