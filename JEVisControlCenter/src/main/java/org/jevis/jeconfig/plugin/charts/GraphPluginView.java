/**
 * Copyright (C) 2015 Envidatec GmbH <info@envidatec.com>
 * <p>
 * This file is part of JEConfig.
 * <p>
 * JEConfig is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation in version 3.
 * <p>
 * JEConfig is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * JEConfig. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * JEConfig is part of the OpenJEVis project, further project information are
 * published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.jeconfig.plugin.charts;

import com.google.common.util.concurrent.AtomicDouble;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.renderer.spi.LabelledMarkerRenderer;
import eu.hansolo.fx.charts.MatrixPane;
import eu.hansolo.fx.charts.data.MatrixChartItem;
import eu.hansolo.fx.charts.tools.Helper;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisObject;
import org.jevis.commons.chart.ChartDataModel;
import org.jevis.commons.dataprocessing.AggregationPeriod;
import org.jevis.commons.dataprocessing.ManipulationMode;
import org.jevis.commons.i18n.I18n;
import org.jevis.commons.utils.AlphanumComparator;
import org.jevis.commons.ws.json.JsonObject;
import org.jevis.jeapi.ws.JEVisDataSourceWS;
import org.jevis.jeapi.ws.JEVisObjectWS;
import org.jevis.jeconfig.Constants;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.Plugin;
import org.jevis.jeconfig.application.Chart.AnalysisTimeFrame;
import org.jevis.jeconfig.application.Chart.ChartElements.MultiChartZoomer;
import org.jevis.jeconfig.application.Chart.ChartElements.TableEntry;
import org.jevis.jeconfig.application.Chart.ChartElements.TableHeader;
import org.jevis.jeconfig.application.Chart.ChartElements.XYChartSerie;
import org.jevis.jeconfig.application.Chart.ChartPluginElements.Columns.ColorColumn;
import org.jevis.jeconfig.application.Chart.ChartPluginElements.DataPointNoteDialog;
import org.jevis.jeconfig.application.Chart.ChartPluginElements.DataPointTableViewPointer;
import org.jevis.jeconfig.application.Chart.ChartPluginElements.TableTopDatePicker;
import org.jevis.jeconfig.application.Chart.ChartSettings;
import org.jevis.jeconfig.application.Chart.ChartType;
import org.jevis.jeconfig.application.Chart.Charts.*;
import org.jevis.jeconfig.application.Chart.Charts.MultiAxis.MultiAxisBubbleChart;
import org.jevis.jeconfig.application.Chart.TimeFrame;
import org.jevis.jeconfig.application.Chart.data.AnalysisDataModel;
import org.jevis.jeconfig.application.tools.ColorHelper;
import org.jevis.jeconfig.dialog.*;
import org.jevis.jeconfig.plugin.AnalysisRequest;
import org.joda.time.DateTime;

import java.text.NumberFormat;
import java.util.*;

/**
 * @author Florian Simon <florian.simon@envidatec.com>
 */
public class GraphPluginView implements Plugin {

    private static final Logger logger = LogManager.getLogger(GraphPluginView.class);
    public static String PLUGIN_NAME = "Graph Plugin";
    //    private final List<ChartView> charts = new ArrayList<>();
    private final DoubleProperty zoomDurationMillis = new SimpleDoubleProperty(750.0);
    private ToolBarView toolBarView;
    private AnalysisDataModel dataModel;
    //private GraphController controller;
    private StringProperty name = new SimpleStringProperty("Graph");
    private StringProperty id = new SimpleStringProperty("*NO_ID*");
    private JEVisDataSource ds;
    private ToolBar toolBar;
    private String tooltip = I18n.getInstance().getString("pluginmanager.graph.tooltip");
    private boolean firstStart = true;
    private final ScrollPane sp = new ScrollPane();
    public static String JOB_NAME = "Graph Update";
    private Double xAxisLowerBound;
    private Double xAxisUpperBound;
    private boolean zoomed = false;
    //this.chartView = new ChartView(dataModel);
    private VBox vBox = new VBox();
    private BorderPane border = new BorderPane(sp);
    private Tooltip tp;
    private List<Chart> allCharts = new ArrayList<>();

