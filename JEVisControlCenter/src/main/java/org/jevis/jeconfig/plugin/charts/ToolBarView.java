/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.jeconfig.plugin.charts;

import com.jfoenix.controls.JFXDatePicker;
import com.jfoenix.controls.JFXTimePicker;
import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisObject;
import org.jevis.commons.dataprocessing.ManipulationMode;
import org.jevis.commons.datetime.DateHelper;
import org.jevis.commons.relationship.ObjectRelations;
import org.jevis.jeconfig.Constants;
import org.jevis.jeconfig.GlobalToolBar;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.application.Chart.ChartElements.DateValueAxis;
import org.jevis.jeconfig.application.Chart.ChartPluginElements.PickerCombo;
import org.jevis.jeconfig.application.Chart.Charts.MultiAxis.MultiAxisChart;
import org.jevis.jeconfig.application.Chart.Charts.MultiAxis.regression.RegressionType;
import org.jevis.jeconfig.application.Chart.TimeFrame;
import org.jevis.jeconfig.application.Chart.data.AnalysisDataModel;
import org.jevis.jeconfig.dialog.ChartSelectionDialog;
import org.jevis.jeconfig.dialog.LoadAnalysisDialog;
import org.jevis.jeconfig.dialog.Response;
import org.jevis.jeconfig.dialog.SaveAnalysisDialog;
import org.jevis.jeconfig.tool.I18n;
import org.jevis.jeconfig.tool.NumberSpinner;
import org.joda.time.DateTime;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author broder
 */
public class ToolBarView {

    private static final Logger logger = LogManager.getLogger(ToolBarView.class);
    private final JEVisDataSource ds;
    private final GraphPluginView graphPluginView;
    private final ObjectRelations objectRelations;
    private AnalysisDataModel model;
    private ComboBox<JEVisObject> listAnalysesComboBox;
    private Boolean _initialized = false;
    private ToggleButton save;
    private ToggleButton loadNew;
    private ToggleButton exportCSV;
    private ToggleButton exportImage;
    private ToggleButton reload;
    private ToggleButton delete;
    private ToggleButton autoResize;
    private ToggleButton select;
    private ToggleButton disableIcons;
    private ToggleButton zoomOut;
    private PickerCombo pickerCombo;
    private ComboBox<TimeFrame> presetDateBox;
    private JFXDatePicker pickerDateStart;
    private JFXTimePicker pickerTimeStart;
    private JFXDatePicker pickerDateEnd;
    private JFXTimePicker pickerTimeEnd;
    private DateHelper dateHelper = new DateHelper();
    private ToolBar toolBar;
    private Boolean changed = false;

