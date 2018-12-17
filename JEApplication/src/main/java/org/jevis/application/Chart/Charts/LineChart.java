package org.jevis.application.Chart.Charts;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gillius.jfxutils.chart.ChartPanManager;
import org.gillius.jfxutils.chart.JFXChartUtil;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisObject;
import org.jevis.api.JEVisSample;
import org.jevis.application.Chart.ChartDataModel;
import org.jevis.application.Chart.ChartElements.DateValueAxis;
import org.jevis.application.Chart.ChartElements.Note;
import org.jevis.application.Chart.ChartElements.TableEntry;
import org.jevis.application.Chart.ChartElements.XYChartSerie;
import org.jevis.application.Chart.data.RowNote;
import org.jevis.application.application.AppLocale;
import org.jevis.application.application.SaveResourceBundle;
import org.jevis.application.dialog.NoteDialog;
import org.jevis.commons.dataprocessing.ManipulationMode;
import org.jevis.commons.unit.UnitManager;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormat;

import javax.measure.unit.Unit;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.jevis.commons.dataprocessing.ManipulationMode.RUNNING_MEAN;

public class LineChart implements Chart {
    private static SaveResourceBundle rb = new SaveResourceBundle(AppLocale.BUNDLE_ID, AppLocale.getInstance().getLocale());
    private static final Logger logger = LogManager.getLogger(LineChart.class);
    private String chartName;
    private Integer chartId;
    private List<String> unit = new ArrayList<>();
    private List<ChartDataModel> chartDataModels;
    private Boolean hideShowIcons;
    private ObservableList<XYChart.Series<Number, Number>> series = FXCollections.observableArrayList();
    private List<XYChartSerie> xyChartSerieList = new ArrayList<>();
    private javafx.scene.chart.LineChart<Number, Number> lineChart;
    private List<Color> hexColors = new ArrayList<>();
    private Number valueForDisplay;
    private ObservableList<TableEntry> tableData = FXCollections.observableArrayList();
    private Region lineChartRegion;
    private Period period;
    private boolean asDuration = false;
    private AtomicReference<DateTime> timeStampOfFirstSample = new AtomicReference<>(DateTime.now());
    private ManipulationMode addSeriesOfType;
    private AtomicReference<DateTime> timeStampOfLastSample;
    private AtomicBoolean addManipulationToTitle;
    private AtomicReference<ManipulationMode> manipulationMode;
    private Boolean[] changedBoth;
    private NumberAxis numberAxis = new NumberAxis();
    private Axis dateAxis = new DateValueAxis();

    public LineChart(List<ChartDataModel> chartDataModels, Boolean hideShowIcons, ManipulationMode addSeriesOfType, Integer chartId, String chartName) {
        this.chartDataModels = chartDataModels;
        this.hideShowIcons = hideShowIcons;
        this.chartId = chartId;
        this.chartName = chartName;
        this.addSeriesOfType = addSeriesOfType;
        init();
    }

    private void init() {

        timeStampOfLastSample = new AtomicReference<>(new DateTime(2001, 1, 1, 0, 0, 0));
        changedBoth = new Boolean[]{false, false};

        addManipulationToTitle = new AtomicBoolean(false);
        manipulationMode = new AtomicReference<>(ManipulationMode.NONE);

        chartDataModels.forEach(singleRow -> {
            if (!singleRow.getSelectedcharts().isEmpty()) {
                try {
                    xyChartSerieList.add(generateSerie(changedBoth, singleRow));

                } catch (JEVisException e) {
                    logger.error("Error: Cant create series for data rows: ", e);
                }
            }
        });

        if (asDuration) {
            ((DateValueAxis) dateAxis).setAsDuration(true);
            ((DateValueAxis) dateAxis).setTimeStampFromFirstSample(timeStampOfFirstSample.get());
        }

        generateXAxis(changedBoth);
        dateAxis.setAutoRanging(true);

        generateYAxis();
        numberAxis.setAutoRanging(true);

        lineChart = new javafx.scene.chart.LineChart<Number, Number>(dateAxis, numberAxis, series);

        applyColors();

        setTitle(getUpdatedChartName());

        lineChart.setLegendVisible(false);
        lineChart.setCreateSymbols(true);

        initializeZoom();
    }

