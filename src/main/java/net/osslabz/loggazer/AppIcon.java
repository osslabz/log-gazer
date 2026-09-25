package net.osslabz.loggazer;

import java.awt.Taskbar;
import java.awt.Toolkit;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AppIcon {

    private static final Logger log = LoggerFactory.getLogger(AppIcon.class);

    private static final String ICON_PATH = "/icon/icon-256.png";

    private AppIcon() {
        // intentionally empty
    }

    static void setTaskbarIcon() {

        if (Taskbar.isTaskbarSupported()) {
            log.debug("Taskbar is supported");
            var taskbar = Taskbar.getTaskbar();

            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                log.debug("Taskbar.Feature.ICON_IMAGE is supported");
                final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
                var dockIcon = defaultToolkit.getImage(LogGazerApp.class.getResource(ICON_PATH));
                taskbar.setIconImage(dockIcon);
            } else {
                log.debug("Taskbar.Feature.ICON_IMAGE is NOT supported");
            }
        } else {
            log.debug("Taskbar is NOT supported");
        }
    }

    static void addTo(Stage stage) {

        Image appIconImage = new Image(LogGazerApp.class.getResourceAsStream(ICON_PATH));

        stage.getIcons().add(appIconImage);
    }
}
