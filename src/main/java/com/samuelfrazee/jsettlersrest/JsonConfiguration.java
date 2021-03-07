package com.samuelfrazee.jsettlersrest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import soc.game.SOCPlayer;

@SpringBootConfiguration
public class JsonConfiguration {

    @Autowired
    private SOCPlayerSerializer socPlayerSerializer;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.serializerByType(SOCPlayer.class, socPlayerSerializer);
    }
}
