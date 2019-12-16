
package org.jevis.jeconfig.application.Chart.Charts;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisObject;
import org.jevis.api.JEVisSample;
import org.jevis.api.JEVisUnit;
import org.jevis.commons.chart.ChartDataModel;
import org.jevis.commons.database.ObjectHandler;
import org.jevis.commons.dataprocessing.AggregationPeriod;
import org.jevis.commons.dataprocessing.CleanDataObject;
import org.jevis.commons.dataprocessing.ManipulationMode;
import org.jevis.commons.dataprocessing.VirtualSample;
import org.jevis.commons.datetime.WorkDays;
import org.jevis.commons.json.JsonLimitsConfig;
import org.jevis.commons.unit.UnitManager;
import org.jevis.commons.ws.json.JsonObject;
import org.jevis.jeapi.ws.JEVisDataSourceWS;
import org.jevis.jeapi.ws.JEVisObjectWS;
import org.jevis.jeconfig.application.Chart.ChartElements.DateAxis;
import org.jevis.jeconfig.application.Chart.ChartElements.Note;
import org.jevis.jeconfig.application.Chart.ChartElements.TableEntry;
import org.jevis.jeconfig.application.Chart.ChartElements.XYChartSerie;
import org.jevis.jeconfig.application.Chart.Charts.MultiAxis.MultiAxisAreaChart;
import org.jevis.jeconfig.application.Chart.Charts.MultiAxis.MultiAxisChart;
import org.jevis.jeconfig.application.Chart.Charts.MultiAxis.regression.RegressionType;
import org.jevis.jeconfig.application.Chart.Charts.jfx.NumberAxis;
import org.jevis.jeconfig.application.Chart.Zoom.ChartPanManager;
import org.jevis.jeconfig.application.Chart.Zoom.JFXChartUtil;
import org.jevis.jeconfig.application.Chart.data.AnalysisDataModel;
import org.jevis.jeconfig.application.Chart.data.RowNote;
import org.jevis.jeconfig.application.tools.ColorHelper;
import org.jevis.jeconfig.dialog.NoteDialog;
import org.jevis.jeconfig.tool.I18n;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.threeten.extra.Days;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.jevis.commons.dataprocessing.ManipulationMode.RUNNING_MEAN;

public class XYChart implements Chart {
    private static final Logger logger = LogManager.getLogger(XYChart.class);
    private final Boolean showRawData;
    private final Boolean showSum;
    final int polyRegressionDegree;
    final RegressionType regressionType;
    final Boolean calcRegression;
    private final Boolean showL1L2;
    private final Integer chartId;
    Boolean hideShowIcons;
    //ObservableList<MultiAxisAreaChart.Series<Number, Number>> series = FXCollections.observableArrayList();
    private List<Color> hexColors = new ArrayList<>();
    ObservableList<TableEntry> tableData = FXCollections.observableArrayList();
    DateTime now = DateTime.now();
    AtomicReference<DateTime> timeStampOfFirstSample = new AtomicReference<>(now);
    AtomicReference<DateTime> timeStampOfLastSample = new AtomicReference<>(new DateTime(2001, 1, 1, 0, 0, 0));
    NumberAxis y1Axis = new NumberAxis();
    NumberAxis y2Axis = new NumberAxis();
    DateAxis dateAxis;
    List<ChartDataModel> chartDataModels;
    MultiAxisChart chart;
    Double minValue = Double.MAX_VALUE;
    Double maxValue = -Double.MAX_VALUE;
    boolean asDuration = false;
    private String chartName;
    private List<String> unitY1 = new ArrayList<>();
    private List<String> unitY2 = new ArrayList<>();
    List<XYChartSerie> xyChartSerieList = new ArrayList<>();
    private DateTime valueForDisplay;
    private Region areaChartRegion;
    private Period period;
    private ManipulationMode addSeriesOfType;
    private AtomicBoolean addManipulationToTitle;
    private AtomicReference<ManipulationMode> manipulationMode;
    private Boolean[] changedBoth;
    private DateTimeFormatter dtfOutLegend = DateTimeFormat.forPattern("EE. dd.MM.yyyy HH:mm");
    private ChartSettingsFunction chartSettingsFunction = new ChartSettingsFunction() {
        @Override
        public void applySetting(org.jevis.jeconfig.application.Chart.Charts.jfx.Chart chart) {

        }
    };
    private WorkDays workDays = new WorkDays(null);
    private ChartPanManager panner;
    private JFXChartUtil jfxChartUtil;

