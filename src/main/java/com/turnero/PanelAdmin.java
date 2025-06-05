package com.turnero;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PanelAdmin extends Application {

    private Label lblTurno = new Label("Turno actual: ...");
    private Label lblCategoria = new Label("");

    @Override
    public void start(Stage stage) {
        Button btnSiguiente = new Button("Siguiente Turno");
        btnSiguiente.setOnAction(e -> avanzarTurno());

        VBox root = new VBox(15, lblTurno, lblCategoria, btnSiguiente);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-font-size: 16px;");

        Scene scene = new Scene(root, 300, 200);
        stage.setScene(scene);
        stage.setTitle("Panel de Administración");
        stage.show();

        actualizarTurnoActual();
    }

    private void actualizarTurnoActual() {
        try {
            URL url = new URL("http://localhost:8080/api/turnos/actual");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                InputStream input = conn.getInputStream();
                ObjectMapper mapper = new ObjectMapper();
                Turno turno = mapper.readValue(input, Turno.class);

                lblTurno.setText("Turno actual: " + turno.getNumero());
                lblCategoria.setText("Categoría: " + turno.getCategoria());
            } else {
                lblTurno.setText("Sin turnos disponibles");
                lblCategoria.setText("");
            }
        } catch (Exception e) {
            lblTurno.setText("Error de conexión");
            lblCategoria.setText("");
        }
    }

    private void avanzarTurno() {
        try {
            URL url = new URL("http://localhost:8080/api/turnos/next");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");

            if (conn.getResponseCode() == 200) {
                actualizarTurnoActual(); // muestra el nuevo actual
            } else {
                lblTurno.setText("No hay más turnos");
                lblCategoria.setText("");
            }
        } catch (Exception e) {
            lblTurno.setText("Error al avanzar");
            lblCategoria.setText("");
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
