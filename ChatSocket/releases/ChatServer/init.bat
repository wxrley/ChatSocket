@echo off
start javaw --module-path "lib" --add-modules javafx.controls,javafx.fxml -jar ChatSocketServer.jar
exit