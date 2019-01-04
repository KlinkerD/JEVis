/**
 * Copyright (C) 2015 Envidatec GmbH <info@envidatec.com>
 * <p>
 * This file is part of JEApplication.
 * <p>
 * JEApplication is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation in version 3.
 * <p>
 * JEApplication is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * JEApplication. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * JEApplication is part of the OpenJEVis project, further project information
 * are published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.jeconfig.dialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jevis.api.JEVisClass;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisObject;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.application.Chart.ChartDataModel;
import org.jevis.jeconfig.application.Chart.ChartSettings;
import org.jevis.jeconfig.application.Chart.data.GraphDataModel;
import org.jevis.jeconfig.application.jevistree.JEVisTree;
import org.jevis.jeconfig.application.jevistree.JEVisTreeFactory;
import org.jevis.jeconfig.application.jevistree.TreePlugin;
import org.jevis.jeconfig.application.jevistree.UserSelection;
import org.jevis.jeconfig.application.jevistree.plugin.ChartPlugin;
import org.jevis.jeconfig.tool.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Florian Simon <florian.simon@envidatec.com>
 */
public class ChartSelectionDialog {

    private final JEVisDataSource _ds;
    private Response _response = Response.CANCEL;
    private GraphDataModel data;
    private Stage stage;
    private boolean init = true;
    private JEVisTree tree;
    //    private ObservableList<String> chartsList = FXCollections.observableArrayList();
    private ChartPlugin chartPlugin = null;

    /**
     * @param ds
     * @param data
     */
    public ChartSelectionDialog(JEVisDataSource ds, GraphDataModel data) {
        this._ds = ds;
        this.data = data;

        this.tree = JEVisTreeFactory.buildDefaultGraphTree(ds, data);

    }

    public Response show() {
        _response = Response.CANCEL;

        if (stage != null) {
            stage.close();
            stage = null;
        }

        stage = new Stage();

        stage.setTitle(I18n.getInstance().getString("graph.selection.title"));

        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);
        stage.initOwner(JEConfig.getStage());

        //1180 for the columns

        double maxScreenWidth = Screen.getPrimary().getBounds().getMaxX();
        stage.setWidth(maxScreenWidth - 20);

        stage.setHeight(768);
        stage.setResizable(true);

        TabPane tabpane = new TabPane();

        Tab tabConfiguration = new Tab(I18n.getInstance().getString("graph.tabs.configuration"));
        tabConfiguration.closableProperty().setValue(false);

        VBox root = new VBox();

        String ICON = "1404313956_evolution-tasks.png";
        Node headerNode = DialogHeader.getDialogHeader(ICON, I18n.getInstance().getString("graph.selection.header"));

        Separator sep = new Separator(Orientation.HORIZONTAL);

        AnchorPane treePane = new AnchorPane();


        for (TreePlugin plugin : tree.getPlugins()) {
            if (plugin instanceof ChartPlugin) {
                chartPlugin = (ChartPlugin) plugin;
                if (data != null && data.getSelectedData() != null && !data.getSelectedData().isEmpty()) {
                    //chartPlugin.setData(data);
                }
            }
        }


        treePane.getChildren().setAll(tree);
        AnchorPane.setTopAnchor(tree, 0d);
        AnchorPane.setRightAnchor(tree, 0d);
        AnchorPane.setBottomAnchor(tree, 0d);
        AnchorPane.setLeftAnchor(tree, 0d);

        HBox buttonBox = new HBox(10);
        Region spacer = new Region();
        Button ok = new Button(I18n.getInstance().getString("graph.selection.load"));
        Button removeAllSelections = new Button(I18n.getInstance().getString("graph.selection.removeselections"));

        ok.setDefaultButton(true);

        HBox.setHgrow(removeAllSelections, Priority.NEVER);
        HBox.setHgrow(ok, Priority.NEVER);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox.setMargin(removeAllSelections, new Insets(10));
        HBox.setMargin(ok, new Insets(10));
        buttonBox.getChildren().setAll(tree.getSearchFilterBar(), spacer, removeAllSelections, ok);
        root.getChildren().setAll(headerNode, treePane, sep, buttonBox);

        VBox.setVgrow(treePane, Priority.ALWAYS);
        VBox.setVgrow(sep, Priority.NEVER);
        VBox.setVgrow(buttonBox, Priority.NEVER);


