package net.osslabz.loggazer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchDataTest {

    @Test
    void keepsMatchesWhenCallerChangesItsList() {
        List<Integer> matches = new ArrayList<>(List.of(3, 7));
        TabContent.SearchData searchData = new TabContent.SearchData();
        searchData.setMatches(matches);

        matches.clear();

        assertEquals(2, searchData.numMatches());
    }

    @Test
    void wrapsToFirstMatchAfterLast() {
        TabContent.SearchData searchData = new TabContent.SearchData();
        searchData.setMatches(List.of(3, 7));
        searchData.setCurrentMatchIndex(1);

        searchData.moveToNextMatch();

        assertEquals(3, searchData.getCurrentMatchPosition());
    }

    @Test
    void wrapsToLastMatchBeforeFirst() {
        TabContent.SearchData searchData = new TabContent.SearchData();
        searchData.setMatches(List.of(3, 7));
        searchData.setCurrentMatchIndex(0);

        searchData.moveToPrevMatch();

        assertEquals(7, searchData.getCurrentMatchPosition());
    }
}
