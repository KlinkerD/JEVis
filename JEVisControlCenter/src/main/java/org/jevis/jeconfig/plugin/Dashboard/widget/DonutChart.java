package org.jevis.jeconfig.plugin.Dashboard.widget;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.chart.ChartData;
import javafx.scene.image.ImageView;
import org.jevis.api.JEVisDataSource;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.plugin.Dashboard.datahandler.LastValueHandler;
import org.jevis.jeconfig.plugin.Dashboard.datahandler.SampleHandler;

public class DonutChart extends Widget {

    Tile tile = new Tile(Tile.SkinType.DONUT_CHART);

    ChartData chartData1 = new ChartData("Strom", 24.0, Tile.GREEN);
    ChartData chartData2 = new ChartData("Wasser", 10.0, Tile.BLUE);
    ChartData chartData3 = new ChartData("Gas", 12.0, Tile.RED);
    ChartData chartData4 = new ChartData("Lüftung", 13.0, Tile.YELLOW_ORANGE);
    private LastValueHandler sampleHandler;

    public DonutChart(JEVisDataSource jeVisDataSource) {
        super(jeVisDataSource);
        sampleHandler = new LastValueHandler(jeVisDataSource);
        sampleHandler.setMultiSelect(true);
        sampleHandler.lastUpdate.addListener((observable, oldValue, newValue) -> {
            System.out.println("sample Handler indicates update");
            sampleHandler.getValuePropertyMap().forEach((s, samplesList) -> {
                try {
                    System.out.println("Update with samples: " + samplesList.size());
                    if (!samplesList.isEmpty()) {
                        if (samplesList.size() > 1) {
                            String name = sampleHandler.getAttributeMap().get(s).getObject().getName();
                            ChartData chartData = new ChartData(name, samplesList.get(samplesList.size() - 1).getValueAsDouble(), Tile.GREEN);
                            tile.setValue(samplesList.get(samplesList.size() - 1).getValueAsDouble());
                            tile.setReferenceValue(samplesList.get(samplesList.size() - 2).getValueAsDouble());
                            tile.getChartData().add(chartData);
                        }
                        tile.setValue(samplesList.get(samplesList.size() - 1).getValueAsDouble());
                    } else {
                        tile.setValue(0.0);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });

    }

    @Override
    public void update(WidgetData data, boolean hasNewData) {

    }

    public SampleHandler getSampleHandler() {


        return sampleHandler;

    }

    @Override
    public void init() {

        tile = TileBuilder.create()
                .skinType(Tile.SkinType.DONUT_CHART)
                .prefSize(config.size.get().getWidth(), config.size.get().getHeight())
                .title("Energieverteilung")
                .text("Some text")
                .textVisible(false)
//                .chartData(chartData1, chartData2, chartData3, chartData4)
                .backgroundColor(config.backgroundColor.getValue())
                .build();

        config.backgroundColor.addListener((observable, oldValue, newValue) -> {
//            tile.setBackgroundColor(newValue);
            tile.setBackgroundColor(newValue);
        });

        config.fontColor.addListener((observable, oldValue, newValue) -> {
            tile.setTextColor(newValue);
        });

        tile.setAnimated(true);
        setGraphic(tile);
    }

    @Override
    public String typeID() {
        return "Donut";
    }

    @Override
    public ImageView getImagePreview() {
        return JEConfig.getImage("widget/DonutChart.png", previewSize.getHeight(), previewSize.getWidth());
    }
}
