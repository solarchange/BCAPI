package com.parallax.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.parallax.dto.multiaddr.Tx;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * Takes care of different timestamp formats that are retuned by the blockchain api.
 */
public class SolyaraDateDeserializer extends JsonDeserializer<Tx> {

    /**
     * blockchain api date formats
     */
    private static final String[] DATE_FORMATS = new String[] {
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm'Z'"
    };

    /**
     * Deserializes blochain api transactions information and sets all the fields.
     */
    @Override
    public Tx deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String hash = node.get("hash").asText();
        int confirmations = (Integer) node.get("confirmations").numberValue();
        Long change = node.get("change").longValue();
        String dateString = node.get("time_utc").asText();
        Date date = null;

        for (String format : DATE_FORMATS) {
            try {
                date = new SimpleDateFormat(format, Locale.US).parse(dateString);
            } catch (ParseException ignored) {
            }
        }
        if (date == null) {
            throw new IllegalArgumentException("Unparseable date: \"" + dateString
                    + "\". Supported formats: " + Arrays.toString(DATE_FORMATS));
        }

        return new Tx(hash, confirmations, change, date);
    }
}
