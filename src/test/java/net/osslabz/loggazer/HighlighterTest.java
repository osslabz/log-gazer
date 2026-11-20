package net.osslabz.loggazer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;
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
                text.length(), Highlighter.highlightLogLevelRegularFile(text).length());
        assertEquals(text.length(), Highlighter.highlightLogLevel(text).length());
    }

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
            text.append('\n');
        }
        return text.toString();
    }

    private static List<Collection<String>> styles(StyleSpans<Collection<String>> spans) {
        return spans.stream().map(StyleSpan::getStyle).toList();
    }

    private static List<StyleSpan<Collection<String>>> unstyledSpans(StyleSpans<Collection<String>> spans) {
        return spans.stream().filter(span -> span.getStyle().isEmpty()).toList();
    }

    private static String readTestData(String name) throws Exception {
        return Files.readString(
                Path.of(HighlighterTest.class.getResource("/test-data/" + name).toURI()));
    }
}
