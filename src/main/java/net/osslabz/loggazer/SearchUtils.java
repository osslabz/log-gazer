package net.osslabz.loggazer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fxmisc.richtext.CodeArea;


public class SearchUtils {

    private SearchUtils() {
        // intentionally empty
    }


    public static List<Integer> findAllMatches(CodeArea codeArea, String searchTerm) {

        List<Integer> matchPositions = new ArrayList<>();
        String text = codeArea.getText();
        Pattern pattern = Pattern.compile(Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            matchPositions.add(matcher.start());
        }

        return matchPositions;
    }

}