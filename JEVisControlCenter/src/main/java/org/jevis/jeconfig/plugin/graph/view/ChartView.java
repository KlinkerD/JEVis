/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.jeconfig.plugin.graph.view;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gillius.jfxutils.chart.ChartPanManager;
import org.gillius.jfxutils.chart.JFXChartUtil;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisObject;
import org.jevis.api.JEVisSample;
import org.jevis.api.JEVisUnit;
import org.jevis.application.dialog.NoteDialog;
import org.jevis.application.jevistree.AlphanumComparator;
import org.jevis.application.jevistree.plugin.ChartDataModel;
import org.jevis.application.jevistree.plugin.ChartSettings;
import org.jevis.application.jevistree.plugin.TableEntry;
import org.jevis.commons.unit.JEVisUnitImp;
import org.jevis.commons.unit.UnitManager;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.plugin.graph.data.GraphDataModel;
import org.jevis.jeconfig.tool.I18n;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import java.text.NumberFormat;
import java.util.*;

import static javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY;

/**
 * @author broder
 */
public class ChartView implements Observer {

    private final GraphDataModel dataModel;

    private AreaChart<Number, Number> areaChart;
    private LineChart<Number, Number> lineChart;
    private BarChart<Number, Number> barChart;
    private BubbleChart<Number, Number> bubbleChart;
    private ScatterChart<Number, Number> scatterChart;
    private PieChart pieChart;

    private Region areaChartRegion;
    private final TableView table;
    private final ObservableList<TableEntry> tableData = FXCollections.observableArrayList();
    private final Logger logger = LogManager.getLogger(ChartView.class);
    private ObservableList<String> chartsList = FXCollections.observableArrayList();

    private Number valueForDisplay;

    private TableColumn<TableEntry, Color> buildColorColumn(String columnName) {
        TableColumn<TableEntry, Color> column = new TableColumn(columnName);
        column.setPrefWidth(100);
        column.setMaxWidth(100);
        column.setMinWidth(100);

        column.setCellValueFactory(param -> {
            return new SimpleObjectProperty<>(param.getValue().getColor());
        });
        column.setCellFactory(new Callback<TableColumn<TableEntry, Color>, TableCell<TableEntry, Color>>() {
            @Override
            public TableCell<TableEntry, Color> call(TableColumn<TableEntry, Color> param) {
                TableCell<TableEntry, Color> cell = new TableCell<TableEntry, Color>() {
                    @Override
                    public void commitEdit(Color newValue) {
                        super.commitEdit(newValue);
                    }

                    @Override
                    protected void updateItem(Color item, boolean empty) {
                        super.updateItem(item, empty); //To change body of generated methods, choose Tools | Templates.
                        if (!empty && item != null) {
                            StackPane hbox = new StackPane();
//                            if (getChartType().equals(ChartSettings.ChartType.AREA)) {
//                                hbox.setBackground(
//                                        new Background(
//                                                new BackgroundFill(item.deriveColor(1, 1, 50, 0.3), CornerRadii.EMPTY, Insets.EMPTY)));
//                            } else {
                            hbox.setBackground(
                                    new Background(
                                            new BackgroundFill(item, CornerRadii.EMPTY, Insets.EMPTY)));
//                            }

                            setText(null);
                            setGraphic(hbox);
                        } else {
                            setText(null);
                            setGraphic(null);
                        }

                    }

                };
                return cell;
            }
        });

        return column;

    }

    public XYChart getAreaChart() {
        return areaChart;
    }

    public void drawDefaultAreaChart() {
        ObservableList<XYChart.Series<Number, Number>> series = FXCollections.observableArrayList();

        ObservableList<XYChart.Data<Number, Number>> series1Data = FXCollections.observableArrayList();
        XYChart.Data<Number, Number> data1 = new XYChart.Data<Number, Number>(new GregorianCalendar(2012, 11, 15).getTime().getTime(), 2);
        Rectangle rect = new Rectangle(0, 0);
        rect.setVisible(false);
        data1.setNode(rect);
        series1Data.add(data1);
        series1Data.add(new XYChart.Data<Number, Number>(new GregorianCalendar(2014, 5, 3).getTime().getTime(), 4));

        ObservableList<XYChart.Data<Number, Number>> series2Data = FXCollections.observableArrayList();
        series2Data.add(new XYChart.Data<Number, Number>(new GregorianCalendar(2014, 0, 13).getTime().getTime(), 8));
        series2Data.add(new XYChart.Data<Number, Number>(new GregorianCalendar(2014, 7, 27).getTime().getTime(), 4));

        series.add(new XYChart.Series<>("Series1", series1Data));
        series.add(new XYChart.Series<>("Series2", series2Data));

        NumberAxis numberAxis = new NumberAxis();
//        DateAxis dateAxis = new DateAxis();
        Axis dateAxis = new DateValueAxis();
        areaChart = new AreaChart<>(dateAxis, numberAxis, series);
        areaChart.setTitle("default");
    }

