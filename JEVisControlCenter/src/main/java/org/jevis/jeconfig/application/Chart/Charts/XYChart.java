package org.jevis.jeconfig.application.Chart.Charts;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gillius.jfxutils.chart.ChartPanManager;
import org.gillius.jfxutils.chart.JFXChartUtil;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisObject;
import org.jevis.api.JEVisSample;
import org.jevis.commons.dataprocessing.ManipulationMode;
import org.jevis.commons.unit.UnitManager;
import org.jevis.jeconfig.application.Chart.ChartDataModel;
import org.jevis.jeconfig.application.Chart.ChartElements.DateValueAxis;
import org.jevis.jeconfig.application.Chart.ChartElements.Note;
import org.jevis.jeconfig.application.Chart.ChartElements.TableEntry;
import org.jevis.jeconfig.application.Chart.ChartElements.XYChartSerie;
import org.jevis.jeconfig.application.Chart.customJFXChartUtil;
import org.jevis.jeconfig.application.Chart.data.RowNote;
import org.jevis.jeconfig.dialog.NoteDialog;
import org.jevis.jeconfig.tool.I18n;
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

public class XYChart implements Chart {
    private static final Logger logger = LogManager.getLogger(XYChart.class);
    Boolean hideShowIcons;
    ObservableList<javafx.scene.chart.XYChart.Series<Number, Number>> series = FXCollections.observableArrayList();
    List<Color> hexColors = new ArrayList<>();
    ObservableList<TableEntry> tableData = FXCollections.observableArrayList();
    AtomicReference<DateTime> timeStampOfFirstSample = new AtomicReference<>(DateTime.now());
    AtomicReference<DateTime> timeStampOfLastSample;
    NumberAxis numberAxis = new NumberAxis();
    Axis dateAxis = new DateValueAxis();
    private String chartName;
    private List<String> unit = new ArrayList<>();
    private List<ChartDataModel> chartDataModels;
    private List<XYChartSerie> xyChartSerieList = new ArrayList<>();
    private javafx.scene.chart.Chart chart;
    private Number valueForDisplay;
    private Region areaChartRegion;
    private Period period;
    private boolean asDuration = false;
    private ManipulationMode addSeriesOfType;
    private AtomicBoolean addManipulationToTitle;
    private AtomicReference<ManipulationMode> manipulationMode;
    private Boolean[] changedBoth;

    public XYChart(List<ChartDataModel> chartDataModels, Boolean hideShowIcons, ManipulationMode addSeriesOfType, Integer chartId, String chartName) {
        this.chartDataModels = chartDataModels;
        this.hideShowIcons = hideShowIcons;
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

        finalizeChart();

        getChart().setStyle("-fx-font-size: " + 12 + "px;");
        getChart().setAnimated(false);
        applyColors();

        getChart().setTitle(getUpdatedChartName());

        getChart().setLegendVisible(false);
        //((javafx.scene.chart.XYChart)getChart()).setCreateSymbols(true);

        initializeZoom();
    }

    public void finalizeChart() {
        setChart(new javafx.scene.chart.AreaChart<Number, Number>(dateAxis, numberAxis, series));
    }

    public XYChartSerie generateSerie(Boolean[] changedBoth, ChartDataModel singleRow) throws JEVisException {
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

    void checkManipulation(ChartDataModel singleRow) throws JEVisException {
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
        getChart().setTitle(getUpdatedChartName());
    }

    public void generateYAxis() {
        numberAxis.setAutoRanging(true);

        for (ChartDataModel singleRow : chartDataModels) {
            String currentUnit = UnitManager.getInstance().format(singleRow.getUnit());
            if (currentUnit.equals("") || currentUnit.equals(Unit.ONE.toString()))
                currentUnit = singleRow.getUnit().getLabel();
            if (!unit.contains(currentUnit)) {
                unit.add(currentUnit);
            }
        }

        if (chartDataModels != null && chartDataModels.size() > 0) {

            if (unit.isEmpty()) unit.add(I18n.getInstance().getString("plugin.graph.chart.valueaxis.nounit"));

        }

        StringBuilder allUnits = new StringBuilder();
        for (String s : unit) {
            if (unit.indexOf(s) == 0) allUnits.append(s);
            else allUnits.append(", ").append(s);
        }
        numberAxis.setLabel(allUnits.toString());
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
            overall = period.toString(PeriodFormat.wordBased().withLocale(I18n.getInstance().getLocale()));
        }

        dateAxis.setLabel(I18n.getInstance().getString("plugin.graph.chart.dateaxis.title") + " " + overall);
    }

