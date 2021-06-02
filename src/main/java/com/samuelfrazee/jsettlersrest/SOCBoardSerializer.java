package com.samuelfrazee.jsettlersrest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.stereotype.Component;
import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCRoutePiece;
import soc.game.SOCSettlement;

import java.io.IOException;
import java.util.List;

@Component
public class SOCBoardSerializer extends StdSerializer<SOCBoard> {

    public SOCBoardSerializer() {
        super(SOCBoard.class);
    }

    @Override
    public void serialize(SOCBoard board, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jgen.writeStartObject();

        jgen.writeFieldName("hexLayout");
        jgen.writeArray(board.getHexLayout(), 0, board.getHexLayout().length);

        jgen.writeFieldName("landHexCoords");
        jgen.writeArray(board.getLandHexCoords(), 0, board.getLandHexCoords().length);

        jgen.writeFieldName("numberLayout");
        jgen.writeArray(board.getNumberLayout(), 0, board.getNumberLayout().length);

        jgen.writeFieldName("portsLayout");
        if (board.getPortsLayout() == null) {
            jgen.writeNull();
        } else {
            jgen.writeArray(board.getPortsLayout(), 0, board.getPortsLayout().length);
        }

        jgen.writeFieldName("portsFacing");
        if (board.getPortsFacing() == null) {
            jgen.writeNull();
        } else {
            jgen.writeArray(board.getPortsFacing(), 0, board.getPortsFacing().length);
        }

        jgen.writeFieldName("portsEdges");
        if (board.getPortsEdges() == null) {
            jgen.writeNull();
        } else {
            jgen.writeArray(board.getPortsEdges(), 0, board.getPortsEdges().length);
        }

        jgen.writeNumberField("robberHex", board.getRobberHex());
        jgen.writeNumberField("previousRobberHex", board.getPreviousRobberHex());
        jgen.writeNumberField("portsCount", board.getPortsCount());

        // Prevent infinite JSON recursion
        jgen.writeArrayFieldStart("roadsAndShips");
        for (SOCRoutePiece piece : board.getRoadsAndShips()) {
            jgen.writeStartObject();
            jgen.writeStringField("type", piece.getTypeName(piece.getType()));
            jgen.writeStringField("player", piece.getPlayer().getName());
            jgen.writeNumberField("playerNumber", piece.getPlayerNumber());
            jgen.writeNumberField("coordinates", piece.getCoordinates());
            jgen.writeEndObject();
        }
        jgen.writeEndArray();

        jgen.writeArrayFieldStart("settlements");
        for(SOCSettlement piece: board.getSettlements()) {
            jgen.writeStartObject();
            jgen.writeStringField("player", piece.getPlayer().getName());
            jgen.writeNumberField("playerNumber", piece.getPlayerNumber());
            jgen.writeNumberField("coordinates", piece.getCoordinates());
            jgen.writeEndObject();
        }
        jgen.writeEndArray();

        jgen.writeArrayFieldStart("cities");
        for(SOCCity piece: board.getCities()) {
            jgen.writeStartObject();
            jgen.writeStringField("player", piece.getPlayer().getName());
            jgen.writeNumberField("playerNumber", piece.getPlayerNumber());
            jgen.writeNumberField("coordinates", piece.getCoordinates());
            jgen.writeEndObject();
        }
        jgen.writeEndArray();

        jgen.writeNumberField("boardWidth", board.getBoardWidth());
        jgen.writeNumberField("boardHeight", board.getBoardHeight());
        jgen.writeNumberField("boardEncodingFormat", board.getBoardEncodingFormat());

        jgen.writeEndObject();
    }
}
