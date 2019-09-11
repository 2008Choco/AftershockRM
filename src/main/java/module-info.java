module wtf.choco.aftershock {
    requires javafx.controls;
    requires javafx.fxml;

    requires transitive javafx.graphics;
    requires transitive java.desktop;
    requires transitive java.logging;
    requires transitive java.sql;
    requires transitive com.google.gson;

    opens wtf.choco.aftershock to javafx.fxml;
    opens wtf.choco.aftershock.controller to javafx.fxml;

    exports wtf.choco.aftershock;
    exports wtf.choco.aftershock.controller;
    exports wtf.choco.aftershock.manager;
    exports wtf.choco.aftershock.replay;
    exports wtf.choco.aftershock.structure;
    exports wtf.choco.aftershock.structure.bin;
    exports wtf.choco.aftershock.util;
}