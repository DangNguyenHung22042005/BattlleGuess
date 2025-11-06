module com.battleguess.battleguess {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.desktop;
    requires javafx.swing;
    requires java.sql;

    opens com.battleguess.battleguess to javafx.fxml;
    exports com.battleguess.battleguess;
    exports com.battleguess.battleguess.controller;
    opens com.battleguess.battleguess.controller to javafx.fxml;
    exports com.battleguess.battleguess.canvas;
    opens com.battleguess.battleguess.canvas to javafx.fxml;
    exports com.battleguess.battleguess.canvas.model;
    opens com.battleguess.battleguess.canvas.model to javafx.fxml;
}