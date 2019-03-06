package org.jevis.jeconfig.plugin.Dashboard.widget;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.jevis.api.JEVisDataSource;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.plugin.Dashboard.config.WidgetConfigProperty;
import org.jevis.jeconfig.plugin.Dashboard.datahandler.SampleHandler;
import org.jevis.jeconfig.plugin.Dashboard.datahandler.SimpleDataHandler;

import java.util.ArrayList;
import java.util.List;

public class HighLowWidget extends Widget {

    Tile tile;
    private SimpleDataHandler sampleHandler;


    public HighLowWidget(JEVisDataSource jeVisDataSource) {
        super(jeVisDataSource);

        sampleHandler = new SimpleDataHandler(jeVisDataSource);
        sampleHandler.setMultiSelect(false);
        sampleHandler.lastUpdate.addListener((observable, oldValue, newValue) -> {
            System.out.println("sample Handler indicates update");
            sampleHandler.getValuePropertyMap().forEach((s, samplesList) -> {
                try {
                    System.out.println("Update with samples: " + samplesList.size());
                    if (!samplesList.isEmpty()) {
                        if (samplesList.size() > 1) {
                            tile.setValue(samplesList.get(samplesList.size() - 2).getValueAsDouble());
                            tile.setReferenceValue(samplesList.get(samplesList.size() - 2).getValueAsDouble());
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
//        sampleHandler.getUnitProperty().addListener((observable, oldValue, newValue) -> {
//            try {
//                tile.setUnit(newValue);
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        });
        return sampleHandler;

    }

    @Override
    public void setBackgroundColor(Color color) {
        tile.setBackgroundColor(color);
    }

    @Override
    public void setTitle(String text) {
        tile.setTitle(text);
    }

    @Override
    public void setFontColor(Color color) {
        tile.setForegroundBaseColor(color);
        tile.setTextColor(color);
        tile.setValueColor(color);
    }

    @Override
    public void setCustomFont(Font font) {
        tile.setCustomFont(font);
        tile.setCustomFontEnabled(true);
    }

    @Override
    public void configChanged() {

    }

    @Override
    public void init() {

        tile = TileBuilder.create()
                .skinType(Tile.SkinType.HIGH_LOW)
                .prefSize(config.size.get().getWidth(), config.size.get().getHeight())
//                .title("Strom")
//                .unit("kWh")
//                .description("")
//                .text("")
//                .referenceValue(20.7)
//                .value(26.2)
                .backgroundColor(config.backgroundColor.getValue())
                .textColor(config.fontColor.getValue())
                .build();

        addCommonConfigListeners();

        StringProperty textProperty = new SimpleStringProperty("");
        StringProperty descriptionProperly = new SimpleStringProperty("");
        textProperty.addListener((observable, oldValue, newValue) -> {
            tile.setText(newValue);
        });
        descriptionProperly.addListener((observable, oldValue, newValue) -> {
            tile.setDescription(newValue);
        });
        config.unit.addListener((observable, oldValue, newValue) -> {
            tile.setUnit(newValue);
        });


        String category = "Number Widget";

        List<WidgetConfigProperty> propertyList = new ArrayList<>();
        propertyList.add(new WidgetConfigProperty<String>("Widget.Text", category, "Text", "", textProperty));
        propertyList.add(new WidgetConfigProperty<String>("Widget.description", category, "Description", "", descriptionProperly));


        config.addAdditionalSetting(propertyList);

        tile.setAnimated(true);
        setGraphic(tile);
    }

    @Override
    public String typeID() {
        return "High Low";
    }

    @Override
    public ImageView getImagePreview() {
        return JEConfig.getImage("widget/HighLow.png", previewSize.getHeight(), previewSize.getWidth());
    }

}
