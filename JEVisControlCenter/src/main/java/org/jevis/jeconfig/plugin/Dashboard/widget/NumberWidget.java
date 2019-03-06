package org.jevis.jeconfig.plugin.Dashboard.widget;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisDataSource;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.plugin.Dashboard.config.WidgetConfigProperty;
import org.jevis.jeconfig.plugin.Dashboard.datahandler.SampleHandler;
import org.jevis.jeconfig.plugin.Dashboard.datahandler.SimpleDataHandler;

import java.util.ArrayList;
import java.util.List;

public class NumberWidget extends Widget {

    private static final Logger logger = LogManager.getLogger(NumberWidget.class);
    Tile tile;
//    private SimpleDataHandler sampleHandler;


    public NumberWidget(JEVisDataSource jeVisDataSource) {
        super(jeVisDataSource);

        config.setType(getId());

    }

    @Override
    public void update(WidgetData data, boolean hasNewData) {

    }


    public SampleHandler getSampleHandler() {
        return dataHandler;

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
//        if (sampleHandler != null && sampleHandler instanceof SimpleDataHandler) {
//            ((SimpleDataHandler) getSampleHandler()).lastUpdate.addListener((observable, oldValue, newValue) -> {
//                System.out.println("sample Handler indicates update");
//                sampleHandler.getValuePropertyMap().forEach((s, samplesList) -> {
//                    try {
//                        System.out.println("Update with samples: " + samplesList.size());
//                        if (!samplesList.isEmpty()) {
//                            if (samplesList.size() > 1) {
//                                tile.setValue(samplesList.get(samplesList.size() - 2).getValueAsDouble());
//                                tile.setReferenceValue(samplesList.get(samplesList.size() - 2).getValueAsDouble());
//                            }
//                            tile.setValue(samplesList.get(samplesList.size() - 1).getValueAsDouble());
//                        } else {
//                            tile.setValue(0.0);
//                        }
//
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                });
//            });
//        }
    }

    @Override
    public void init() {
        logger.error("Init()");

        tile = TileBuilder.create()
                .skinType(Tile.SkinType.NUMBER)
                .prefSize(config.size.get().getWidth(), config.size.get().getHeight())
//                .unit("mb")
//                .description("Test")
                .textVisible(true)
                .textColor(config.fontColor.getValue())
                .backgroundColor(config.backgroundColor.getValue())
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
        propertyList.add(new WidgetConfigProperty<String>("NumberWidget.Text", category, "Text", "", textProperty));
        propertyList.add(new WidgetConfigProperty<String>("NumberWidget.description", category, "Description", "", descriptionProperly));


        config.addAdditionalSetting(propertyList);


        if (dataHandler != null && dataHandler instanceof SimpleDataHandler) {
            SimpleDataHandler simpleDataHandler = (SimpleDataHandler) dataHandler;
            logger.error("Add listener to sampleHanlder: {}", simpleDataHandler.uuid.toString());
            simpleDataHandler.lastUpdate.addListener((observable, oldValue, newValue) -> {
                System.out.println("sample Handler indicates update");
                simpleDataHandler.getValuePropertyMap().forEach((s, samplesList) -> {
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


        tile.setAnimated(true);
        setGraphic(tile);
    }

    @Override
    public String typeID() {
        return "Number";
    }

    @Override
    public ImageView getImagePreview() {
        return JEConfig.getImage("widget/HighLow.png", previewSize.getHeight(), previewSize.getWidth());
    }

}
