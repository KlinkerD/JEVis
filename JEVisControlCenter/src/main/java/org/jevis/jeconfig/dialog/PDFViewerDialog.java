package org.jevis.jeconfig.dialog;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.jevis.api.JEVisFile;
import org.jevis.commons.i18n.I18n;
import org.jevis.jeconfig.GlobalToolBar;
import org.jevis.jeconfig.JEConfig;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class PDFViewerDialog {
    private static final Logger logger = LogManager.getLogger(PDFViewerDialog.class);
    private final int iconSize = 32;
    private Stage stage;
    private WebView web = new WebView();
    private WebEngine engine = web.getEngine();

    public PDFViewerDialog() {

        this.engine.setUserStyleSheetLocation(JEConfig.class.getResource("/web/web.css").toExternalForm());

        this.engine.setJavaScriptEnabled(true);
        this.engine.load(JEConfig.class.getResource("/web/viewer.html").toExternalForm());
    }

    public void show(JEVisFile file, Window owner) {

        if (stage != null) {
            stage.close();
            stage = null;
        }

        stage = new Stage();

        stage.setTitle(I18n.getInstance().getString("dialog.pdfviewer.title"));

        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.initOwner(owner);

        double maxScreenWidth = Screen.getPrimary().getBounds().getMaxX();
        double maxScreenHeight = Screen.getPrimary().getBounds().getMaxY();
        stage.setWidth(maxScreenWidth * 0.85);
        stage.setHeight(maxScreenHeight * 0.85);

        stage.setResizable(true);

        BorderPane bp = new BorderPane();

        HBox headerBox = new HBox();
        headerBox.setSpacing(4);

        ToggleButton pdfButton = new ToggleButton("", JEConfig.getImage("pdf_24_2133056.png", iconSize, iconSize));
        Tooltip pdfTooltip = new Tooltip(I18n.getInstance().getString("plugin.reports.toolbar.tooltip.pdf"));
        pdfButton.setTooltip(pdfTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(pdfButton);

        pdfButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("PDF File Destination");
            FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("PDF Files (*.pdf)", ".pdf");
            fileChooser.getExtensionFilters().addAll(pdfFilter);
            fileChooser.setSelectedExtensionFilter(pdfFilter);

            fileChooser.setInitialFileName(file.getFilename());
            File fileDestination = fileChooser.showSaveDialog(stage);
            if (fileDestination != null) {
                File destinationFile = new File(fileDestination + fileChooser.getSelectedExtensionFilter().getExtensions().get(0));
                try {
                    file.saveToFile(destinationFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        ToggleButton printButton = new ToggleButton("", JEConfig.getImage("Print_1493286.png", iconSize, iconSize));
        Tooltip printTooltip = new Tooltip(I18n.getInstance().getString("plugin.reports.toolbar.tooltip.print"));
        printButton.setTooltip(printTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(printButton);

        printButton.setOnAction(event -> {
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            try {
                PDDocument document = PDDocument.load(file.getBytes());
                printerJob.setPageable(new PDFPageable(document));
                if (printerJob.printDialog()) {
                    printerJob.print();
                }
            } catch (IOException | PrinterException e) {
                e.printStackTrace();
            }
        });

        Region spacer = new Region();
        Label fileName = new Label(file.getFilename());
        fileName.setPadding(new Insets(0, 4, 0, 0));
        fileName.setTextFill(Color.web("#0076a3"));
        fileName.setFont(new Font("Cambria", iconSize));

        headerBox.getChildren().addAll(pdfButton, printButton, spacer, fileName);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bp.setTop(headerBox);
        bp.setCenter(web);

        Scene scene = new Scene(bp);
        stage.setScene(scene);

        byte[] bytes = file.getBytes();
        String base64 = Base64.getEncoder().encodeToString(bytes);
        Platform.runLater(() -> engine.executeScript("openFileFromBase64('" + base64 + "')"));

        stage.showAndWait();
    }
}