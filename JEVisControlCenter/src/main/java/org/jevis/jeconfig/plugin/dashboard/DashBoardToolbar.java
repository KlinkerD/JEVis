package org.jevis.jeconfig.plugin.dashboard;

import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import org.apache.commons.math3.util.Precision;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisClass;
import org.jevis.api.JEVisObject;
import org.jevis.jeconfig.GlobalToolBar;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.plugin.dashboard.config2.DashboardPojo;
import org.jevis.jeconfig.plugin.dashboard.timeframe.ToolBarIntervalSelector;
import org.jevis.jeconfig.tool.I18n;
import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.List;

public class DashBoardToolbar extends ToolBar {

    private static final Logger logger = LogManager.getLogger(DashBoardToolbar.class);

    private double iconSize = 20;
    private final DashboardControl dashboardControl;
    private ToolBarIntervalSelector toolBarIntervalSelector;
    private ComboBox<JEVisObject> listAnalysesComboBox;
    private JFXComboBox<Double> listZoomLevel;

    final ImageView lockIcon = JEConfig.getImage("if_lock_blue_68757.png", this.iconSize, this.iconSize);
    final ImageView snapToGridIcon = JEConfig.getImage("Snap_to_Grid.png", this.iconSize, this.iconSize);
    final ImageView unlockIcon = JEConfig.getImage("if_lock-unlock_blue_68758.png", this.iconSize, this.iconSize);
    final ImageView pauseIcon = JEConfig.getImage("pause_32.png", this.iconSize, this.iconSize);
    final ImageView playIcon = JEConfig.getImage("play_32.png", this.iconSize, this.iconSize);
    ToggleButton backgroundButton = new ToggleButton("", JEConfig.getImage("if_32_171485.png", this.iconSize, this.iconSize));
    ToggleButton runUpdateButton = new ToggleButton("", this.playIcon);
    ToggleButton unlockButton = new ToggleButton("", this.lockIcon);
    ToggleButton snapGridButton = new ToggleButton("", snapToGridIcon);
    ToggleButton treeButton = new ToggleButton("", JEConfig.getImage("Data.png", this.iconSize, this.iconSize));
    ToggleButton settingsButton = new ToggleButton("", JEConfig.getImage("Service Manager.png", this.iconSize, this.iconSize));
    ToggleButton save = new ToggleButton("", JEConfig.getImage("save.gif", this.iconSize, this.iconSize));
    ToggleButton exportPNG = new ToggleButton("", JEConfig.getImage("export-image.png", this.iconSize, this.iconSize));
    ToggleButton newButton = new ToggleButton("", JEConfig.getImage("1390343812_folder-open.png", this.iconSize, this.iconSize));
    ToggleButton delete = new ToggleButton("", JEConfig.getImage("if_trash_(delete)_16x16_10030.gif", this.iconSize, this.iconSize));
    ToggleButton zoomIn = new ToggleButton("", JEConfig.getImage("zoomIn_32.png", this.iconSize, this.iconSize));
    ToggleButton zoomOut = new ToggleButton("", JEConfig.getImage("zoomOut_32.png", this.iconSize, this.iconSize));
    ToggleButton enlarge = new ToggleButton("", JEConfig.getImage("enlarge_32.png", this.iconSize, this.iconSize));
    ToggleButton newB = new ToggleButton("", JEConfig.getImage("list-add.png", 18, 18));
    ToggleButton reload = new ToggleButton("", JEConfig.getImage("1403018303_Refresh.png", this.iconSize, this.iconSize));
    ToggleButton navigator = new ToggleButton("", JEConfig.getImage("Data.png", this.iconSize, this.iconSize));
    Tooltip reloadTooltip = new Tooltip(I18n.getInstance().getString("plugin.graph.toolbar.tooltip.reload"));
    private boolean disableEventListener = false;

    public DashBoardToolbar(DashboardControl dashboardControl) {
        this.dashboardControl = dashboardControl;
        initLayout();
        this.dashboardControl.registerToolBar(this);
    }

