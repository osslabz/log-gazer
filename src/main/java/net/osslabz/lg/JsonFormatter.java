package net.osslabz.lg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonFormatter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    static String format(String text) {
        try {
            Object json = OBJECT_MAPPER.readValue(text, Object.class);
            String formattedJson = OBJECT_MAPPER.writeValueAsString(json);
            return formattedJson;
        } catch (JsonProcessingException e) {
            log.warn("Couldn't format JSON. error: {}", e.getLocation());
        }
        return text;
    }
}