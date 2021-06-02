package com.samuelfrazee.jsettlersrest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.stereotype.Component;
import soc.game.SOCInventory;
import java.io.IOException;

@Component
public class SOCInventorySerializer extends StdSerializer<SOCInventory> {

    public SOCInventorySerializer() {
        super(SOCInventory.class);
    }

    @Override
    public void serialize(SOCInventory inventory, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jgen.writeStartObject();

        jgen.writeNumberField("total", inventory.getTotal());
        jgen.writeNumberField("numVPItems", inventory.getNumVPItems());
        jgen.writeNumberField("numUnplayed", inventory.getNumUnplayed());

        jgen.writeObjectField("new", inventory.getByState(SOCInventory.NEW));
        jgen.writeObjectField("playable", inventory.getByState(SOCInventory.PLAYABLE));
        jgen.writeObjectField("kept", inventory.getByState(SOCInventory.KEPT));

        jgen.writeEndObject();

    }

}
