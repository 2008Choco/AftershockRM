module wtf.choco.aftershock {
    requires javafx.controls;
    requires javafx.fxml;

    requires transitive javafx.graphics;
    requires transitive gson;
    requires transitive java.sql;
    requires transitive java.logging;

    opens wtf.choco.aftershock to javafx.fxml;

    exports wtf.choco.aftershock;
    exports wtf.choco.aftershock.manager;
    exports wtf.choco.aftershock.replay;
    exports wtf.choco.aftershock.structure;
    exports wtf.choco.aftershock.util;
}