module LogGazerModule {

    requires javafx.controls;
    requires javafx.graphics;
    requires java.desktop; // for AWT stuff

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;

    requires org.apache.commons.compress;

    requires org.slf4j;

    requires java.prefs;
    requires org.apache.commons.lang3;

    exports net.osslabz.loggazer to javafx.graphics;
}