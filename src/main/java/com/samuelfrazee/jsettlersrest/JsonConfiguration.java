package com.samuelfrazee.jsettlersrest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import soc.game.SOCBoard;
import soc.game.SOCInventory;
import soc.game.SOCPlayer;
import soc.game.SOCResourceSet;

@SpringBootConfiguration
public class JsonConfiguration {

    @Autowired
    private SOCPlayerSerializer socPlayerSerializer;

    @Autowired
    private SOCBoardSerializer socGameSerializer;

    @Autowired
    private SOCResourceSetSerializer socResourceSetSerializer;

    @Autowired
    private SOCInventorySerializer socInventorySerializer;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder ->
                builder.serializerByType(SOCPlayer.class, socPlayerSerializer)
                        .serializerByType(SOCBoard.class, socGameSerializer)
                        .serializerByType(SOCResourceSet.class, socResourceSetSerializer)
                        .serializerByType(SOCInventory.class, socInventorySerializer);
    }
}
