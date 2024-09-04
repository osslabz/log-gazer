package net.osslabz.lg;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.fxmisc.richtext.CodeArea;

import java.io.File;

@Data
@AllArgsConstructor
public class TabContent {

    private final File file;

    private final String originalContent;

    private final CodeArea codeArea;

}