package net.osslabz.loggazer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JsonUtilsTest {

    @Test
    void unescapesLineBreaksAndTabsButKeepsEscapedBackslashes() {
        String json =
                "{\"level\":\"INFO\",\"path\":\"C:\\\\new\\\\tmp\",\"folders\":\"C:\\\\\\nD:\\\\\",\"message\":\"first\\nsecond\\tthird\"}";

        assertEquals(
                String.join(
                        System.lineSeparator(),
                        "{",
                        "  \"level\" : \"INFO\",",
                        "  \"path\" : \"C:\\\\new\\\\tmp\",",
                        "  \"folders\" : \"C:\\\\",
                        "D:\\\\\",",
                        "  \"message\" : \"first",
                        "second\tthird\"",
                        "}",
                        ""),
                JsonUtils.format(json));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[]", "{\"a\":1}", "[1,2]", "  {\"a\":1}  ", "\t[1]\t"})
    void recognizesBracketedLineAsJson(String line) {
        assertTrue(JsonUtils.lineMightBeJson(line));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "{", "}", "[", "]", "{]", "[}", "2025-01-01 INFO started", "{a} trailing"})
    void rejectsLineThatIsNotBracketed(String line) {
        assertFalse(JsonUtils.lineMightBeJson(line));
    }

    @Test
    void acceptsBracketedNonJsonLineAsJsonCandidate() {
        assertTrue(JsonUtils.lineMightBeJson("[INFO] started [again]"));
    }

    @Test
    void treatsSingleJsonLineAsJson() {
        assertTrue(JsonUtils.textMightBeJson("{\"a\":1}"));
    }

    @Test
    void treatsTextThatStartsAndEndsWithBracketsAsJson() {
        assertTrue(JsonUtils.textMightBeJson("{\"a\":1}\nplain line\n{\"b\":2}"));
    }

    @Test
    void treatsTextOfOnlyJsonLinesAsJson() {
        assertTrue(JsonUtils.textMightBeJson("{\"a\":1}\n[2]\n{\"b\":3}"));
    }

    @Test
    void rejectsTextWithFewJsonLinesAmongPlainOnes() {
        assertFalse(JsonUtils.textMightBeJson("plain line\n{\"a\":1}\n{\"b\":2}\nplain line"));
    }

    @Test
    void acceptsMixedTextWithMoreThanFiveJsonLines() {
        String text = "plain line\n" + "{\"a\":1}\n".repeat(6);

        assertTrue(JsonUtils.textMightBeJson(text));
    }

    @Test
    void rejectsMixedTextWithExactlyFiveJsonLines() {
        String text = "plain line\n" + "{\"a\":1}\n".repeat(5);

        assertFalse(JsonUtils.textMightBeJson(text));
    }

    @Test
    void rejectsPlainLogWithBracketedLevelPrefixes() {
        assertFalse(JsonUtils.textMightBeJson("[INFO] started\n[ERROR] failed\n"));
    }

    @Test
    void formatReturnsEmptyTextUnchanged() {
        assertEquals("", JsonUtils.format(""));
    }

    @Test
    void formatKeepsNonJsonLinesAndTerminatesThemWithLineSeparator() {
        assertEquals(
                "plain" + System.lineSeparator() + "[INFO] started" + System.lineSeparator(),
                JsonUtils.format("plain\n[INFO] started"));
    }

    @Test
    void formatKeepsMalformedJsonLineUnchanged() {
        String malformed = "{\"a\": [1, 2}";

        assertEquals(malformed + System.lineSeparator(), JsonUtils.format(malformed));
    }

    @Test
    void formatKeepsUnicodeIntact() {
        assertEquals(
                String.join(System.lineSeparator(), "{", "  \"msg\" : \"Grüße 日本語 \uD83D\uDE00\"", "}", ""),
                JsonUtils.format("{\"msg\":\"Grüße 日本語 \uD83D\uDE00\"}"));
    }

    @Test
    void formatHandlesJsonArrayLine() {
        assertEquals(String.join(System.lineSeparator(), "[ 1, 2 ]", ""), JsonUtils.format("[1,2]"));
    }

    @Test
    void formatHandlesLargeInput() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 20_000; i++) {
            text.append("{\"level\":\"INFO\",\"n\":%d}".formatted(i)).append('\n');
        }

        String formatted = JsonUtils.format(text.toString());

        assertEquals(20_000, formatted.split("\"level\"", -1).length - 1);
        assertTrue(formatted.contains("\"n\" : 19999"));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 4, 5, 6, 7, 8})
    void formattedJsonLineParsesBackToSameValue(long seed) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Random random = new Random(seed);
        // no control characters in strings: format deliberately turns \n and \t escapes into real characters
        String line = mapper.writeValueAsString(randomObject(random, 3));

        String formatted = JsonUtils.format(line);

        assertEquals(mapper.readTree(line), mapper.readTree(formatted));
    }

    private static Map<String, Object> randomObject(Random random, int depth) {
        Map<String, Object> object = new LinkedHashMap<>();
        int size = random.nextInt(5);
        for (int i = 0; i < size; i++) {
            object.put("k" + i + randomWord(random), randomValue(random, depth));
        }
        return object;
    }

    private static Object randomValue(Random random, int depth) {
        int kind = random.nextInt(depth > 0 ? 6 : 4);
        return switch (kind) {
            case 0 -> random.nextInt();
            case 1 -> random.nextBoolean();
            case 2 -> randomWord(random);
            case 3 -> null;
            case 4 -> randomObject(random, depth - 1);
            default -> Arrays.asList(randomValue(random, depth - 1), randomValue(random, depth - 1));
        };
    }

    private static String randomWord(Random random) {
        String alphabet = "abc XYZ 09 äöü日\uD83D\uDE00\"'{}[]:,/";
        StringBuilder word = new StringBuilder();
        int length = random.nextInt(8);
        for (int i = 0; i < length; i++) {
            int codePoint = alphabet.codePointAt(
                    alphabet.offsetByCodePoints(0, random.nextInt(alphabet.codePointCount(0, alphabet.length()))));
            word.appendCodePoint(codePoint);
        }
        return word.toString();
    }
}
