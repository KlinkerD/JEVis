package org.jevis.jeconfig.plugin.dashboard.timeframe;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.jeconfig.GlobalToolBar;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.plugin.dashboard.DashboardControl;

public class ToolBarIntervalSelector extends HBox {

    private static final Logger logger = LogManager.getLogger(ToolBarIntervalSelector.class);
    private TimeFrameEdior timeFrameEdior;
    Double iconSize = 20d;
    ToggleButton prevButton = new ToggleButton("", JEConfig.getImage("arrow_left.png", iconSize, iconSize));
    ToggleButton nextButton = new ToggleButton("", JEConfig.getImage("arrow_right.png", iconSize, iconSize));
    TimeFactoryBox timeFactoryBox = new TimeFactoryBox(false);
    ObservableList<TimeFrameFactory> timeFrames;
    private boolean disableEventListener=false;
    private DashboardControl controller;

    public ToolBarIntervalSelector(DashboardControl controller) {
        super();
        this.setAlignment(Pos.CENTER_LEFT);
        Button dateButton = new Button("");
        dateButton.setMinWidth(100);
        this.controller=controller;

        GlobalToolBar.changeBackgroundOnHoverUsingBinding(prevButton);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(nextButton);

        timeFactoryBox.setPrefWidth(200);
        timeFactoryBox.setMinWidth(200);

        timeFrames = FXCollections.observableArrayList(controller.getAllTimeFrames().getAll());
        timeFactoryBox.getItems().addAll(timeFrames);

        dateButton.setText(controller.getActiveTimeFrame().format(controller.getInterval()));
        dateButton.setTooltip(new Tooltip(controller.getInterval().toString()));

        this.timeFrameEdior = new TimeFrameEdior(controller.getActiveTimeFrame(), controller.getInterval());
        this.timeFrameEdior.getIntervalProperty().addListener((observable, oldValue, newValue) -> {
            if (disableEventListener) return;
            controller.setInterval(newValue);
        });

        dateButton.setOnAction(event -> {
            if (this.timeFrameEdior.isShowing()) {
                this.timeFrameEdior.hide();
            } else {
                this.timeFrameEdior.setDate(controller.getInterval().getEnd());
                Point2D point = dateButton.localToScreen(0.0, 0.0);
                this.timeFrameEdior.show(dateButton, point.getX() - 40, point.getY() + 40);
            }
        });

        timeFactoryBox.selectValue(controller.getActiveTimeFrame());

        timeFactoryBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (disableEventListener) return;
            controller.setActiveTimeFrame(newValue);
        });

        prevButton.setOnAction(event -> {
            controller.setPrevInteval();
        });

        nextButton.setOnAction(event -> {
            controller.setNextInterval();
        });

        controller.getActiveIntervalProperty().addListener((observable, oldValue, newValue) -> {
            if (disableEventListener) return;
            dateButton.setText(controller.getActiveTimeFrame().format(controller.getInterval()));
        });

        Region spacer = new Region();
        spacer.setMinWidth(10);
        spacer.setMaxWidth(10);

        getChildren().addAll(timeFactoryBox, spacer, prevButton, dateButton, nextButton);
    }

    public void updateView(){
        logger.debug("updateView: timeframe: '{}', interval: '{}' date: '{}'",controller.getActiveTimeFrame(),controller.getInterval(),controller.getInterval().getEnd());
        disableEventListener=true;

        timeFactoryBox.selectValue(controller.getActiveTimeFrame());
        timeFrameEdior.setTimeFrame(controller.getActiveTimeFrame());
        timeFrameEdior.setIntervalProperty(controller.getInterval());
        timeFrameEdior.setDate(controller.getInterval().getEnd());


        disableEventListener=false;
    }


}
