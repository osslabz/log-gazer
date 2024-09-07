package net.osslabz.loggazer;

import com.fasterxml.jackson.core.JsonFactory;
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

    static StyleSpans<Collection<String>> highlightLogLevel(String text) {
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
}