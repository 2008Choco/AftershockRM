module wtf.choco.aftershock {
    requires javafx.fxml;
    requires fr.brouillard.oss.cssfx;

    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires transitive java.desktop;
    requires transitive java.logging;
    requires transitive java.sql;
    requires transitive com.google.gson;

    opens wtf.choco.aftershock to javafx.fxml;
    opens wtf.choco.aftershock.control to javafx.fxml;
    opens wtf.choco.aftershock.controller to javafx.fxml;

    exports wtf.choco.aftershock;
    exports wtf.choco.aftershock.control;
    exports wtf.choco.aftershock.controller;
    exports wtf.choco.aftershock.event;
    exports wtf.choco.aftershock.keybind;
    exports wtf.choco.aftershock.manager;
    exports wtf.choco.aftershock.replay;
    exports wtf.choco.aftershock.structure;
    exports wtf.choco.aftershock.util;
    exports wtf.choco.aftershock.util.function;
}