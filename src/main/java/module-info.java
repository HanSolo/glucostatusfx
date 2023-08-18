module eu.hansolo.fx.glucostatus {
    // Java
    requires java.base;
    requires java.net.http;

    // Java-FX
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.media;
    requires javafx.swing;

    // 3rd Party
    requires transitive eu.hansolo.jdktools;
    requires transitive eu.hansolo.toolbox;
    requires transitive eu.hansolo.toolboxfx;
    requires transitive eu.hansolo.applefx;
    requires com.google.gson;
    //requires com.dustinredmond.fxtrayicon;
    requires FXTrayIcon;

    exports eu.hansolo.fx.glucostatus;
    exports eu.hansolo.fx.glucostatus.notification;
}