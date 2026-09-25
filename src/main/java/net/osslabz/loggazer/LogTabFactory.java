package net.osslabz.loggazer;

import java.io.File;
import java.util.UUID;
import javafx.scene.control.Tab;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LogTabFactory {

    private static final Logger log = LoggerFactory.getLogger(LogTabFactory.class);

    private LogTabFactory() {
        // intentionally empty
    }

    static TabContent createContent(File file, String rawContent) {

        CodeArea codeArea = new CodeArea(rawContent);
        codeArea.setEditable(false);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        return new TabContent(file, codeArea.getText(), codeArea);
    }

    static Tab createTab(TabContent tabContent) {

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(tabContent.getCodeArea());

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        VBox contentBox = new VBox(scrollPane);
        contentBox.setFillWidth(true);

        scrollPane.addEventFilter(ScrollEvent.SCROLL, scrollEvent -> {
            log.trace("scrollEvent={}", scrollEvent);
            scrollPane.scrollYBy(scrollEvent.getDeltaY() * -1);
            //   scrollPane.scrollXBy(scrollEvent.getDeltaX());

            scrollEvent.consume();
        });

        String fileName = tabContent.getFile().getName();
        Tab tab = new Tab(fileName, contentBox);

        tab.setId(UUID.randomUUID() + "_" + fileName);
        return tab;
    }
}
