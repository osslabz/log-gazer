package net.osslabz.loggazer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
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
    void emptyTextProducesNoSpansWithoutFailing() {
        assertDoesNotThrow(() -> Highlighter.highlightLogLevel(""));
        assertEquals(0, Highlighter.highlightLogLevel("").length());
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

    private static List<StyleSpan<Collection<String>>> unstyledSpans(StyleSpans<Collection<String>> spans) {
        return spans.stream().filter(span -> span.getStyle().isEmpty()).toList();
    }

    private static String readTestData(String name) throws Exception {
        return Files.readString(
                Path.of(HighlighterTest.class.getResource("/test-data/" + name).toURI()));
    }
}
