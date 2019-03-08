package org.jevis.jeconfig.plugin.Dashboard.widget;

import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.chart.PieChart;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisDataSource;
import org.jevis.commons.dataprocessing.ManipulationMode;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.application.Chart.ChartDataModel;
import org.jevis.jeconfig.application.Chart.Charts.LineChart;
import org.jevis.jeconfig.application.Chart.Charts.MultiAxis.MultiAxisLineChart;
import org.jevis.jeconfig.plugin.Dashboard.config.WidgetConfig;
import org.jevis.jeconfig.plugin.Dashboard.datahandler.DataModelDataHandler;
import org.joda.time.Interval;

public class ChartWidget extends Widget {

    private static final Logger logger = LogManager.getLogger(PieChart.class);
    public static String WIDGET_ID = "Chart";
    private LineChart lineChart;
    private DataModelDataHandler sampleHandler;
    private ChartDataModel chartDataModel;

    public ChartWidget(JEVisDataSource jeVisDataSource) {
        super(jeVisDataSource, new WidgetConfig(WIDGET_ID));
    }

    public ChartWidget(JEVisDataSource jeVisDataSource, WidgetConfig config) {
        super(jeVisDataSource, config);
    }


    @Override
    public void update(Interval interval) {
        logger.info("Update: {}", interval);
        //if config changed
        if (config.hasChanged("")) {
            lineChart.setChartSettings(chart1 -> {
                MultiAxisLineChart multiAxisLineChart = (MultiAxisLineChart) chart1;
                multiAxisLineChart.setAnimated(true);
                multiAxisLineChart.setLegendSide(Side.BOTTOM);
                multiAxisLineChart.setLegendVisible(true);

            });

            setChartLabel((MultiAxisLineChart) lineChart.getChart(), config.fontColor.get());
        }

        sampleHandler.setInterval(interval);
        sampleHandler.update();

        Platform.runLater(() -> {
            lineChart.updateChart();

        });

    }

    private void setChartLabel(MultiAxisLineChart chart, Color newValue) {
        chart.getY1Axis().setTickLabelFill(newValue);
        chart.getXAxis().setTickLabelFill(newValue);

        chart.getXAxis().setLabel("");
        chart.getY1Axis().setLabel("");
    }


    @Override
    public void init() {

        sampleHandler = new DataModelDataHandler(getDataSource(), config.getDataHandlerNode());
        sampleHandler.setMultiSelect(true);
//        chartDataModel = new ChartDataModel();


//        List<ChartDataModel> chartDataModelList = new ArrayList<>();
//        chartDataModelList.add(chartDataModel);
        lineChart = new LineChart(sampleHandler.getDataModel(), false, ManipulationMode.NONE, 0, "");

        setGraphic(lineChart.getChart());
    }


    @Override
    public String typeID() {
        return WIDGET_ID;
    }

    @Override
    public ImageView getImagePreview() {
        return JEConfig.getImage("widget/DonutWidget.png", previewSize.getHeight(), previewSize.getWidth());
    }
}
