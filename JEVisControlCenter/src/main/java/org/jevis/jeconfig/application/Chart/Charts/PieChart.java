package org.jevis.jeconfig.application.Chart.Charts;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisSample;
import org.jevis.api.JEVisUnit;
import org.jevis.commons.chart.ChartDataModel;
import org.jevis.commons.dataprocessing.ManipulationMode;
import org.jevis.commons.unit.ChartUnits.QuantityUnits;
import org.jevis.commons.unit.UnitManager;
import org.jevis.jeconfig.application.Chart.ChartElements.TableEntry;
import org.jevis.jeconfig.application.Chart.Zoom.ChartPanManager;
import org.jevis.jeconfig.application.Chart.Zoom.JFXChartUtil;
import org.jevis.jeconfig.tool.I18n;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;


public class PieChart implements Chart {
    private static final Logger logger = LogManager.getLogger(PieChart.class);
    private final Integer chartId;
    private String chartName;
    private String unit;
    private List<ChartDataModel> chartDataModels;
    private Boolean hideShowIcons;
    private ObservableList<javafx.scene.chart.PieChart.Data> series = FXCollections.observableArrayList();
    private javafx.scene.chart.PieChart pieChart;
    private List<Color> hexColors = new ArrayList<>();
    private DateTime valueForDisplay;
    private ObservableList<TableEntry> tableData = FXCollections.observableArrayList();
    private Region pieChartRegion;
    private Period period;
    private boolean legendMode = false;
    private ChartSettingsFunction chartSettingsFunction = new ChartSettingsFunction() {
        @Override
        public void applySetting(javafx.scene.chart.Chart chart) {

        }
    };

    public PieChart(List<ChartDataModel> chartDataModels, Boolean hideShowIcons, Integer chartId, String chartName) {
        this.chartDataModels = chartDataModels;
        this.hideShowIcons = hideShowIcons;
        this.chartName = chartName;
        this.chartId = chartId;
        init();
    }

    private void init() {
        List<Double> listSumsPiePieces = new ArrayList<>();
        List<String> listTableEntryNames = new ArrayList<>();

        if (chartDataModels != null && chartDataModels.size() > 0) {
            unit = UnitManager.getInstance().format(chartDataModels.get(0).getUnit());
            if (unit.equals("")) unit = I18n.getInstance().getString("plugin.graph.chart.valueaxis.nounit");
            period = chartDataModels.get(0).getAttribute().getDisplaySampleRate();
        }

        hexColors.clear();
        if (chartDataModels != null) {
            for (ChartDataModel singleRow : chartDataModels) {
                if (!singleRow.getSelectedcharts().isEmpty()) {
                    Double sumPiePiece = 0d;
                    QuantityUnits qu = new QuantityUnits();
                    boolean isQuantity = qu.isQuantityUnit(singleRow.getUnit());

                    List<JEVisSample> samples = singleRow.getSamples();
                    int samplecount = samples.size();
                    for (JEVisSample sample : samples) {
                        try {
                            sumPiePiece += sample.getValueAsDouble();
                        } catch (JEVisException e) {
                            logger.error(e);
                        }
                    }

                    if (qu.isSumCalculable(singleRow.getUnit()) && singleRow.getManipulationMode().equals(ManipulationMode.NONE)) {
                        try {
                            Period p = new Period(samples.get(0).getTimestamp(), samples.get(1).getTimestamp());
                            double factor = Period.hours(1).toStandardDuration().getMillis() / p.toStandardDuration().getMillis();
                            sumPiePiece = sumPiePiece / factor;
                        } catch (Exception e) {
                            logger.error("Couldn't calculate periods");
                            sumPiePiece = 0d;
                        }
                    } else if (!isQuantity) {
                        sumPiePiece = sumPiePiece / samplecount;
                    }

                    listSumsPiePieces.add(sumPiePiece);
                    listTableEntryNames.add(singleRow.getObject().getName());
                    hexColors.add(singleRow.getColor());
                }
            }
        }

        Double whole = 0d;
        List<Double> listPercentages = new ArrayList<>();
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        for (Double d : listSumsPiePieces) whole += d;
        for (Double d : listSumsPiePieces) listPercentages.add(d / whole);

        series.clear();
        for (String name : listTableEntryNames) {
            QuantityUnits qu = new QuantityUnits();
            JEVisUnit currentUnit = chartDataModels.get(listTableEntryNames.indexOf(name)).getUnit();
            String currentUnitString = "";
            if (qu.isQuantityUnit(currentUnit)) currentUnitString = getUnit(currentUnit);
            else currentUnitString = getUnit(qu.getSumUnit(currentUnit));
            String seriesName = name + " - " + nf.format(listSumsPiePieces.get(listTableEntryNames.indexOf(name)))
                    + " " + currentUnitString
                    + " (" + nf.format(listPercentages.get(listTableEntryNames.indexOf(name)) * 100) + " %)";

            javafx.scene.chart.PieChart.Data data = new javafx.scene.chart.PieChart.Data(seriesName, listSumsPiePieces.get(listTableEntryNames.indexOf(name)));
            series.add(data);

        }


        if (pieChart == null) {
            pieChart = new javafx.scene.chart.PieChart(series);
        }

        pieChart.setTitle(chartName);
        pieChart.setLegendVisible(false);
        pieChart.applyCss();

        applyColors();


        chartSettingsFunction.applySetting(pieChart);


    }

