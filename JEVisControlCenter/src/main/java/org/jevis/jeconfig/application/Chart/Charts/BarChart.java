package org.jevis.jeconfig.application.Chart.Charts;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisException;
import org.jevis.commons.chart.ChartDataModel;
import org.jevis.commons.dataprocessing.ManipulationMode;
import org.jevis.commons.i18n.I18n;
import org.jevis.commons.unit.UnitManager;
import org.jevis.jeconfig.application.Chart.ChartElements.BarChartSerie;
import org.jevis.jeconfig.application.Chart.ChartElements.TableEntry;
import org.jevis.jeconfig.application.Chart.ChartType;
import org.jevis.jeconfig.application.tools.ColorHelper;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class BarChart implements Chart {
    private static final Logger logger = LogManager.getLogger(BarChart.class);
    private final Integer chartId;
    AtomicReference<DateTime> timeStampOfFirstSample = new AtomicReference<>(DateTime.now());
    AtomicReference<DateTime> timeStampOfLastSample = new AtomicReference<>(new DateTime(2001, 1, 1, 0, 0, 0));
    NumberAxis y1Axis = new NumberAxis();
    NumberAxis y2Axis = new NumberAxis();
    private String chartName;
    private String unit;
    private List<ChartDataModel> chartDataModels;
    private Boolean hideShowIcons;
    private List<BarChartSerie> barChartSerieList = new ArrayList<>();
    private de.gsi.chart.XYChart barChart;
    private List<Color> hexColors = new ArrayList<>();
    private DateTime valueForDisplay;
    private ObservableList<TableEntry> tableData = FXCollections.observableArrayList();
    private Region barChartRegion;
    private Period period;
    private Region areaChartRegion;
    private boolean asDuration = false;
    private AtomicReference<ManipulationMode> manipulationMode;
    private DateTime nearest;
    private ChartType chartType = ChartType.BAR;

    public BarChart(List<ChartDataModel> chartDataModels, Boolean hideShowIcons, Integer chartId, String chartName) {
        this.chartDataModels = chartDataModels;
        this.hideShowIcons = hideShowIcons;
        this.chartId = chartId;
        this.chartName = chartName;
        init();
    }

    private void init() {
        manipulationMode = new AtomicReference<>(ManipulationMode.NONE);

        chartDataModels.forEach(singleRow -> {
            if (!singleRow.getSelectedcharts().isEmpty()) {
                try {
                    BarChartSerie serie = new BarChartSerie(singleRow, hideShowIcons);
                    barChartSerieList.add(serie);
                    hexColors.add(ColorHelper.toColor(singleRow.getColor()));

                } catch (JEVisException e) {
                    e.printStackTrace();
                }
            }
        });

        if (chartDataModels != null && chartDataModels.size() > 0) {
            unit = UnitManager.getInstance().format(chartDataModels.get(0).getUnit());
            if (unit.equals("")) unit = I18n.getInstance().getString("plugin.graph.chart.valueaxis.nounit");
        }

        DefaultNumericAxis numberAxis = new DefaultNumericAxis();
        de.gsi.chart.axes.spi.CategoryAxis catAxis = new de.gsi.chart.axes.spi.CategoryAxis();

        barChart = new XYChart(numberAxis, catAxis);

        barChart.setTitle(chartName);
        barChart.setLegendVisible(false);
        barChart.getXAxis().setAutoRanging(true);
        //barChart.getXAxis().setLabel(I18n.getInstance().getString("plugin.graph.chart.dateaxis.title"));
//        barChart.getXAxis().setTickLabelRotation(-90);
        barChart.getXAxis().setName(unit);

        //initializeZoom();
//        setTimer();
        addSeriesToChart();
    }

    public void addSeriesToChart() {
        for (BarChartSerie barChartSerie : barChartSerieList) {
            Platform.runLater(() -> {
                barChart.getDatasets().add(barChartSerie.getDataSet());
                tableData.add(barChartSerie.getTableEntry());
            });
        }
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
    public void setRegion(Region region) {
        barChartRegion = region;
    }

    @Override
    public List<ChartDataModel> getChartDataModels() {
        return chartDataModels;
    }

    @Override
    public String getChartName() {
        return chartName;
    }

    @Override
    public void setTitle(String s) {

    }

    @Override
    public Integer getChartId() {
        return chartId;
    }

    @Override
    public void updateTable(MouseEvent mouseEvent, DateTime valueForDisplay) {

    }

    @Override
    public void updateTableZoom(double lowerBound, double upperBound) {

    }

    @Override
    public void applyColors() {
        for (int i = 0; i < hexColors.size(); i++) {
            Color currentColor = hexColors.get(i);
            String hexColor = ColorHelper.toRGBCode(currentColor);
            String preIdent = ".default-color" + i;
            Node node = barChart.lookup(preIdent + ".chart-bar");
            if (node != null) {
                node.setStyle("-fx-bar-fill: " + hexColor + ";");
            }
        }
    }

    @Override
    public de.gsi.chart.XYChart getChart() {
        return barChart;
    }

    @Override
    public ChartType getChartType() {
        return chartType;
    }

    @Override
    public Region getRegion() {
        return barChartRegion;
    }


}