    @Override
    public void update(Observable o, Object arg) {
//        try {
//            getChartsList();
//            if (chartsList.size() == 1) this.drawAreaChart("");
//            else if (chartsList.size() > 1) getChartViews();
//
//        } catch (JEVisException ex) {
//            Logger.getLogger(ChartView.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }


    public ChartView(GraphDataModel dataModel) {
        this.dataModel = dataModel;
        dataModel.addObserver(this);

        table = new TableView();

        table.sortPolicyProperty().set((Callback<TableView<TableEntry>, Boolean>) param -> {
            AlphanumComparator ac = new AlphanumComparator();
            Comparator<TableEntry> comparator = (t1, t2) -> ac.compare(t1.getName(), t2.getName());
            FXCollections.sort(table.getItems(), comparator);
            return true;
        });

        table.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
//        table.setFixedCellSize(25);
//        table.prefHeightProperty().bind(Bindings.size(table.getItems()).multiply(table.getFixedCellSize()).add(30));
        TableColumn name = new TableColumn(I18n.getInstance().getString("plugin.graph.table.name"));
        name.setCellValueFactory(new PropertyValueFactory<TableEntry, String>("name"));
        name.setSortable(false);

//        TableColumn colorCol = new TableColumn("Color333");
//        colorCol.setCellValueFactory(new PropertyValueFactory<TableEntry, String>("color"));
        TableColumn colorCol = buildColorColumn(I18n.getInstance().getString("plugin.graph.table.color"));
        colorCol.setSortable(false);

        TableColumn value = new TableColumn(I18n.getInstance().getString("plugin.graph.table.value"));
        value.setCellValueFactory(new PropertyValueFactory<TableEntry, String>("value"));
        value.setStyle("-fx-alignment: CENTER-RIGHT");
        value.setSortable(false);

        TableColumn dateCol = new TableColumn(I18n.getInstance().getString("plugin.graph.table.date"));
        dateCol.setCellValueFactory(new PropertyValueFactory<TableEntry, String>("date"));
        dateCol.setStyle("-fx-alignment: CENTER");
        dateCol.setSortable(false);

        TableColumn note = new TableColumn(I18n.getInstance().getString("plugin.graph.table.note"));
        note.setCellValueFactory(new PropertyValueFactory<TableEntry, String>("note"));
        note.setStyle("-fx-alignment: CENTER");
        note.setPrefWidth(15);
        note.setSortable(false);

        TableColumn minCol = new TableColumn(I18n.getInstance().getString("plugin.graph.table.min"));
        minCol.setCellValueFactory(new PropertyValueFactory<TableEntry, String>("min"));
        minCol.setStyle("-fx-alignment: CENTER-RIGHT");
        minCol.setSortable(false);

        TableColumn maxCol = new TableColumn(I18n.getInstance().getString("plugin.graph.table.max"));
        maxCol.setCellValueFactory(new PropertyValueFactory<TableEntry, String>("max"));
        maxCol.setStyle("-fx-alignment: CENTER-RIGHT");
        maxCol.setSortable(false);

        TableColumn avgCol = new TableColumn(I18n.getInstance().getString("plugin.graph.table.avg"));
        avgCol.setCellValueFactory(new PropertyValueFactory<TableEntry, String>("avg"));
        avgCol.setStyle("-fx-alignment: CENTER-RIGHT");
        avgCol.setSortable(false);

        TableColumn sumCol = new TableColumn(I18n.getInstance().getString("plugin.graph.table.sum"));
        sumCol.setCellValueFactory(new PropertyValueFactory<TableEntry, String>("sum"));
        sumCol.setStyle("-fx-alignment: CENTER-RIGHT");
        sumCol.setSortable(false);

        final ObservableList<TableEntry> tableData = FXCollections.observableArrayList();
        TableEntry tableEntry = new TableEntry("empty");
        tableData.add(tableEntry);
        table.setItems(tableData);

        table.getColumns().addAll(name, colorCol, value, dateCol, note, minCol, maxCol, avgCol, sumCol);
    }

    private void disableTable() {
        table.setVisible(false);
        table.setItems(tableData);
        table.setFixedCellSize(25);
        table.prefHeightProperty().unbind();
        table.setPrefHeight(0);
    }

    private void setTableStandard() {
        table.setVisible(true);
        table.setItems(tableData);
        table.setFixedCellSize(25);
        table.prefHeightProperty().bind(Bindings.size(table.getItems()).multiply(table.getFixedCellSize()).add(30));
    }

    private ChartSettings.ChartType chartType = ChartSettings.ChartType.AREA;
    private String chartName = "";

    private void showNote(MouseEvent mouseEvent, Set<ChartDataModel> selectedData, String chartName, ChartSettings.ChartType chartType) {
        Point2D mouseCoordinates = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());
        Double x = null;
        switch (chartType.toString()) {
            case ("AREA"):
                x = areaChart.getXAxis().sceneToLocal(mouseCoordinates).getX();
                break;
            case ("LINE"):
                x = lineChart.getXAxis().sceneToLocal(mouseCoordinates).getX();
                break;
            case ("BAR"):
                x = barChart.getXAxis().sceneToLocal(mouseCoordinates).getX();
                break;
            case ("BUBBLE"):
                x = bubbleChart.getXAxis().sceneToLocal(mouseCoordinates).getX();
                break;
            case ("SCATTER"):
                x = scatterChart.getXAxis().sceneToLocal(mouseCoordinates).getX();
                break;
            case ("PIE"):
                //valueForDisplay = pieChart.getXAxis().sceneToLocal(mouseCoordinates).getValueForDisplay();
                break;
            default:
                x = areaChart.getXAxis().sceneToLocal(mouseCoordinates).getX();
                break;
        }
        if (x != null) {
            Map<String, String> map = new HashMap<>();
            Number valueForDisplay = null;
            switch (chartType.toString()) {
                case ("AREA"):
                    valueForDisplay = areaChart.getXAxis().getValueForDisplay(x);
                    break;
                case ("LINE"):
                    valueForDisplay = lineChart.getXAxis().getValueForDisplay(x);
                    break;
                case ("BAR"):
                    valueForDisplay = barChart.getXAxis().getValueForDisplay(x);
                    break;
                case ("BUBBLE"):
                    valueForDisplay = bubbleChart.getXAxis().getValueForDisplay(x);
                    break;
                case ("SCATTER"):
                    valueForDisplay = scatterChart.getXAxis().getValueForDisplay(x);
                    break;
                case ("PIE"):
                    //valueForDisplay = pieChart.getXAxis().getValueForDisplay(valueForDisplay);
                    break;
                default:
                    valueForDisplay = areaChart.getXAxis().getValueForDisplay(x);
                    break;
            }
            for (ChartDataModel singleRow : selectedData) {
                if (Objects.isNull(chartName) || chartName.equals("") || singleRow.get_selectedCharts().contains(chartName)) {
                    try {
                        Double higherKey = singleRow.getSampleMap().higherKey(valueForDisplay.doubleValue());
                        Double lowerKey = singleRow.getSampleMap().lowerKey(valueForDisplay.doubleValue());

                        Double nearest = higherKey;
                        if (nearest == null) nearest = lowerKey;

                        if (lowerKey != null && higherKey != null) {
                            Double lower = Math.abs(lowerKey - valueForDisplay.doubleValue());
                            Double higher = Math.abs(higherKey - valueForDisplay.doubleValue());
                            if (lower < higher) {
                                nearest = lowerKey;
                            }

                            String note = singleRow.getSampleMap().get(nearest).getNote();

                            String title = "";
                            title += singleRow.getObject().getName();
//                            if (singleRow.getDataProcessor() != null)
//                                title += " (" + singleRow.getDataProcessor().getName() + ")";

                            map.put(title, note);
                        }
                    } catch (Exception ex) {
                        logger.error("Error: could not get note", ex);
                    }
                }
            }

            NoteDialog nd = new NoteDialog(map);

            nd.showAndWait().ifPresent(response -> {
                if (response.getButtonData().getTypeCode() == ButtonType.OK.getButtonData().getTypeCode()) {

                } else if (response.getButtonData().getTypeCode() == ButtonType.CANCEL.getButtonData().getTypeCode()) {

                }
            });
        }
    }

