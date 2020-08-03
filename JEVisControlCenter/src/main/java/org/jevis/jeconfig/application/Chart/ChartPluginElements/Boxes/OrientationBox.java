package org.jevis.jeconfig.application.Chart.ChartPluginElements.Boxes;

import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import org.jevis.commons.i18n.I18n;
import org.jevis.jeconfig.application.Chart.ChartSetting;

public class OrientationBox extends ComboBox<Orientation> {

    public OrientationBox(ChartSetting chartSetting) {
        super(FXCollections.observableArrayList(Orientation.values()));

        Callback<ListView<Orientation>, ListCell<Orientation>> callback = new Callback<ListView<Orientation>, ListCell<Orientation>>() {
            @Override
            public ListCell<Orientation> call(ListView<Orientation> param) {

                return new ListCell<Orientation>() {
                    @Override
                    protected void updateItem(Orientation item, boolean empty) {
                        super.updateItem(item, empty);

                        setText(null);
                        setGraphic(null);

                        if (item != null && !empty) {
                            switch (item) {
                                case HORIZONTAL:
                                    setText(I18n.getInstance().getString("plugin.graph.orientation.horizontal"));
                                    break;
                                case VERTICAL:
                                    setText(I18n.getInstance().getString("plugin.graph.orientation.vertical"));
                                    break;
                            }
                        }
                    }
                };
            }
        };

        setCellFactory(callback);
        setButtonCell(callback.call(null));

        this.getSelectionModel().select(chartSetting.getOrientation());

        this.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue == null || newValue != oldValue) {
                chartSetting.setOrientation(newValue);
            }
        });
    }

}