    private XYChartSerie generateSerie(Boolean[] changedBoth, ChartDataModel singleRow) throws JEVisException {
        XYChartSerie serie = new XYChartSerie(singleRow, hideShowIcons);

        hexColors.add(singleRow.getColor());
        series.add(serie.getSerie());
        tableData.add(serie.getTableEntry());

        /**
         * check if timestamps are in serie
         */

        if (serie.getTimeStampFromFirstSample().isBefore(timeStampOfFirstSample.get())) {
            timeStampOfFirstSample.set(serie.getTimeStampFromFirstSample());
            changedBoth[0] = true;
        }

        if (serie.getTimeStampFromLastSample().isAfter(timeStampOfLastSample.get())) {
            timeStampOfLastSample.set(serie.getTimeStampFromLastSample());
            changedBoth[1] = true;
        }

        /**
         * check if theres a manipulation for changing the x axis values into duration instead of concrete timestamps
         */

        checkManipulation(singleRow);
        return serie;
    }

    private void checkManipulation(ChartDataModel singleRow) throws JEVisException {
        asDuration = singleRow.getManipulationMode().equals(ManipulationMode.SORTED_MIN)
                || singleRow.getManipulationMode().equals(ManipulationMode.SORTED_MAX);

        if (singleRow.getManipulationMode().equals(RUNNING_MEAN)
                || singleRow.getManipulationMode().equals(ManipulationMode.CENTRIC_RUNNING_MEAN)) {
            addManipulationToTitle.set(true);
        } else addManipulationToTitle.set(false);

        manipulationMode.set(singleRow.getManipulationMode());

        if (!addSeriesOfType.equals(ManipulationMode.NONE)) {
            ManipulationMode oldMode = singleRow.getManipulationMode();
            singleRow.setManipulationMode(addSeriesOfType);
            XYChartSerie serie2 = new XYChartSerie(singleRow, hideShowIcons);

            hexColors.add(singleRow.getColor().darker());
            series.add(serie2.getSerie());
            tableData.add(serie2.getTableEntry());

            singleRow.setManipulationMode(oldMode);
        }
    }

    @Override
    public void setTitle(String chartName) {
        this.chartName = chartName;
        lineChart.setTitle(getUpdatedChartName());
    }

    private void generateYAxis() {

        for (ChartDataModel singleRow : chartDataModels) {
            String currentUnit = UnitManager.getInstance().format(singleRow.getUnit());
            if (currentUnit.equals("") || currentUnit.equals(Unit.ONE))
                currentUnit = singleRow.getUnit().getLabel();
            if (!unit.contains(currentUnit)) {
                unit.add(currentUnit);
            }
        }

        if (chartDataModels != null && chartDataModels.size() > 0) {

            if (unit.isEmpty()) unit.add(rb.getString("plugin.graph.chart.valueaxis.nounit"));

        }

        String allUnits = "";
        for (String s : unit) {
            if (unit.indexOf(s) == 0) allUnits += s;
            else allUnits += ", " + s;
        }
        numberAxis.setLabel(allUnits);
    }

    private void generateXAxis(Boolean[] changedBoth) {
        if (!asDuration) ((DateValueAxis) dateAxis).setAsDuration(false);
        else {
            ((DateValueAxis) dateAxis).setAsDuration(true);
            ((DateValueAxis) dateAxis).setTimeStampFromFirstSample(timeStampOfFirstSample.get());
        }

        if (chartDataModels != null && chartDataModels.size() > 0) {

            if (chartDataModels.get(0).getSamples().size() > 1) {
                try {
                    period = new Period(chartDataModels.get(0).getSamples().get(0).getTimestamp(),
                            chartDataModels.get(0).getSamples().get(1).getTimestamp());
                } catch (JEVisException e) {
                    e.printStackTrace();
                }
            }
        }

        String overall = "-";
        if (changedBoth[0] && changedBoth[1]) {
            Period period = new Period(timeStampOfFirstSample.get(), timeStampOfLastSample.get());
            period = period.minusSeconds(period.getSeconds());
            period = period.minusMillis(period.getMillis());
            overall = period.toString(PeriodFormat.wordBased().withLocale(AppLocale.getInstance().getLocale()));
        }

        dateAxis.setLabel(rb.getString("plugin.graph.chart.dateaxis.title") + " " + overall);
    }

    @Override
    public void initializeZoom() {
        ChartPanManager panner = null;

        lineChart.setOnMouseMoved(mouseEvent -> {
            updateTable(mouseEvent, null);
        });

        panner = new ChartPanManager(lineChart);

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

        lineChartRegion = JFXChartUtil.setupZooming(lineChart, mouseEvent -> {

            if (mouseEvent.getButton() != MouseButton.PRIMARY
                    || mouseEvent.isShortcutDown()) {
                mouseEvent.consume();
                if (mouseEvent.isControlDown()) {
                    showNote(mouseEvent);
                }
            }
        });

        JFXChartUtil.addDoublePrimaryClickAutoRangeHandler(lineChart);

    }

    @Override
    public ObservableList<TableEntry> getTableData() {
        return tableData;
    }

    @Override
    public Period getPeriod() {
        return period;
    }

