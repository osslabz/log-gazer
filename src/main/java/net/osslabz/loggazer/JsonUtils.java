package net.osslabz.loggazer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

@Slf4j
public class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    static boolean textMightBeJson(String currentText) {
        String trimmed = currentText.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    static String format(String text) {

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            int lineNumber = 1;
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (textMightBeJson(line)) {
                    log.debug("line {} appears to be JSON", lineNumber);
                    sb.append(formatLine(line)).append("\n");
                } else {
                    log.debug("line {} does not appear to be JSON", lineNumber);
                    sb.append(line).append("\n");
                }
                lineNumber++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    private static String formatLine(String text) {
        try {
            Object json = OBJECT_MAPPER.readValue(text, Object.class);
            return OBJECT_MAPPER.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            log.warn("Couldn't format JSON.", e);
        }
        return text;
    }
}