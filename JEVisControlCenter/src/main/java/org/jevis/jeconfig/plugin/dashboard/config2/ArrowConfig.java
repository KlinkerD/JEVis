package org.jevis.jeconfig.plugin.dashboard.config2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.commons.i18n.I18n;
import org.jevis.jeconfig.plugin.dashboard.DashboardControl;
import org.jevis.jeconfig.plugin.dashboard.widget.ArrowWidget.SHAPE;

import static org.jevis.jeconfig.plugin.dashboard.widget.ArrowWidget.ARROW_ORIENTATION;

public class ArrowConfig {

    private static final Logger logger = LogManager.getLogger(ArrowConfig.class);

    private ARROW_ORIENTATION orientation = ARROW_ORIENTATION.LEFT_RIGHT;
    private SHAPE shape = SHAPE.ARROW;
    private final String JSON_ORIENTATION = "orientation";
    private final String JSON_SHAPE = "shape";

    final DashboardControl dashboardControl;

    public ArrowConfig(DashboardControl control) {
        this(control, null);
    }

    public ARROW_ORIENTATION getOrientation() {
        return orientation;
    }

    public SHAPE getShape() {
        return shape;
    }

    public ArrowConfig(DashboardControl control, JsonNode jsonNode) {
        this.dashboardControl = control;

        if (jsonNode != null) {
            String orientationStrg = jsonNode.get(JSON_ORIENTATION).asText(ARROW_ORIENTATION.LEFT_RIGHT.toString());
            orientation = ARROW_ORIENTATION.valueOf(orientationStrg);

            String shapeStrg = jsonNode.get(JSON_SHAPE).asText(SHAPE.ARROW.toString());
            shape = SHAPE.valueOf(shapeStrg);
        }
    }


    private class ArrowTab extends Tab implements ConfigTab {
        ArrowConfig limit;

        public ArrowTab(String text, ArrowConfig limit) {
            super(text);
            this.limit = limit;
        }

        @Override
        public void commitChanges() {
            //TODO;
        }
    }

    public Tab getConfigTab() {
        ArrowTab tab = new ArrowTab(I18n.getInstance().getString("plugin.dashboard.arrowwidget.tab")
                , this);

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setVgap(8);
        gridPane.setHgap(8);


        AnchorPane editorPane = new AnchorPane();

        Label limitTypeLabel = new Label(I18n.getInstance().getString("plugin.dashboard.arrowwidget.orientation"));

        ComboBox<ARROW_ORIENTATION> orientationTypeBox = new ComboBox<>(FXCollections.observableArrayList(ARROW_ORIENTATION.values()));
        Callback<ListView<ARROW_ORIENTATION>, ListCell<ARROW_ORIENTATION>> cellFactory = new Callback<ListView<ARROW_ORIENTATION>, ListCell<ARROW_ORIENTATION>>() {
            @Override
            public ListCell<ARROW_ORIENTATION> call(ListView<ARROW_ORIENTATION> param) {
                return new ListCell<ARROW_ORIENTATION>() {
                    @Override
                    protected void updateItem(ARROW_ORIENTATION item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && !empty) {
                            switch (item) {
                                case BOTTOM_TOP:
                                    setText(I18n.getInstance().getString("plugin.dashboard.arrowwidget.orientation.bottomTop"));
                                    break;
                                case TOP_BOTTOM:
                                    setText(I18n.getInstance().getString("plugin.dashboard.arrowwidget.orientation.topBottom"));
                                    break;
                                case RIGHT_LEFT:
                                    setText(I18n.getInstance().getString("plugin.dashboard.arrowwidget.orientation.rightLeft"));
                                    break;
                                case LEFT_RIGHT:
                                    setText(I18n.getInstance().getString("plugin.dashboard.arrowwidget.orientation.leftRight"));
                                    break;
                            }

                        }
                    }
                };
            }
        };
        orientationTypeBox.setCellFactory(cellFactory);
        orientationTypeBox.setButtonCell(cellFactory.call(null));

        orientationTypeBox.getSelectionModel().select(orientation);

        orientationTypeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            orientation = newValue;
        });

        //------------------------------- shape

        Label shapeLabel = new Label(I18n.getInstance().getString("plugin.dashboard.arrowwidget.orientation"));

        ComboBox<SHAPE> shapeBox = new ComboBox<>(FXCollections.observableArrayList(SHAPE.values()));
        Callback<ListView<SHAPE>, ListCell<SHAPE>> shapeCellFactory = new Callback<ListView<SHAPE>, ListCell<SHAPE>>() {
            @Override
            public ListCell<SHAPE> call(ListView<SHAPE> param) {
                return new ListCell<SHAPE>() {
                    @Override
                    protected void updateItem(SHAPE item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && !empty) {
                            switch (item) {
                                case ARROW:
                                    setText(I18n.getInstance().getString("plugin.dashboard.arrowwidget.shape.arrow"));
                                    break;
                                case LINE:
                                    setText(I18n.getInstance().getString("plugin.dashboard.arrowwidget.shape.line"));
                                    break;
                            }

                        }
                    }
                };
            }
        };
        shapeBox.setCellFactory(shapeCellFactory);
        shapeBox.setButtonCell(shapeCellFactory.call(null));

        shapeBox.getSelectionModel().select(shape);

        shapeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            shape = newValue;
        });


        gridPane.addRow(0, limitTypeLabel, orientationTypeBox);
        gridPane.addRow(1, shapeLabel, shapeBox);
        //gridPane.add(new Separator(Orientation.HORIZONTAL), 0, 2, 2, 1);
        //gridPane.add(editorPane, 0, 3, 2, 1);


        tab.setContent(gridPane);
        return tab;
    }


    public ObjectNode toJSON() {
        ObjectNode dataNode = JsonNodeFactory.instance.objectNode();
        dataNode.put(JSON_ORIENTATION, orientation.toString());
        dataNode.put(JSON_SHAPE, shape.toString());

        return dataNode;
    }


}