    public void initLayout(){
        logger.debug("InitLayout");
        ObservableList<JEVisObject> observableList = this.dashboardControl.getAllDashboards();
        this.listAnalysesComboBox = new ComboBox<>(observableList);
        setCellFactoryForComboBox();
        listAnalysesComboBox.setTooltip(new Tooltip(listAnalysesComboBox+"  :> "+this.listAnalysesComboBox.getSelectionModel().getSelectedItem()));
        this.listAnalysesComboBox.setPrefWidth(350);
        this.listAnalysesComboBox.setMinWidth(350);

        GlobalToolBar.changeBackgroundOnHoverUsingBinding(treeButton);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(settingsButton);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(save);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(exportPNG);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(newButton);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(delete);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(zoomIn);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(zoomOut);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(enlarge);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(newB);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(reload);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(backgroundButton);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(navigator);

        reload.setTooltip(reloadTooltip);

        listZoomLevel = buildZoomLevelListView();

        this.listAnalysesComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if(disableEventListener) return;
                if(oldValue!=newValue){
                    this.dashboardControl.selectDashboard(newValue);

                    Platform.runLater(() -> {
                        //** workaround for the Combobox **/
                        this.runUpdateButton.requestFocus();
                    });
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        this.listZoomLevel.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(disableEventListener) return;
            dashboardControl.setZoomFactor(newValue);
        });

        reload.setOnAction(event -> {
            this.dashboardControl.reload();
        });

        exportPNG.setOnAction(event -> {
            this.dashboardControl.toPNG();
        });

        save.setOnAction(event -> {
            this.dashboardControl.save();
        });

//        unlockButton.selectedProperty().setValue(this.dashboardControl.editableProperty.getValue());
//        snapGridButton.selectedProperty().setValue(this.dashboardControl.enableSnapToGridProperty.getValue());
//        snapGridButton.setDisable(true);
//        navigator.setDisable(true);

        unlockButton.setOnAction(event -> {
            this.snapGridButton.setDisable(!unlockButton.isSelected());
            this.dashboardControl.enableSnapToGridProperty.setValue(unlockButton.isSelected());
            this.dashboardControl.setEditable(unlockButton.isSelected());
            this.dashboardControl.showGrid(unlockButton.isSelected());
            this.snapGridButton.setSelected(this.dashboardControl.enableSnapToGridProperty.get());
            navigator.setDisable(!unlockButton.isSelected());
        });

        snapGridButton.setOnAction(event -> {
            this.dashboardControl.setSnapToGrid(snapGridButton.isSelected());
        });


        this.runUpdateButton.setOnAction(event -> {
            this.dashboardControl.switchUpdating();
        });

        this.backgroundButton.setOnAction(event -> {
            this.dashboardControl.startWallpaperSelection();
        });

        navigator.setOnAction(event -> {
            this.dashboardControl.openWidgetNavigator();
        });


        zoomIn.setOnAction(event -> {
            this.dashboardControl.zoomIn();
        });

        zoomOut.setOnAction(event -> {
            this.dashboardControl.zoomOut();
        });

        toolBarIntervalSelector = new ToolBarIntervalSelector(this.dashboardControl);

        newB.setOnAction(event -> {
            this.dashboardControl.createNewDashboard();
        });


        Separator sep1 = new Separator();
        Separator sep2 = new Separator();
        Separator sep3 = new Separator();
        Separator sep4 = new Separator();


        newButton.setDisable(true);
        delete.setDisable(true);

        updateView(dashboardControl.getActiveDashboard());

        Region spacerForRightSide = new Region();
        HBox.setHgrow(spacerForRightSide, Priority.ALWAYS);
        getItems().clear();
        getItems().setAll(
                this.listAnalysesComboBox, newB
                , sep3, toolBarIntervalSelector
                , sep1, zoomOut, zoomIn, listZoomLevel,reload
                , sep4, newButton, save, delete, navigator, exportPNG
                , sep2, this.runUpdateButton, this.unlockButton, snapGridButton);
    }



    private JFXComboBox<Double> buildZoomLevelListView(){
        ObservableList<Double> zoomLevel = FXCollections.observableArrayList();

        zoomLevel.addAll(   DashboardControl.fitToScreen,DashboardControl.fitToWidth,DashboardControl.fitToHeight
                            ,0.3d,0.4d,0.5d,0.6d,0.7d,0.8d,0.9d,
                             1.0d, 1.1d, 1.2d, 1.3d, 1.4d, 1.5d, 1.6d, 1.7d, 1.8d, 1.9d, 2.0d, 2.5d, 3.0d );


        JFXComboBox<Double> doubleComboBox = new JFXComboBox<>(zoomLevel);
        DecimalFormat df = new DecimalFormat("##0%");
        df.setMaximumFractionDigits(2);
        Callback<ListView<Double>, ListCell<Double>> cellFactory = new Callback<ListView<Double>, ListCell<Double>>() {
            @Override
            public ListCell<Double> call(ListView<Double> param) {
                return new ListCell<Double>(){
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
//                        System.out.println("item: "+item+" empty:"+empty);

                        if(item!=null){
                            if(item==DashboardControl.fitToScreen){
                                setText(I18n.getInstance().getString("plugin.dashboard.zoom.fitscreen"));
                            } else  if(item==DashboardControl.fitToWidth){
                                setText(I18n.getInstance().getString("plugin.dashboard.zoom.fitwidth"));
                            } else  if(item==DashboardControl.fitToHeight){
                                setText(I18n.getInstance().getString("plugin.dashboard.zoom.fitheight"));
                            } else{
                                setText(df.format(Precision.round(item, 2)));
                            }
                        }
                    }
                };
            }


        };

        doubleComboBox.setCellFactory(cellFactory);
        doubleComboBox.setButtonCell(cellFactory.call(null));
        doubleComboBox.setValue(1.0d);
