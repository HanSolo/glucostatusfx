module eu.hansolo.fx.glucostatus {
    // Java
    requires java.base;
    requires java.net.http;

    // Java-FX
    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires transitive javafx.media;
    requires transitive javafx.swing;

    // 3rd Party
    requires transitive eu.hansolo.toolbox;
    requires transitive eu.hansolo.toolboxfx;
    requires transitive eu.hansolo.applefx;
    requires com.google.gson;
    requires FXTrayIcon;

    exports eu.hansolo.fx.glucostatus;
    exports eu.hansolo.fx.glucostatus.notification;
}