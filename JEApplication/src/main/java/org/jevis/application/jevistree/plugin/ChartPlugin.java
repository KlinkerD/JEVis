/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.application.jevistree.plugin;

import javafx.scene.control.Button;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisClass;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisObject;
import org.jevis.application.Chart.ChartPluginElements.*;
import org.jevis.application.Chart.ChartSettings;
import org.jevis.application.Chart.data.GraphDataModel;
import org.jevis.application.application.AppLocale;
import org.jevis.application.application.SaveResourceBundle;
import org.jevis.application.jevistree.JEVisTree;
import org.jevis.application.jevistree.JEVisTreeRow;
import org.jevis.application.jevistree.TreePlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * @author
 */
public class ChartPlugin implements TreePlugin {
    private static final Logger logger = LogManager.getLogger(ChartPlugin.class);
    private final Image img = new Image(ChartPlugin.class.getResourceAsStream("/icons/" + "list-add.png"));
    private final ImageView image = new ImageView(img);
    private JEVisTree jeVisTree;
    private SaveResourceBundle rb = new SaveResourceBundle("jeapplication", AppLocale.getInstance().getLocale());
    private final String chartTitle = rb.getString("graph.title");
    private GraphDataModel data;
    private List<TreeTableColumn<JEVisTreeRow, Long>> allColumns;
    private int numberOfChartsPerAnalysis = 5;

    public JEVisTree getTree() {
        return jeVisTree;
    }

    @Override
    public void setTree(JEVisTree tree) {
        jeVisTree = tree;
        if (data == null)
            data = new GraphDataModel(jeVisTree.getJEVisDataSource());
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

        TreeTableColumn<JEVisTreeRow, Long> column = new TreeTableColumn();
        column.setEditable(true);

        image.fitHeightProperty().set(20);
        image.fitWidthProperty().set(20);

        Button addChart = new Button(rb.getString("graph.table.addchart"), image);
        if (getData().getCharts().isEmpty()) {
            data.getCharts().add(new ChartSettings(0, chartTitle));
        }

        ColorColumn colorColumn = new ColorColumn(jeVisTree, rb.getString("graph.table.color"));
        colorColumn.setGraphDataModel(data);

        addChart.setOnAction(event -> {
            if (data.getCharts().size() < 5) {
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

                SelectionColumn selectColumn = new SelectionColumn(jeVisTree, colorColumn, id);
                selectColumn.setGraphDataModel(data);
                column.getColumns().add(column.getColumns().size() - 6, selectColumn.getSelectionColumn());
            }
        });

        column.setGraphic(addChart);

        List<TreeTableColumn> selectionColumns = new ArrayList<>();

        for (int i = 0; i < getData().getCharts().size(); i++) {
            SelectionColumn selectColumn = new SelectionColumn(jeVisTree, colorColumn, getData().getCharts().get(i).getId());
            selectColumn.setGraphDataModel(data);
            selectionColumns.add(selectColumn.getSelectionColumn());
        }

        AggregationColumn aggregationColumn = new AggregationColumn(jeVisTree, rb.getString("graph.table.interval"));
        aggregationColumn.setGraphDataModel(data);

        DataProcessorColumn dataProcessorColumn = new DataProcessorColumn(jeVisTree, rb.getString("graph.table.cleaning"));
        dataProcessorColumn.setGraphDataModel(data);

        DateColumn startDateColumn = new DateColumn(jeVisTree, rb.getString("graph.table.startdate"), DateColumn.DATE_TYPE.START);
        startDateColumn.setGraphDataModel(data);
        DateColumn endDateColumn = new DateColumn(jeVisTree, rb.getString("graph.table.enddate"), DateColumn.DATE_TYPE.END);
        endDateColumn.setGraphDataModel(data);

        UnitColumn unitColumn = new UnitColumn(jeVisTree, rb.getString("graph.table.unit"));
        unitColumn.setGraphDataModel(data);

        for (TreeTableColumn ttc : selectionColumns) column.getColumns().add(ttc);
        column.getColumns().addAll(colorColumn.getColorColumn(), aggregationColumn.getAggregationColumn(),
                dataProcessorColumn.getDataProcessorColumn(), startDateColumn.getDateColumn(), endDateColumn.getDateColumn(),
                unitColumn.getUnitColumn());

        allColumns.add(column);
    }

    public GraphDataModel getData() {
        return data;
    }

    public void setData(GraphDataModel data) {
        this.data = data;
    }

    public void selectNone() {
        data.selectNone();
        jeVisTree.refresh();
    }

    public int getNumberOfChartsPerAnalysis() {
        retrieveNumberOfChartsPerAnalysisFromJEVis();
        return numberOfChartsPerAnalysis;
    }

    private void retrieveNumberOfChartsPerAnalysisFromJEVis() {
        try {
            JEVisDataSource ds = getTree().getJEVisDataSource();
            JEVisClass graphClass = ds.getJEVisClass("Graph Plugin");
            List<JEVisObject> listGraphPlugins = ds.getObjects(graphClass, false);
            JEVisAttribute chartsPerAnalysisAttribute = listGraphPlugins.get(0).getAttribute("Number of Charts per Analysis");
            numberOfChartsPerAnalysis = chartsPerAnalysisAttribute.getLatestSample().getValueAsLong().intValue();
        } catch (Exception e) {

        }
    }
}
