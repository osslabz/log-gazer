package net.osslabz.loggazer;

import java.io.File;
import java.util.function.Consumer;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;

final class FileMenu {

    private FileMenu() {
        // intentionally empty
    }

    static MenuBar createMenuBar(Consumer<File> openFile) {

        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem openMenuItem = new MenuItem("Open");
        openMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        openMenuItem.setOnAction(e -> chooseFile(openFile));
        fileMenu.getItems().add(openMenuItem);
        menuBar.getMenus().add(fileMenu);
        return menuBar;
    }

    private static void chooseFile(Consumer<File> openFile) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Files", "*.*"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            openFile.accept(file);
        }
    }
}
