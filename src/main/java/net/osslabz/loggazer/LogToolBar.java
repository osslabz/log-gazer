package net.osslabz.loggazer;

import java.util.List;
import java.util.function.Supplier;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;

final class LogToolBar extends ToolBar {

    private static final String SEARCH_QUERY_PLACEHOLDER = "Search...";

    private final Supplier<TabContent> currentTabContent;

    private final Button buttonMarkLogLevel;

    private final Button buttonFormatJson;

    private final TextField searchField;

    private final Button searchButton;

    private final Button prevMatchButton;

    private final Button nextMatchButton;

    private final Label matchCountLabel;

    LogToolBar(Supplier<TabContent> currentTabContent) {

        this.currentTabContent = currentTabContent;

        this.buttonMarkLogLevel = createAndCofigureMarkLogLevelButton();
        this.buttonFormatJson = createButtonFormatJson();

        this.searchField = new TextField();
        searchField.setMinWidth(20);
        searchField.setPromptText(SEARCH_QUERY_PLACEHOLDER);
        searchField.setOnAction(e -> performSearch());
        searchField.setDisable(true);

        this.searchButton = new Button("Search");
        searchButton.setOnAction(e -> performSearch());
        searchButton.setDisable(true);

        this.prevMatchButton = new Button("◀ Previous");
        prevMatchButton.setOnAction(e -> navigateToPreviousMatch());
        prevMatchButton.setDisable(true);

        this.nextMatchButton = new Button("Next ▶");
        nextMatchButton.setOnAction(e -> navigateToNextMatch());
        nextMatchButton.setDisable(true);

        this.matchCountLabel = new Label();

        resetSearch();

        getItems()
                .addAll(
                        this.buttonFormatJson,
                        new Separator(),
                        this.buttonMarkLogLevel,
                        new Separator(),
                        this.searchField,
                        this.searchButton,
                        this.prevMatchButton,
                        this.nextMatchButton,
                        this.matchCountLabel);
    }

    void focusSearchOnShortcut(Scene scene) {

        KeyCombination searchKeyCombination = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (searchKeyCombination.match(event) && currentTabContent.get() != null) {
                this.searchField.requestFocus();
            }
        });
    }

    void showTab(TabContent tabContent) {

        String currentText = tabContent.getCodeArea().getText();

        enableButtons(true);
        navigateToCurrentMatch();

        this.buttonFormatJson.setDisable(!JsonUtils.textMightBeJson(currentText));
    }

    void showNoTab() {

        enableButtons(false);
        resetSearch();
    }

    private void performSearch() {

        TabContent currentTabContent = this.currentTabContent.get();
        if (currentTabContent == null) {
            return;
        }

        CodeArea codeArea = currentTabContent.getCodeArea();

        String searchTerm = searchField.getText();
        if (searchTerm.isEmpty()) {
            currentTabContent.updateSearch(searchTerm, List.of());
            matchCountLabel.setText("");
            toggleSearchButtons(false);
            return;
        }

        List<Integer> matchPositions = SearchUtils.findAllMatches(codeArea, searchTerm);

        currentTabContent.updateSearch(searchTerm, matchPositions);

        if (matchPositions.isEmpty()) {
            codeArea.selectRange(codeArea.getAnchor(), codeArea.getAnchor());
            matchCountLabel.setText("No matches found");
            toggleSearchButtons(false);
            return;
        }

        toggleSearchButtons(true);
        navigateToCurrentMatch();
    }

    private void navigateToCurrentMatch() {

        TabContent tabContent = this.currentTabContent.get();
        if (tabContent == null || tabContent.getSearchData().getCurrentMatchIndex() == -1) {
            resetSearch();
            return;
        }

        int currentMatchIndex = tabContent.getSearchData().getCurrentMatchIndex();
        int numMatches = tabContent.getSearchData().numMatches();

        searchField.setText(tabContent.getSearchData().getQuery());
        matchCountLabel.setText(String.format("%d of %d matches", currentMatchIndex + 1, numMatches));

        if (currentMatchIndex >= 0 && currentMatchIndex < numMatches) {
            toggleSearchButtons(true);
            int position = tabContent.getSearchData().getCurrentMatchPosition();
            CodeArea codeArea = tabContent.getCodeArea();
            codeArea.moveTo(position);
            codeArea.selectRange(
                    position, position + tabContent.getSearchData().getQuery().length());
            codeArea.requestFollowCaret();
            codeArea.requestFocus();
        }
    }

    private void resetSearch() {

        searchField.clear();
        matchCountLabel.setText("");
    }

    private void navigateToNextMatch() {

        TabContent currentTabContent = this.currentTabContent.get();
        if (currentTabContent == null || currentTabContent.getSearchData().getCurrentMatchIndex() == -1) {
            return;
        }
        currentTabContent.getSearchData().moveToNextMatch();
        navigateToCurrentMatch();
    }

    private void navigateToPreviousMatch() {

        TabContent currentTabContent = this.currentTabContent.get();
        if (currentTabContent == null || currentTabContent.getSearchData().getCurrentMatchIndex() == -1) {
            return;
        }
        currentTabContent.getSearchData().moveToPrevMatch();
        navigateToCurrentMatch();
    }

    private Button createButtonFormatJson() {

        Button button = new Button("Format JSON");
        button.setDisable(true);

        button.setOnAction(evt -> {
            TabContent selectedTabContent = this.currentTabContent.get();
            if (selectedTabContent == null) {
                return;
            }

            CodeArea codeArea = selectedTabContent.getCodeArea();
            String originalContent = selectedTabContent.getOriginalContent();

            String currentText = codeArea.getText();

            if (currentText.equals(originalContent)) {
                codeArea.replaceText(JsonUtils.format(currentText));
            } else {
                codeArea.replaceText(originalContent);
            }

            // replaceText drops the styling, so the toggle must forget it was marked
            selectedTabContent.setLogLevelMarked(false);

            // match positions refer to the replaced text
            String query = selectedTabContent.getSearchData().getQuery();
            if (query != null) {
                this.searchField.setText(query);
                performSearch();
            }
        });
        return button;
    }

    private Button createAndCofigureMarkLogLevelButton() {

        Button button = new Button("Mark Log Level");
        button.setDisable(true);

        button.setOnAction(evt -> {
            TabContent tabContent = this.currentTabContent.get();
            if (tabContent == null) {
                return;
            }

            CodeArea codeArea = tabContent.getCodeArea();

            String currentText = codeArea.getText();

            if (tabContent.isLogLevelMarked()) {
                codeArea.setStyleSpans(0, Highlighter.computeEmptyStyle(currentText));
            } else {
                codeArea.setStyleSpans(0, Highlighter.highlightLogLevel(currentText));
            }
            tabContent.setLogLevelMarked(!tabContent.isLogLevelMarked());
        });
        return button;
    }

    private void enableButtons(boolean enable) {

        this.buttonMarkLogLevel.setDisable(!enable);
        this.buttonFormatJson.setDisable(!enable);
        this.searchField.setDisable(!enable);
        this.searchButton.setDisable(!enable);
        this.toggleSearchButtons(false);
    }

    private void toggleSearchButtons(boolean enable) {
        this.prevMatchButton.setDisable(!enable);
        this.nextMatchButton.setDisable(!enable);
    }
}
