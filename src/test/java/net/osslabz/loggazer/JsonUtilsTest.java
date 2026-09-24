package net.osslabz.loggazer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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
}