    private Node formatNote(String note, Color color) {
        Node output = null;
        if (note != null) {
            HBox hbox = new HBox();
            hbox.setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
            double iconSize = 12;
            Boolean changed = false;

            if (note.contains("limit(Step1)")) {
                hbox.getChildren().add(JEConfig.getImage("Warning-icon.png", iconSize, iconSize));
                changed = true;
            }
            if (note.contains("gap(") || note.contains("limit(Default)") || note.contains("limit(Static)") || note.contains("limit(Average)")
                    || note.contains("limit(Median)") || note.contains("limit(Interpolation)") || note.contains("limit(Min)") || note.contains("limit(Max)")) {
                hbox.getChildren().add(JEConfig.getImage("rodentia-icons_dialog-warning.png", iconSize, iconSize));
                changed = true;
            }

            if (hbox.getChildren().size() == 2) {
                hbox.getChildren().clear();
                hbox.getChildren().add(JEConfig.getImage("rodentia-icons_process-stop.png", iconSize, iconSize));
                changed = true;
            }

            if (changed) output = hbox;
        }
        return output;
    }

    public Region getAreaChartRegion() {
        return areaChartRegion;
    }

    public TableView getLegend() {
        return table;
    }

    public String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private class QuantityUnits {
        Unit _kg = SI.KILOGRAM;
        final JEVisUnit kg = new JEVisUnitImp(_kg);
        Unit _t = NonSI.METRIC_TON;
        final JEVisUnit t = new JEVisUnitImp(_t);
        Unit _l = NonSI.LITER;
        final JEVisUnit l = new JEVisUnitImp(_l);
        Unit _m3 = SI.CUBIC_METRE;
        final JEVisUnit m3 = new JEVisUnitImp(_m3);
        Unit _Wh = SI.WATT.times(NonSI.HOUR);
        final JEVisUnit Wh = new JEVisUnitImp(_Wh);
        Unit _kWh = SI.KILO(SI.WATT).times(NonSI.HOUR);
        final JEVisUnit kWh = new JEVisUnitImp(_kWh);
        Unit _MWh = SI.MEGA(SI.WATT).times(NonSI.HOUR);
        final JEVisUnit MWh = new JEVisUnitImp(_MWh);
        Unit _GWh = SI.GIGA(SI.WATT).times(NonSI.HOUR);
        final JEVisUnit GWh = new JEVisUnitImp(_GWh);

        public List<JEVisUnit> get() {
            return new ArrayList<>(Arrays.asList(kg, t, l, m3, Wh, kWh, MWh, GWh));
        }
    }

