package net.osslabz.loggazer;

import java.util.Collection;
import java.util.List;
import org.fxmisc.richtext.model.StyleSpan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HighlighterTest {

    @Test
    void usesLevelThatComesFirstInLine() {
        String text = "2025-01-01 INFO retry after ERROR from upstream\n2025-01-01 ERROR retry after INFO from upstream\n";

        List<Collection<String>> styles = Highlighter.highlightLogLevelRegularFile(text).stream().map(StyleSpan::getStyle).toList();

        assertEquals(List.of(List.of("info"), List.of("error")), styles);
    }
}
