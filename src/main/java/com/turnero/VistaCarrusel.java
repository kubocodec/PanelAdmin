package com.turnero;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class VistaCarrusel {

    private FlowPane galeria = new FlowPane(15, 15);
    private Label lblMensaje = new Label();

    public VBox construir() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #f8f9fa;");

        Label titulo = new Label("🖼️ GESTIÓN DE IMÁGENES DEL CARRUSEL");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        titulo.setTextFill(Color.web("#2c3e50"));

        Button btnSubir = new Button("📤 Subir imagen...");
        btnSubir.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 14px; -fx-cursor: hand;");
        btnSubir.setPrefHeight(40);
        btnSubir.setOnAction(e -> subirImagen());

        lblMensaje.setFont(Font.font("Arial", 13));

        HBox toolbar = new HBox(15, btnSubir, lblMensaje);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(galeria);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(450);
        scroll.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        galeria.setPadding(new Insets(15));

        root.getChildren().addAll(titulo, toolbar, scroll);
        cargarImagenes();
        return root;
    }

    private void cargarImagenes() {
        new Thread(() -> {
            try {
                URL url = new URL("http://" + ConfigManager.getIp() + ":8080/api/carrusel/lista");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                List<String> nombres = new ObjectMapper().readValue(conn.getInputStream(), new TypeReference<List<String>>() {});
                Platform.runLater(() -> {
                    galeria.getChildren().clear();
                    for (String nombre : nombres) {
                        galeria.getChildren().add(crearTarjetaImagen(nombre));
                    }
                    if (nombres.isEmpty()) {
                        Label vacio = new Label("No hay imágenes cargadas. Sube una con el botón.");
                        vacio.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
                        galeria.getChildren().add(vacio);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarMensaje("Error al cargar imágenes", false));
            }
        }).start();
    }

    private VBox crearTarjetaImagen(String nombre) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.1),8,0,0,2);");
        card.setPrefWidth(160);

        ImageView iv = new ImageView();
        iv.setFitWidth(220);
        iv.setFitHeight(160);
        iv.setPreserveRatio(true);

        new Thread(() -> {
            try {
                URL imgUrl = new URL("http://" + ConfigManager.getIp() + ":8080/api/carrusel/imagen/" + nombre);
                Image img = new Image(imgUrl.toString(), 140, 100, true, true);
                Platform.runLater(() -> iv.setImage(img));
            } catch (Exception ignored) {}
        }).start();

        Label lblNombre = new Label(nombre.length() > 22 ? nombre.substring(0, 19) + "..." : nombre);
        lblNombre.setFont(Font.font("Arial", 11));
        lblNombre.setTextFill(Color.web("#7f8c8d"));

        Button btnEliminar = new Button("🗑️ Eliminar");
        btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 11px; -fx-cursor: hand;");
        btnEliminar.setOnAction(e -> eliminarImagen(nombre));

        card.getChildren().addAll(iv, lblNombre, btnEliminar);
        card.setPrefWidth(200);
        return card;
    }

    private void subirImagen() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar imagen");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png"));

        File archivo = chooser.showOpenDialog(new Stage());
        if (archivo == null) return;

        // Validar tamaño (máximo 5 MB)
        final long MAX_BYTES = 5 * 1024 * 1024L;
        if (archivo.length() > MAX_BYTES) {
            mostrarMensaje("❌ El archivo supera el tamaño máximo permitido (5 MB). Tamaño actual: "
                    + String.format("%.1f", archivo.length() / (1024.0 * 1024.0)) + " MB", false);
            return;
        }

        new Thread(() -> {
            try {
                String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                URL url = new URL("http://" + ConfigManager.getIp() + ":8080/api/carrusel/subir");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (OutputStream os = conn.getOutputStream();
                     PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) {

                    pw.append("--").append(boundary).append("\r\n");
                    pw.append("Content-Disposition: form-data; name=\"archivo\"; filename=\"")
                      .append(archivo.getName()).append("\"\r\n");
                    pw.append("Content-Type: image/").append(archivo.getName().endsWith(".png") ? "png" : "jpeg").append("\r\n\r\n");
                    pw.flush();

                    try (FileInputStream fis = new FileInputStream(archivo)) {
                        byte[] buf = new byte[4096];
                        int read;
                        while ((read = fis.read(buf)) != -1) os.write(buf, 0, read);
                    }
                    os.flush();

                    pw.append("\r\n--").append(boundary).append("--\r\n");
                    pw.flush();
                }

                final int code = conn.getResponseCode();
                Platform.runLater(() -> {
                    if (code == 200) { mostrarMensaje("✅ Imagen subida", true); cargarImagenes(); }
                    else mostrarMensaje("Error al subir imagen (código " + code + ")", false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarMensaje("Error de conexión: " + ex.getMessage(), false));
            }
        }).start();
    }

    private void eliminarImagen(String nombre) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar la imagen \"" + nombre + "\"?\nEsta acción no se puede deshacer.",
                ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirmar eliminación");
        conf.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        URL url = new URL("http://" + ConfigManager.getIp() + ":8080/api/carrusel/eliminar/" + nombre);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("DELETE");
                        final int code = conn.getResponseCode();
                        Platform.runLater(() -> {
                            if (code == 200) { mostrarMensaje("✅ Imagen eliminada", true); cargarImagenes(); }
                            else mostrarMensaje("Error al eliminar", false);
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> mostrarMensaje("Error de conexión", false));
                    }
                }).start();
            }
        });
    }

    private void mostrarMensaje(String msg, boolean ok) {
        lblMensaje.setText(msg);
        lblMensaje.setTextFill(ok ? Color.web("#27ae60") : Color.web("#c0392b"));
    }
}