    @Override
    public DateTime getStartDateTime() {
        return chartDataModels.get(0).getSelectedStart();
    }

    @Override
    public DateTime getEndDateTime() {
        return chartDataModels.get(0).getSelectedEnd();
    }

    @Override
    public void updateChart() {
        //xyChartSerieList.clear();
        //series.clear();
        tableData.clear();
        unit.clear();
        //hexColors.clear();
        if (chartDataModels.size() > 0) {
            if (chartDataModels.size() <= xyChartSerieList.size()) {

                if (chartDataModels.size() < xyChartSerieList.size()) {
                    for (int i = xyChartSerieList.size(); i > chartDataModels.size(); i--) {
                        xyChartSerieList.remove(i - 1);
                        series.remove(i - 1);
                        hexColors.remove(i - 1);
                    }
                }

                xyChartSerieList.forEach(xyChartSerie -> {
                    int i = xyChartSerieList.indexOf(xyChartSerie);
                    hexColors.set(i, chartDataModels.get(i).getColor());
                    xyChartSerie.setSingleRow(chartDataModels.get(i));
                    try {
                        xyChartSerie.generateSeriesFromSamples();
                    } catch (JEVisException e) {
                        e.printStackTrace();
                    }

                    tableData.add(xyChartSerie.getTableEntry());

                    if (xyChartSerie.getTimeStampFromFirstSample().isBefore(timeStampOfFirstSample.get())) {
                        timeStampOfFirstSample.set(xyChartSerie.getTimeStampFromFirstSample());
                        changedBoth[0] = true;
                    }

                    if (xyChartSerie.getTimeStampFromLastSample().isAfter(timeStampOfLastSample.get())) {
                        timeStampOfLastSample.set(xyChartSerie.getTimeStampFromLastSample());
                        changedBoth[1] = true;
                    }

                    try {
                        checkManipulation(chartDataModels.get(i));
                    } catch (JEVisException e) {
                        e.printStackTrace();
                    }
                });

            } else if (chartDataModels.size() > xyChartSerieList.size()) {

                for (int i = xyChartSerieList.size() - 1; i < chartDataModels.size(); i++) {
                    try {
                        xyChartSerieList.add(generateSerie(changedBoth, chartDataModels.get(i)));
                    } catch (JEVisException e) {
                        e.printStackTrace();
                    }
                }
            }

            applyColors();

            generateXAxis(changedBoth);
            dateAxis.setAutoRanging(true);

            generateYAxis();
            numberAxis.setAutoRanging(true);

            setTitle(getUpdatedChartName());

            lineChart.getXAxis().layout();
            lineChart.getYAxis().layout();
            lineChart.layout();
        }
    }

    private String getUpdatedChartName() {
        String newName = chartName;
        switch (manipulationMode.get()) {
            case CENTRIC_RUNNING_MEAN:
                newName += " [" + rb.getString("plugin.graph.manipulation.centricrunningmean") + "]";
                break;
            case RUNNING_MEAN:
                newName += " [" + rb.getString("plugin.graph.manipulation.runningmean") + "]";
                break;
            case MAX:
                break;
            case MIN:
                break;
            case AVERAGE:
                break;
            case NONE:
                break;
            case TOTAL:
                break;
            case SORTED_MAX:
                newName += " [" + rb.getString("plugin.graph.manipulation.sortedmax") + "]";
                break;
            case SORTED_MIN:
                newName += " [" + rb.getString("plugin.graph.manipulation.sortedmin") + "]";
                break;
            case MEDIAN:
                break;
        }
        return newName;
    }

    @Override
    public void setDataModels(List<ChartDataModel> chartDataModels) {
        this.chartDataModels = chartDataModels;
    }

    @Override
    public String getChartName() {
        return chartName;
    }

    @Override
    public Integer getChartId() {
        return null;
    }

