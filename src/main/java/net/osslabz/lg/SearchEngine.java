package net.osslabz.lg;

import javafx.scene.control.Alert;
import org.fxmisc.richtext.CodeArea;

class SearchEngine {
    private final CodeArea codeArea;

    public SearchEngine(CodeArea codeArea) {
        this.codeArea = codeArea;
    }

    public void search(String query) {
        String content = codeArea.getText();
        int index = content.indexOf(query);
        if (index != -1) {
            codeArea.selectRange(index, index + query.length());
            codeArea.requestFocus();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Text not found");
            alert.showAndWait();
        }
    }
}