    private ToggleButton runUpdateButton;
    private ChangeListener<JEVisObject> analysisComboBoxChangeListener = (observable, oldValue, newValue) -> {
        if ((oldValue == null) || (Objects.nonNull(newValue))) {

            if (changed) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setResizable(true);
                Label text = new Label(I18n.getInstance().getString("plugin.graph.dialog.changed.text"));
                text.setWrapText(true);
                alert.getDialogPane().setContent(text);

                alert.showAndWait().ifPresent(buttonType -> {
                    if (buttonType.equals(ButtonType.OK)) {
                        changed = false;
                        getGraphPluginView().handleRequest(Constants.Plugin.Command.SAVE);
                    }
                });
            }
            model.setCurrentAnalysis(newValue);
            model.resetToolbarSettings();
            model.setGlobalAnalysisTimeFrame(model.getGlobalAnalysisTimeFrame());
            Platform.runLater(this::updateLayout);
            changed = false;
        }
    };

    private JEVisDataSource getDs() {
        return ds;
    }

    private ToggleButton addSeriesRunningMean;
    private ImageView pauseIcon;
    private ImageView playIcon;
    private ToggleButton showRawData;
    private ToggleButton showSum;
    private ToggleButton showL1L2;
    private ToggleButton calcRegression;

    public ToolBarView(AnalysisDataModel model, JEVisDataSource ds, GraphPluginView graphPluginView) {
        this.model = model;
        this.ds = ds;
        this.objectRelations = new ObjectRelations(ds);
        this.graphPluginView = graphPluginView;
    }

    public ToolBar getToolbar(JEVisDataSource ds) {
        if (toolBar == null) {
            toolBar = new ToolBar();
            toolBar.setId("ObjectPlugin.Toolbar");

            updateLayout();
        }

        return toolBar;
    }


    public void setupAnalysisComboBoxListener() {
        listAnalysesComboBox.valueProperty().addListener(analysisComboBoxChangeListener);
    }

    private void resetZoom() {
        graphPluginView.getCharts().forEach(chartView -> {
            MultiAxisChart chart = (MultiAxisChart) chartView.getChart().getChart();
            DateValueAxis dateValueAxis = (DateValueAxis) chart.getXAxis();
            dateValueAxis.setAutoRanging(true);
            ValueAxis valueAxis1 = (ValueAxis) chart.getY1Axis();
            valueAxis1.setAutoRanging(true);
            ValueAxis valueAxis2 = (ValueAxis) chart.getY2Axis();
            valueAxis2.setAutoRanging(true);
        });
    }

    private void addSeriesRunningMean() {
        model.setAddSeries(ManipulationMode.RUNNING_MEAN);
    }

    private void setCellFactoryForComboBox() {
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
                            if (!model.getMultipleDirectories())
                                setText(obj.getName());
                            else {
                                String prefix = objectRelations.getObjectPath(obj);

                                setText(prefix + obj.getName());
                            }
                        }

                    }
                };
            }
        };

        listAnalysesComboBox.setCellFactory(cellFactory);
        listAnalysesComboBox.setButtonCell(cellFactory.call(null));

        listAnalysesComboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                ComboBoxListViewSkin<?> skin = (ComboBoxListViewSkin<?>) listAnalysesComboBox.getSkin();
                if (skin != null) {
                    ListView<?> popupContent = (ListView<?>) skin.getPopupContent();
                    if (popupContent != null) {
                        popupContent.scrollTo(model.getObservableListAnalyses().indexOf(model.getCurrentAnalysis()));
                    }
                }
            });
        });
    }

    private void loadNewDialog() {

        LoadAnalysisDialog dialog = new LoadAnalysisDialog(ds, model);

        dialog.show();

        if (dialog.getResponse() == Response.NEW) {

            getGraphPluginView().handleRequest(Constants.Plugin.Command.NEW);
        } else if (dialog.getResponse() == Response.LOAD) {

        }

    }

    private void hideShowIconsInGraph() {
        model.setHideShowIcons(!model.getHideShowIcons());
    }

    private void autoResizeInGraph() {
        model.setAutoResize(!model.getAutoResize());
    }

    private void showRawDataInGraph() {
        model.setShowRawData(!model.getShowRawData());
    }

    private void showSumInGraph() {
        model.setShowSum(!model.getShowSum());
    }

    private void showL1L2InGraph() {
        model.setShowL1L2(!model.getShowL1L2());
    }

    private void calcRegression() {

        if (!model.calcRegression()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

            Label polyDegreeLabel = new Label("Degree:");
            NumberSpinner polyDegreeNumberSpinner = new NumberSpinner(new BigDecimal(1), new BigDecimal(1));
            polyDegreeNumberSpinner.setMin(new BigDecimal(1));
            polyDegreeNumberSpinner.setMax(new BigDecimal(11));

            Label regressionTypeLabel = new Label("Type");
            ObservableList<RegressionType> regressionTypes = FXCollections.observableArrayList(RegressionType.values());
            regressionTypes.remove(0);
            ComboBox<RegressionType> regressionTypeComboBox = new ComboBox<>(regressionTypes);
            regressionTypeComboBox.getSelectionModel().select(RegressionType.POLY);
            regressionTypeComboBox.setDisable(true);

            GridPane gridPane = new GridPane();
            gridPane.setVgap(4);
            gridPane.setHgap(4);

            gridPane.add(regressionTypeLabel, 0, 0);
            gridPane.add(regressionTypeComboBox, 1, 0);

            gridPane.add(polyDegreeLabel, 0, 1);
            gridPane.add(polyDegreeNumberSpinner, 1, 1);

            regressionTypeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.equals(oldValue)) {
                    if (newValue.equals(RegressionType.POLY)) {
                        gridPane.add(polyDegreeLabel, 0, 1);
                        gridPane.add(polyDegreeNumberSpinner, 1, 1);
                    } else {
                        gridPane.getChildren().removeAll(polyDegreeLabel, polyDegreeNumberSpinner);
                    }
                }
            });

            alert.getDialogPane().setContent(gridPane);

            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType.getButtonData().isDefaultButton()) {
                    model.setPolyRegressionDegree(polyDegreeNumberSpinner.getNumber().toBigInteger().intValue());
                    model.setRegressionType(regressionTypeComboBox.getSelectionModel().getSelectedItem());
                    model.setCalcRegression(!model.calcRegression());
                }
            });
        } else {
            model.setPolyRegressionDegree(-1);
            model.setRegressionType(RegressionType.NONE);
            model.setCalcRegression(!model.calcRegression());
        }
    }

    public ComboBox<JEVisObject> getListAnalysesComboBox() {
        return listAnalysesComboBox;
    }

    private void changeSettings() {
        ChartSelectionDialog dia = new ChartSelectionDialog(ds, model);

        if (dia.show() == Response.OK) {

            model.setCharts(dia.getChartPlugin().getData().getCharts());
            model.setSelectedData(dia.getChartPlugin().getData().getSelectedData());
            changed = true;
        }
    }


    public void selectFirst() {
        if (!_initialized) {
            model.updateListAnalyses();
        }
        listAnalysesComboBox.getSelectionModel().selectFirst();
    }

    public void select(JEVisObject obj) {
        getListAnalysesComboBox().getSelectionModel().select(obj);
    }

    public void setDisableToolBarIcons(boolean bool) {
        listAnalysesComboBox.setDisable(bool);
        save.setDisable(bool);
        loadNew.setDisable(bool);
        exportCSV.setDisable(bool);
        exportImage.setDisable(bool);
        reload.setDisable(bool);
        runUpdateButton.setDisable(bool);
        delete.setDisable(bool);
        autoResize.setDisable(bool);
        select.setDisable(bool);
        showRawData.setDisable(bool);
        showSum.setDisable(bool);
        showL1L2.setDisable(bool);
        calcRegression.setDisable(bool);
        disableIcons.setDisable(bool);
        zoomOut.setDisable(bool);
        presetDateBox.setDisable(bool);
        pickerDateStart.setDisable(bool);
        pickerDateEnd.setDisable(bool);
        pickerTimeStart.setDisable(bool);
        pickerTimeEnd.setDisable(bool);
    }

    public PickerCombo getPickerCombo() {
        return pickerCombo;
    }

    public void updateLayout() {
        Platform.runLater(() -> {

            listAnalysesComboBox = new ComboBox<>(model.getObservableListAnalyses());

            listAnalysesComboBox.setPrefWidth(300);

            setCellFactoryForComboBox();

            if (model.getCurrentAnalysis() != null) {
                listAnalysesComboBox.getSelectionModel().select(model.getCurrentAnalysis());
            }

            if (!listAnalysesComboBox.getItems().isEmpty()) {

                dateHelper.setStartTime(model.getWorkdayStart());
                dateHelper.setEndTime(model.getWorkdayEnd());
            }

            toolBar.getItems().clear();
            pickerCombo = new PickerCombo(model, null);
            pickerCombo.updateCellFactory();
            presetDateBox = pickerCombo.getPresetDateBox();
            pickerDateStart = pickerCombo.getStartDatePicker();
            pickerTimeStart = pickerCombo.getStartTimePicker();
            pickerDateEnd = pickerCombo.getEndDatePicker();
            pickerTimeEnd = pickerCombo.getEndTimePicker();

            createToolbarIcons();

            Separator sep1 = new Separator();
            Separator sep2 = new Separator();
            Separator sep3 = new Separator();
            Separator sep4 = new Separator();

            if (!JEConfig.getExpert()) {
                toolBar.getItems().addAll(listAnalysesComboBox,
                        sep1, presetDateBox, pickerDateStart, pickerDateEnd,
                        sep2, reload, zoomOut,
                        sep3, loadNew, save, delete, select, exportCSV, exportImage,
                        sep4, calcRegression, showL1L2, showSum, disableIcons, autoResize, runUpdateButton);
            } else {
                toolBar.getItems().addAll(listAnalysesComboBox,
                        sep1, presetDateBox, pickerDateStart, pickerDateEnd,
                        sep2, reload, zoomOut,
                        sep3, loadNew, save, delete, select, exportCSV, exportImage,
                        sep4, calcRegression, showL1L2, showRawData, showSum, disableIcons, autoResize, runUpdateButton);
            }

            setupAnalysisComboBoxListener();
            pickerCombo.addListener();
            startToolbarIconListener();

        });
    }

    private void startToolbarIconListener() {
        reload.selectedProperty().addListener((observable, oldValue, newValue) -> graphPluginView.handleRequest(Constants.Plugin.Command.RELOAD));

        runUpdateButton.setOnAction(action -> {
            if (runUpdateButton.isSelected()) {
                model.setRunUpdate(true);
                runUpdateButton.setGraphic(pauseIcon);
                model.setTimer();
            } else {
                model.setRunUpdate(false);
                runUpdateButton.setGraphic(playIcon);
                model.stopTimer();
            }
        });

        exportCSV.setOnAction(action -> {
            GraphExportCSV ge = null;
            if (graphPluginView.isZoomed()) {
                ge = new GraphExportCSV(
                        ds,
                        model,
                        new DateTime(graphPluginView.getxAxisLowerBound().longValue()),
                        new DateTime(graphPluginView.getxAxisUpperBound().longValue()));
            } else {
                ge = new GraphExportCSV(
                        ds,
                        model,
                        model.getGlobalAnalysisTimeFrame().getStart(),
                        model.getGlobalAnalysisTimeFrame().getEnd());
            }

            try {
                ge.export();
            } catch (FileNotFoundException | UnsupportedEncodingException | JEVisException e) {
                logger.error("Error: could not export to file.", e);
            }
        });

        exportImage.setOnAction(action -> {
            GraphExportImage ge = new GraphExportImage(model);

            if (ge.getDestinationFile() != null) {

                ge.export(graphPluginView.getvBox());

                Platform.runLater(() -> {
                    JEConfig.getStage().setMaximized(false);
                    double height = JEConfig.getStage().getHeight();
                    double width = JEConfig.getStage().getWidth();
                    JEConfig.getStage().setWidth(0);
                    JEConfig.getStage().setHeight(0);
                    JEConfig.getStage().setHeight(height);
                    JEConfig.getStage().setWidth(width);
                });

            }

        });

        save.setOnAction(action -> {
            new SaveAnalysisDialog(ds, model, pickerCombo, listAnalysesComboBox, changed);
        });

        loadNew.setOnAction(event -> {
            loadNewDialog();
        });

        zoomOut.setOnAction(event -> resetZoom());

        select.setOnAction(event -> {
            changeSettings();
        });

        delete.setOnAction(event -> getGraphPluginView().handleRequest(Constants.Plugin.Command.DELETE));

        showRawData.setOnAction(event -> showRawDataInGraph());

        showSum.setOnAction(event -> showSumInGraph());

        showL1L2.setOnAction(event -> showL1L2InGraph());

        calcRegression.setOnAction(event -> calcRegression());

        disableIcons.setOnAction(event -> hideShowIconsInGraph());

        addSeriesRunningMean.setOnAction(event -> addSeriesRunningMean());

        autoResize.setOnAction(event -> autoResizeInGraph());
    }

    private void createToolbarIcons() {
        double iconSize = 20;

        save = new ToggleButton("", JEConfig.getImage("save.gif", iconSize, iconSize));
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(save);

        Tooltip saveTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.save"));
        save.setTooltip(saveTooltip);

        loadNew = new ToggleButton("", JEConfig.getImage("1390343812_folder-open.png", iconSize, iconSize));
        Tooltip loadNewTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.loadNew"));
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(loadNew);
        loadNew.setTooltip(loadNewTooltip);

        exportCSV = new ToggleButton("", JEConfig.getImage("export-csv.png", iconSize, iconSize));
        Tooltip exportCSVTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.exportCSV"));
        exportCSV.setTooltip(exportCSVTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(exportCSV);

        exportImage = new ToggleButton("", JEConfig.getImage("export-image.png", iconSize, iconSize));
        Tooltip exportImageTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.exportImage"));
        exportImage.setTooltip(exportImageTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(exportImage);

        reload = new ToggleButton("", JEConfig.getImage("1403018303_Refresh.png", iconSize, iconSize));
        Tooltip reloadTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.reload"));
        reload.setTooltip(reloadTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(reload);

        pauseIcon = JEConfig.getImage("pause_32.png", iconSize, iconSize);
        playIcon = JEConfig.getImage("play_32.png", iconSize, iconSize);

        runUpdateButton = new ToggleButton("", playIcon);
        Tooltip runUpdateTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.runupdate"));
        runUpdateButton.setTooltip(runUpdateTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(runUpdateButton);
        runUpdateButton.setSelected(model.getRunUpdate());
        runUpdateButton.styleProperty().bind(
                Bindings
                        .when(runUpdateButton.hoverProperty())
                        .then(
                                new SimpleStringProperty("-fx-background-insets: 1 1 1;"))
                        .otherwise(Bindings
                                .when(runUpdateButton.selectedProperty())
                                .then("-fx-background-insets: 1 1 1;")
                                .otherwise(
                                        new SimpleStringProperty("-fx-background-color: transparent;-fx-background-insets: 0 0 0;"))));

        delete = new ToggleButton("", JEConfig.getImage("if_trash_(delete)_16x16_10030.gif", iconSize, iconSize));
        Tooltip deleteTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.delete"));
        delete.setTooltip(deleteTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(delete);

        autoResize = new ToggleButton("", JEConfig.getImage("if_full_screen_61002.png", iconSize, iconSize));
        Tooltip autoResizeTip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.autosize"));
        autoResize.setTooltip(autoResizeTip);
        autoResize.setSelected(model.getAutoResize());
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(autoResize);
        autoResize.styleProperty().bind(
                Bindings
                        .when(autoResize.hoverProperty())
                        .then(
                                new SimpleStringProperty("-fx-background-insets: 1 1 1;"))
                        .otherwise(Bindings
                                .when(autoResize.selectedProperty())
                                .then("-fx-background-insets: 1 1 1;")
                                .otherwise(
                                        new SimpleStringProperty("-fx-background-color: transparent;-fx-background-insets: 0 0 0;"))));

        select = new ToggleButton("", JEConfig.getImage("Data.png", iconSize, iconSize));
        Tooltip selectTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.select"));
        select.setTooltip(selectTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(select);

        showRawData = new ToggleButton("", JEConfig.getImage("raw_199316.png", iconSize, iconSize));
        Tooltip showRawDataTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.showrawdata"));
        showRawData.setTooltip(showRawDataTooltip);
        showRawData.setSelected(model.getShowRawData());
        showRawData.styleProperty().bind(
                Bindings
                        .when(showRawData.hoverProperty())
                        .then(
                                new SimpleStringProperty("-fx-background-insets: 1 1 1;"))
                        .otherwise(Bindings
                                .when(showRawData.selectedProperty())
                                .then("-fx-background-insets: 1 1 1;")
                                .otherwise(
                                        new SimpleStringProperty("-fx-background-color: transparent;-fx-background-insets: 0 0 0;"))));

        showSum = new ToggleButton("", JEConfig.getImage("Sum_132399.png", iconSize, iconSize));
        Tooltip showSumTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.showsum"));
        showSum.setTooltip(showSumTooltip);
        showSum.setSelected(model.getShowSum());
        showSum.styleProperty().bind(
                Bindings
                        .when(showSum.hoverProperty())
                        .then(
                                new SimpleStringProperty("-fx-background-insets: 1 1 1;"))
                        .otherwise(Bindings
                                .when(showSum.selectedProperty())
                                .then("-fx-background-insets: 1 1 1;")
                                .otherwise(
                                        new SimpleStringProperty("-fx-background-color: transparent;-fx-background-insets: 0 0 0;"))));

        showL1L2 = new ToggleButton("", JEConfig.getImage("l1l2.png", iconSize, iconSize));
        Tooltip showL1L2Tooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.showl1l2"));
        showL1L2.setTooltip(showL1L2Tooltip);
        showL1L2.setSelected(model.getShowL1L2());
        showL1L2.styleProperty().bind(
                Bindings
                        .when(showL1L2.hoverProperty())
                        .then(
                                new SimpleStringProperty("-fx-background-insets: 1 1 1;"))
                        .otherwise(Bindings
                                .when(showL1L2.selectedProperty())
                                .then("-fx-background-insets: 1 1 1;")
                                .otherwise(
                                        new SimpleStringProperty("-fx-background-color: transparent;-fx-background-insets: 0 0 0;"))));

        calcRegression = new ToggleButton("", JEConfig.getImage("regression.png", iconSize, iconSize));
        Tooltip calcRegressionTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.calcregression"));
        calcRegression.setTooltip(calcRegressionTooltip);
        calcRegression.setSelected(model.calcRegression());
        calcRegression.styleProperty().bind(
                Bindings
                        .when(calcRegression.hoverProperty())
                        .then(
                                new SimpleStringProperty("-fx-background-insets: 1 1 1;"))
                        .otherwise(Bindings
                                .when(calcRegression.selectedProperty())
                                .then("-fx-background-insets: 1 1 1;")
                                .otherwise(
                                        new SimpleStringProperty("-fx-background-color: transparent;-fx-background-insets: 0 0 0;"))));

        disableIcons = new ToggleButton("", JEConfig.getImage("1415304498_alert.png", iconSize, iconSize));
        Tooltip disableIconsTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.disableicons"));
        disableIcons.setTooltip(disableIconsTooltip);
        disableIcons.setSelected(model.getHideShowIcons());
        disableIcons.styleProperty().bind(
                Bindings
                        .when(disableIcons.hoverProperty())
                        .then(
                                new SimpleStringProperty("-fx-background-insets: 1 1 1;"))
                        .otherwise(Bindings
                                .when(disableIcons.selectedProperty())
                                .then("-fx-background-insets: 1 1 1;")
                                .otherwise(
                                        new SimpleStringProperty("-fx-background-color: transparent;-fx-background-insets: 0 0 0;"))));

        addSeriesRunningMean = new ToggleButton("", JEConfig.getImage("1415304498_alert.png", iconSize, iconSize));
        Tooltip addSeriesRunningMeanTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.disableicons"));
        addSeriesRunningMean.setTooltip(addSeriesRunningMeanTooltip);
        addSeriesRunningMean.styleProperty().bind(
                Bindings
                        .when(addSeriesRunningMean.hoverProperty())
                        .then(
                                new SimpleStringProperty("-fx-background-insets: 1 1 1;"))
                        .otherwise(Bindings
                                .when(addSeriesRunningMean.selectedProperty())
                                .then("-fx-background-insets: 1 1 1;")
                                .otherwise(
                                        new SimpleStringProperty("-fx-background-color: transparent;-fx-background-insets: 0 0 0;"))));


        zoomOut = new ToggleButton("", JEConfig.getImage("ZoomOut.png", iconSize, iconSize));
        Tooltip zoomOutTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.zoomout"));
        zoomOut.setTooltip(zoomOutTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(zoomOut);

        if (!_initialized) {
            save.setDisable(false);
            delete.setDisable(false);

            setDisableToolBarIcons(true);

            _initialized = true;
        }
    }

    public Boolean getChanged() {
        return changed;
    }

    public void setChanged(Boolean changed) {
        this.changed = changed;
    }

    public GraphPluginView getGraphPluginView() {
        return graphPluginView;
    }
}