//        doubleComboBox.setPrefWidth(100d);

        return doubleComboBox;
    }

    public ComboBox<JEVisObject> getListAnalysesComboBox() {
        return this.listAnalysesComboBox;
    }

    public void setUpdateRunning(boolean updateRunning) {
        Platform.runLater(() -> {
            if (updateRunning) {
                this.runUpdateButton.setGraphic(this.pauseIcon);
            } else {
                this.runUpdateButton.setGraphic(this.playIcon);
            }
        });
    }

    public void setEditable(boolean editable) {
        Platform.runLater(() -> {
            if (editable) {
                this.unlockButton.setGraphic(this.unlockIcon);
            } else {
                this.unlockButton.setGraphic(this.lockIcon);
            }
        });

    }

    public void updateZoomLevelView(double zoomLvL){
        disableEventListener =true;
        listZoomLevel.setValue(zoomLvL);
        disableEventListener =false;
    }

    public void updateDashboardList(final ObservableList<JEVisObject> dashboardList,final DashboardPojo dashboardSettings){
        logger.debug("updateDashboardList: {}", dashboardSettings);
        disableEventListener =true;

        listAnalysesComboBox.setItems(dashboardList);

        if (dashboardSettings.getDashboardObject() != null) {
                this.listAnalysesComboBox.getSelectionModel().select(dashboardSettings.getDashboardObject());
        }

        disableEventListener =false;
    }

    public void updateView(final DashboardPojo dashboardSettings) {
        logger.debug("updateDashboard: {}", dashboardSettings);

        unlockButton.selectedProperty().setValue(this.dashboardControl.editableProperty.getValue());
        snapGridButton.selectedProperty().setValue(this.dashboardControl.enableSnapToGridProperty.getValue());

        navigator.setDisable(dashboardControl.editableProperty.getValue());
        snapGridButton.setDisable(dashboardControl.enableSnapToGridProperty.getValue());

        listZoomLevel.setValue(dashboardControl.getZoomFactory());
        toolBarIntervalSelector.updateView();

        updateDashboardList(dashboardControl.getAllDashboards(), dashboardSettings);
    }


    private void setCellFactoryForComboBox() {
        try {
            JEVisClass organisationClass = DashBoardToolbar.this.dashboardControl.getDataSource().getJEVisClass("Organization");


            Callback<ListView<JEVisObject>, ListCell<JEVisObject>> cellFactory = new Callback<ListView<JEVisObject>, ListCell<JEVisObject>>() {
                @Override
                public ListCell<JEVisObject> call(ListView<JEVisObject> param) {
                    return new ListCell<JEVisObject>() {
                        @Override
                        protected void updateItem(JEVisObject obj, boolean empty) {
                            super.updateItem(obj, empty);
                            if (empty || obj == null || obj.getName() == null) {
                                setText(I18n.getInstance().getString("plugin.dashboard.toolbar.list.new"));
                            } else {
                                String prefix = "";
                                try {
                                    JEVisObject parentObj = null;
                                    JEVisObject secoundParentObj = null;
                                    JEVisObject thirdParentObj = null;

                                    try {
                                        parentObj = obj.getParents().get(0);
                                    } catch (NullPointerException np) {
                                    }
                                    try {
                                        secoundParentObj = parentObj.getParents().get(0);
                                    } catch (Exception np) {
                                    }
                                    try {
                                        thirdParentObj = secoundParentObj.getParents().get(0);
                                    } catch (Exception np) {
                                    }

                                    if (secoundParentObj != null && secoundParentObj.getJEVisClass().equals(organisationClass)) {
                                        prefix += secoundParentObj.getName() + " / ";
                                    }
                                    if (thirdParentObj != null && thirdParentObj.getJEVisClass().equals(organisationClass)) {
                                        prefix += thirdParentObj.getName() + " / ";
                                    }


                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                setText(prefix + obj.getName());
                            }

                        }
                    };
                }
            };

            this.listAnalysesComboBox.setCellFactory(cellFactory);
            this.listAnalysesComboBox.setButtonCell(cellFactory.call(null));
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

}
