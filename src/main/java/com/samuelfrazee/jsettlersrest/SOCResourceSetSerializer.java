package com.samuelfrazee.jsettlersrest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.stereotype.Component;
import soc.game.*;

import java.io.IOException;

@Component
public class SOCResourceSetSerializer extends StdSerializer<SOCResourceSet> {

    public SOCResourceSetSerializer() {
        super(SOCResourceSet.class);
    }

    @Override
    public void serialize(SOCResourceSet resourceSet, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jgen.writeStartObject();

        jgen.writeNumberField("total", resourceSet.getTotal());
        jgen.writeNumberField("knownTotal", resourceSet.getKnownTotal());
        jgen.writeBooleanField("empty", resourceSet.isEmpty());

        jgen.writeNumberField("clay", resourceSet.getAmount(SOCResourceConstants.CLAY));
        jgen.writeNumberField("ore", resourceSet.getAmount(SOCResourceConstants.ORE));
        jgen.writeNumberField("wood", resourceSet.getAmount(SOCResourceConstants.WOOD));
        jgen.writeNumberField("wheat", resourceSet.getAmount(SOCResourceConstants.WHEAT));
        jgen.writeNumberField("sheep", resourceSet.getAmount(SOCResourceConstants.SHEEP));
        //jgen.writeNumberField("unknown", amounts[SOCResourceConstants.UNKNOWN]);

        jgen.writeEndObject();

    }

}