    @Override
    public void updateTable(MouseEvent mouseEvent, Number valueForDisplay) {
        Point2D mouseCoordinates = null;
        if (mouseEvent != null) mouseCoordinates = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());
        Double x = null;
        if (valueForDisplay == null) {

            x = lineChart.getXAxis().sceneToLocal(mouseCoordinates).getX();

            if (x != null) {
                valueForDisplay = lineChart.getXAxis().getValueForDisplay(x);

            }
        }
        if (valueForDisplay != null) {
            setValueForDisplay(valueForDisplay);
            Number finalValueForDisplay = valueForDisplay;
            xyChartSerieList.parallelStream().forEach(serie -> {
                try {
                    TableEntry tableEntry = serie.getTableEntry();
                    TreeMap<Double, JEVisSample> sampleTreeMap = serie.getSampleMap();
                    Double higherKey = sampleTreeMap.higherKey(finalValueForDisplay.doubleValue());
                    Double lowerKey = sampleTreeMap.lowerKey(finalValueForDisplay.doubleValue());

                    Double nearest = higherKey;
                    if (nearest == null) nearest = lowerKey;

                    if (lowerKey != null && higherKey != null) {
                        Double lower = Math.abs(lowerKey - finalValueForDisplay.doubleValue());
                        Double higher = Math.abs(higherKey - finalValueForDisplay.doubleValue());
                        if (lower < higher) {
                            nearest = lowerKey;
                        }
                    }

                    NumberFormat nf = NumberFormat.getInstance();
                    nf.setMinimumFractionDigits(2);
                    nf.setMaximumFractionDigits(2);
                    Double valueAsDouble = sampleTreeMap.get(nearest).getValueAsDouble();
                    String note = sampleTreeMap.get(nearest).getNote();
                    Note formattedNote = new Note(note);
                    String formattedDouble = nf.format(valueAsDouble);

                    if (!asDuration) {
                        tableEntry.setDate(new DateTime(Math.round(nearest))
                                .toString(DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")));
                    } else {
                        tableEntry.setDate((new DateTime(Math.round(nearest)).getMillis() -
                                timeStampOfFirstSample.get().getMillis()) / 1000 / 60 / 60 + " h");
                    }
                    tableEntry.setNote(formattedNote.getNote());
                    String unit = serie.getUnit();
                    tableEntry.setValue(formattedDouble + " " + unit);
                    tableEntry.setPeriod(getPeriod().toString(PeriodFormat.wordBased().withLocale(AppLocale.getInstance().getLocale())));
                } catch (Exception ex) {
                }

            });
        }
    }

    @Override
    public void showNote(MouseEvent mouseEvent) {
        if (manipulationMode.get().equals(ManipulationMode.NONE)) {

            Point2D mouseCoordinates = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            Double x = lineChart.getXAxis().sceneToLocal(mouseCoordinates).getX();

            if (x != null) {
                Map<String, RowNote> map = new HashMap<>();
                Number valueForDisplay = null;
                valueForDisplay = lineChart.getXAxis().getValueForDisplay(x);

                for (XYChartSerie serie : xyChartSerieList) {
                    try {
                        Double higherKey = serie.getSampleMap().higherKey(valueForDisplay.doubleValue());
                        Double lowerKey = serie.getSampleMap().lowerKey(valueForDisplay.doubleValue());

                        Double nearest = higherKey;
                        if (nearest == null) nearest = lowerKey;

                        if (lowerKey != null && higherKey != null) {
                            Double lower = Math.abs(lowerKey - valueForDisplay.doubleValue());
                            Double higher = Math.abs(higherKey - valueForDisplay.doubleValue());
                            if (lower < higher) {
                                nearest = lowerKey;
                            }

                            JEVisSample nearestSample = serie.getSampleMap().get(nearest);

                            String title = "";
                            title += serie.getSingleRow().getObject().getName();

                            JEVisObject dataObject;
                            if (serie.getSingleRow().getDataProcessor() != null)
                                dataObject = serie.getSingleRow().getDataProcessor();
                            else dataObject = serie.getSingleRow().getObject();

                            String userNote = getUserNoteForTimeStamp(nearestSample, nearestSample.getTimestamp());

                            RowNote rowNote = new RowNote(dataObject, nearestSample, title, userNote);

                            map.put(title, rowNote);
                        }
                    } catch (Exception ex) {
                        logger.error("Error: could not get note", ex);
                    }
                }

                NoteDialog nd = new NoteDialog(map);

                nd.showAndWait().ifPresent(response -> {
                    if (response.getButtonData().getTypeCode() == ButtonType.OK.getButtonData().getTypeCode()) {
                        saveUserNotes(nd.getNoteMap());
                    } else if (response.getButtonData().getTypeCode() == ButtonType.CANCEL.getButtonData().getTypeCode()) {

                    }
                });
            }
        }
    }

    @Override
    public void applyColors() {
        lineChart.applyCss();

        for (int i = 0; i < hexColors.size(); i++) {
            Color currentColor = hexColors.get(i);
            String hexColor = toRGBCode(currentColor);
            String preIdent = ".default-color" + i;

            Node node = lineChart.lookup(preIdent + ".chart-series-line");
            node.setStyle("-fx-stroke: " + hexColor + "; -fx-stroke-width: 2px; ");
        }
    }

    @Override
    public Number getValueForDisplay() {
        return valueForDisplay;
    }

    @Override
    public void setValueForDisplay(Number valueForDisplay) {
        this.valueForDisplay = valueForDisplay;
    }

    @Override
    public javafx.scene.chart.Chart getChart() {
        return lineChart;
    }

    @Override
    public Region getRegion() {
        return lineChartRegion;
    }

}