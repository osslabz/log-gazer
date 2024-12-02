package net.osslabz.loggazer;


import java.io.File;
import java.util.List;
import org.fxmisc.richtext.CodeArea;


public class TabContent {

    private final File file;

    private final String originalContent;

    private final CodeArea codeArea;

    private final SearchData searchData;


    public TabContent(File file, String originalContent, CodeArea codeArea) {

        this.file = file;
        this.originalContent = originalContent;
        this.codeArea = codeArea;
        this.searchData = new SearchData();
    }


    public void updateSearch(String query, List<Integer> matches) {

        this.searchData.setQuery(query);
        this.searchData.setMatches(matches);
        this.searchData.setCurrentMatchIndex(matches.isEmpty() ? -1 : 0);
    }


    public File getFile() {

        return file;
    }


    public String getOriginalContent() {

        return originalContent;
    }


    public CodeArea getCodeArea() {

        return codeArea;
    }


    public SearchData getSearchData() {

        return searchData;
    }


    public static class SearchData {

        private String query;

        private List<Integer> matches;

        private int currentMatchIndex = -1;


        public String getQuery() {

            return query;
        }


        public void setQuery(String query) {

            this.query = query;
        }


        public List<Integer> getMatches() {

            return matches;
        }


        public void setMatches(List<Integer> matches) {

            this.matches = matches;
        }


        public int getCurrentMatchIndex() {

            return currentMatchIndex;
        }


        public void setCurrentMatchIndex(int currentMatchIndex) {

            this.currentMatchIndex = currentMatchIndex;
        }


        public int numMatches() {

            return matches != null ? matches.size() : 0;
        }


        public int getCurrentMatchPosition() {

            return numMatches() != 0 ? matches.get(this.currentMatchIndex) : -1;
        }


        public void moveToNextMatch() {

            if (currentMatchIndex < numMatches() - 1) {
                currentMatchIndex++;
            } else {
                currentMatchIndex = 0;
            }
        }


        public void moveToPrevMatch() {

            if (currentMatchIndex > 0) {
                currentMatchIndex--;
            } else {
                currentMatchIndex = numMatches() - 1;
            }
        }
    }
}