    @Override
    public void initializeZoom() {
        ChartPanManager panner = null;

        getChart().setOnMouseMoved(mouseEvent -> {
            updateTable(mouseEvent, null);
        });

        panner = new ChartPanManager((javafx.scene.chart.XYChart<?, ?>) getChart());

        panner.setMouseFilter(mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.SECONDARY
                    && (mouseEvent.getButton() != MouseButton.PRIMARY
                    || !mouseEvent.isShortcutDown())) {
                mouseEvent.consume();
            }
        });
        panner.start();

        areaChartRegion = customJFXChartUtil.setupZooming((javafx.scene.chart.XYChart<?, ?>) getChart(), mouseEvent -> {

            if (mouseEvent.getButton() != MouseButton.PRIMARY
                    || mouseEvent.isShortcutDown()) {
                mouseEvent.consume();
                if (mouseEvent.isControlDown()) {
                    showNote(mouseEvent);
                }
            }
        });

        JFXChartUtil.addDoublePrimaryClickAutoRangeHandler((javafx.scene.chart.XYChart<?, ?>) getChart());

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
        timeStampOfFirstSample.set(DateTime.now());
        timeStampOfLastSample.set(new DateTime(2001, 1, 1, 0, 0, 0));
        changedBoth = new Boolean[]{false, false};
        //xyChartSerieList.clear();
        //series.clear();
        //tableData.clear();
        unit.clear();
        //hexColors.clear();
        if (chartDataModels.size() > 0) {
            if (chartDataModels.size() <= xyChartSerieList.size()) {
                /**
                 * if the new chart data model contains fewer or equal count of chart series as the old one
                 */

                if (chartDataModels.size() < xyChartSerieList.size()) {
                    /**
                     * remove the series which are in excess
                     */

                    for (int i = chartDataModels.size(); i < xyChartSerieList.size(); i++) {
                        xyChartSerieList.remove(i);
                        series.remove(i);
                        hexColors.remove(i);
                        tableData.remove(i);
                    }

                }

                updateXYChartSeries();

            } else {
                /**
                 * if there are more new data rows then old ones
                 */

                updateXYChartSeries();

                for (int i = xyChartSerieList.size(); i < chartDataModels.size(); i++) {
                    try {
                        xyChartSerieList.add(generateSerie(changedBoth, chartDataModels.get(i)));
                    } catch (JEVisException e) {
                        e.printStackTrace();
                    }
                }
            }

            series.forEach(serie -> {
                serie.getData().forEach(numberNumberData -> {
                    if (numberNumberData.getNode() != null)
                        if (numberNumberData.getNode().getClass().equals(HBox.class))
                            numberNumberData.getNode().setVisible(hideShowIcons);
                });
            });

            applyColors();

            generateXAxis(changedBoth);
            generateYAxis();

            getChart().setTitle(getUpdatedChartName());

            ((javafx.scene.chart.XYChart) getChart()).getXAxis().setAutoRanging(true);
            ((javafx.scene.chart.XYChart) getChart()).getYAxis().setAutoRanging(true);
            ((javafx.scene.chart.XYChart) getChart()).getXAxis().layout();
            ((javafx.scene.chart.XYChart) getChart()).getYAxis().layout();
            getChart().layout();
        }
    }

    private void updateXYChartSeries() {
        for (int i = 0; i < xyChartSerieList.size(); i++) {
            XYChartSerie xyChartSerie = xyChartSerieList.get(i);

            hexColors.set(i, chartDataModels.get(i).getColor());
            xyChartSerie.setSingleRow(chartDataModels.get(i));
            try {
                xyChartSerie.generateSeriesFromSamples();
            } catch (JEVisException e) {
                e.printStackTrace();
            }

            tableData.set(i, xyChartSerie.getTableEntry());

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
        }
    }

    private String getUpdatedChartName() {
        String newName = chartName;
        switch (manipulationMode.get()) {
            case CENTRIC_RUNNING_MEAN:
                String centricrunningmean = I18n.getInstance().getString("plugin.graph.manipulation.centricrunningmean");
                if (!newName.contains(centricrunningmean))
                    newName += " [" + centricrunningmean + "]";
                break;
            case RUNNING_MEAN:
                String runningmean = I18n.getInstance().getString("plugin.graph.manipulation.runningmean");
                newName += " [" + runningmean + "]";
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
                String sortedmax = I18n.getInstance().getString("plugin.graph.manipulation.sortedmax");
                if (!newName.contains(sortedmax))
                    newName += " [" + sortedmax + "]";
                break;
            case SORTED_MIN:
                String sortedmin = I18n.getInstance().getString("plugin.graph.manipulation.sortedmin");
                if (!newName.contains(sortedmin))
                    newName += " [" + sortedmin + "]";
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
    public void setHideShowIcons(Boolean hideShowIcons) {
        this.hideShowIcons = hideShowIcons;
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

            x = ((javafx.scene.chart.XYChart) getChart()).getXAxis().sceneToLocal(Objects.requireNonNull(mouseCoordinates)).getX();

            valueForDisplay = (Number) ((javafx.scene.chart.XYChart) getChart()).getXAxis().getValueForDisplay(x);

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
                    tableEntry.setPeriod(getPeriod().toString(PeriodFormat.wordBased().withLocale(I18n.getInstance().getLocale())));
                } catch (Exception ex) {
                }

            });
        }
    }

    @Override
    public void showNote(MouseEvent mouseEvent) {
        if (manipulationMode.get().equals(ManipulationMode.NONE)) {

            Point2D mouseCoordinates = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            double x = ((javafx.scene.chart.XYChart) getChart()).getXAxis().sceneToLocal(mouseCoordinates).getX();

            Map<String, RowNote> map = new HashMap<>();
            Number valueForDisplay = null;
            valueForDisplay = (Number) ((javafx.scene.chart.XYChart) getChart()).getXAxis().getValueForDisplay(x);

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
                if (response.getButtonData().getTypeCode().equals(ButtonType.OK.getButtonData().getTypeCode())) {
                    saveUserNotes(nd.getNoteMap());
                }
            });
        }
    }

    @Override
    public void applyColors() {
        getChart().applyCss();

        for (int i = 0; i < hexColors.size(); i++) {
            Color currentColor = hexColors.get(i);
            Color brighter = currentColor.deriveColor(1, 1, 50, 0.3);
            String hexColor = toRGBCode(currentColor);
            String hexBrighter = toRGBCode(brighter) + "55";
            String preIdent = ".default-color" + i;
            Node node = getChart().lookup(preIdent + ".chart-series-area-fill");
            Node nodew = getChart().lookup(preIdent + ".chart-series-area-line");

            node.setStyle("-fx-fill: linear-gradient(" + hexColor + "," + hexBrighter + ");"
                    + "  -fx-background-insets: 0 0 -1 0, 0, 1, 2;"
                    + "  -fx-background-radius: 3px, 3px, 2px, 1px;");
            nodew.setStyle("-fx-stroke: " + hexColor + "; -fx-stroke-width: 2px; ");
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
        return chart;
    }

    public void setChart(javafx.scene.chart.Chart chart) {
        this.chart = chart;
    }

    @Override
    public Region getRegion() {
        return areaChartRegion;
    }

}