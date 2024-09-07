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

    static boolean textMightBeJson(String text) {
        if (lineMightBeJson(text)) {
            return true;
        }
        int numLines = 0;
        int numJsonLines = 0;
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                numLines++;
                if (lineMightBeJson(line)) {
                    numJsonLines++;
                    if (numJsonLines > 5) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return numLines == numJsonLines;
    }


    private static boolean lineMightBeJson(String line) {
        String trimmed = line.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }


    static String format(String text) {

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (lineMightBeJson(line)) {
                    sb.append(formatLine(line)).append(System.lineSeparator());
                } else {
                    sb.append(line).append(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String formattedJson = sb.toString();
        formattedJson = formattedJson.replace("\\n", System.lineSeparator());
        formattedJson = formattedJson.replace("\\t", "\t");
        return formattedJson;
    }


    private static String formatLine(String line) {
        try {
            Object json = OBJECT_MAPPER.readValue(line, Object.class);
            return OBJECT_MAPPER.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            log.warn("Couldn't format JSON.", e);
        }
        return line;
    }
}