    @Override
    public void setChartSettings(ChartSettingsFunction function) {
        this.chartSettingsFunction = function;
    }


    public void addToolTipText() {
        final Label caption = new Label("");
        caption.setTextFill(Color.DARKORANGE);
        caption.setStyle("-fx-font: 24 arial;");

        for (final javafx.scene.chart.PieChart.Data data : series) {
            data.getNode().addEventHandler(MouseEvent.MOUSE_PRESSED, //--> is null weil noch nicht da
                    new EventHandler<MouseEvent>() {
                        @Override
                        public void handle(MouseEvent e) {
                            caption.setTranslateX(e.getSceneX());
                            caption.setTranslateY(e.getSceneY());
                            caption.setText(data.getPieValue() + "%");
                        }
                    });
        }
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
        init();
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
    public ChartPanManager getPanner() {
        return null;
    }

    @Override
    public JFXChartUtil getJfxChartUtil() {
        return null;
    }

    @Override
    public String getChartName() {
        return chartName;
    }

    @Override
    public void setTitle(String s) {
        chartName = s;
    }

    @Override
    public Integer getChartId() {
        return null;
    }

    @Override
    public void updateTable(MouseEvent mouseEvent, DateTime valueForDisplay) {

    }

    @Override
    public void showNote(MouseEvent mouseEvent) {

    }

    @Override
    public void applyColors() {

        for (int i = 0; i < hexColors.size(); i++) {

            Color currentColor = hexColors.get(i);
            String hexColor = toRGBCode(currentColor);
            String preIdent = ".default-color" + i;
            Node node = pieChart.lookup(preIdent + ".chart-pie");
            node.setStyle("-fx-pie-color: " + hexColor + ";");
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
    public DateTime getValueForDisplay() {
        return null;
    }

    @Override
    public void setValueForDisplay(DateTime valueForDisplay) {
        this.valueForDisplay = valueForDisplay;
    }

    @Override
    public javafx.scene.chart.Chart getChart() {
        return pieChart;
    }

    @Override
    public Region getRegion() {
        return pieChartRegion;
    }

    @Override
    public void initializeZoom() {

    }

    public String getUnit(JEVisUnit jeVisUnit) {

        String unit = "";
        if (jeVisUnit != null) {
            unit = UnitManager.getInstance().format(jeVisUnit);
            if (unit.equals("")) unit = jeVisUnit.getLabel();
        }

        if (unit.equals("")) unit = I18n.getInstance().getString("plugin.graph.chart.valueaxis.nounit");

        return unit;
    }

    public void setLegendMode(boolean enable) {
        legendMode = enable;
    }

}