    public void drawAreaChart(String chartName, ChartSettings.ChartType chartType) {
        tableData.clear();
        areaChart = null;
        lineChart = null;
        barChart = null;
        bubbleChart = null;
        scatterChart = null;
        pieChart = null;

        Set<ChartDataModel> selectedData = dataModel.getSelectedData();

        ObservableList series = null;
        String cType = chartType.toString();
        setChartType(chartType);
        if (cType.equals("AREA") || cType.equals("LINE") || cType.equals("BUBBLE")
                || cType.equals("SCATTER")) {
            ObservableList<XYChart.Series<Number, Number>> ol = FXCollections.observableArrayList();
            series = ol;
        } else if (cType.equals("BAR")) {
            ObservableList<BarChart.Series<String, Number>> ol = FXCollections.observableArrayList();
            series = ol;
        } else if (cType.equals("PIE")) {
            ObservableList<PieChart.Data> ol = FXCollections.observableArrayList();
            series = ol;
        }
        List<Color> hexColors = new ArrayList<>();

        String unit = null;
        String title = null;
        List<Double> listSumsPiePieces = new ArrayList<>();
        List<String> listTableEntryNames = new ArrayList<>();

        for (ChartDataModel singleRow : selectedData) {
            if (Objects.isNull(chartName) || chartName.equals("") || singleRow.get_selectedCharts().contains(chartName)) {
                setChartName(chartName);
                unit = UnitManager.getInstance().formate(singleRow.getUnit());
                if (unit.equals("")) unit = I18n.getInstance().getString("plugin.graph.chart.valueaxis.nounit");

                title = I18n.getInstance().getString("plugin.graph.chart.title1");

                hexColors.add(singleRow.getColor());

                if (chartName == "" || chartName == null) {
                    if (singleRow.get_selectedCharts().size() == 1) title = singleRow.get_selectedCharts().get(0);
                } else title = chartName;

                List<JEVisSample> samples = singleRow.getSamples();
                ObservableList series1Data = null;
                TreeMap sampleMap = new TreeMap();

                if (cType.equals("AREA") || cType.equals("LINE") || cType.equals("BUBBLE")
                        || cType.equals("SCATTER")) {
                    ObservableList<XYChart.Data<Number, Number>> tl = FXCollections.observableArrayList();
                    series1Data = tl;
                    sampleMap = new TreeMap<Double, JEVisSample>();
                } else if (cType.equals("BAR")) {
                    ObservableList<BarChart.Data<String, Number>> tl = FXCollections.observableArrayList();
                    series1Data = tl;
                    sampleMap = new TreeMap<Double, JEVisSample>();
                } else if (cType.equals("PIE")) {
                    ObservableList<PieChart.Data> tl = FXCollections.observableArrayList();
                    series1Data = tl;
                }


                String tableEntryName = singleRow.getObject().getName();
                TableEntry tableEntry = new TableEntry(tableEntryName);
                tableEntry.setColor(singleRow.getColor());

                singleRow.setTableEntry(tableEntry);
                tableData.add(tableEntry);
                Boolean isQuantitiy = false;
                Double min = Double.MAX_VALUE;
                Double max = Double.MIN_VALUE;
                Double avg;
                Double sum = 0.0;

                JEVisObject dp = singleRow.getDataProcessor();
                if (Objects.nonNull(dp)) {
                    try {
                        if (Objects.nonNull(dp.getAttribute("Value is a Quantity"))) {
                            if (Objects.nonNull(dp.getAttribute("Value is a Quantity").getLatestSample())) {
                                if (dp.getAttribute("Value is a Quantity").getLatestSample().getValueAsBoolean()) {

                                    isQuantitiy = true;
                                }
                            }
                        }
                    } catch (JEVisException e) {
                        logger.error("Error: could not data processor attribute", e);
                    }
                }

                QuantityUnits qu = new QuantityUnits();
                if (qu.get().contains(singleRow.getUnit())) isQuantitiy = true;

                if (!cType.equals("PIE")) {
                    for (JEVisSample sample : samples) {
                        try {
                            DateTime dateTime = sample.getTimestamp();
                            Double value = sample.getValueAsDouble();
                            Long timestamp = dateTime.getMillis();

                            if (cType.equals("AREA") || cType.equals("LINE") || cType.equals("BUBBLE")
                                    || cType.equals("SCATTER") || cType.equals("BAR")) {
                                sampleMap.put((double) sample.getTimestamp().getMillis(), sample);
                            } else {
                            }

                            if (isQuantitiy) {
                                min = Math.min(value, min);
                                max = Math.max(value, max);
                                sum += value;
                            }

                            if (cType.equals("AREA") || cType.equals("LINE") || cType.equals("BUBBLE")
                                    || cType.equals("SCATTER")) {
                                XYChart.Data<Number, Number> data = new XYChart.Data<Number, Number>(timestamp, value);

                                Node note = formatNote(sample.getNote(), singleRow.getColor());
                                if (note != null && getDataModel().getHideShowIcons()) {
                                    note.setVisible(true);
                                    data.setNode(note);
                                } else {
                                    Rectangle rect = new Rectangle(0, 0);
                                    rect.setFill(singleRow.getColor());
                                    rect.setVisible(false);
                                    data.setNode(rect);
                                }

                                series1Data.add(data);
                            } else if (cType.equals("BAR")) {
                                DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
                                String s = dateTime.toString(dtf);
                                BarChart.Data<String, Number> data = new BarChart.Data<>(s, value);
                                series1Data.add(data);
                            }
                        } catch (JEVisException e) {

                        }
                    }
                }

                if (isQuantitiy) {
                    avg = sum / samples.size();
                    NumberFormat nf_out = NumberFormat.getNumberInstance();
                    nf_out.setMaximumFractionDigits(2);
                    nf_out.setMinimumFractionDigits(2);

                    tableEntry.setMin(nf_out.format(min) + " " + unit);
                    tableEntry.setMax(nf_out.format(max) + " " + unit);
                    tableEntry.setAvg(nf_out.format(avg) + " " + unit);
                    tableEntry.setSum(nf_out.format(sum) + " " + unit);
                }

                XYChart.Series currentSerie = null;
                if (cType.equals("AREA") || cType.equals("LINE") || cType.equals("BUBBLE")
                        || cType.equals("SCATTER")) {
                    currentSerie = new XYChart.Series<>(tableEntryName, series1Data);
                    singleRow.setSampleMap(sampleMap);
                    series.add(currentSerie);
                } else if (cType.equals("BAR")) {
                    currentSerie = new BarChart.Series<>(tableEntryName, series1Data);
                    singleRow.setSampleMap(sampleMap);
                    series.add(currentSerie);
                } else {
                    Double sumPiePiece = 0d;
                    for (JEVisSample sample : samples) {
                        try {
                            sumPiePiece += sample.getValueAsDouble();
                        } catch (JEVisException e) {

                        }
                    }
                    listSumsPiePieces.add(sumPiePiece);
                    listTableEntryNames.add(tableEntryName);
                }
            }
        }

        table.sort();

        if (cType.equals("PIE")) {
            Double whole = 0d;
            List<Double> listPercentages = new ArrayList<>();
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            for (Double d : listSumsPiePieces) whole += d;
            for (Double d : listSumsPiePieces) listPercentages.add(d / whole);
            for (String name : listTableEntryNames) {
                String seriesName = name + " - " + nf.format(listSumsPiePieces.get(listTableEntryNames.indexOf(name)))
                        + " " + unit + " (" + nf.format(listPercentages.get(listTableEntryNames.indexOf(name)) * 100) + " %)";

                PieChart.Data data = new PieChart.Data(seriesName, listSumsPiePieces.get(listTableEntryNames.indexOf(name)));
                series.add(data);

            }
        }

        NumberAxis numberAxis = new NumberAxis();
        Axis dateAxis = new DateValueAxis();

        switch (chartType.toString()) {
            case ("AREA"):
                areaChart = new AreaChart<>(dateAxis, numberAxis, series);
                areaChart.applyCss();
                setTableStandard();
                break;
            case ("LINE"):
                lineChart = new LineChart<>(dateAxis, numberAxis, series);
                lineChart.applyCss();
                setTableStandard();
                break;
            case ("BAR"):
                CategoryAxis catAxis = new CategoryAxis();
                barChart = new BarChart<>(catAxis, numberAxis, series);
                barChart.applyCss();
                setTableStandard();
                break;
            case ("BUBBLE"):
                bubbleChart = new BubbleChart<>(dateAxis, numberAxis, series);
                bubbleChart.applyCss();
                setTableStandard();
                break;
            case ("SCATTER"):
                scatterChart = new ScatterChart<>(dateAxis, numberAxis, series);
                scatterChart.applyCss();
                setTableStandard();
                break;
            case ("PIE"):
                pieChart = new PieChart(series);
                pieChart.applyCss();
                disableTable();
                break;
            default:
                areaChart = new AreaChart<>(dateAxis, numberAxis, series);
                areaChart.applyCss();
                setTableStandard();
                break;
        }

        for (int i = 0; i < hexColors.size(); i++) {
            Color currentColor = hexColors.get(i);
            Color brighter = currentColor.deriveColor(1, 1, 50, 0.3);
            String hexColor = toRGBCode(currentColor);
            String hexBrighter = toRGBCode(brighter) + "55";
            String preIdent = ".default-color" + i;
            Node node = null;
            Node nodew = null;
            switch (chartType.toString()) {
                case ("AREA"):
                    node = areaChart.lookup(preIdent + ".chart-series-area-fill");
                    nodew = areaChart.lookup(preIdent + ".chart-series-area-line");
                    node.setStyle("-fx-fill: linear-gradient(" + hexColor + "," + hexBrighter + ");"
                            + "  -fx-background-insets: 0 0 -1 0, 0, 1, 2;"
                            + "  -fx-background-radius: 3px, 3px, 2px, 1px;");
                    nodew.setStyle("-fx-stroke: " + hexColor + "; -fx-stroke-width: 2px; ");
                    break;
                case ("LINE"):
                    node = lineChart.lookup(preIdent + ".chart-series-line");
                    node.setStyle("-fx-stroke: " + hexColor + "; -fx-stroke-width: 2px; ");
                    break;
                case ("BAR"):
                    node = barChart.lookup(preIdent + ".chart-bar");
                    node.setStyle("-fx-bar-fill: " + hexColor + ";");
                    break;
                case ("BUBBLE"):
                    node = bubbleChart.lookup(preIdent + ".chart-series-area-fill");
                    nodew = bubbleChart.lookup(preIdent + ".chart-series-area-line");
                    break;
                case ("SCATTER"):
                    node = scatterChart.lookup(preIdent + ".chart-symbol");
                    node.setStyle("-fx-background-color: " + hexColor + ";");
                    break;
                case ("PIE"):
                    node = pieChart.lookup(preIdent + ".chart-pie");
                    node.setStyle("-fx-pie-color: " + hexColor + ";");
                    break;
                default:
                    node = areaChart.lookup(preIdent + ".chart-series-area-fill");
                    nodew = areaChart.lookup(preIdent + ".chart-series-area-line");
                    node.setStyle("-fx-fill: linear-gradient(" + hexColor + "," + hexColor + ");"
                            + "  -fx-background-insets: 0 0 -1 0, 0, 1, 2;"
                            + "  -fx-background-radius: 3px, 3px, 2px, 1px;");
                    nodew.setStyle("-fx-stroke: " + hexColor + "; -fx-stroke-width: 2px; ");
                    break;
            }

        }

        ChartPanManager panner = null;
        switch (chartType.toString()) {
            case ("AREA"):
                areaChart.setTitle(title);
                areaChart.setLegendVisible(false);
                areaChart.setCreateSymbols(true);
                areaChart.layout();

                areaChart.getXAxis().setAutoRanging(true);
                areaChart.getXAxis().setLabel(I18n.getInstance().getString("plugin.graph.chart.dateaxis.title"));
                areaChart.getYAxis().setAutoRanging(true);
                areaChart.getYAxis().setLabel(unit);

                areaChart.setOnMouseMoved(mouseEvent -> {
                    updateTable(chartName, chartType, mouseEvent, null);
                });

                panner = new ChartPanManager(areaChart);
                break;
            case ("LINE"):
                lineChart.setTitle(title);
                lineChart.setLegendVisible(false);
                lineChart.setCreateSymbols(true);
                lineChart.layout();

                lineChart.getXAxis().setAutoRanging(true);
                lineChart.getXAxis().setLabel(I18n.getInstance().getString("plugin.graph.chart.dateaxis.title"));
                lineChart.getYAxis().setAutoRanging(true);
                lineChart.getYAxis().setLabel(unit);

                lineChart.setOnMouseMoved(mouseEvent -> {
                    updateTable(chartName, chartType, mouseEvent, null);
                });

                panner = new ChartPanManager(lineChart);
                break;
            case ("BAR"):
                barChart.setTitle(title);
                barChart.setLegendVisible(false);
                barChart.layout();

                barChart.getXAxis().setAutoRanging(true);
                barChart.getXAxis().setLabel(I18n.getInstance().getString("plugin.graph.chart.dateaxis.title"));
                barChart.getYAxis().setAutoRanging(true);
                barChart.getXAxis().setTickLabelRotation(-90);
                barChart.getYAxis().setLabel(unit);

                barChart.setOnMouseMoved(mouseEvent -> {
                    //updateTable(chartName, chartType, selectedData, finalUnitBar, mouseEvent);
                });

                //panner = new ChartPanManager(barChart);
                break;
            case ("BUBBLE"):
                bubbleChart.setTitle(title);
                bubbleChart.setLegendVisible(false);
                bubbleChart.layout();

                bubbleChart.getXAxis().setAutoRanging(true);
                bubbleChart.getXAxis().setLabel(I18n.getInstance().getString("plugin.graph.chart.dateaxis.title"));
                bubbleChart.getYAxis().setAutoRanging(true);
                bubbleChart.getYAxis().setLabel(unit);

                bubbleChart.setOnMouseMoved(mouseEvent -> {
                    updateTable(chartName, chartType, mouseEvent, null);
                });

                panner = new ChartPanManager(bubbleChart);
                break;
            case ("SCATTER"):
                scatterChart.setTitle(title);
                scatterChart.setLegendVisible(false);
                scatterChart.layout();

                scatterChart.getXAxis().setAutoRanging(true);
                scatterChart.getXAxis().setLabel(I18n.getInstance().getString("plugin.graph.chart.dateaxis.title"));
                scatterChart.getYAxis().setAutoRanging(true);
                scatterChart.getYAxis().setLabel(unit);

                scatterChart.setOnMouseMoved(mouseEvent -> {
                    updateTable(chartName, chartType, mouseEvent, null);
                });

                panner = new ChartPanManager(scatterChart);
                break;
            case ("PIE"):
                table.setVisible(false);
                pieChart.setTitle(title);
                pieChart.setLegendVisible(false);
                pieChart.layout();

//                pieChart.setOnMouseMoved(mouseEvent -> {
//                    updateTable(chartName, chartType, selectedData, finalUnitPie, mouseEvent);
//                });

                //panner = new ChartPanManager(pieChart);
                break;
            default:
                areaChart.setTitle(title);
                areaChart.setLegendVisible(false);
                areaChart.setCreateSymbols(false);
                areaChart.layout();

                areaChart.getXAxis().setAutoRanging(true);
                areaChart.getXAxis().setLabel(I18n.getInstance().getString("plugin.graph.chart.dateaxis.title"));
                areaChart.getYAxis().setAutoRanging(true);
                areaChart.getYAxis().setLabel(unit);

                areaChart.setOnMouseMoved(mouseEvent -> {
                    updateTable(chartName, chartType, mouseEvent, null);
                });

                panner = new ChartPanManager(areaChart);
                break;
        }
        if (panner != null) {
            panner.setMouseFilter(mouseEvent -> {
                if (mouseEvent.getButton() == MouseButton.SECONDARY
                        || (mouseEvent.getButton() == MouseButton.PRIMARY
                        && mouseEvent.isShortcutDown())) {
                } else {
                    mouseEvent.consume();
                }
            });
            panner.start();
        }

        switch (chartType.toString()) {
            case ("AREA"):
                areaChartRegion = JFXChartUtil.setupZooming(areaChart, mouseEvent -> {

                    if (mouseEvent.getButton() != MouseButton.PRIMARY
                            || mouseEvent.isShortcutDown()) {
                        mouseEvent.consume();
                        if (mouseEvent.isControlDown()) {
                            showNote(mouseEvent, selectedData, chartName, chartType);
                        }
                    }
                });

                JFXChartUtil.addDoublePrimaryClickAutoRangeHandler(areaChart);
                break;
            case ("LINE"):
                areaChartRegion = JFXChartUtil.setupZooming(lineChart, mouseEvent -> {

                    if (mouseEvent.getButton() != MouseButton.PRIMARY
                            || mouseEvent.isShortcutDown()) {
                        mouseEvent.consume();
                        if (mouseEvent.isControlDown()) {
                            showNote(mouseEvent, selectedData, chartName, chartType);
                        }
                    }
                });

                JFXChartUtil.addDoublePrimaryClickAutoRangeHandler(lineChart);
                break;
            case ("BAR"):
                areaChartRegion = barChart;
//                areaChartRegion = JFXChartUtil.setupZooming(barChart, mouseEvent -> {
//
//                    if (mouseEvent.getButton() != MouseButton.PRIMARY
//                            || mouseEvent.isShortcutDown()) {
//                        mouseEvent.consume();
//                        if (mouseEvent.isControlDown()) {
//                            showNote(mouseEvent, selectedData, chartName, chartType);
//                        }
//                    }
//                });
//
//                JFXChartUtil.addDoublePrimaryClickAutoRangeHandler(areaChart);
                break;
            case ("BUBBLE"):
                areaChartRegion = JFXChartUtil.setupZooming(bubbleChart, mouseEvent -> {

                    if (mouseEvent.getButton() != MouseButton.PRIMARY
                            || mouseEvent.isShortcutDown()) {
                        mouseEvent.consume();
                        if (mouseEvent.isControlDown()) {
                            showNote(mouseEvent, selectedData, chartName, chartType);
                        }
                    }
                });

                JFXChartUtil.addDoublePrimaryClickAutoRangeHandler(areaChart);
                break;
            case ("SCATTER"):
                areaChartRegion = JFXChartUtil.setupZooming(scatterChart, mouseEvent -> {

                    if (mouseEvent.getButton() != MouseButton.PRIMARY
                            || mouseEvent.isShortcutDown()) {
                        mouseEvent.consume();
                        if (mouseEvent.isControlDown()) {
                            showNote(mouseEvent, selectedData, chartName, chartType);
                        }
                    }
                });

                JFXChartUtil.addDoublePrimaryClickAutoRangeHandler(areaChart);
                break;
            case ("PIE"):
                areaChartRegion = pieChart;
//                areaChartRegion = JFXChartUtil.setupZooming(pieChart, mouseEvent -> {
//
//                    if (mouseEvent.getButton() != MouseButton.PRIMARY
//                            || mouseEvent.isShortcutDown()) {
//                        mouseEvent.consume();
//                        if (mouseEvent.isControlDown()) {
//                            showNote(mouseEvent, selectedData, chartName, chartType);
//                        }
//                    }
//                });
//
//                JFXChartUtil.addDoublePrimaryClickAutoRangeHandler(areaChart);
                break;
            default:
                areaChartRegion = JFXChartUtil.setupZooming(areaChart, mouseEvent -> {

                    if (mouseEvent.getButton() != MouseButton.PRIMARY
                            || mouseEvent.isShortcutDown()) {
                        mouseEvent.consume();
                        if (mouseEvent.isControlDown()) {
                            showNote(mouseEvent, selectedData, chartName, chartType);
                        }
                    }
                });

                JFXChartUtil.addDoublePrimaryClickAutoRangeHandler(areaChart);
                break;
        }
    }

