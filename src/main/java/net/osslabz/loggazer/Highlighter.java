package net.osslabz.loggazer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
public class Highlighter {

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private static final Set<String> LOG_LEVEL = Set.of("FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE");

    static StyleSpans<Collection<String>> computeEmptyStyle(String currentText) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        spansBuilder.add(Collections.emptyList(), currentText.length());
        return spansBuilder.create();
    }

    static StyleSpans<Collection<String>> highlighLogLevel(String text) {
        if (JsonUtils.textMightBeJson(text)) {
            return highlightLogLevelJson(text);
        } else {
            return highlightRegularFile(text);
        }
    }

    private static StyleSpans<Collection<String>> highlightLogLevelJson(String text) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        int numLines = 0;

        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {

            String line = null;
            int logSegmentLength = 0;
            String segmentLevel = null;
            while ((line = reader.readLine()) != null) {
                numLines++;

                if (line.equals("{")) {
                    logSegmentLength = line.length() + 1;
                    segmentLevel = null;
                } else if (line.equals("}")) {
                    spansBuilder.add(List.of(segmentLevel.toLowerCase()), logSegmentLength + line.length() + 1);
                } else {
                    logSegmentLength += line.length() + 1;

                    for (String logLevel : LOG_LEVEL) {
                        if (line.contains(logLevel)) {
                            segmentLevel = logLevel;
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        StyleSpans<Collection<String>> styleSpans = spansBuilder.create();

        log.debug("file contains {} lines, {} spans calculated", numLines, styleSpans.length());

        return styleSpans;
    }

    static StyleSpans<Collection<String>> highlightRegularFile(String text) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        int numLines = 0;

        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {

            String lastLogLevel = null;

            String line = null;
            while ((line = reader.readLine()) != null) {
                numLines++;

                boolean matchedLine = false;

                List<String> styles = new ArrayList<>(2);

                for (String logLevel : LOG_LEVEL) {
                    if (line.contains(logLevel)) {
                        styles.add(logLevel.toLowerCase());
                        lastLogLevel = logLevel;
                        matchedLine = true;
                        break;
                    }
                }
                if (!matchedLine) {
                    log.debug("No log level found on line {}, using previous level {}", numLines, lastLogLevel);
                    styles.add(lastLogLevel.toLowerCase());
                }

                spansBuilder.add(styles, line.length() + 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        StyleSpans<Collection<String>> styleSpans = spansBuilder.create();

        log.debug("file contains {} lines, {} spans calculated", numLines, styleSpans.length());

        return styleSpans;
    }


    static StyleSpans<Collection<String>> highlightJson(String code) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        int lastPos = 0;
        try {
            JsonParser parser = JSON_FACTORY.createParser(code);
            while (!parser.isClosed()) {
                JsonToken jsonToken = parser.nextToken();

                int length = parser.getTextLength();
                // Because getTextLength() does contain the surrounding ""
                if (jsonToken == JsonToken.VALUE_STRING || jsonToken == JsonToken.FIELD_NAME) {
                    length += 2;
                }

                String className = jsonTokenToClassName(jsonToken);
                if (!className.isEmpty()) {
                    int start = (int) parser.getTokenLocation().getCharOffset();
                    // Fill the gaps, since Style Spans need to be contiguous.
                    if (start > lastPos) {
                        int noStyleLength = start - lastPos;
                        spansBuilder.add(Collections.emptyList(), noStyleLength);
                    }
                    lastPos = start + length;

                    spansBuilder.add(Collections.singleton(className), length);
                }
            }
        } catch (IOException e) {
            // Ignoring JSON parsing exception in the context of
            // syntax highlighting
        }
        if (lastPos == 0) {
            spansBuilder.add(Collections.emptyList(), code.length());
        }
        return spansBuilder.create();
    }

    static String jsonTokenToClassName(JsonToken jsonToken) {
        if (jsonToken == null) {
            return "";
        }
        switch (jsonToken) {
            case FIELD_NAME:
                return "json-property";
            case VALUE_STRING:
                return "json-string";
            case VALUE_NUMBER_FLOAT:
            case VALUE_NUMBER_INT:
                return "json-number";
            default:
                return "";
        }
    }
}