    public XYChart(AnalysisDataModel analysisDataModel, List<ChartDataModel> selectedModels, Integer chartId, String chartName) {
        this.chartDataModels = selectedModels;
        this.dateAxis = new DateAxis();
        this.showRawData = analysisDataModel.getShowRawData();
        this.showSum = analysisDataModel.getShowSum();
        this.showL1L2 = analysisDataModel.getShowL1L2();
        this.regressionType = analysisDataModel.getRegressionType();
        this.hideShowIcons = analysisDataModel.getHideShowIcons();
        this.calcRegression = analysisDataModel.calcRegression();
        this.polyRegressionDegree = analysisDataModel.getPolyRegressionDegree();
        this.chartId = chartId;
        this.chartName = chartName;
        this.addSeriesOfType = analysisDataModel.getAddSeries();
        if (!chartDataModels.isEmpty()) {
            workDays = new WorkDays(chartDataModels.get(0).getObject());
            workDays.setEnabled(analysisDataModel.isCustomWorkDay());
        }

        init();
    }

    public void init() {
        initializeChart();

        hexColors.clear();
        chart.getData().clear();
        tableData.clear();

        changedBoth = new Boolean[]{false, false};

        addManipulationToTitle = new AtomicBoolean(false);
        manipulationMode = new AtomicReference<>(ManipulationMode.NONE);

        ChartDataModel sumModelY1 = null;
        ChartDataModel sumModelY2 = null;
        for (ChartDataModel singleRow : chartDataModels) {
            if (!singleRow.getSelectedcharts().isEmpty()) {
                try {
                    if (showRawData && singleRow.getDataProcessor() != null) {
                        ChartDataModel newModel = singleRow.clone();
                        newModel.setDataProcessor(null);
                        newModel.setAttribute(null);
                        newModel.setSamples(null);
                        newModel.setUnit(null);
                        newModel.setColor(ColorHelper.toRGBCode(ColorHelper.toColor(newModel.getColor()).darker()));

                        singleRow.setAxis(0);
                        newModel.setAxis(1);
                        xyChartSerieList.add(generateSerie(changedBoth, newModel));
                    }

                    xyChartSerieList.add(generateSerie(changedBoth, singleRow));

                    if (singleRow.hasForecastData()) {
                        try {
                            XYChartSerie forecast = new XYChartSerie(singleRow, hideShowIcons, true);

                            hexColors.add(ColorHelper.toColor(ColorHelper.colorToBrighter(singleRow.getColor())));
                            xyChartSerieList.add(forecast);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (calcRegression) {
                        chart.setRegressionColor(singleRow.getAxis(), ColorHelper.toColor(singleRow.getColor()));
                        chart.setRegression(singleRow.getAxis(), regressionType, polyRegressionDegree);
                    }

                    if (showL1L2 && singleRow.getDataProcessor() != null) {
                        CleanDataObject cleanDataObject = new CleanDataObject(singleRow.getDataProcessor(), new ObjectHandler(singleRow.getObject().getDataSource()));
                        singleRow.updateScaleFactor();
                        Double scaleFactor = singleRow.getScaleFactor();
                        if (cleanDataObject.getLimitsEnabled()) {
                            List<JsonLimitsConfig> limitsConfigs = cleanDataObject.getLimitsConfig();
                            for (int i = 0; i < limitsConfigs.size(); i++) {
                                JsonLimitsConfig limitsConfig = limitsConfigs.get(i);
                                String max = limitsConfig.getMax();
                                if (max != null && !max.equals("")) {
                                    Double value = Double.parseDouble(max) * scaleFactor;
                                    List<Double> list = Arrays.asList(25d, 20d, 5d, 20d);
                                    ObservableList<Double> doubles = FXCollections.observableList(list);
                                    if (i == 0) {
                                        chart.setLimitLine("L1 MAX", value, ColorHelper.toColor(singleRow.getColor()).brighter(), singleRow.getAxis(), doubles);
                                    } else {
                                        chart.setLimitLine("L2 MAX", value, ColorHelper.toColor(singleRow.getColor()).darker(), singleRow.getAxis(), doubles);
                                    }
                                }

                                String min = limitsConfig.getMin();
                                if (min != null && !min.equals("")) {
                                    Double value = Double.parseDouble(min) * scaleFactor;
                                    List<Double> list = Arrays.asList(2d, 21d);
                                    ObservableList<Double> doubles = FXCollections.observableList(list);
                                    if (i == 0) {
                                        chart.setLimitLine("L1 MIN", value, ColorHelper.toColor(singleRow.getColor()).brighter(), singleRow.getAxis(), doubles);
                                    } else {
                                        chart.setLimitLine("L2 MIN", value, ColorHelper.toColor(singleRow.getColor()).darker(), singleRow.getAxis(), doubles);
                                    }
                                }
                            }
                        }
                    }

                    if (showSum && sumModelY1 == null) {
                        sumModelY1 = singleRow.clone();
                    }

                    if (showSum && sumModelY2 == null) {
                        sumModelY2 = singleRow.clone();
                    }

                } catch (JEVisException e) {
                    logger.error("Error: Cant create series for data rows: ", e);
                }
            }
        }

        List<ChartDataModel> sumModels = new ArrayList<>();
        sumModels.add(sumModelY1);
        sumModels.add(sumModelY2);

        if (showSum && chartDataModels.size() > 1 && sumModelY1 != null) {
            try {
                for (ChartDataModel sumModel : sumModels) {
                    int index = sumModels.indexOf(sumModel);
                    JsonObject json = new JsonObject();
                    json.setId(9999999999L);
                    json.setName("~" + I18n.getInstance().getString("plugin.graph.table.sum"));
                    if (index == 0) {
                        json.setName(json.getName() + " " + I18n.getInstance().getString("plugin.graph.chartplugin.axisbox.y1"));
                    } else {
                        json.setName(json.getName() + " " + I18n.getInstance().getString("plugin.graph.chartplugin.axisbox.y2"));
                    }
                    JEVisObject test = new JEVisObjectWS((JEVisDataSourceWS) chartDataModels.get(0).getObject().getDataSource(), json);

                    sumModel.setObject(test);
                    sumModel.setAxis(index);
                    if (index == 0) {
                        sumModel.setColor(ColorHelper.toRGBCode(Color.BLACK));
                    } else {
                        sumModel.setColor(ColorHelper.toRGBCode(Color.SADDLEBROWN));
                    }
                    Map<DateTime, JEVisSample> sumSamples = new HashMap<>();
                    boolean hasData = false;
                    for (ChartDataModel model : chartDataModels) {
                        if (model.getAxis() == index) {
                            hasData = true;
                            for (JEVisSample jeVisSample : model.getSamples()) {
                                try {
                                    DateTime ts = jeVisSample.getTimestamp();
                                    Double value = jeVisSample.getValueAsDouble();
                                    if (!sumSamples.containsKey(ts)) {
                                        JEVisSample smp = new VirtualSample(ts, value);
                                        smp.setNote("sum");
                                        sumSamples.put(ts, smp);
                                    } else {
                                        JEVisSample smp = sumSamples.get(ts);

                                        smp.setValue(smp.getValueAsDouble() + value);
                                    }
                                } catch (JEVisException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    ArrayList<JEVisSample> arrayList = new ArrayList<>(sumSamples.values());
                    arrayList.sort((o1, o2) -> {
                        try {
                            if (o1.getTimestamp().isBefore(o2.getTimestamp())) {
                                return -1;
                            } else if (o1.getTimestamp().equals(o2.getTimestamp())) {
                                return 0;
                            } else {
                                return 1;
                            }
                        } catch (JEVisException e) {
                            e.printStackTrace();
                        }
                        return -1;
                    });

                    sumModel.setSamples(arrayList);
                    sumModel.setSomethingChanged(false);

                    try {
                        if (hasData) {
                            xyChartSerieList.add(generateSerie(changedBoth, sumModel));
                        }
                    } catch (JEVisException e) {
                        e.printStackTrace();
                    }
                }
            } catch (JEVisException e) {
                logger.error("Could not generate sum of data rows: ", e);
            }
        }

        addSeriesToChart();

        if (asDuration) {
            dateAxis.setFirstTS(timeStampOfFirstSample.get());
            dateAxis.setAsDuration(true);
        }

        generateXAxis(changedBoth);

        generateYAxis();

        getChart().setStyle("-fx-font-size: " + 12 + "px;");

        getChart().setAnimated(false);

        getChart().setTitle(getUpdatedChartName());

        getChart().setLegendVisible(false);
        //((javafx.scene.chart.XYChart)getChart()).setCreateSymbols(true);

        chartSettingsFunction.applySetting(getChart());

        initializeZoom();

        Platform.runLater(() -> updateTable(null, timeStampOfFirstSample.get()));
    }

    public void addSeriesToChart() {
        for (XYChartSerie xyChartSerie : xyChartSerieList) {
            int index = xyChartSerieList.indexOf(xyChartSerie);
            Platform.runLater(() -> {
                if (showSum && index < xyChartSerieList.size() - 2) {
                    xyChartSerie.getSerie().getData().forEach(numberNumberData -> {
                        MultiAxisChart.Data node = (MultiAxisChart.Data) numberNumberData;
                        node.setExtraValue(0);
                    });
                }
                chart.getData().add(xyChartSerie.getSerie());
                tableData.add(xyChartSerie.getTableEntry());
            });
        }
    }

    public void initializeChart() {
        setChart(new MultiAxisAreaChart(dateAxis, y1Axis, y2Axis));
    }

    public XYChartSerie generateSerie(Boolean[] changedBoth, ChartDataModel singleRow) throws JEVisException {
        XYChartSerie serie = new XYChartSerie(singleRow, hideShowIcons, false);

        hexColors.add(ColorHelper.toColor(singleRow.getColor()));

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
            XYChartSerie serie2 = new XYChartSerie(singleRow, hideShowIcons, false);

            hexColors.add(ColorHelper.toColor(singleRow.getColor()).darker());

            chart.getData().add(serie2.getSerie());

            tableData.add(serie2.getTableEntry());

            singleRow.setManipulationMode(oldMode);
        }
    }

    @Override
    public void setTitle(String chartName) {
        this.chartName = chartName;
        getChart().setTitle(getUpdatedChartName());
    }


    @Override
    public void setChartSettings(ChartSettingsFunction function) {
        chartSettingsFunction = function;
    }

    public void generateYAxis() {
        y1Axis.setAutoRanging(true);
        y2Axis.setAutoRanging(true);

        for (XYChartSerie serie : xyChartSerieList) {
            if (serie.getUnit() != null) {
                String currentUnit = UnitManager.getInstance().format(serie.getUnit());
                if (serie.getyAxis() == 0) {
                    if (!unitY1.contains(currentUnit)) {
                        unitY1.add(currentUnit);
                    }
                } else if (serie.getyAxis() == 1) {
                    if (!unitY2.contains(currentUnit)) {
                        unitY2.add(currentUnit);
                    }
                }
            } else {
                logger.warn("Row has no unit");
            }
        }

        if (chartDataModels != null && chartDataModels.size() > 0) {

            if (unitY1.isEmpty() && unitY2.isEmpty()) {
                unitY1.add(I18n.getInstance().getString("plugin.graph.chart.valueaxis.nounit"));
                unitY2.add(I18n.getInstance().getString("plugin.graph.chart.valueaxis.nounit"));
            } else if (unitY1.isEmpty()) {
                unitY1.add(I18n.getInstance().getString("plugin.graph.chart.valueaxis.nounit"));
            }

        }

        StringBuilder allUnitsY1 = new StringBuilder();
        for (String s : unitY1) {
            if (unitY1.indexOf(s) == 0) allUnitsY1.append(s);
            else allUnitsY1.append(", ").append(s);
        }

        StringBuilder allUnitsY2 = new StringBuilder();
        for (String s : unitY2) {
            if (unitY2.indexOf(s) == 0) allUnitsY2.append(s);
            else allUnitsY2.append(", ").append(s);
        }

        if (!unitY1.isEmpty()) y1Axis.setLabel(allUnitsY1.toString());
        if (!unitY2.isEmpty()) y2Axis.setLabel(allUnitsY2.toString());

    }

    public void generateXAxis(Boolean[] changedBoth) {
        dateAxis.setAutoRanging(false);
        dateAxis.setUpperBound((double) chartDataModels.get(0).getSelectedEnd().getMillis());
        dateAxis.setLowerBound((double) chartDataModels.get(0).getSelectedStart().getMillis());

        setTickUnitFromPeriod(dateAxis);

        if (!asDuration) dateAxis.setAsDuration(false);
        else {
            dateAxis.setAsDuration(true);
            dateAxis.setFirstTS(timeStampOfFirstSample.get());
        }

        Period realPeriod = Period.minutes(15);
        if (chartDataModels != null && chartDataModels.size() > 0) {

            if (chartDataModels.get(0).getSamples().size() > 1) {
                try {
                    List<JEVisSample> samples = chartDataModels.get(0).getSamples();
                    period = new Period(samples.get(0).getTimestamp(),
                            samples.get(1).getTimestamp());
                    timeStampOfFirstSample.set(samples.get(0).getTimestamp());
                    timeStampOfLastSample.set(samples.get(samples.size() - 1).getTimestamp());
                    realPeriod = new Period(samples.get(0).getTimestamp(),
                            samples.get(1).getTimestamp());
                } catch (JEVisException e) {
                    e.printStackTrace();
                }
            }
        }

        updateXAxisLabel(timeStampOfFirstSample.get(), timeStampOfLastSample.get());
    }

    public void setTickUnitFromPeriod(DateAxis dateAxis) {
        double millisMinor = 60000;
        double millisMajor = 900000;
        String format = "";

        AggregationPeriod period = chartDataModels.get(0).getAggregationPeriod();
        AggregationPeriod durationPeriod = AggregationPeriod.NONE;
        Long millisDuration = timeStampOfLastSample.get().getMillis() - timeStampOfFirstSample.get().getMillis();
        if (millisDuration >= Period.days(1).toStandardDuration().getMillis() * (365.25 - Days.of(60).getAmount()) ) {
            durationPeriod = AggregationPeriod.MONTHLY;
        } else if (millisDuration >= Period.days(1).toStandardDuration().getMillis() * 30.4375) {
            durationPeriod = AggregationPeriod.WEEKLY;
        } else if (millisDuration >= Period.days(4).toStandardDuration().getMillis()) {
            durationPeriod = AggregationPeriod.DAILY;
        } else if (millisDuration >= Period.hours(8).toStandardDuration().getMillis()) {
            durationPeriod = AggregationPeriod.HOURLY;
        }


        if (durationPeriod == period) {
        } else if (durationPeriod == AggregationPeriod.NONE) {
            period = durationPeriod;
        } else if (durationPeriod == AggregationPeriod.HOURLY && period == AggregationPeriod.NONE) {
            period = AggregationPeriod.HOURLY;
        } else if (durationPeriod == AggregationPeriod.DAILY &&
                (period == AggregationPeriod.NONE || period == AggregationPeriod.HOURLY)) {
            period = AggregationPeriod.DAILY;
        } else if (durationPeriod == AggregationPeriod.WEEKLY &&
                (period == AggregationPeriod.NONE || period == AggregationPeriod.HOURLY || period == AggregationPeriod.DAILY)) {
            period = AggregationPeriod.WEEKLY;
        } else if (durationPeriod == AggregationPeriod.MONTHLY &&
                (period == AggregationPeriod.NONE || period == AggregationPeriod.HOURLY || period == AggregationPeriod.DAILY || period == AggregationPeriod.WEEKLY)) {
            period = AggregationPeriod.MONTHLY;
        }


        switch (period) {
            case NONE:
                millisMinor = (double) Period.minutes(1).toStandardDuration().getMillis();
                millisMajor = (double) Period.minutes(15).toStandardDuration().getMillis();
                format = "EEE, " + DateTimeFormat.patternForStyle("SS", I18n.getInstance().getLocale());
                break;
            case HOURLY:
                millisMinor = (double) Period.minutes(15).toStandardDuration().getMillis();
                millisMajor = (double) Period.hours(1).toStandardDuration().getMillis();
                format = "EEE, " + DateTimeFormat.patternForStyle("SS", I18n.getInstance().getLocale());
                break;
            case DAILY:
                millisMinor = (double) Period.hours(1).toStandardDuration().getMillis();
                millisMajor = (double) Period.days(1).toStandardDuration().getMillis();
                format = "EEE, " + DateTimeFormat.patternForStyle("M-", I18n.getInstance().getLocale());
                break;
            case WEEKLY:
                System.out.println("Wekk ....");
                millisMinor = (double) Period.days(1).toStandardDuration().getMillis();
                millisMajor = (double) Period.weeks(1).toStandardDuration().getMillis();
                format = "EEE, " + DateTimeFormat.patternForStyle("L-", I18n.getInstance().getLocale());
                break;
            case MONTHLY:
                millisMinor = (double) Period.weeks(1).toStandardDuration().getMillis();
                millisMajor = (double) Period.days(1).toStandardDuration().getMillis() * 30.4375;
                format = "EEE, " + DateTimeFormat.patternForStyle("L-", I18n.getInstance().getLocale());
                break;
            case QUARTERLY:
                millisMinor = (double) Period.days(1).toStandardDuration().getMillis() * 30.4375;
                millisMajor = (double) Period.days(1).toStandardDuration().getMillis() * 30.4375 * 4;
                format = "EEE, " + DateTimeFormat.patternForStyle("L-", I18n.getInstance().getLocale());
                break;
            case YEARLY:
                millisMinor = (double) Period.days(1).toStandardDuration().getMillis() * 30.4375;
                millisMajor = (double) Period.days(1).toStandardDuration().getMillis() * 365.25;
                format = "EEE, " + DateTimeFormat.patternForStyle("L-", I18n.getInstance().getLocale());
                break;
        }

        dateAxis.setMinorTickUnit(millisMinor);
        dateAxis.setTickUnit(millisMajor);
        dateAxis.setCurrentFormatterProperty(format);
    }

    public void updateXAxisLabel(DateTime firstTS, DateTime lastTS) {

        String overall = String.format("%s %s %s",
                dtfOutLegend.print(firstTS),
                I18n.getInstance().getString("plugin.graph.chart.valueaxis.until"),
                dtfOutLegend.print(lastTS));

        dateAxis.setLabel(I18n.getInstance().getString("plugin.graph.chart.dateaxis.title") + " " + overall);
    }

    private Period removeWorkdayInterval(DateTime workStart, DateTime workEnd) {
//        System.out.println("workStart.before÷ " + workStart + "|" + workEnd);
        if (workDays.getWorkdayStart().isAfter(workDays.getWorkdayEnd())) {
            workStart = workStart.plusDays(1);
        }

        workStart = workStart.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
        DateTime workEnd2 = workEnd.plusDays(1).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
//        System.out.println("workStart.after÷ " + workStart + "|" + workEnd2);
        return new Period(workStart, workEnd2);
    }

    @Override
    public void initializeZoom() {
        panner = null;

        getChart().setOnMouseMoved(mouseEvent -> {
            updateTable(mouseEvent, null);
        });

        panner = new ChartPanManager((MultiAxisChart<?, ?>) getChart());

        panner.setMouseFilter(mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.SECONDARY
                    && (mouseEvent.getButton() != MouseButton.PRIMARY
                    || !mouseEvent.isShortcutDown())) {
                mouseEvent.consume();
            }
        });
        panner.start();

        jfxChartUtil = new JFXChartUtil(chartDataModels.get(0).getSelectedStart().getMillis(), chartDataModels.get(0).getSelectedEnd().getMillis());

        jfxChartUtil.addDoublePrimaryClickAutoRangeHandler((MultiAxisChart<?, ?>) getChart());

    }

    @Override
    public JFXChartUtil getJfxChartUtil() {
        return jfxChartUtil;
    }

    @Override
    public void setRegion(Region region) {
        areaChartRegion = region;
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
        return timeStampOfFirstSample.get();
    }

    @Override
    public DateTime getEndDateTime() {
        return timeStampOfLastSample.get();
    }

    @Override
    public void updateChart() {
        timeStampOfFirstSample.set(DateTime.now());
        timeStampOfLastSample.set(new DateTime(2001, 1, 1, 0, 0, 0));
        changedBoth = new Boolean[]{false, false};
        //xyChartSerieList.clear();
        //series.clear();
        //tableData.clear();
        unitY1.clear();
        unitY2.clear();
        //hexColors.clear();
        int chartDataModelsSize = chartDataModels.size();
        int xyChartSerieListSize = xyChartSerieList.size();

        if (chartDataModelsSize > 0) {
            if (chartDataModelsSize <= xyChartSerieListSize) {
                /**
                 * if the new chart data model contains fewer or equal times the chart series as the old one
                 */

                if (chartDataModelsSize < xyChartSerieListSize && !showRawData) {
                    /**
                     * remove the series which are in excess
                     */

                    xyChartSerieList.subList(chartDataModelsSize, xyChartSerieListSize).clear();
                    chart.getData().subList(chartDataModelsSize, xyChartSerieListSize).clear();
                    hexColors.subList(chartDataModelsSize, xyChartSerieListSize).clear();
                    tableData.subList(chartDataModelsSize, xyChartSerieListSize).clear();

                }

                timeStampOfFirstSample = new AtomicReference<>(DateTime.now());
                timeStampOfLastSample = new AtomicReference<>(new DateTime(2001, 1, 1, 0, 0, 0));
                updateXYChartSeries();

            } else {
                /**
                 * if there are more new data rows then old ones
                 */
                timeStampOfFirstSample = new AtomicReference<>(DateTime.now());
                timeStampOfLastSample = new AtomicReference<>(new DateTime(2001, 1, 1, 0, 0, 0));

                updateXYChartSeries();

                for (int i = xyChartSerieListSize; i < chartDataModelsSize; i++) {
                    try {
                        xyChartSerieList.add(generateSerie(changedBoth, chartDataModels.get(i)));
                    } catch (JEVisException e) {
                        e.printStackTrace();
                    }
                }
            }

            Platform.runLater(() -> {
                chart.getData().forEach(serie -> ((MultiAxisChart.Series) serie).getData().forEach(numberNumberData -> {
                    if (((MultiAxisChart.Data) numberNumberData).getNode() != null)
                        if (((MultiAxisChart.Data) numberNumberData).getNode().getClass().equals(HBox.class)) {
                            ((MultiAxisChart.Data) numberNumberData).getNode().setVisible(hideShowIcons);
                        }
                }));
            });

            applyColors();

            generateXAxis(changedBoth);
            generateYAxis();

            getChart().setTitle(getUpdatedChartName());
            getChart().layout();
        }

    }

    private void updateXYChartSeries() {
        for (int i = 0; i < xyChartSerieList.size(); i++) {
            XYChartSerie xyChartSerie = xyChartSerieList.get(i);

            ChartDataModel model = null;
            if (!showRawData) {
                model = chartDataModels.get(i);
            } else {
                for (ChartDataModel mdl : chartDataModels) {
                    if (mdl.getObject().equals(xyChartSerie.getSingleRow().getObject())) {
                        model = mdl;
                        break;
                    }
                }
            }

            if (model != null) {
                hexColors.set(i, ColorHelper.toColor(model.getColor()));
                xyChartSerie.setSingleRow(model);
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
                    checkManipulation(model);
                } catch (JEVisException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    String getUpdatedChartName() {
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
        return chartId;
    }

    @Override
    public void updateTable(MouseEvent mouseEvent, DateTime valueForDisplay) {
        Point2D mouseCoordinates = null;
        if (mouseEvent != null) mouseCoordinates = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());
        Double x = null;
        if (valueForDisplay == null) {

            x = ((MultiAxisChart) getChart()).getXAxis().sceneToLocal(Objects.requireNonNull(mouseCoordinates)).getX();

            valueForDisplay = ((DateAxis) ((MultiAxisChart) getChart()).getXAxis()).getDateTimeForDisplay(x);

        }
        if (valueForDisplay != null) {
            setValueForDisplay(valueForDisplay);
            DateTime finalValueForDisplay = valueForDisplay;
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);

            xyChartSerieList.parallelStream().forEach(serie -> {
                try {

                    TableEntry tableEntry = serie.getTableEntry();
                    TreeMap<DateTime, JEVisSample> sampleTreeMap = serie.getSampleMap();

                    DateTime nearest = null;
                    if (sampleTreeMap.get(finalValueForDisplay) != null) {
                        nearest = finalValueForDisplay;
                    } else {
                        nearest = sampleTreeMap.lowerKey(finalValueForDisplay);
                    }

                    JEVisSample sample = sampleTreeMap.get(nearest);

                    Note formattedNote = new Note(sample, serie.getSingleRow().getNoteSamples().get(sample.getTimestamp()));

                    if (!asDuration) {
                        DateTime finalNearest = nearest;
                        Platform.runLater(() -> {
                            if (finalNearest != null) {
                                tableEntry.setDate(finalNearest
                                        .toString(DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")));
                            } else tableEntry.setValue("-");
                        });
                    } else {
                        DateTime finalNearest1 = nearest;
                        Platform.runLater(() -> tableEntry.setDate((finalNearest1.getMillis() -
                                timeStampOfFirstSample.get().getMillis()) / 1000 / 60 / 60 + " h"));
                    }
                    Platform.runLater(() -> tableEntry.setNote(formattedNote.getNoteAsString()));
                    String unit = serie.getUnit();

                    if (!sample.getNote().contains("Zeros")) {
                        Double valueAsDouble = null;
                        String formattedDouble = null;
                        if (!serie.getSingleRow().isStringData()) {
                            valueAsDouble = sample.getValueAsDouble();
                            formattedDouble = nf.format(valueAsDouble);
                            String finalFormattedDouble = formattedDouble;
                            Platform.runLater(() -> tableEntry.setValue(finalFormattedDouble + " " + unit));
                        } else {
                            Platform.runLater(() -> {
                                try {
                                    tableEntry.setValue(sample.getValueAsString() + " " + unit);
                                } catch (JEVisException e) {
                                    e.printStackTrace();
                                }
                            });
                        }

                    } else Platform.runLater(() -> tableEntry.setValue("- " + unit));

//                    tableEntry.setPeriod(getPeriod().toString(PeriodFormat.wordBased().withLocale(I18n.getInstance().getLocale())));

                } catch (Exception ex) {
//                    ex.printStackTrace();
                }

            });
        }
    }

    @Override
    public void updateTableZoom(Long lowerBound, Long upperBound) {
        DateTime lower = new DateTime(lowerBound);
        DateTime upper = new DateTime(upperBound);

        xyChartSerieList.parallelStream().forEach(serie -> {
            try {

                double min = Double.MAX_VALUE;
                double max = -Double.MAX_VALUE;
                double avg = 0.0;
                Double sum = 0.0;
                long zeroCount = 0;

                List<JEVisSample> samples = serie.getSingleRow().getSamples();
                List<JEVisSample> newList = new ArrayList<>();
                JEVisUnit unit = serie.getSingleRow().getUnit();

                for (JEVisSample smp : samples) {
                    if ((smp.getTimestamp().equals(lower) || smp.getTimestamp().isAfter(lower)) && smp.getTimestamp().isBefore(upper)) {

                        newList.add(smp);
                        Double currentValue = smp.getValueAsDouble();

                        if (!smp.getNote().contains("Zeros")) {
                            min = Math.min(min, currentValue);
                            max = Math.max(max, currentValue);
                            sum += currentValue;
                        } else {
                            zeroCount++;
                        }
                    }
                }

                if (manipulationMode.get().equals(ManipulationMode.CUMULATE)) {
                    avg = max / samples.size();
                    sum = max;
                }

                double finalMin = min;
                double finalMax = max;
                Double finalSum = sum;
                long finalZeroCount = zeroCount;
                double finalAvg = avg;
                try {
                    serie.updateTableEntry(newList, unit, finalMin, finalMax, finalAvg, finalSum, finalZeroCount);
                } catch (JEVisException e) {
                    logger.error("Could not update Table Entry for {}", serie.getSingleRow().getObject().getName(), e);
                }


            } catch (Exception ex) {
            }

        });

        Platform.runLater(() -> updateXAxisLabel(lower, upper));
    }

    @Override
    public void showNote(MouseEvent mouseEvent) {
        if (manipulationMode.get().equals(ManipulationMode.NONE)) {

            Point2D mouseCoordinates = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            double x = ((MultiAxisChart) getChart()).getXAxis().sceneToLocal(mouseCoordinates).getX();

            Map<String, RowNote> map = new HashMap<>();
            DateTime valueForDisplay = null;
            valueForDisplay = ((DateAxis) ((MultiAxisChart) getChart()).getXAxis()).getDateTimeForDisplay(x);

            for (XYChartSerie serie : xyChartSerieList) {
                try {
                    DateTime nearest = serie.getSampleMap().lowerKey(valueForDisplay);

                    if (nearest != null) {

                        JEVisSample nearestSample = serie.getSampleMap().get(nearest);

                        String title = "";
                        title += serie.getSingleRow().getObject().getName();

                        JEVisObject dataObject;
                        if (serie.getSingleRow().getDataProcessor() != null)
                            dataObject = serie.getSingleRow().getDataProcessor();
                        else dataObject = serie.getSingleRow().getObject();

                        String userNote = getUserNoteForTimeStamp(nearestSample, nearestSample.getTimestamp());
                        String userValue = getUserValueForTimeStamp(nearestSample, nearestSample.getTimestamp());

                        System.out.println("NoteEditor.nearestSample: " + nearestSample);
                        System.out.println("NoteEditor.noteSample   :" + serie.getSingleRow().getNoteSamples().get(nearestSample.getTimestamp()));
                        RowNote rowNote = new RowNote(dataObject, nearestSample, serie.getSingleRow().getNoteSamples().get(nearestSample.getTimestamp()), title, userNote, userValue, serie.getSingleRow().getScaleFactor());

                        map.put(title, rowNote);
                    }
                } catch (Exception ex) {
                    logger.error("Error: could not get note", ex);
                }
            }

            NoteDialog nd = new NoteDialog(map);

            nd.showAndWait().ifPresent(response -> {
                if (response.getButtonData().getTypeCode().equals(ButtonType.OK.getButtonData().getTypeCode())) {
                    saveUserEntries(nd.getNoteMap());
                }
            });
        }
    }


    @Override
    public void applyColors() {

        for (int i = 0; i < hexColors.size(); i++) {
            Color currentColor = hexColors.get(i);
            Color brighter = currentColor.deriveColor(1, 1, 50, 0.3);
            String hexColor = ColorHelper.toRGBCode(currentColor);
            String hexBrighter = ColorHelper.toRGBCode(brighter) + "55";
            String preIdent = ".default-color" + i;
            Node node = getChart().lookup(preIdent + ".chart-series-area-fill");
            Node nodew = getChart().lookup(preIdent + ".chart-series-area-line");

            if (node != null) {
                node.setStyle("-fx-fill: linear-gradient(" + hexColor + "," + hexBrighter + ");"
                        + "  -fx-background-insets: 0 0 -1 0, 0, 1, 2;"
                        + "  -fx-background-radius: 3px, 3px, 2px, 1px;");
            }
            if (nodew != null) {
                nodew.setStyle("-fx-stroke: " + hexColor + "; -fx-stroke-width: 2px; ");
            }
        }
    }

    @Override
    public DateTime getValueForDisplay() {
        return valueForDisplay;
    }

    @Override
    public void setValueForDisplay(DateTime valueForDisplay) {
        this.valueForDisplay = valueForDisplay;
    }

    @Override
    public org.jevis.jeconfig.application.Chart.Charts.jfx.Chart getChart() {
        return chart;
    }

    public void setChart(MultiAxisChart chart) {
        this.chart = chart;
    }

    @Override
    public Region getRegion() {
        return areaChartRegion;
    }

    @Override
    public void checkForY2Axis() {
        try {
            boolean hasY2Axis = false;
            for (XYChartSerie serie : xyChartSerieList) {
                if (serie.getyAxis() == 1) {
                    hasY2Axis = true;
                    break;
                }
            }

            if (!hasY2Axis) y2Axis.setVisible(false);
            else y2Axis.setVisible(true);
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    @Override
    public void applyBounds() {
        dateAxis.setUpperBound((double) chartDataModels.get(0).getSelectedEnd().getMillis());
        dateAxis.setLowerBound((double) chartDataModels.get(0).getSelectedStart().getMillis());
    }

    @Override
    public List<ChartDataModel> getChartDataModels() {
        return chartDataModels;
    }

    @Override
    public ChartPanManager getPanner() {
        return panner;
    }

    public Double getMinValue() {
        return minValue;
    }

    public void setMinValue(Double minValue) {
        this.minValue = minValue;
    }

    public Double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
    }

    public List<Color> getHexColors() {
        return hexColors;
    }

    public List<String> getUnitY1() {
        return unitY1;
    }
}