package net.osslabz.loggazer;


import org.fxmisc.richtext.CodeArea;

import java.io.File;

public class TabContent {

    private final File file;

    private final String originalContent;

    private final CodeArea codeArea;

    public TabContent(File file, String originalContent, CodeArea codeArea) {
        this.file = file;
        this.originalContent = originalContent;
        this.codeArea = codeArea;
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
}