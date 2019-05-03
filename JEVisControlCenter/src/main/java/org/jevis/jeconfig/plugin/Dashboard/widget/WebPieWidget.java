package org.jevis.jeconfig.plugin.Dashboard.widget;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.AtomicDouble;
import com.jfoenix.controls.JFXButton;
import com.sun.javafx.charts.Legend;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisDataSource;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.plugin.Dashboard.config.WidgetConfig;
import org.jevis.jeconfig.plugin.Dashboard.datahandler.DataModelDataHandler;
import org.joda.time.Interval;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class WebPieWidget extends Widget {
    private static final Logger logger = LogManager.getLogger(WebPieWidget.class);
    public static String WIDGET_ID = "Web Pie";
    private PieChart chart;
    private NumberFormat nf = NumberFormat.getInstance();
    private JFXButton openAnalysisButton = new JFXButton();
    private DataModelDataHandler sampleHandler;
    private WidgetLegend legend = new WidgetLegend();
    private GraphAnalysisLinker graphAnalysisLinker;
    private ObjectMapper mapper = new ObjectMapper();
    private BorderPane borderPane = new BorderPane();
    private VBox legendPane = new VBox();
    private WebView webView = new WebView();


    public WebPieWidget(JEVisDataSource jeVisDataSource) {
        super(jeVisDataSource, new WidgetConfig(WIDGET_ID));
    }

    public WebPieWidget(JEVisDataSource jeVisDataSource, WidgetConfig config) {
        super(jeVisDataSource, config);
    }


    @Override
    public void update(Interval interval) {
        logger.debug("Pie.Update: {}", interval);
//        chart = new PieChart();
        sampleHandler.setInterval(interval);
        sampleHandler.update();


        ObservableList<PieChart.Data> series = FXCollections.observableArrayList();
        List<Legend.LegendItem> legendItemList = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        if (config.hasChanged("")) {

            borderPane.setMaxWidth(config.size.getValue().getWidth());
            chart.setLabelsVisible(true);
            chart.setLabelLineLength(18);
            chart.setLegendVisible(false);
            chart.setAnimated(false);
            chart.setMinWidth(320d);/** tmp solution for an unified look**/
            chart.setMaxWidth(320d);

//            nf.setMinimumFractionDigits(config.decimals.getValue());
//            nf.setMaximumFractionDigits(config.decimals.getValue());
            nf.setMinimumFractionDigits(0);/** tmp solution**/
            nf.setMaximumFractionDigits(0);


            Platform.runLater(() -> {
                //            legend.setBackground(new Background(new BackgroundFill(config.backgroundColor.getValue(), CornerRadii.EMPTY, Insets.EMPTY)));
                legend.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));


            });
        }

        /** data Update **/
        AtomicDouble total = new AtomicDouble(0);
        sampleHandler.getDataModel().forEach(chartDataModel -> {
            try {
                if (!chartDataModel.getSamples().isEmpty()) {
//                        System.out.println("Pie Sample: " + chartDataModel.getSamples());
                    total.set(total.get() + chartDataModel.getSamples().get(chartDataModel.getSamples().size() - 1).getValueAsDouble());
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        });

        StringBuilder stringBuilder = new StringBuilder();

        HTMLPie WebPieWidget = new HTMLPie();


//        String content =
//                "Hello World!";


//        var data = {
//                series: [5, 3, 4]
//};
//
//        var sum = function(a, b) { return a + b };
//
//        new Chartist.Pie('.ct-chart', data, {
//                labelInterpolationFnc: function(value) {
//            return Math.round(value / data.series.reduce(sum) * 100) + '%';
//        }
//});

//        sampleHandler.getDataModel().forEach(chartDataModel -> {
//            try {
//
//                String dataName = chartDataModel.getObject().getName();
//                double value = 0;
//
//
//                boolean hasNoData = chartDataModel.getSamples().isEmpty();
//
//                String textValue = "";
//
//
//                if (!hasNoData) {
//                    logger.debug("Samples: ({}) {}", dataName, chartDataModel.getSamples());
//                    try {
//                        value = chartDataModel.getSamples().get(chartDataModel.getSamples().size() - 1).getValueAsDouble();
//
//                        double proC = (value / total.get()) * 100;
//                        if (Double.isInfinite(proC)) proC = 100;
//                        if (Double.isNaN(proC)) proC = 0;
//
//
//                        textValue = nf.format(value) + " " + UnitManager.getInstance().format(chartDataModel.getUnitLabel()) + "\n" + nf.format(proC) + "%";
//
//
//                    } catch (Exception ex) {
////                        value = 1;/** pie pease will be missing if its 0 **/
////                        textValue = "n.a. ";
//                        logger.error(ex);
//                    }
//                } else {
//                    logger.debug("Empty Samples for: {}", config.title.get());
//                    value = 1;
//                    textValue = "n.a.  " + UnitManager.getInstance().format(chartDataModel.getUnitLabel()) + "\n" + nf.format(0) + "%";
//
//                }
//
//
//                legendItemList.add(legend.buildLegendItem(dataName, chartDataModel.getColor(), config.fontColor.getValue(), config.fontSize.get()));
//
//                PieChart.Data pieData = new PieChart.Data(textValue, value);
//                series.add(pieData);
//                colors.add(chartDataModel.getColor());
//
//
//            } catch (Exception ex) {
//                logger.error(ex);
//            }
//        });

        WebEngine webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);


        /** redrawing **/
        Platform.runLater(() -> {

            webEngine.loadContent(WebPieWidget.getPiePage(), "text/html");
        });
    }


    @Override
    public void init() {

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonInString = mapper.writeValueAsString(config.getConfigNode(WidgetConfig.DATA_HANDLER_NODE));

            sampleHandler = new DataModelDataHandler(getDataSource(), config.getConfigNode(WidgetConfig.DATA_HANDLER_NODE));
            sampleHandler.setMultiSelect(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        chart = new PieChart();
        /** Dummy data to render pie**/
        ObservableList<PieChart.Data> series = FXCollections.observableArrayList();
        series.add(new PieChart.Data("A", 1));
        series.add(new PieChart.Data("B", 1));
        series.add(new PieChart.Data("C", 1));
        series.add(new PieChart.Data("D", 1));

//        graphAnalysisLinker = new GraphAnalysisLinker(getDataSource(), null);
//        openAnalysisButton = graphAnalysisLinker.buildLinkerButton();

        legendPane.setPadding(new Insets(10, 5, 5, 0));

        legend.setMaxWidth(100);
        legend.setPrefWidth(100);
        legend.setPrefHeight(10);

//        legendPane.getChildren().setAll(legend);
//        borderPane.setCenter(chart);
//        borderPane.setRight(legendPane);

        Platform.runLater(() -> {
//            legendPane.getChildren().setAll(legend);
            borderPane.setCenter(webView);
//            borderPane.setRight(legendPane);
            setGraphic(borderPane);
        });


    }


    private Legend.LegendItem buildLegendItem(String name, Color color, Color fontColor) {
        Rectangle r = new Rectangle();
        r.setX(0);
        r.setY(0);
        r.setWidth(12);
        r.setHeight(12);
        r.setArcWidth(20);
        r.setArcHeight(20);
        r.setStroke(color);
        r.setFill(color);
        /**
         * TODO: replace this hack with an own implementation of an legend
         */
        Legend.LegendItem item = new Legend.LegendItem(name, r);
        try {
            Field privateStringField = Legend.LegendItem.class.
                    getDeclaredField("label");
            privateStringField.setAccessible(true);
            Label label = (Label) privateStringField.get(item);
            label.setTextFill(fontColor);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return item;
    }

    @Override
    public String typeID() {
        return WIDGET_ID;
    }

    @Override
    public ImageView getImagePreview() {
        return JEConfig.getImage("widget/DonutWidget.png", previewSize.getHeight(), previewSize.getWidth());
    }

    public void applyColors(List<Color> colors) {

        for (int i = 0; i < colors.size(); i++) {

            Color currentColor = colors.get(i);
            String hexColor = toRGBCode(currentColor);
            String preIdent = ".default-color" + i;
            Node node = chart.lookup(preIdent + ".chart-pie");
            node.setStyle("-fx-pie-color: " + hexColor + ";");

//            System.out.println(preIdent + ".chart-pie " + "-fx-pie-color: " + hexColor + ";" + " color: " + currentColor.toString());
        }
    }

    private String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}