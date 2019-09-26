package org.jevis.jeconfig.plugin.reports;

import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.jevis.api.*;
import org.jevis.commons.relationship.ObjectRelations;
import org.jevis.commons.utils.AlphanumComparator;
import org.jevis.jeconfig.Constants;
import org.jevis.jeconfig.GlobalToolBar;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.Plugin;
import org.jevis.jeconfig.tool.I18n;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ReportPlugin implements Plugin {
    private static final Logger logger = LogManager.getLogger(ReportPlugin.class);
    public static String PLUGIN_NAME = "Report Plugin";
    public static String REPORT_CLASS = "Periodic Report";
    private final JEVisDataSource ds;
    private final String title;
    private final BorderPane borderPane;
    private final ObjectRelations objectRelations;
    private final ToolBar toolBar;
    private boolean initialized = false;
    private ListView<JEVisObject> listView;
    private WebView web = new WebView();
    private ComboBox<DateTime> dateTimeComboBox;

    public ReportPlugin(JEVisDataSource ds, String title) {
        this.ds = ds;
        this.title = title;
        this.borderPane = new BorderPane();
        this.toolBar = new ToolBar();

        initToolBar();

        this.objectRelations = new ObjectRelations(ds);
    }

    private void initToolBar() {
        ToggleButton reload = new ToggleButton("", JEConfig.getImage("1403018303_Refresh.png", 20, 20));
        Tooltip reloadTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.reload"));
        reload.setTooltip(reloadTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(reload);

        reload.setOnAction(event -> {
            initialized = false;
            ds.clearCache();
            try {
                ds.preload();
            } catch (JEVisException e) {
                e.printStackTrace();
            }

            getContentNode();
        });

        Separator sep1 = new Separator(Orientation.VERTICAL);

        ToggleButton pdfButton = new ToggleButton("", JEConfig.getImage("pdf_24_2133056.png", 20, 20));
        Tooltip pdfTooltip = new Tooltip(I18n.getInstance().getString("plugin.reports.toolbar.tooltip.pdf"));
        pdfButton.setTooltip(pdfTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(pdfButton);

        pdfButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("PDF File Destination");
            DateTimeFormatter fmtDate = DateTimeFormat.forPattern("yyyyMMdd");
            FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("PDF Files (*.pdf)", ".pdf");
            fileChooser.getExtensionFilters().addAll(pdfFilter);
            fileChooser.setSelectedExtensionFilter(pdfFilter);

            JEVisObject selectedItem = listView.getSelectionModel().getSelectedItem();
            fileChooser.setInitialFileName(selectedItem.getName() + fmtDate.print(new DateTime()));
            File file = fileChooser.showSaveDialog(JEConfig.getStage());
            if (file != null) {
                File destinationFile = new File(file + fileChooser.getSelectedExtensionFilter().getExtensions().get(0));
                try {
                    JEVisAttribute last_report_pdf = selectedItem.getAttribute("Last Report PDF");
                    DateTime dateTime = dateTimeComboBox.getSelectionModel().getSelectedItem();
                    List<JEVisSample> samples = last_report_pdf.getSamples(dateTime, dateTime);
                    samples.get(0).getValueAsFile().saveToFile(destinationFile);
                } catch (JEVisException | IOException e) {
                    e.printStackTrace();
                }
            }
        });

        ToggleButton xlsxButton = new ToggleButton("", JEConfig.getImage("xlsx_315594.png", 20, 20));
        Tooltip xlsxTooltip = new Tooltip(I18n.getInstance().getString("plugin.reports.toolbar.tooltip.xlsx"));
        xlsxButton.setTooltip(xlsxTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(xlsxButton);

        xlsxButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("XLSX File Destination");
            DateTimeFormatter fmtDate = DateTimeFormat.forPattern("yyyyMMdd");
            FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", ".xlsx");
            fileChooser.getExtensionFilters().addAll(pdfFilter);
            fileChooser.setSelectedExtensionFilter(pdfFilter);

            JEVisObject selectedItem = listView.getSelectionModel().getSelectedItem();
            fileChooser.setInitialFileName(selectedItem.getName() + fmtDate.print(new DateTime()));
            File file = fileChooser.showSaveDialog(JEConfig.getStage());
            if (file != null) {
                File destinationFile = new File(file + fileChooser.getSelectedExtensionFilter().getExtensions().get(0));
                try {
                    JEVisAttribute last_report_pdf = selectedItem.getAttribute("Last Report");
                    DateTime dateTime = dateTimeComboBox.getSelectionModel().getSelectedItem();
                    List<JEVisSample> samples = last_report_pdf.getSamples(dateTime.minusMinutes(1), dateTime.plusMinutes(1));
                    samples.get(0).getValueAsFile().saveToFile(destinationFile);
                } catch (JEVisException | IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Separator sep2 = new Separator(Orientation.VERTICAL);

        ToggleButton printButton = new ToggleButton("", JEConfig.getImage("Print_1493286.png", 20, 20));
        Tooltip printTooltip = new Tooltip(I18n.getInstance().getString("plugin.reports.toolbar.tooltip.print"));
        printButton.setTooltip(printTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(printButton);

        printButton.setOnAction(event -> {
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            try {
                JEVisObject selectedItem = listView.getSelectionModel().getSelectedItem();
                JEVisAttribute last_report_pdf = selectedItem.getAttribute("Last Report PDF");
                DateTime dateTime = dateTimeComboBox.getSelectionModel().getSelectedItem();
                List<JEVisSample> samples = last_report_pdf.getSamples(dateTime, dateTime);
                PDDocument document = PDDocument.load(samples.get(0).getValueAsFile().getBytes());
                printerJob.setPageable(new PDFPageable(document));
                if (printerJob.printDialog()) {
                    printerJob.print();
                }
            } catch (IOException | JEVisException | PrinterException e) {
                e.printStackTrace();
            }
        });

        toolBar.getItems().setAll(reload, sep1, pdfButton, xlsxButton, sep2, printButton);
    }

    @Override
    public String getClassName() {
        return "Report Plugin";
    }

    @Override
    public String getName() {
        return title;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public StringProperty nameProperty() {
        return null;
    }

    @Override
    public String getUUID() {
        return null;
    }

    @Override
    public void setUUID(String id) {

    }

    @Override
    public String getToolTip() {
        return null;
    }

    @Override
    public StringProperty uuidProperty() {
        return null;
    }

    @Override
    public Node getMenu() {
        return null;
    }

    @Override
    public boolean supportsRequest(int cmdType) {
        return false;
    }

    @Override
    public Node getToolbar() {
        return toolBar;
    }

    @Override
    public void updateToolbar() {

    }

    @Override
    public JEVisDataSource getDataSource() {
        return ds;
    }

    @Override
    public void setDataSource(JEVisDataSource ds) {

    }

    @Override
    public void handleRequest(int cmdType) {

    }

    @Override
    public Node getContentNode() {
        if (!initialized) {
            init();
        }
        return borderPane;
    }

    private void init() {
        SplitPane sp = new SplitPane();
        sp.setDividerPositions(.3d);
        sp.setOrientation(Orientation.HORIZONTAL);
        sp.setId("mainsplitpane");
        sp.setStyle("-fx-background-color: " + Constants.Color.LIGHT_GREY2);

        ObservableList<JEVisObject> reports = FXCollections.observableArrayList(getAllReports());
        AlphanumComparator ac = new AlphanumComparator();
        reports.sort((o1, o2) -> {
            String name1 = objectRelations.getObjectPath(o1) + o1.getName();
            String name2 = objectRelations.getObjectPath(o2) + o2.getName();
            return ac.compare(name1, name2);
        });

        listView = new ListView<>(reports);
        listView.setPrefWidth(250);
        setupCellFactory(listView);

        BorderPane view = new BorderPane();
        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue != oldValue) {
                loadReport(view, newValue);
            }
        });

        sp.getItems().setAll(listView, view);

        GridPane.setFillWidth(view, true);
        GridPane.setFillHeight(listView, true);
        GridPane.setFillHeight(view, true);

        borderPane.setCenter(sp);

        initialized = true;
    }

    private void loadReport(BorderPane view, JEVisObject reportObject) {
        JEVisAttribute lastReportPDFAttribute = null;
        try {
            lastReportPDFAttribute = reportObject.getAttribute("Last Report PDF");
        } catch (JEVisException e) {
            logger.error("Could not get 'Last Report' Attribute from object {}:{}", reportObject.getName(), reportObject.getID(), e);
        }

        if (lastReportPDFAttribute != null) {
            List<JEVisSample> allSamples = lastReportPDFAttribute.getAllSamples();
            if (allSamples.size() > 0) {
                JEVisSample lastSample = allSamples.get(allSamples.size() - 1);
                Map<DateTime, JEVisSample> sampleMap = new HashMap<>();
                List<DateTime> dateTimeList = new ArrayList<>();
                for (JEVisSample jeVisSample : allSamples) {
                    try {
                        dateTimeList.add(jeVisSample.getTimestamp());
                        sampleMap.put(jeVisSample.getTimestamp(), jeVisSample);
                    } catch (JEVisException e) {
                        logger.error("Could not add date to dat list.");
                    }
                }
                dateTimeComboBox = new ComboBox<>(FXCollections.observableList(dateTimeList));
                Callback<ListView<DateTime>, ListCell<DateTime>> cellFactory = new Callback<ListView<DateTime>, ListCell<DateTime>>() {
                    @Override
                    public ListCell<DateTime> call(ListView<DateTime> param) {
                        return new ListCell<DateTime>() {
                            @Override
                            protected void updateItem(DateTime obj, boolean empty) {
                                super.updateItem(obj, empty);
                                if (obj == null || empty) {
                                    setGraphic(null);
                                    setText(null);
                                } else {
                                    setText(obj.toString("yyyy-MM-dd HH:mm"));
                                }
                            }
                        };
                    }
                };

                dateTimeComboBox.setCellFactory(cellFactory);
                dateTimeComboBox.setButtonCell(cellFactory.call(null));

                try {
                    dateTimeComboBox.getSelectionModel().select(lastSample.getTimestamp());
                } catch (JEVisException e) {
                    logger.error("Could not get Time Stamp of last sample.");
                    dateTimeComboBox.getSelectionModel().select(dateTimeList.size() - 1);
                }

                HBox hBox = new HBox();
                hBox.setPadding(new Insets(4, 4, 4, 4));
                hBox.setSpacing(4);
                ImageView leftImage = JEConfig.getImage("left.png", 20, 20);
                ImageView rightImage = JEConfig.getImage("right.png", 20, 20);

                leftImage.setOnMouseClicked(event -> {
                    int i = dateTimeComboBox.getSelectionModel().getSelectedIndex();
                    if (i > 0) {
                        dateTimeComboBox.getSelectionModel().select(i - 1);
                    }
                });

                rightImage.setOnMouseClicked(event -> {
                    int i = dateTimeComboBox.getSelectionModel().getSelectedIndex();
                    if (i < sampleMap.size()) {
                        dateTimeComboBox.getSelectionModel().select(i + 1);
                    }
                });

                Separator sep1 = new Separator(Orientation.VERTICAL);
                Separator sep2 = new Separator(Orientation.VERTICAL);

                Label labelDateTimeComboBox = new Label(I18n.getInstance().getString("plugin.reports.selectionbox.label"));
                labelDateTimeComboBox.setAlignment(Pos.CENTER_LEFT);

                hBox.getChildren().addAll(labelDateTimeComboBox, leftImage, sep1, dateTimeComboBox, sep2, rightImage);

                view.setTop(hBox);

                WebEngine engine = web.getEngine();
                String url = JEConfig.class.getResource("/web/viewer.html").toExternalForm();

                // connect CSS styles to customize pdf.js appearance
                engine.setUserStyleSheetLocation(JEConfig.class.getResource("/web/web.css").toExternalForm());

                engine.setJavaScriptEnabled(true);
                engine.load(url);

                view.setCenter(web);

                dateTimeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue.equals(oldValue)) {
                        try {
//                            byte[] data = FileUtils.readFileToByteArray(new File("/path/to/another/file"));

                            byte[] bytes = sampleMap.get(newValue).getValueAsFile().getBytes();
                            String base64 = Base64.getEncoder().encodeToString(bytes);
                            engine.executeScript("openFileFromBase64('" + base64 + "')");
                        } catch (Exception e) {
                            logger.error("Could not load report for {}:{} for ts {}", reportObject.getName(), reportObject.getID(), newValue.toString(), e);
                        }
                    }
                });

                engine.getLoadWorker()
                        .stateProperty()
                        .addListener((observable, oldValue, newValue) -> {
                            if (newValue == Worker.State.SUCCEEDED) {
                                try {

                                    byte[] bytes = sampleMap.get(lastSample.getTimestamp()).getValueAsFile().getBytes();
                                    String base64 = Base64.getEncoder().encodeToString(bytes);
                                    // call JS function from Java code
                                    engine.executeScript("openFileFromBase64('" + base64 + "')");
                                } catch (Exception e) {
                                    logger.error("Could not load latest report for {}:{}", reportObject.getName(), reportObject.getID(), e);
                                }
                            }
                        });
            }
        } else {
            view.setTop(null);
            view.setCenter(null);
        }
    }

    private void setupCellFactory(ListView<JEVisObject> listView) {
        Callback<ListView<JEVisObject>, ListCell<JEVisObject>> cellFactory = new Callback<ListView<JEVisObject>, ListCell<JEVisObject>>() {
            @Override
            public ListCell<JEVisObject> call(ListView<JEVisObject> param) {
                return new ListCell<JEVisObject>() {
                    @Override
                    protected void updateItem(JEVisObject obj, boolean empty) {
                        super.updateItem(obj, empty);
                        if (obj == null || empty) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            String prefix = objectRelations.getObjectPath(obj);

                            setText(prefix + obj.getName());
                        }

                    }
                };
            }
        };

        listView.setCellFactory(cellFactory);
    }

    private List<JEVisObject> getAllReports() {
        List<JEVisObject> list = new ArrayList<>();
        JEVisClass reportClass = null;
        try {
            reportClass = ds.getJEVisClass(REPORT_CLASS);
            list = ds.getObjects(reportClass, true);
        } catch (JEVisException e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public ImageView getIcon() {
        return JEConfig.getImage("Report.png", 20, 20);
    }

    @Override
    public void fireCloseEvent() {

    }

    @Override
    public void setHasFocus() {

    }

    @Override
    public void openObject(Object object) {

    }

    @Override
    public int getPrefTapPos() {
        return 3;
    }
}