    public void updateTablesSimultaneously(String chartName, ChartSettings.ChartType chartType, MouseEvent mouseEvent, Number valueForDisplay) {
        updateTable(chartName, chartType, mouseEvent, valueForDisplay);
    }

    private void updateTable(String chartName, ChartSettings.ChartType chartType, MouseEvent
            mouseEvent, Number valueForDisplay) {
        Point2D mouseCoordinates = null;
        if (mouseEvent != null) mouseCoordinates = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());
        Double x = null;
        if (valueForDisplay == null) {
            switch (chartType.toString()) {
                case ("AREA"):
                    x = getAreaChart().getXAxis().sceneToLocal(mouseCoordinates).getX();
                    break;
                case ("LINE"):
                    x = getLineChart().getXAxis().sceneToLocal(mouseCoordinates).getX();
                    break;
                case ("BAR"):
                    //valueForDisplay = getBarChart().getXAxis().sceneToLocal(mouseCoordinates).getValueForDisplay();
                    break;
                case ("BUBBLE"):
                    //valueForDisplay = getBubbleChart().getXAxis().sceneToLocal(mouseCoordinates).getValueForDisplay();
                    break;
                case ("SCATTER"):
                    //valueForDisplay = getScatterChart().getXAxis().sceneToLocal(mouseCoordinates).getValueForDisplay();
                    break;
                case ("PIE"):
                    //valueForDisplay = pieChart.getXAxis().sceneToLocal(mouseCoordinates).getValueForDisplay();
                    break;
                default:
                    x = getAreaChart().getXAxis().sceneToLocal(mouseCoordinates).getX();
                    break;
            }
        }
        if (x != null) {
            switch (chartType.toString()) {
                case ("AREA"):
                    valueForDisplay = (Number) getAreaChart().getXAxis().getValueForDisplay(x);
                    break;
                case ("LINE"):
                    valueForDisplay = getLineChart().getXAxis().getValueForDisplay(x);
                    break;
                case ("BAR"):
                    //valueForDisplay = getBarChart().getXAxis().getValueForDisplay(valueForDisplay);
                    break;
                case ("BUBBLE"):
                    //valueForDisplay = getBubbleChart().getXAxis().getValueForDisplay(valueForDisplay);
                    break;
                case ("SCATTER"):
                    //valueForDisplay = getScatterChart().getXAxis().getValueForDisplay(valueForDisplay);
                    break;
                case ("PIE"):
                    //valueForDisplay = pieChart.getXAxis().getValueForDisplay(valueForDisplay);
                    break;
                default:
                    valueForDisplay = (Number) getAreaChart().getXAxis().getValueForDisplay(x);
                    break;
            }
        }
        if (valueForDisplay != null) {
            setValueForDisplay(valueForDisplay);
            getTableData().clear();
            for (ChartDataModel singleRow : getDataModel().getSelectedData()) {
                if (Objects.isNull(chartName) || chartName.equals("") || singleRow.get_selectedCharts().contains(chartName)) {
                    try {
                        TreeMap<Double, JEVisSample> sampleTreeMap = singleRow.getSampleMap();
                        Double higherKey = sampleTreeMap.higherKey(valueForDisplay.doubleValue());
                        Double lowerKey = sampleTreeMap.lowerKey(valueForDisplay.doubleValue());

                        Double nearest = higherKey;
                        if (nearest == null) nearest = lowerKey;

                        if (lowerKey != null && higherKey != null) {
                            Double lower = Math.abs(lowerKey - valueForDisplay.doubleValue());
                            Double higher = Math.abs(higherKey - valueForDisplay.doubleValue());
                            if (lower < higher) {
                                nearest = lowerKey;
                            }
                        }

                        NumberFormat nf = NumberFormat.getInstance();
                        nf.setMinimumFractionDigits(2);
                        nf.setMaximumFractionDigits(2);
                        Double valueAsDouble = sampleTreeMap.get(nearest).getValueAsDouble();
                        String note = sampleTreeMap.get(nearest).getNote();
                        Node formattedNote = formatNote(note, singleRow.getColor());
                        String formattedDouble = nf.format(valueAsDouble);
                        TableEntry tableEntry = singleRow.getTableEntry();
                        tableEntry.setDate(new DateTime(Math.round(nearest)).toString(DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")));
                        tableEntry.setNote(formattedNote);
                        String unit = UnitManager.getInstance().formate(singleRow.getUnit());
                        tableEntry.setValue(formattedDouble + " " + unit);
                        getTableData().add(tableEntry);

                        getTable().layout();

                    } catch (Exception ex) {
                    }
                }
            }
        }
        table.sort();
    }

    public TableView getTable() {
        return table;
    }

    public ObservableList<TableEntry> getTableData() {
        return tableData;
    }

    public LineChart<Number, Number> getLineChart() {
        return lineChart;
    }

    public BarChart<Number, Number> getBarChart() {
        return barChart;
    }

    public BubbleChart<Number, Number> getBubbleChart() {
        return bubbleChart;
    }

    public ScatterChart<Number, Number> getScatterChart() {
        return scatterChart;
    }

    public PieChart getPieChart() {
        return pieChart;
    }

    public GraphDataModel getDataModel() {
        return dataModel;
    }

    public Number getValueForDisplay() {
        return valueForDisplay;
    }

    public void setValueForDisplay(Number valueForDisplay) {
        this.valueForDisplay = valueForDisplay;
    }

    public ChartSettings.ChartType getChartType() {
        return chartType;
    }

    public void setChartType(ChartSettings.ChartType chartType) {
        this.chartType = chartType;
    }

    public String getChartName() {
        return this.chartName;
    }

    public void setChartName(String chartName) {
        this.chartName = chartName;
    }
}
