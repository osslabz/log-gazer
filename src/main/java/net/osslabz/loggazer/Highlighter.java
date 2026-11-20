package net.osslabz.loggazer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Highlighter {

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
        if (text.isEmpty()) {
            return computeEmptyStyle(text);
        }
        if (JsonUtils.textMightBeJson(text)) {
            return highlightLogLevelJson(text);
        } else {
            return highlightLogLevelRegularFile(text);
        }
    }

    private static StyleSpans<Collection<String>> highlightLogLevelJson(String text) {

        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        String logLevelForCurrentSegment = null;
        boolean insideJson = false;

        int logSegmentLength = 0;

        int numLines = 0;
        int numSegmentsOpened = 0;
        int numSegmentsClosed = 0;

        for (Line current : lines(text)) {

            String line = current.content();
            int fullLineLength = current.length();

            if ("{".equals(line)) {
                insideJson = true;
                logSegmentLength = fullLineLength;
                numSegmentsOpened++;
            } else if ("}".equals(line)) {
                addSegment(spansBuilder, logLevelForCurrentSegment, logSegmentLength + fullLineLength);
                logLevelForCurrentSegment = null;
                insideJson = false;
                logSegmentLength = 0;
                numSegmentsClosed++;
            } else {
                if (insideJson) {
                    logSegmentLength += fullLineLength;
                    if (logLevelForCurrentSegment == null) {
                        logLevelForCurrentSegment = determineLogLevelForLine(line);
                    }
                } else if (JsonUtils.lineMightBeJson(line)) {
                    String logLevel = determineLogLevelForLine(line);
                    spansBuilder.add(
                            logLevel != null ? List.of(logLevel.toLowerCase(Locale.ROOT)) : Collections.emptyList(),
                            fullLineLength);
                } else {
                    spansBuilder.add(Collections.emptyList(), fullLineLength);
                }
            }
            numLines++;
        }
        if (insideJson) {
            addSegment(spansBuilder, logLevelForCurrentSegment, logSegmentLength);
        }
        log.debug(
                "file contains {} lines, with {} numSegmentsOpened and {}  numSegmentsClosed closed",
                numLines,
                numSegmentsOpened,
                numSegmentsClosed);

        return spansBuilder.create();
    }

    private static void addSegment(StyleSpansBuilder<Collection<String>> spansBuilder, String logLevel, int length) {
        spansBuilder.add(
                logLevel != null ? List.of(logLevel.toLowerCase(Locale.ROOT)) : Collections.emptyList(), length);
    }

    // Offsets must match the code area, so every line carries the length of its own \r\n, \n or \r terminator.
    private static List<Line> lines(String text) {
        List<Line> lines = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = start;
            while (end < text.length() && text.charAt(end) != '\n' && text.charAt(end) != '\r') {
                end++;
            }
            int next = end;
            if (next < text.length()) {
                next += text.startsWith("\r\n", end) ? 2 : 1;
            }
            lines.add(new Line(text.substring(start, end), next - start));
            start = next;
        }
        return lines;
    }

    private record Line(String content, int length) {}

    private static String determineLogLevelForLine(String line) {
        String firstLogLevel = null;
        int firstIndex = Integer.MAX_VALUE;
        for (String logLevel : LOG_LEVEL) {
            int index = line.indexOf(logLevel);
            if (index != -1 && index < firstIndex) {
                firstLogLevel = logLevel;
                firstIndex = index;
            }
        }
        return firstLogLevel;
    }

    static StyleSpans<Collection<String>> highlightLogLevelRegularFile(String text) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        int numLines = 0;
        String lastLogLevel = null;

        for (Line current : lines(text)) {
            numLines++;

            List<String> styles = new ArrayList<>(2);

            String logLevel = determineLogLevelForLine(current.content());
            if (logLevel != null) {
                styles.add(logLevel.toLowerCase(Locale.ROOT));
                lastLogLevel = logLevel;
            } else if (lastLogLevel != null) {
                log.debug("No log level found on line {}, using previous level {}", numLines, lastLogLevel);
                styles.add(lastLogLevel.toLowerCase(Locale.ROOT));
            } else {
                log.trace("No log level found on line {} and no previous logLevel set, no highlighting", numLines);
            }
            spansBuilder.add(styles, current.length());
        }

        return spansBuilder.create();
    }
}
