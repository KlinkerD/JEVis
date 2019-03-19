/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.jeconfig.application.jevistree.plugin;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisClass;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisObject;
import org.jevis.jeconfig.application.Chart.ChartPluginElements.Columns.*;
import org.jevis.jeconfig.application.Chart.ChartSettings;
import org.jevis.jeconfig.application.Chart.data.GraphDataModel;
import org.jevis.jeconfig.application.jevistree.JEVisTree;
import org.jevis.jeconfig.application.jevistree.JEVisTreeRow;
import org.jevis.jeconfig.application.jevistree.TreePlugin;
import org.jevis.jeconfig.tool.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * @author
 */
public class ChartPluginTree implements TreePlugin {
    public static int NO_OF_COLUMNS = 5;
    private final Image img = new Image(ChartPluginTree.class.getResourceAsStream("/icons/" + "list-add.png"));
    private final ImageView image = new ImageView(img);
    private JEVisTree jeVisTree;
    private final String chartTitle = I18n.getInstance().getString("graph.title");
    private GraphDataModel data;
    private List<TreeTableColumn<JEVisTreeRow, Long>> allColumns;
    private JEVisDataSource dataSource;
    private SimpleBooleanProperty addedChart = new SimpleBooleanProperty(false);

    public JEVisTree getTree() {
        return jeVisTree;
    }

    public ChartPluginTree(GraphDataModel data) {
        this.data = data;
    }

    @Override
    public void setTree(JEVisTree tree) {
        jeVisTree = tree;
        dataSource = tree.getJEVisDataSource();
//        if (data == null)
//            data = new GraphDataModel(jeVisTree.getJEVisDataSource());
    }

    @Override
    public void selectionFinished() {

    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public List<TreeTableColumn<JEVisTreeRow, Long>> getColumns() {
        if (allColumns == null) buildColumns();

        return allColumns;
    }

    private void buildColumns() {
        if (allColumns == null)
            allColumns = new ArrayList<>();
        else allColumns.clear();

        TreeTableColumn<JEVisTreeRow, Long> column = new TreeTableColumn<>();
        column.setEditable(true);

        image.fitHeightProperty().set(20);
        image.fitWidthProperty().set(20);

        Button addChart = new Button(I18n.getInstance().getString("graph.table.addchart"), image);
        if (getData().getCharts().isEmpty()) {
            data.getCharts().add(new ChartSettings(0, chartTitle));
        }

        ColorColumn colorColumn = new ColorColumn(jeVisTree, dataSource, I18n.getInstance().getString("graph.table.color"));
        colorColumn.setGraphDataModel(data);

        List<TreeTableColumn<JEVisTreeRow, Boolean>> selectionColumns = new ArrayList<TreeTableColumn<JEVisTreeRow, Boolean>>();

        addChart.setOnAction(event -> {
            if (data.getCharts().size() < getMaxChartsFromService()) {
                List<String> oldNames = new ArrayList<>();
                for (ChartSettings set : data.getCharts()) {
                    oldNames.add(set.getName());
                }

                String newName = chartTitle;
                int i = 1;
                while (oldNames.contains(newName)) {
                    boolean found = false;
                    for (String s : oldNames) {
                        if (s.equals(newName)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        i++;
                        newName += " " + i;
                    } else oldNames.add(newName);
                }

                int id = 0;
                for (ChartSettings set : data.getCharts()) {
                    id = Math.max(id, set.getId());
                    id++;
                }

                data.getCharts().add(new ChartSettings(id, newName));
                addedChartProperty().setValue(Boolean.TRUE);

                SelectionColumn selectColumn = new SelectionColumn(jeVisTree, dataSource, colorColumn, id, selectionColumns, column);
                selectColumn.setGraphDataModel(data);
                column.getColumns().add(column.getColumns().size() - NO_OF_COLUMNS, selectColumn.getSelectionColumn());
            }
        });

        column.setGraphic(addChart);

        for (int i = 0; i < getData().getCharts().size(); i++) {
            SelectionColumn selectColumn = new SelectionColumn(jeVisTree, dataSource, colorColumn, getData().getCharts().get(i).getId(), selectionColumns, column);
            selectColumn.setGraphDataModel(data);
            selectionColumns.add(selectColumn.getSelectionColumn());
        }

        AggregationColumn aggregationColumn = new AggregationColumn(jeVisTree, dataSource, I18n.getInstance().getString("graph.table.interval"));
        aggregationColumn.setGraphDataModel(data);

        DataProcessorColumn dataProcessorColumn = new DataProcessorColumn(jeVisTree, dataSource, I18n.getInstance().getString("graph.table.cleaning"));
        dataProcessorColumn.setGraphDataModel(data);

//        DateColumn startDateColumn = new DateColumn(jeVisTree, dataSource, I18n.getInstance().getString("graph.table.startdate"), DateColumn.DATE_TYPE.START);
//        startDateColumn.setGraphDataModel(data);
//        DateColumn endDateColumn = new DateColumn(jeVisTree, dataSource, I18n.getInstance().getString("graph.table.enddate"), DateColumn.DATE_TYPE.END);
//        endDateColumn.setGraphDataModel(data);
//        startDateColumn.getDateColumn(), endDateColumn.getDateColumn(),

        UnitColumn unitColumn = new UnitColumn(jeVisTree, dataSource, I18n.getInstance().getString("graph.table.unit"));
        unitColumn.setGraphDataModel(data);

        AxisColumn axisColumn = new AxisColumn(jeVisTree, dataSource, I18n.getInstance().getString("graph.table.axis"));
        axisColumn.setGraphDataModel(data);

        for (TreeTableColumn<JEVisTreeRow, Boolean> ttc : selectionColumns) column.getColumns().add(ttc);
        column.getColumns().addAll(colorColumn.getColorColumn(), aggregationColumn.getAggregationColumn(),
                dataProcessorColumn.getDataProcessorColumn(),
                unitColumn.getUnitColumn(), axisColumn.getAxisColumn());

        allColumns.add(column);
    }

    private int getMaxChartsFromService() {
        int max = 5;

        try {
            JEVisDataSource ds = getTree().getJEVisDataSource();
            JEVisClass graphClass = ds.getJEVisClass("Graph Plugin");
            List<JEVisObject> listGraphPlugins = ds.getObjects(graphClass, false);
            JEVisAttribute chartsPerAnalysisAttribute = listGraphPlugins.get(0).getAttribute("Number of Charts per Analysis");
            max = chartsPerAnalysisAttribute.getLatestSample().getValueAsLong().intValue();

        } catch (Exception e) {
        }

        return max;
    }

    public GraphDataModel getData() {
        return data;
    }

//    public void setData(GraphDataModel data) {
//        this.data = data;
//    }

    public void selectNone() {
        data.selectNone();
        jeVisTree.refresh();
    }

    public boolean isAddedChart() {
        return addedChart.get();
    }

    public SimpleBooleanProperty addedChartProperty() {
        return addedChart;
    }
}