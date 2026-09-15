package net.osslabz.loggazer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HighlighterTest {

    @Test
    void usesLevelThatComesFirstInLine() {
        String text = "2025-01-01 INFO retry after ERROR from upstream\n2025-01-01 ERROR retry after INFO from upstream\n";

        List<Collection<String>> styles = Highlighter.highlightLogLevelRegularFile(text).stream().map(StyleSpan::getStyle).toList();

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
            assertEquals(List.of(List.of("info")), Highlighter.highlightLogLevel(regularLog).stream().map(StyleSpan::getStyle).toList());
            assertEquals(List.of(List.of("info")), Highlighter.highlightLogLevel(jsonLog).stream().map(StyleSpan::getStyle).toList());
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }


    private static List<StyleSpan<Collection<String>>> unstyledSpans(StyleSpans<Collection<String>> spans) {
        return spans.stream().filter(span -> span.getStyle().isEmpty()).toList();
    }


    private static String readTestData(String name) throws Exception {
        return Files.readString(Path.of(HighlighterTest.class.getResource("/test-data/" + name).toURI()));
    }
}