        removeAllSelections.setOnAction(event -> {
            try {
                chartPlugin.selectNone();
            } catch (Exception ex) {
            }
        });

        tabConfiguration.setContent(root);

        Tab tabChartsSettings = new Tab(I18n.getInstance().getString("graph.tabs.charts"));
        tabChartsSettings.closableProperty().setValue(false);
        tabChartsSettings.setDisable(true);

        VBox vboxCharts = new VBox();

        TabPane tabPaneCharts = new TabPane();

//        chartsList = data.getChartsList();chartPlugin
        for (ChartSettings cset : chartPlugin.getData().getCharts()) {
            tabPaneCharts.getTabs().add(getChartTab(cset));
        }


        //Disabled, for finding bugs
//        chartPlugin.getData().getChartsList().addListener((ListChangeListener<? super String>) c -> {
//            while (c.next()) {
//                if (c.wasAdded() || c.wasRemoved() || c.wasUpdated()) {
//                    tabPaneCharts.getTabs().clear();
//                    for (String s : chartPlugin.getData().getChartsList()) {
//                        tabPaneCharts.getTabs().add(getChartTab(s));
//                    }
//                }
//            }
//        });

        vboxCharts.getChildren().add(tabPaneCharts);
        tabChartsSettings.setContent(vboxCharts);

        tabpane.getTabs().addAll(tabConfiguration, tabChartsSettings);

        Scene scene = new Scene(tabpane);
        stage.setScene(scene);

        if (data != null && data.getSelectedData() != null && !data.getSelectedData().isEmpty()) {
            List<UserSelection> listUS = new ArrayList<>();
            for (ChartDataModel cdm : data.getSelectedData()) {
                for (int i : cdm.getSelectedcharts()) {
                    for (ChartSettings set : data.getCharts()) {
                        if (set.getId() == i)
                            listUS.add(new UserSelection(UserSelection.SelectionType.Object, cdm.getObject()));
                    }
                }
            }

            if (!listUS.isEmpty()) tree.openUserSelectionNoChildren(listUS);
            else {
                openFirstDataDir();
            }
        } else {
            openFirstDataDir();
        }

        ok.setOnAction(event -> {
            tree.setUserSelectionEnded();

            _response = Response.OK;

            stage.close();
            stage = null;

        });

        stage.showAndWait();

        return _response;
    }

    private void openFirstDataDir() {
        List<UserSelection> listUS = new ArrayList<>();
        JEVisObject firstDataDir = null;
        try {
            JEVisClass classDataDirectory = _ds.getJEVisClass("Data Directory");
            List<JEVisObject> listDataDirectories = _ds.getObjects(classDataDirectory, false);
            if (!listDataDirectories.isEmpty()) firstDataDir = listDataDirectories.get(0);
        } catch (JEVisException e) {

        }
        if (firstDataDir != null) listUS.add(new UserSelection(UserSelection.SelectionType.Object, firstDataDir));
        if (!listUS.isEmpty()) tree.openUserSelection(listUS);
    }

    private Tab getChartTab(ChartSettings cset) {
        Tab newTab = new Tab(cset.getName());
        newTab.setClosable(false);

        GridPane gp = new GridPane();

        Label labelName = new Label(I18n.getInstance().getString("graph.tabs.tab.name"));
        TextField textFieldName = new TextField();
        textFieldName.setText(cset.getName());

        textFieldName.textProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue == null || newValue != oldValue) {
                for (ChartSettings c : data.getCharts()) {
                    if (c.getName().contains(cset.getName())) c.setName(newValue);
                }
            }
        });

        Label labelChartType = new Label(I18n.getInstance().getString("graph.tabs.tab.charttype"));

        ObservableList<String> listChartTypes = FXCollections.observableArrayList();
        ComboBox<String> boxChartType = new ComboBox<>(listChartTypes);

        gp.add(labelName, 0, 1);
        gp.add(textFieldName, 1, 1);

        gp.add(labelChartType, 0, 3);
        gp.add(boxChartType, 1, 3);

        newTab.setContent(gp);

        return newTab;
    }


    public GraphDataModel getSelectedData() {
        return data;
    }

    public void setData(GraphDataModel data) {
        this.data = data;
    }

    public ChartPlugin getChartPlugin() {
        return chartPlugin;
    }

    public enum Response {
        OK, CANCEL
    }
}