    public GraphPluginView(JEVisDataSource ds, String newname) {
        this.dataModel = new AnalysisDataModel(ds, this);
//        this.dataModel.addObserver(this);

        //this.controller = new GraphController(this, dataModel);
        this.toolBarView = new ToolBarView(dataModel, ds, this);
        getToolbar();

        this.vBox.setStyle("-fx-faint-focus-color: transparent; -fx-focus-color: transparent;");
        this.sp.setStyle("-fx-faint-focus-color: transparent; -fx-focus-color: transparent;");
        this.sp.setFitToWidth(true);
        this.sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        this.border.setStyle("-fx-background-color: " + Constants.Color.LIGHT_GREY2);

        this.ds = ds;
        this.name.set(newname);

        /**
         * If scene size changes and old value is not 0.0 (firsts draw) redraw
         * TODO: resizing an window manually will cause a lot of resize changes and so redraws, solve this better
         */
        border.heightProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(0.0) && dataModel.getSelectedData() != null && !dataModel.getSelectedData().isEmpty()) {
                Platform.runLater(this::autoSize);
            }
        });
        border.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(0.0) && dataModel.getSelectedData() != null && !dataModel.getSelectedData().isEmpty()) {
                Platform.runLater(this::autoSize);
            }
        });

        border.setStyle("-fx-background-color: " + Constants.Color.LIGHT_GREY2 + "; -fx-faint-focus-color: transparent; -fx-focus-color: transparent;");

    }

    private void autoSize() {
        Long chartsPerScreen = dataModel.getChartsPerScreen();

        Platform.runLater(() -> {
            AtomicDouble autoMinSize = new AtomicDouble(0);
            double autoMinSizeNormal = 220;
            double autoMinSizeLogical = 50;

            if (dataModel.getSelectedData() != null) {
                double maxHeight = border.getHeight();

                for (ChartSettings chartSettings : dataModel.getCharts()) {
                    if (chartSettings.getChartType().equals(ChartType.LOGICAL)) {
                        autoMinSize.set(autoMinSizeLogical);
                    } else {
                        autoMinSize.set(autoMinSizeNormal);
                    }
                }

                autoSize(autoMinSize.get(), maxHeight, chartsPerScreen, vBox);
                formatCharts();

                for (ChartSettings settings : dataModel.getCharts()) {
                    if (settings.getChartType() == ChartType.HEAT_MAP) {
                        Platform.runLater(this::formatCharts);
                    }
                }
            }
        });
    }

    @Override
    public String getClassName() {
        return PLUGIN_NAME;
    }

    @Override
    public void setHasFocus() {

        if (firstStart) {
            firstStart = false;

            VBox vBox = new VBox();
            vBox.setAlignment(Pos.CENTER);
            vBox.setSpacing(10);

            String style = "-fx-background-color: linear-gradient(#e2e2e2,#eeeeee);" +
                    "    -fx-background-insets: 0,1,4,5;\n" +
                    "    -fx-background-radius: 9,8,5,4;\n" +
                    "    -fx-padding: 15 30 15 30;\n" +
                    "    -fx-font-family: \"Cambria\";\n" +
                    "    -fx-font-size: 32px;\n" +
                    "    -fx-text-alignment: left;\n" +
                    "    -fx-text-fill: #0076a3;\n";

            Button newAnalysis = new Button(I18n.getInstance().getString("plugin.graph.analysis.new"), JEConfig.getImage("Data.png", 32, 32));
            newAnalysis.setStyle(style);
            newAnalysis.setAlignment(Pos.CENTER);

            Button loadAnalysis = new Button(I18n.getInstance().getString("plugin.graph.analysis.load"), JEConfig.getImage("1390343812_folder-open.png", 32, 32));
            loadAnalysis.setStyle(style);
            loadAnalysis.setAlignment(Pos.CENTER);

            newAnalysis.setOnAction(event -> newAnalysis());

            loadAnalysis.setOnAction(event -> openDialog());

            Region top = new Region();

            vBox.getChildren().setAll(top, loadAnalysis, newAnalysis);

            this.sp.setContent(vBox);

            Platform.runLater(() -> top.setPrefHeight(border.getHeight() / 3));
        }
    }

    private void openDialog() {

        LoadAnalysisDialog dialog = new LoadAnalysisDialog(ds, dataModel);
        toolBarView.setDisableToolBarIcons(false);

        dialog.show();

        if (dialog.getResponse() == Response.NEW) {

            newAnalysis();

        } else if (dialog.getResponse() == Response.LOAD) {

//            dataModel.setGlobalAnalysisTimeFrame(dataModel.getGlobalAnalysisTimeFrame());
//            dataModel.updateSamples();
//            dataModel.setCharts(dataModel.getCharts());
//            dataModel.setSelectedData(dataModel.getSelectedData());
        }
    }

    @Override
    public String getName() {
        return name.get();
    }

    @Override
    public void setName(String value) {
        name.set(value);
    }

    @Override
    public StringProperty nameProperty() {
        return name;
    }

    @Override
    public String getUUID() {
        return id.get();
    }

    @Override
    public void setUUID(String newid) {
        id.set(newid);
    }

    @Override
    public String getToolTip() {
        return tooltip;
    }

    @Override
    public StringProperty uuidProperty() {
        return id;
    }

    private void newAnalysis() {

        ChartSelectionDialog selectionDialog = new ChartSelectionDialog(ds, dataModel);

//        AnalysisTimeFrame atf = new AnalysisTimeFrame();
//        atf.setActiveTimeFrame(TimeFrame.CUSTOM);
//
//        dataModel.setAnalysisTimeFrame(atf);

        if (selectionDialog.show() == Response.OK) {
            toolBarView.setDisableToolBarIcons(false);

            dataModel.setCharts(selectionDialog.getChartPlugin().getData().getCharts());
            dataModel.setSelectedData(selectionDialog.getChartPlugin().getData().getSelectedData());
        }
    }

    @Override
    public Node getMenu() {
        return null;
    }

    @Override
    public Node getToolbar() {
        if (toolBar == null) {
            toolBar = toolBarView.getToolbar(getDataSource());
        }
        return toolBar;
    }

    @Override
    public void updateToolbar() {
        toolBarView.updateLayout();
    }

    @Override
    public JEVisDataSource getDataSource() {
        return ds;
    }

    @Override
    public void setDataSource(JEVisDataSource ds) {
        this.ds = ds;
    }

    @Override
    public boolean supportsRequest(int cmdType) {
        switch (cmdType) {
            case Constants.Plugin.Command.SAVE:
                return true;
            case Constants.Plugin.Command.DELETE:
                return true;
            case Constants.Plugin.Command.EXPAND:
                return false;
            case Constants.Plugin.Command.NEW:
                return true;
            case Constants.Plugin.Command.RELOAD:
                return true;
            case Constants.Plugin.Command.ADD_TABLE:
                return false;
            case Constants.Plugin.Command.EDIT_TABLE:
                return false;
            case Constants.Plugin.Command.CREATE_WIZARD:
                return false;
            case Constants.Plugin.Command.FIND_OBJECT:
                return false;
            case Constants.Plugin.Command.PASTE:
                return false;
            case Constants.Plugin.Command.COPY:
                return false;
            case Constants.Plugin.Command.CUT:
                return false;
            case Constants.Plugin.Command.FIND_AGAIN:
                return false;
            default:
                return false;
        }
    }

    @Override
    public void handleRequest(int cmdType) {
        try {
            switch (cmdType) {
                case Constants.Plugin.Command.SAVE:
                    new SaveAnalysisDialog(ds, dataModel, toolBarView);
                    break;
                case Constants.Plugin.Command.DELETE:
                    new DeleteAnalysisDialog(ds, dataModel, toolBarView.getListAnalysesComboBox());
                    break;
                case Constants.Plugin.Command.EXPAND:
                    break;
                case Constants.Plugin.Command.NEW:
                    new NewAnalysisDialog(ds, dataModel, this, toolBarView.getChanged());
                    break;
                case Constants.Plugin.Command.RELOAD:
                    JEVisObject currentAnalysis = dataModel.getCurrentAnalysis();
                    ManipulationMode currentManipulationMode = dataModel.getManipulationMode();
                    AggregationPeriod currentAggregationPeriod = dataModel.getAggregationPeriod();
                    AnalysisTimeFrame currentTimeframe = dataModel.getGlobalAnalysisTimeFrame();
                    dataModel.setCurrentAnalysis(null);
                    dataModel.setCurrentAnalysis(currentAnalysis);
                    dataModel.setCharts(new ArrayList<>());
                    dataModel.updateSelectedData();

                    dataModel.setManipulationMode(currentManipulationMode);
                    dataModel.setAggregationPeriod(currentAggregationPeriod);
                    dataModel.isGlobalAnalysisTimeFrame(true);
                    dataModel.setAnalysisTimeFrameForAllModels(currentTimeframe);
                    break;
                case Constants.Plugin.Command.ADD_TABLE:
                    break;
                case Constants.Plugin.Command.EDIT_TABLE:
                    break;
                case Constants.Plugin.Command.CREATE_WIZARD:
                    break;
                case Constants.Plugin.Command.FIND_OBJECT:
                    break;
                case Constants.Plugin.Command.PASTE:
                    break;
                case Constants.Plugin.Command.COPY:
                    break;
                case Constants.Plugin.Command.CUT:
                    break;
                case Constants.Plugin.Command.FIND_AGAIN:
                    break;
            }
        } catch (Exception ex) {
        }

    }


    @Override
    public void fireCloseEvent() {
    }

    @Override
    public ImageView getIcon() {
        return JEConfig.getImage("1415314386_Graph.png", 20, 20);
    }

    @Override
    public Node getContentNode() {
        return border;
    }

    public void update() {
        allCharts = new ArrayList<>();

        JEConfig.getStatusBar().progressProgressJob(GraphPluginView.JOB_NAME, 1, "");

        Platform.runLater(() -> {
            vBox.getChildren().clear();
            sp.setContent(vBox);
            try {
                tp.hide();
            } catch (Exception ignored) {
            }
        });

        Long horizontalPies = dataModel.getHorizontalPies();
        Long horizontalTables = dataModel.getHorizontalTables();
        int countOfPies = (int) dataModel.getCharts().stream().filter(charts -> charts.getChartType() == ChartType.PIE).count();
        int countOfTables = (int) dataModel.getCharts().stream().filter(charts -> charts.getChartType() == ChartType.TABLE).count();

        Platform.runLater(() -> {

            AtomicDouble autoMinSize = new AtomicDouble(0);
            double autoMinSizeNormal = 220;
            double autoMinSizeLogical = 50;

            if (dataModel.getSelectedData() != null) {

                List<HBox> pieFrames = new ArrayList<>();
                List<HBox> tableFrames = new ArrayList<>();

                AlphanumComparator ac = new AlphanumComparator();
                try {
                    dataModel.getCharts().sort((s1, s2) -> ac.compare(s1.getName(), s2.getName()));
                } catch (Exception e) {
                }

                int noOfPie = 0;
                int noOfTable = 0;
                int currentPieFrame = 0;
                int currentTableFrame = 0;

                for (ChartSettings chartSettings : dataModel.getCharts()) {
                    JEConfig.getStatusBar().progressProgressJob(GraphPluginView.JOB_NAME, 1, "Create" + chartSettings.getChartType() + " chart");
                    if (chartSettings.getChartType().equals(ChartType.LOGICAL)) {
                        autoMinSize.set(autoMinSizeLogical);
                    } else {
                        autoMinSize.set(autoMinSizeNormal);
                    }

                    BorderPane bp = new BorderPane();
                    bp.setStyle("-fx-faint-focus-color: transparent; -fx-focus-color: transparent;");

                    bp.setMinHeight(autoMinSize.get());

                    bp.setMaxWidth(sp.getMaxWidth());

                    /**
                     * Add offset for every data object because of the table legend
                     * Every row has about 25 pixel with the default font
                     */
                    int dataSizeOffset = 30;
                    /** Calculate maxsize based on the amount of Data **/
                    int dataSize = 0;

                    Chart chart = null;
                    if (chartSettings.getChartType() != ChartType.LOGICAL) {
                        JEConfig.getStatusBar().progressProgressJob(GraphPluginView.JOB_NAME, 1, "Create" + chartSettings.getChartType() + " chart");
                        chart = getChart(chartSettings, null);
                        allCharts.add(chart);
                    } else {
                        dataSize = 4;
                    }

                    for (ChartDataModel chartDataModel : dataModel.getSelectedData()) {
                        for (int i : chartDataModel.getSelectedcharts()) {
                            if (i == chartSettings.getId()) {
                                dataSize++;
                            }
                        }
                    }

                    bp.setMinHeight(autoMinSize.get());
                    bp.setPrefHeight(autoMinSize.get() + (dataSize * dataSizeOffset));

                    if (chartSettings.getChartType() != ChartType.TABLE) {
                        switch (chartSettings.getChartType()) {
                            case PIE:
                            case HEAT_MAP:
                            case BUBBLE:
                                if (chart != null) {
                                    bp.setCenter(chart.getRegion());
                                }
                                break;
                            case LOGICAL:
                                createLogicalCharts(bp, chartSettings);
                                break;
                            default:
                                if (chart != null) {
                                    bp.setCenter(chart.getChart());
                                }
                                break;
                        }
                    } else if (chart != null) {
                        ScrollPane scrollPane = new ScrollPane();

                        TableHeader tableHeader = new TableHeader(chartSettings.getChartType(), chart.getTableData());
                        tableHeader.maxWidthProperty().bind(bp.widthProperty());

                        scrollPane.setContent(tableHeader);
                        scrollPane.setFitToHeight(true);
                        scrollPane.setFitToWidth(true);
                        scrollPane.hbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.NEVER);
                        scrollPane.vbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                        bp.setCenter(scrollPane);
                    }

                    if (chartSettings.getChartType() != ChartType.PIE && chartSettings.getChartType() != ChartType.HEAT_MAP
                            && chartSettings.getChartType() != ChartType.LOGICAL && chart != null) {
                        TableHeader tableHeader = new TableHeader(chartSettings.getChartType(), chart.getTableData());
                        tableHeader.maxWidthProperty().bind(bp.widthProperty());
                        if (chartSettings.getChartType() != ChartType.TABLE) {
                            bp.setTop(tableHeader);
                        } else {
                            TableChart tableChart = (TableChart) chart;

                            bp.setTop(tableChart.getTopPicker());
                        }
                    } else if (chartSettings.getChartType() != ChartType.LOGICAL) {
                        bp.setTop(null);
                    }

                    bp.setBottom(null);

                    DragResizerXY.makeResizable(bp);

                    Separator sep = new Separator();
                    sep.setOrientation(Orientation.HORIZONTAL);
                    if (chartSettings.getChartType() == ChartType.PIE) {

                        if (pieFrames.isEmpty()) {
                            HBox hBox = new HBox();
                            hBox.setFillHeight(true);
                            pieFrames.add(hBox);
                        }

                        HBox hBox = null;
                        if (currentPieFrame < pieFrames.size()) {
                            hBox = pieFrames.get(currentPieFrame);
                        } else {
                            hBox = new HBox();
                            hBox.setFillHeight(true);
                            pieFrames.add(hBox);
                        }
                        hBox.getChildren().add(bp);
                        HBox.setHgrow(bp, Priority.ALWAYS);
                        noOfPie++;

                        if (noOfPie == horizontalPies || noOfPie == countOfPies) {
                            vBox.getChildren().add(pieFrames.get(currentPieFrame));
                            vBox.getChildren().add(sep);
                            currentPieFrame++;
                        }
                    } else if (chartSettings.getChartType() == ChartType.TABLE) {

                        if (tableFrames.isEmpty()) {
                            HBox hBox = new HBox();
                            hBox.setFillHeight(true);
                            tableFrames.add(hBox);
                        }

                        HBox hBox;
                        if (currentTableFrame < tableFrames.size()) {
                            hBox = tableFrames.get(currentTableFrame);
                        } else {
                            hBox = new HBox();
                            hBox.setFillHeight(true);
                            tableFrames.add(hBox);
                        }
                        hBox.getChildren().add(bp);
                        HBox.setHgrow(bp, Priority.ALWAYS);
                        noOfTable++;

                        if (noOfTable == horizontalTables || noOfTable == countOfTables) {
                            int finalCurrentTableFrame = currentTableFrame;
                            vBox.getChildren().add(tableFrames.get(finalCurrentTableFrame));
                            vBox.getChildren().add(sep);
                            currentTableFrame++;
                        }
                    } else {
                        vBox.getChildren().add(bp);
                        vBox.getChildren().add(sep);
                    }

                    JEConfig.getStatusBar().progressProgressJob(GraphPluginView.JOB_NAME, 1, "Finished Chart");
                }
            }
        });

        Platform.runLater(() -> {
            toolBarView.updateLayout();

            formatCharts();

            JEConfig.getStatusBar().finishProgressJob(GraphPluginView.JOB_NAME, "done");

            for (ChartSettings settings : dataModel.getCharts()) {
                if (settings.getChartType() == ChartType.HEAT_MAP) {
                    Platform.runLater(this::formatCharts);
                }
            }


            StringBuilder allFormulas = new StringBuilder();
            for (Chart chart : allCharts) {
                List<Chart> notActive = new ArrayList<>(allCharts);
                notActive.remove(chart);
                ChartType chartType = chart.getChartType();

                setupListener(chart, notActive, chartType);

                if (chart instanceof XYChart && dataModel.calcRegression()) {
                    allFormulas.append(((XYChart) chart).getRegressionFormula().toString());
                }
            }

            Platform.runLater(this::autoSize);

            if (dataModel.calcRegression()) {
                Alert infoBox = new Alert(Alert.AlertType.INFORMATION);
                infoBox.setResizable(true);
                TextArea textArea = new TextArea(allFormulas.toString());
                textArea.setWrapText(true);
                textArea.setPrefWidth(450);
                textArea.setPrefHeight(200);
                infoBox.getDialogPane().setContent(textArea);
                infoBox.show();
            }
        });
    }

    private Chart getChart(ChartSettings chart, List<ChartDataModel> chartDataModels) {

        if (chartDataModels == null) {
            chartDataModels = new ArrayList<>();

            for (ChartDataModel singleRow : dataModel.getSelectedData()) {
                for (int i : singleRow.getSelectedcharts()) {
                    if (i == chart.getId()) {
                        chartDataModels.add(singleRow);
                    }
                }
            }
        }

        switch (chart.getChartType()) {
            case LOGICAL:
                return new LogicalChart(dataModel, chartDataModels, chart.getId(), chart.getName());
            case LINE:
                return new LineChart(dataModel, chartDataModels, chart.getId(), chart.getName());
            case BAR:
                return new BarChart(chartDataModels, dataModel.getShowIcons(), chart.getId(), chart.getName());
            case COLUMN:
                return new ColumnChart(dataModel, chartDataModels, chart.getId(), chart.getName());
            case BUBBLE:
                return new BubbleChart(dataModel, chartDataModels, chart.getId(), chart.getName());
            case SCATTER:
                return new ScatterChart(dataModel, chartDataModels, chart.getId(), chart.getName());
            case PIE:
                return new PieChart(dataModel, chartDataModels, chart.getId(), chart.getName());
            case TABLE:
                return new TableChart(dataModel, chartDataModels, chart.getId(), chart.getName());
            case HEAT_MAP:
                return new HeatMapChart(chartDataModels, chart.getId(), chart.getName());
            case AREA:
            default:
                return new AreaChart(dataModel, chartDataModels, chart.getId(), chart.getName());
        }
    }

    private void formatCharts() {
        Platform.runLater(() -> {
            for (Chart cv : allCharts) {
                if (cv.getChartType().equals(ChartType.BUBBLE)) {
                    MultiAxisBubbleChart bubbleChart = (MultiAxisBubbleChart) cv.getRegion();
                    String yUnit = ((BubbleChart) cv).getyUnit();

                    bubbleChart.getData().forEach(numberNumberSeries -> {
                        MultiAxisBubbleChart.Series bubbleChartSeries = (MultiAxisBubbleChart.Series) numberNumberSeries;
                        bubbleChartSeries.getData().forEach(numberNumberData -> {
                            Node bubble = ((MultiAxisBubbleChart.Data) numberNumberData).getNode();
                            if (bubble instanceof StackPane) {
                                StackPane stackPane = (StackPane) bubble;
                                if (stackPane.getShape() != null && stackPane.getShape() instanceof Circle) {
                                    Circle circle = (Circle) stackPane.getShape();
                                    DoubleProperty fontSize = new SimpleDoubleProperty(20);

                                    NumberFormat nf = NumberFormat.getInstance();
                                    nf.setMinimumFractionDigits(0);
                                    nf.setMaximumFractionDigits(0);
                                    String countValue = nf.format(((MultiAxisBubbleChart.Data) numberNumberData).getExtraValue());
                                    nf.setMinimumFractionDigits(2);
                                    nf.setMaximumFractionDigits(2);
                                    String yValue = nf.format(((MultiAxisBubbleChart.Data) numberNumberData).getYValue());

                                    Label labelCount = new Label(countValue);
                                    Tooltip tooltipYValue = new Tooltip(yValue + " " + yUnit);
                                    labelCount.setAlignment(Pos.CENTER);
                                    labelCount.setBackground(
                                            new Background(
                                                    new BackgroundFill(Color.WHITE, new CornerRadii(5), new Insets(1, 1, 1, 1))
                                            )
                                    );

                                    labelCount.setMinWidth((countValue.length() + 1) * 20d);

//                                    if (circle.radiusProperty().get() / 5 > 18d) {
//                                        fontSize.bind(Bindings.divide(circle.radiusProperty(), 5));
//                                        labelCount.minWidthProperty().bind(Bindings.divide(circle.radiusProperty(), 5).add(4));
//                                    } else {
                                    fontSize.set(16d);
//                                    }

                                    labelCount.styleProperty().bind(Bindings.concat("-fx-font-size:", fontSize.asString(), ";"));
//                                    label.styleProperty().bind(Bindings.concat("-fx-font-size:", 20d, ";"));
                                    Tooltip.install(bubble, tooltipYValue);
                                    stackPane.getChildren().setAll(labelCount);
                                }
                                bubble.setOnMouseClicked(event -> bubble.toFront());
                            }
                        });
                    });
                } else if (cv.getChartType().equals(ChartType.COLUMN)) {
//                    MultiAxisBarChart columnChart = (MultiAxisBarChart) cv.getChartRegion();
//                    try {
//                        columnChart.getData().forEach(numberNumberSeries -> {
//                            MultiAxisBarChart.Series columnChartSeries = (MultiAxisBarChart.Series) numberNumberSeries;
//                            columnChartSeries.getData().forEach(data -> {
//                                final StackPane node = (StackPane) ((MultiAxisBarChart.Data) data).getNode();
//                                NumberFormat nf = NumberFormat.getInstance();
//                                nf.setMinimumFractionDigits(2);
//                                nf.setMaximumFractionDigits(2);
//                                String valueString = nf.format(((MultiAxisBarChart.Data) data).getYValue());
//                                final Text dataText = new Text(valueString + "");
//
//                                node.getChildren().add(dataText);
//
//                                Bounds bounds = node.getBoundsInParent();
//                                dataText.setLayoutX(Math.round(bounds.getMinX() + bounds.getWidth() / 2 - dataText.prefWidth(-1) / 2));
//                                dataText.setLayoutY(Math.round(bounds.getMinY() - dataText.prefHeight(-1) * 0.5));
//                            });
//                        });
//                    } catch (Exception e) {
//                        logger.error(e);
//                    }
                } else if (cv.getChartType().equals(ChartType.BAR)) {
                    javafx.scene.chart.BarChart barChart = (javafx.scene.chart.BarChart) cv.getRegion();
                    try {
                        barChart.getData().forEach(numberNumberSeries -> {
                            javafx.scene.chart.BarChart.Series barChartSeries = (javafx.scene.chart.BarChart.Series) numberNumberSeries;
                            barChartSeries.getData().forEach(data -> {
                                final StackPane node = (StackPane) ((javafx.scene.chart.BarChart.Data) data).getNode();
                                NumberFormat nf = NumberFormat.getInstance();
                                nf.setMinimumFractionDigits(2);
                                nf.setMaximumFractionDigits(2);
                                String valueString = nf.format(((javafx.scene.chart.BarChart.Data) data).getXValue());
                                final Text dataText = new Text(valueString + "");
                                dataText.setPickOnBounds(false);
                                dataText.setFont(new Font(12));

                                node.getChildren().add(dataText);

                                Bounds bounds = node.getBoundsInParent();
                                dataText.setLayoutX(Math.round(bounds.getMinX() + bounds.getWidth() + 4));
                                dataText.setLayoutY(Math.round(bounds.getMinY() - dataText.prefHeight(-1) * 0.5));
                            });
                        });
                    } catch (Exception e) {
                        logger.error(e);
                    }
                } else if (cv.getChartType().equals(ChartType.HEAT_MAP)) {
                    VBox spVer = (VBox) cv.getRegion();
                    MatrixPane<MatrixChartItem> matrixHeatMap = null;
                    for (Node node : spVer.getChildren()) {
                        if (node instanceof HBox) {
                            HBox spHor = (HBox) node;
                            matrixHeatMap = spHor.getChildren().stream().filter(node1 -> node1 instanceof MatrixPane).findFirst().map(node1 -> (MatrixPane<MatrixChartItem>) node1).orElse(matrixHeatMap);
                        }
                    }

                    if (matrixHeatMap != null) {
                        double pixelHeight = matrixHeatMap.getMatrix().getPixelHeight();
                        double pixelWidth = matrixHeatMap.getMatrix().getPixelWidth();
                        double spacerSizeFactor = matrixHeatMap.getMatrix().getSpacerSizeFactor();

                        double leftAxisWidth = 0;
                        for (Node node : spVer.getChildren()) {
                            if (node instanceof HBox) {
                                HBox spHor = (HBox) node;
                                boolean isLeftAxis = true;
                                for (Node node1 : spHor.getChildren()) {
                                    if (node1 instanceof GridPane) {
                                        GridPane leftAxis = (GridPane) node1;
                                        if (isLeftAxis) {
                                            leftAxisWidth = leftAxis.getWidth();
                                            isLeftAxis = false;
                                        }
                                        for (Node node2 : leftAxis.getChildren()) {
                                            if (node2 instanceof Label) {
                                                ((Label) node2).setPrefHeight(pixelHeight - (spacerSizeFactor * 2));
                                            }
                                        }
                                    }
                                }
                            } else if (node instanceof GridPane) {
                                GridPane bottomAxis = (GridPane) node;
                                Node found = null;
                                for (Node node11 : bottomAxis.getChildren()) {
                                    if (node11 instanceof Region) {
                                        found = node11;
                                        break;
                                    }
                                }
                                Region firstFreeSpace = (Region) found;
                                if (firstFreeSpace != null) {
                                    firstFreeSpace.setPrefWidth(leftAxisWidth + 4);
                                }

                                for (Node node1 : bottomAxis.getChildren()) {
                                    if (node1 instanceof HBox) {
                                        HBox hBox = ((HBox) node1);
                                        if (GridPane.getRowIndex(hBox) == 0) {
                                            hBox.setPrefWidth(pixelWidth - (spacerSizeFactor * 2));
                                        }
                                    }
                                }
                            }
                        }

                        tp = new Tooltip("");

                        HeatMapChart chart = (HeatMapChart) cv;

                        double width = matrixHeatMap.getMatrix().getWidth() - matrixHeatMap.getMatrix().getInsets().getLeft() - matrixHeatMap.getMatrix().getInsets().getRight();
                        double height = matrixHeatMap.getMatrix().getHeight() - matrixHeatMap.getMatrix().getInsets().getTop() - matrixHeatMap.getMatrix().getInsets().getBottom();
                        double pixelSize = Math.min((width / chart.getCOLS()), (height / chart.getROWS()));
                        double spacer = pixelSize * spacerSizeFactor;
                        double pixelWidthMinusDoubleSpacer = pixelWidth - spacer * 2;
                        double pixelHeightMinusDoubleSpacer = pixelHeight - spacer * 2;

                        double spacerPlusPixelWidthMinusDoubleSpacer = spacer + pixelWidthMinusDoubleSpacer;
                        double spacerPlusPixelHeightMinusDoubleSpacer = spacer + pixelHeightMinusDoubleSpacer;

                        MatrixPane<MatrixChartItem> finalMatrixHeatMap = matrixHeatMap;
                        matrixHeatMap.setOnMouseMoved(new EventHandler<MouseEvent>() {
                            @Override
                            public void handle(MouseEvent t) {
                                Node node = (Node) t.getSource();
                                for (int y = 0; y < chart.getROWS(); y++) {
                                    for (int x = 0; x < chart.getCOLS(); x++) {
                                        if (Helper.isInRectangle(t.getX(), t.getY(), x * pixelWidth + spacer, y * pixelHeight + spacer, x * pixelWidth + spacerPlusPixelWidthMinusDoubleSpacer, y * pixelHeight + spacerPlusPixelHeightMinusDoubleSpacer)) {
                                            Double value = null;
                                            for (Map.Entry<MatrixXY, Double> entry : chart.getMatrixData().entrySet()) {
                                                MatrixXY matrixXY = entry.getKey();
                                                if (matrixXY.getY() == y && matrixXY.getX() == x) {
                                                    value = entry.getValue();
                                                    break;
                                                }
                                            }

                                            if (value != null) {
                                                Double finalValue = value;
                                                Platform.runLater(() -> tp.setText(finalValue.toString() + " " + chart.getUnit()));
                                                Platform.runLater(() -> tp.show(node, finalMatrixHeatMap.getScene().getWindow().getX() + t.getSceneX(), finalMatrixHeatMap.getScene().getWindow().getY() + t.getSceneY()));
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    }
                }

//                cv.getChart().applyColors();
//
//                cv.getChart().checkForY2Axis();
//
//                cv.getChart().applyBounds();
//
//                cv.getTableView().sort();
            }
        });
    }

    private void autoSize(Double autoMinSize, double maxHeight, Long chartsPerScreen, VBox vBox) {
        double totalPrefHeight; /**
         * If auto size is on or if its only one chart scale the chart to maximize screen size
         */

//        if (!hasLogicalCharts()) {
        if (dataModel.getCharts().size() == 1 || dataModel.getAutoResize()) {
            /**
             * If all children take more space then the maximum available size
             * set all on min size. after this the free space will be reallocate
             */
            totalPrefHeight = calculationTotalPrefSize(vBox);

            if (chartsPerScreen != null && dataModel.getCharts().size() > 1) {
                ObservableList<Node> children = vBox.getChildren();
                int i = 0;
                for (Node node : children) {
                    if (node instanceof Separator) i++;
                }
                double height = border.getHeight() - (4.5 * i);

                for (Node node : children) {
                    if (node instanceof BorderPane) {
                        ((BorderPane) node).setPrefHeight((height) / chartsPerScreen);
                    } else if (node instanceof HBox) {
                        ((HBox) node).setPrefHeight((height) / chartsPerScreen);
                    }
                }
            } else {
                if (totalPrefHeight > maxHeight) {
                    for (Node node : vBox.getChildren()) {
                        if (node instanceof BorderPane) {
                            ((BorderPane) node).setPrefHeight(autoMinSize);
                        } else if (node instanceof HBox) {
                            ((HBox) node).setPrefHeight(autoMinSize);
                        }
                    }
                }

                /**
                 * Recalculate total prefsize
                 */
                totalPrefHeight = calculationTotalPrefSize(vBox);

                /**
                 * Reallocate free space equal to all children
                 */
                if (totalPrefHeight < maxHeight) {
                    /** size/2 because there is an separator for every chart **/
                    final double freeSpacePart = (maxHeight - totalPrefHeight) / (vBox.getChildren().size() / 2);
                    vBox.getChildren().forEach(node -> {
                        if (node instanceof Pane) {
                            ((Pane) node).setPrefHeight(((Pane) node).getPrefHeight() + freeSpacePart);
                        }
                    });
                }
            }
        }
//        }

        Platform.runLater(vBox::toFront);
    }

    @Override
    public void openObject(Object object) {
        try {
            firstStart = false;
            if (object instanceof AnalysisRequest) {

                /**
                 * clear old model
                 */
                dataModel.setCharts(new ArrayList<>());
                dataModel.setData(new HashSet<>());

                AnalysisRequest analysisRequest = (AnalysisRequest) object;
                JEVisObject jeVisObject = analysisRequest.getObject();
                if (jeVisObject.getJEVisClassName().equals("Analysis")) {

                    dataModel.setCurrentAnalysis(analysisRequest.getObject());
                    dataModel.setCharts(new ArrayList<>());
                    dataModel.updateSelectedData();

                    dataModel.setManipulationMode(analysisRequest.getManipulationMode());
                    dataModel.setAggregationPeriod(analysisRequest.getAggregationPeriod());
                    AnalysisTimeFrame analysisTimeFrame = new AnalysisTimeFrame(TimeFrame.CUSTOM);
                    analysisTimeFrame.setStart(analysisRequest.getStartDate());
                    analysisTimeFrame.setEnd(analysisRequest.getEndDate());

                    dataModel.isGlobalAnalysisTimeFrame(true);

                    dataModel.setAnalysisTimeFrameForAllModels(analysisTimeFrame);


                } else if (jeVisObject.getJEVisClassName().equals("Data") || jeVisObject.getJEVisClassName().equals("Clean Data")) {

                    ChartDataModel chartDataModel = new ChartDataModel(ds);

                    try {
                        if (jeVisObject.getJEVisClassName().equals("Data")) {
                            chartDataModel.setObject(jeVisObject);
                        } else if (jeVisObject.getJEVisClassName().equals("Clean Data")) {
                            chartDataModel.setDataProcessor(jeVisObject);
                            chartDataModel.setObject(jeVisObject.getParents().get(0));
                        }
                    } catch (JEVisException e) {

                    }

                    List<Integer> list = new ArrayList<>();
                    list.add(0);
                    chartDataModel.setSelectedCharts(list);
                    chartDataModel.setColor(ColorHelper.toRGBCode(Color.BLUE));
                    chartDataModel.setSomethingChanged(true);

                    Set<ChartDataModel> chartDataModels = new HashSet<>();
                    chartDataModels.add(chartDataModel);

                    ChartSettings chartSettings = new ChartSettings(chartDataModel.getObject().getName());
                    chartSettings.setId(0);
                    chartSettings.setChartType(ChartType.AREA);
                    AnalysisTimeFrame analysisTimeFrame = new AnalysisTimeFrame(TimeFrame.PREVIEW);
                    analysisTimeFrame.setStart(analysisRequest.getStartDate());
                    analysisTimeFrame.setEnd(analysisRequest.getEndDate());
                    chartSettings.setAnalysisTimeFrame(analysisTimeFrame);
                    List<ChartSettings> chartSettingsList = new ArrayList<>();
                    chartSettingsList.add(chartSettings);

                    org.jevis.commons.ws.json.JsonObject newJsonObject = new JsonObject();
                    newJsonObject.setName("Temp");
                    newJsonObject.setId(0L);
                    newJsonObject.setJevisClass("Analysis");
                    JEVisObject newObject = new JEVisObjectWS((JEVisDataSourceWS) ds, newJsonObject) {
                        @Override
                        public String toString() {
                            return I18n.getInstance().getString("plugin.graph.analysis.tempanalysis");
                        }
                    };
                    dataModel.setTemporary(true);
                    dataModel.setCurrentAnalysisNOEVENT(newObject);
                    dataModel.setCharts(chartSettingsList);
                    dataModel.setData(chartDataModels);
                    dataModel.setAggregationPeriod(analysisRequest.getAggregationPeriod());
                    dataModel.setManipulationMode(analysisRequest.getManipulationMode());
                    dataModel.isGlobalAnalysisTimeFrame(true);
                    dataModel.setGlobalAnalysisTimeFrameNOEVENT(analysisTimeFrame);
                    toolBarView.getPickerCombo().updateCellFactory();
                    dataModel.update();

                }

                toolBarView.setChanged(true);
                toolBarView.setDisableToolBarIcons(false);
            }


        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    @Override
    public int getPrefTapPos() {
        return 2;
    }

    private void setupListener(Chart cv, List<Chart> notActive, ChartType chartType) {
        if (cv.getChart() != null) {
            switch (chartType) {
                /**
                 * Area, Line and Scatter use same listeners -> no break
                 */
                case AREA:
                case LINE:
                case SCATTER:

                    setupNoteDialog(cv);

                    setupMouseMoved(cv, notActive);

                    setupLinkedZoom(cv, notActive);

                    setupLabelRenderer(cv);

                    break;
                case LOGICAL:
                    setupMouseMoved(cv, notActive);
                    break;
                case TABLE:
                    TableChart chart = (TableChart) cv;
                    TableTopDatePicker tableTopDatePicker = chart.getTableTopDatePicker();
                    ComboBox<DateTime> datePicker = tableTopDatePicker.getDatePicker();
                    ChartDataModel singleRow = chart.getSingleRow();
                    datePicker.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                        if (datePicker.getSelectionModel().selectedIndexProperty().get() < singleRow.getSamples().size()
                                && datePicker.getSelectionModel().selectedIndexProperty().get() > -1 && !chart.isBlockDatePickerEvent()) {

                            cv.updateTable(null, newValue);

                            notActive.forEach(na -> {
                                if (!na.getChartType().equals(ChartType.PIE)
                                        && !na.getChartType().equals(ChartType.BAR)
                                        && !na.getChartType().equals(ChartType.BUBBLE)
                                        && !na.getChartType().equals(ChartType.COLUMN)) {
                                    na.updateTable(null, newValue);
                                }
                            });
                        }
                    });

                    tableTopDatePicker.getLeftImage().setOnMouseClicked(event -> {
                        int i = datePicker.getSelectionModel().getSelectedIndex() - 1;
                        if (i > -1) {
                            Platform.runLater(() -> datePicker.getSelectionModel().select(i));
                        }
                    });

                    tableTopDatePicker.getRightImage().setOnMouseClicked(event -> {
                        int i = datePicker.getSelectionModel().getSelectedIndex() - 1;
                        if (i < singleRow.getSamples().size()) {
                            Platform.runLater(() -> datePicker.getSelectionModel().select(i));
                        }
                    });
                    break;

                case BAR:
                    break;
                case BUBBLE:
                    setupMouseMovedBubble(cv);
                    break;
                case PIE:
                    break;
                case HEAT_MAP:
                    break;
                case COLUMN:
                    setupMouseMoved(cv, notActive);
                    break;
                default:
                    break;
            }
        }
    }

    private void setupLabelRenderer(Chart cv) {

        LabelledMarkerRenderer labelledMarkerRenderer = new LabelledMarkerRenderer();
        XYChart xyChart = (XYChart) cv;
        if (xyChart.getShowIcons()) {
            for (XYChartSerie xyChartSerie : xyChart.getXyChartSerieList()) {
                labelledMarkerRenderer.getDatasets().addAll(xyChartSerie.getNoteDataSet());
            }

            cv.getChart().getRenderers().add(labelledMarkerRenderer);
        }
    }

    private void setupMouseMovedBubble(Chart cv) {
        MultiAxisBubbleChart bubbleChart = (MultiAxisBubbleChart) cv.getRegion();
        bubbleChart.setOnMouseMoved(event -> {
            cv.updateTable(event, null);
        });
    }

    private void setupNoteDialog(Chart cv) {
        XYChart xyChart = (XYChart) cv;
        cv.getChart().getPlugins().add(new DataPointNoteDialog(xyChart.getXyChartSerieList(), this));
    }

    private void setupMouseMoved(Chart cv, List<Chart> notActive) {

        DataPointTableViewPointer dataPointTableViewPointer = new DataPointTableViewPointer(cv, notActive);
        cv.getChart().getPlugins().add(dataPointTableViewPointer);
    }

    private void setupLinkedZoom(Chart ac, List<Chart> notActive) {

        MultiChartZoomer multiChartZoomer = new MultiChartZoomer(AxisMode.X, notActive, ac);
        ac.getChart().getPlugins().add(multiChartZoomer);
    }

    private double calculationTotalPrefSize(Pane pane) {
        double totalPrefHight = 0;
        for (Node node : pane.getChildren()) {
            if (node instanceof Separator) {
                /** Separator has no preSize so tested a working one, the real size can only be taken after rendering **/
                totalPrefHight += 4.5;
            } else if (node instanceof Region) {
                totalPrefHight += ((Region) node).getPrefHeight();
            }
        }
        return totalPrefHight;
    }

    private void createLogicalCharts(BorderPane bp, ChartSettings chartSettings) {
        List<LogicalChart> subCharts = new ArrayList<>();
        VBox vbox = new VBox();

        for (ChartDataModel singleRow : dataModel.getSelectedData()) {
            for (int i : singleRow.getSelectedcharts()) {
                if (i == chartSettings.getId()) {
                    Chart subView = getChart(chartSettings, Collections.singletonList(singleRow));
                    LogicalChart logicalChart = (LogicalChart) subView;
                    subCharts.add(logicalChart);
                }
            }
        }
        AlphanumComparator ac = new AlphanumComparator();

        subCharts.sort((o1, o2) -> ac.compare(o1.getChartDataModels().get(0).getTitle(), o2.getChartDataModels().get(0).getTitle()));
        subCharts.forEach(subChart -> vbox.getChildren().add(subChart.getChart()));

        ObservableList<TableEntry> allEntries = FXCollections.observableArrayList();
        Double minValue = Double.MAX_VALUE;
        Double maxValue = -Double.MAX_VALUE;

        for (LogicalChart logicalChart : subCharts) {
            if (subCharts.indexOf(logicalChart) > 0) {
                logicalChart.getChart().setTitle(null);
            }
            allEntries.addAll(logicalChart.getTableData());

            logicalChart.getChartDataModels().get(0).setColor(ColorHelper.toRGBCode(ColorColumn.color_list[subCharts.indexOf(logicalChart)]));
            logicalChart.getChartDataModels().get(0).calcMinAndMax();
            double min = logicalChart.getMinValue();
            double max = logicalChart.getMaxValue();
            minValue = Math.min(minValue, min);
            maxValue = Math.max(maxValue, max);
        }

        allEntries.sort((o1, o2) -> ac.compare(o1.getName(), o2.getName()));
        TableHeader tableHeader = new TableHeader(ChartType.LOGICAL, allEntries);
        bp.setTop(tableHeader);

        if (!minValue.equals(Double.MAX_VALUE) && !maxValue.equals(-Double.MAX_VALUE)) {
            for (LogicalChart logicalChart : subCharts) {
                for (Axis axis : logicalChart.getChart().getAxes()) {
                    if (axis.getSide().isVertical()) {
                        axis.setAutoRanging(false);
                    } else {
                        axis.set(minValue, maxValue);
                    }
                }
            }
        }

        bp.setCenter(vbox);
        allCharts.addAll(subCharts);
    }

    public VBox getvBox() {
        return vBox;
    }

    public Double getxAxisLowerBound() {
        return xAxisLowerBound;
    }

    public Double getxAxisUpperBound() {
        return xAxisUpperBound;
    }

    public boolean isZoomed() {
        return zoomed;
    }
}
