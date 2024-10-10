package net.osslabz.loggazer;

import com.fasterxml.jackson.core.JsonFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Highlighter {

    public static final Logger log = LoggerFactory.getLogger(Highlighter.class);

    private static final Set<String> LOG_LEVEL = Set.of("FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE");

    private Highlighter() {
        // intentionally empty
    }


    static StyleSpans<Collection<String>> computeEmptyStyle(String currentText) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        spansBuilder.add(Collections.emptyList(), currentText.length());
        return spansBuilder.create();
    }

    static StyleSpans<Collection<String>> highlightLogLevel(String text) {
        if (JsonUtils.textMightBeJson(text)) {
            return highlightLogLevelJson(text);
        } else {
            return highlightLogLevelRegularFile(text);
        }
    }

    private static StyleSpans<Collection<String>> highlightLogLevelJson(String text) {


        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {

            String line = null;
            String logLevelForCurrentSegment = null;
            boolean insideJson = false;

            int logSegmentLength = 0;

            int numLines = 0;
            int numSegmentsOpened = 0;
            int numSegmentsClosed = 0;

            while ((line = reader.readLine()) != null) {

                int fullLineLength = line.length() + 1;

                if (line.equals("{")) {
                    insideJson = true;
                    logSegmentLength = fullLineLength;
                    numSegmentsOpened++;
                } else if (line.equals("}")) {
                    if (logLevelForCurrentSegment != null) {
                        spansBuilder.add(List.of(logLevelForCurrentSegment.toLowerCase()), logSegmentLength + fullLineLength);
                    } else {
                        spansBuilder.add(Collections.emptyList(), logSegmentLength + fullLineLength);
                    }
                    logLevelForCurrentSegment = null;
                    insideJson = false;
                    logSegmentLength = 0;
                    numSegmentsClosed++;
                } else {
                    if (insideJson) {
                        logSegmentLength += fullLineLength;
                        if (logLevelForCurrentSegment == null) {
                            logLevelForCurrentSegment = determineLogLevelForSegment(line);
                        }
                    } else {
                        spansBuilder.add(Collections.emptyList(), fullLineLength);
                    }
                }
                numLines++;
            }
            log.debug("file contains {} lines, with {} numSegmentsOpened and {}  numSegmentsClosed closed", numLines, numSegmentsOpened,
                    numSegmentsClosed);

        } catch (IOException e) {
            e.printStackTrace();
        }

        StyleSpans<Collection<String>> styleSpans = spansBuilder.create();

        return styleSpans;
    }


    private static String determineLogLevelForSegment(String line) {
        for (String logLevel : LOG_LEVEL) {
            if (line.contains(logLevel)) {
                return logLevel;
            }
        }
        return null;
    }


    static StyleSpans<Collection<String>> highlightLogLevelRegularFile(String text) {
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