package net.osslabz.loggazer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HighlighterTest {

    @Test
    void usesLevelThatComesFirstInLine() {
        String text =
                "2025-01-01 INFO retry after ERROR from upstream\n2025-01-01 ERROR retry after INFO from upstream\n";

        List<Collection<String>> styles = Highlighter.highlightLogLevelRegularFile(text).stream()
                .map(StyleSpan::getStyle)
                .toList();

        assertEquals(List.of(List.of("info"), List.of("error")), styles);
    }

    @Test
    void highlightsEveryLineOfJsonLinesLog() throws Exception {
        String text = readTestData("example-json-ecs.log");

        assertEquals(List.of(), unstyledSpans(Highlighter.highlightLogLevel(text)));
    }

    @Test
    void highlightsEveryLineOfFormattedJsonLog() throws Exception {
        // the code area holds line feeds only, whatever separator the formatter wrote
        String text = JsonUtils.format(readTestData("example-json-ecs.log")).replace(System.lineSeparator(), "\n");

        assertEquals(List.of(), unstyledSpans(Highlighter.highlightLogLevel(text)));
    }

    @Test
    void usesLocaleIndependentStyleNamesInTurkishLocale() {
        String regularLog = "2025-01-01 INFO started\ncontinued without level\n";
        String jsonLog = "{\"level\":\"INFO\"}\n{\n  \"level\" : \"INFO\"\n}\n";

        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            assertEquals(
                    List.of(List.of("info")),
                    Highlighter.highlightLogLevel(regularLog).stream()
                            .map(StyleSpan::getStyle)
                            .toList());
            assertEquals(
                    List.of(List.of("info")),
                    Highlighter.highlightLogLevel(jsonLog).stream()
                            .map(StyleSpan::getStyle)
                            .toList());
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    void styledTextSpansCoverEveryCharacter() {
        String text = "INFO a\nERROR b\nplain continues\n";

        assertEquals(text.length(), Highlighter.highlightLogLevel(text).length());
    }

    @Test
    void emptyTextProducesNoSpansWithoutFailing() {
        assertDoesNotThrow(() -> Highlighter.highlightLogLevel(""));
        assertEquals(0, Highlighter.highlightLogLevel("").length());
    }

    @Test
    void lineWithoutLevelBeforeAnyLevelIsUnstyled() {
        List<Collection<String>> styles = styles(Highlighter.highlightLogLevelRegularFile("preamble\nINFO started\n"));

        assertEquals(List.of(List.of(), List.of("info")), styles);
    }

    @Test
    void lineWithoutLevelInheritsPreviousLevel() {
        String text = "ERROR boom\n\tat Foo.bar(Foo.java:1)\nINFO next\n";

        assertEquals(
                List.of(List.of("error"), List.of("info")), styles(Highlighter.highlightLogLevelRegularFile(text)));
    }

    @Test
    void adjacentLinesOfSameLevelMergeIntoOneSpan() {
        StyleSpans<Collection<String>> spans = Highlighter.highlightLogLevelRegularFile("WARN a\nWARN b\nWARN c\n");

        assertEquals(1, spans.getSpanCount());
        assertEquals("WARN a\nWARN b\nWARN c\n".length(), spans.length());
    }

    @ParameterizedTest
    @ValueSource(strings = {"FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"})
    void recognizesEveryLogLevel(String level) {
        assertEquals(
                List.of(List.of(level.toLowerCase(Locale.ROOT))),
                styles(Highlighter.highlightLogLevelRegularFile("2025-01-01 " + level + " message\n")));
    }

    @Test
    void matchesLevelsCaseSensitively() {
        assertEquals(List.of(List.of()), styles(Highlighter.highlightLogLevelRegularFile("info error warn\n")));
    }

    @Test
    void doesNotTreatLevelRegexSpecialCharactersAsPattern() {
        String text = "[INFO] (.*) $^ \\ [ERROR]\n";

        assertEquals(List.of(List.of("info")), styles(Highlighter.highlightLogLevelRegularFile(text)));
    }

    @Test
    void usesFirstLevelOfJsonLineSegments() {
        String text = "{\"level\":\"WARN\",\"message\":\"ERROR later\"}\n{\"level\":\"ERROR\"}\n";

        assertEquals(List.of(List.of("warn"), List.of("error")), styles(Highlighter.highlightLogLevel(text)));
    }

    @Test
    void usesFirstLevelFoundInsideMultiLineJsonSegment() {
        String text = "{\n  \"msg\" : \"started\",\n  \"level\" : \"DEBUG\"\n}\n{\n  \"level\" : \"INFO\"\n}\n";

        assertEquals(List.of(List.of("debug"), List.of("info")), styles(Highlighter.highlightLogLevel(text)));
    }

    @Test
    void jsonSegmentWithoutLevelIsUnstyled() {
        String text = "{\n  \"msg\" : \"started\"\n}\n{\n  \"level\" : \"INFO\"\n}\n";

        assertEquals(List.of(List.of(), List.of("info")), styles(Highlighter.highlightLogLevel(text)));
    }

    @Test
    void emptyStyleCoversWholeText() {
        StyleSpans<Collection<String>> spans = Highlighter.computeEmptyStyle("héllo \uD83D\uDE00\nworld");

        assertEquals(1, spans.getSpanCount());
        assertEquals("héllo \uD83D\uDE00\nworld".length(), spans.length());
        assertEquals(List.of(), List.copyOf(spans.getStyleSpan(0).getStyle()));
    }

    @Test
    void emptyStyleOfEmptyTextHasNoLength() {
        assertEquals(0, Highlighter.computeEmptyStyle("").length());
    }

    @ParameterizedTest
    @ValueSource(strings = {"INFO a", "INFO a\nERROR b", "{\"level\":\"INFO\"}", "{\n  \"level\" : \"INFO\"\n}"})
    void spansCoverTextWithoutTrailingLineFeed(String text) {
        assertEquals(text.length(), Highlighter.highlightLogLevel(text).length());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\n  \"level\" : \"INFO\"\n",
                "{\"a\":1}\n{\n  \"level\" : \"INFO\"",
                "{\n  \"level\" : \"INFO\"\n}\n}\n{\"a\":1}\n",
                "{\n  \"level\" : \"INFO\", \"a\" : {}"
            })
    void spansCoverJsonWithUnbalancedBraces(String text) {
        assertEquals(text.length(), Highlighter.highlightLogLevel(text).length());
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
    void spansCoverArbitraryTextLineForLine(long seed) {
        String text = randomLogText(new Random(seed));

        assertEquals(
                expectedStylePerCharacter(text), stylePerCharacter(Highlighter.highlightLogLevelRegularFile(text)));
        assertEquals(text.length(), Highlighter.highlightLogLevel(text).length());
    }

    @Test
    void jsonSegmentWithCarriageReturnLineFeedIsOneSpanOfItsExactLength() {
        String text = "{\r\n \"level\":\"INFO\"\r\n}\r\n";

        StyleSpans<Collection<String>> spans = Highlighter.highlightLogLevel(text);

        assertEquals(1, spans.getSpanCount());
        assertEquals(text.length(), spans.getStyleSpan(0).getLength());
        assertEquals(List.of("info"), List.copyOf(spans.getStyleSpan(0).getStyle()));
    }

    @Test
    void eachJsonSegmentWithCarriageReturnLineFeedKeepsItsOwnSpanLength() {
        String first = "{\r\n \"level\":\"INFO\"\r\n}\r\n";
        String second = "{\r\n \"level\":\"ERROR\"\r\n}\r\n";

        List<Integer> lengths = Highlighter.highlightLogLevel(first + second).stream()
                .map(StyleSpan::getLength)
                .toList();

        assertEquals(List.of(first.length(), second.length()), lengths);
    }

    @Test
    void jsonLineSpansKeepPerLineLengthsWithMixedLineEndings() {
        String text = "{\"level\":\"INFO\"}\r\n{\"level\":\"ERROR\"}\r{\"level\":\"WARN\"}\n";

        assertEquals(
                List.of(
                        "{\"level\":\"INFO\"}\r\n".length(),
                        "{\"level\":\"ERROR\"}\r".length(),
                        "{\"level\":\"WARN\"}\n".length()),
                Highlighter.highlightLogLevel(text).stream()
                        .map(StyleSpan::getLength)
                        .toList());
    }

    private static List<List<String>> stylePerCharacter(StyleSpans<Collection<String>> spans) {
        List<List<String>> perCharacter = new ArrayList<>();
        for (StyleSpan<Collection<String>> span : spans) {
            for (int i = 0; i < span.getLength(); i++) {
                perCharacter.add(List.copyOf(span.getStyle()));
            }
        }
        return perCharacter;
    }

    // independent of Highlighter: a line takes the earliest level in it, else the previous line's level
    private static List<List<String>> expectedStylePerCharacter(String text) {
        List<List<String>> perCharacter = new ArrayList<>();
        Matcher lines = Pattern.compile("([^\r\n]*)(\r\n|\n|\r)?").matcher(text);
        List<String> previous = List.of();
        int position = 0;
        while (position < text.length() && lines.find(position)) {
            String content = lines.group(1);
            List<String> style = previous;
            int earliest = Integer.MAX_VALUE;
            for (String level : List.of("FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE")) {
                int index = content.indexOf(level);
                if (index != -1 && index < earliest) {
                    earliest = index;
                    style = List.of(level.toLowerCase(Locale.ROOT));
                }
            }
            previous = style;
            for (int i = lines.start(); i < lines.end(); i++) {
                perCharacter.add(style);
            }
            position = lines.end();
        }
        return perCharacter;
    }

    private static final String[] TERMINATORS = {"\n", "\r\n", "\r"};

    private static String randomLogText(Random random) {
        String[] fragments = {
            "INFO",
            "ERROR",
            "WARN",
            "DEBUG",
            "TRACE",
            "FATAL",
            "{",
            "}",
            "[",
            "]",
            "\"level\"",
            ":",
            " ",
            "\t",
            "Grüße",
            "日本語",
            "\uD83D\uDE00",
            ".*",
            "\\"
        };
        StringBuilder text = new StringBuilder();
        int lines = 1 + random.nextInt(30);
        for (int i = 0; i < lines; i++) {
            int fragmentCount = random.nextInt(6);
            for (int j = 0; j < fragmentCount; j++) {
                text.append(fragments[random.nextInt(fragments.length)]);
            }
            text.append(TERMINATORS[random.nextInt(TERMINATORS.length)]);
        }
        return text.toString();
    }

    private static List<Collection<String>> styles(StyleSpans<Collection<String>> spans) {
        return spans.stream().map(StyleSpan::getStyle).toList();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "INFO a\r\nERROR b\r\n",
                "INFO a\r\nERROR b",
                "INFO a\nERROR b\r\nWARN c\rDEBUG d\n",
                "INFO a\r",
                "INFO a\r\r\n\n",
                "{\"level\":\"INFO\"}\r\n{\"level\":\"ERROR\"}\r\n",
                "{\r\n  \"level\" : \"INFO\"\r\n}\r\n{\r\n  \"level\" : \"ERROR\"\r\n}\r\n",
                "{\"a\":1}\n{\r\n  \"level\" : \"INFO\"\r\n}\r"
            })
    void spansCoverTextWithAnyLineEnding(String text) {
        assertEquals(text.length(), Highlighter.highlightLogLevel(text).length());
    }

    @Test
    void assignsLevelsToLinesSeparatedByMixedLineEndings() {
        String text = "INFO a\r\nERROR b\nWARN c\rDEBUG d";

        assertEquals(
                List.of(List.of("info"), List.of("error"), List.of("warn"), List.of("debug")),
                styles(Highlighter.highlightLogLevelRegularFile(text)));
    }

    @Test
    void keepsSpanLengthsPerLineWithCarriageReturnLineFeed() {
        List<Integer> lengths = Highlighter.highlightLogLevelRegularFile("INFO a\r\nERROR b\r\n").stream()
                .map(StyleSpan::getLength)
                .toList();

        assertEquals(List.of(8, 9), lengths);
    }

    private static List<StyleSpan<Collection<String>>> unstyledSpans(StyleSpans<Collection<String>> spans) {
        return spans.stream().filter(span -> span.getStyle().isEmpty()).toList();
    }

    private static String readTestData(String name) throws Exception {
        return Files.readString(
                Path.of(HighlighterTest.class.getResource("/test-data/" + name).toURI()));
    }
}
