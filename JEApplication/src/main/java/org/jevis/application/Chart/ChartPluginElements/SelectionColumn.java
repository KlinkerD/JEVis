package org.jevis.application.Chart.ChartPluginElements;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.jevis.application.Chart.ChartDataModel;
import org.jevis.application.Chart.ChartSettings;
import org.jevis.application.Chart.ChartType;
import org.jevis.application.Chart.data.GraphDataModel;
import org.jevis.application.application.AppLocale;
import org.jevis.application.application.SaveResourceBundle;
import org.jevis.application.jevistree.JEVisTree;
import org.jevis.application.jevistree.JEVisTreeRow;
import org.jevis.application.tools.DisabledItemsComboBox;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <gerrit.schutz@envidatec.com>Gerrit Schutz</gerrit.schutz@envidatec.com>
 */

public class SelectionColumn extends TreeTableColumn<JEVisTreeRow, Boolean> implements ChartPluginColumn {
    public static String COLUMN_ID = "SelectionColumn";
    private SaveResourceBundle rb = new SaveResourceBundle("jeapplication", AppLocale.getInstance().getLocale());
    private TreeTableColumn<JEVisTreeRow, Boolean> selectionColumn;
    private GraphDataModel data;
    private JEVisTree tree;
    private ColorColumn colorColumn;
    private String chartName;
    private Integer selectionColumnIndex;

    public SelectionColumn(JEVisTree tree, ColorColumn colorColumn, Integer selectionColumnIndex, String chartName) {
        this.tree = tree;
        this.colorColumn = colorColumn;
        this.chartName = chartName;
        this.selectionColumnIndex = selectionColumnIndex;
    }


    public TreeTableColumn<JEVisTreeRow, Boolean> getSelectionColumn() {
        return selectionColumn;
    }

    @Override
    public void setGraphDataModel(GraphDataModel graphDataModel) {
        this.data = graphDataModel;
        this.data.addObserver(this);
        update();
    }

    @Override
    public void buildColumn() {
        String chartTitle = rb.getString("graph.title");
        AtomicReference<String> chartName = new AtomicReference<>();
        if (this.chartName != null) chartName.set(this.chartName);
        else chartName.set(chartTitle);

        TreeTableColumn<JEVisTreeRow, Boolean> column = new TreeTableColumn("selection" + selectionColumnIndex);
        column.setId(COLUMN_ID);
        column.setPrefWidth(120);
        column.setEditable(true);
        column.setResizable(false);


        column.setCellValueFactory(param -> {
            ChartDataModel data = getData(param.getValue().getValue());
            //Boolean selectedChart = data.getSelectedcharts().contains(getData().getChartsList().get(selectionColumnIndex));
            Boolean selectedChart = data.getSelectedcharts().contains(chartName.get());
            return new ReadOnlyObjectWrapper<>(data.getSelected() && selectedChart);
        });

        VBox vbox = new VBox();

        TextField textFieldChartName = new TextField(chartName.get());
        textFieldChartName.setPrefWidth(120);
        textFieldChartName.setText(chartName.get());
        textFieldChartName.setEditable(false);
        textFieldChartName.setDisable(true);
        Tooltip tt = new Tooltip("Column id: " + selectionColumnIndex);//DEBUG, remove later
        textFieldChartName.setTooltip(tt);

        DisabledItemsComboBox<String> comboBoxChartType = new DisabledItemsComboBox(ChartType.getlistNamesChartTypes());
        comboBoxChartType.setDisable(true);
        List<String> disabledItems = Arrays.asList(rb.getString("plugin.graph.charttype.scatter.name"),
                rb.getString("plugin.graph.charttype.bubble.name"));
        comboBoxChartType.setDisabledItems(disabledItems);

        if (getData() != null && getData().getCharts() != null && !getData().getCharts().isEmpty()) {
            if (!chartName.get().equals(chartTitle)) {

                final AtomicReference<Boolean> foundChart = new AtomicReference<>(false);
                getData().getCharts().forEach(chart -> {
                    if (chart.getName().equals(chartName.get())) {
                        comboBoxChartType.getSelectionModel().select(ChartType.parseChartIndex(chart.getChartType()));
                        foundChart.set(true);
                    }
                });
                if (!foundChart.get()) comboBoxChartType.getSelectionModel().selectFirst();

            } else {
                comboBoxChartType.getSelectionModel().selectFirst();
            }
        } else {
            if (getData() != null) {
                if (getData().getCharts() == null) getData().setCharts(new HashSet<>());

                if (!chartName.get().equals(chartTitle)) getData().getCharts().add(new ChartSettings(chartTitle));
                else {
                    getData().getCharts().add(new ChartSettings(chartTitle));
                    textFieldChartName.setText(chartTitle);
                    chartName.set(chartTitle);
                }
                comboBoxChartType.getSelectionModel().selectFirst();
            }
        }

        /**
         * Adding a listener for the Chart name
         */

        textFieldChartName.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                getData().getSelectedData().forEach(mdl -> {
                    if (mdl.getSelected()) {
                        if (mdl.getSelectedcharts().contains(oldValue)) {
                            mdl.getSelectedcharts().set(mdl.getSelectedcharts().indexOf(oldValue), newValue);
                        }
                    }
                });
                AtomicReference<ChartSettings> set = new AtomicReference<>();

                getData().getCharts().forEach(chartSettings -> {
                    if (chartSettings.getName().equals(oldValue)) {
                        set.set(chartSettings);
                    }
                });
                getData().getCharts().remove(set.get());
                set.get().setName(newValue);

                getData().getCharts().add(set.get());

                getData().getChartsList().set(selectionColumnIndex, newValue);
            }
        });

        /**
         * Adding a Listener for the Chart Type
         */

        comboBoxChartType.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue == null || newValue != oldValue) {
                getData().getCharts().forEach(chart -> {
                    if (chart.getName().equals(textFieldChartName.getText())) {
                        ChartType type = ChartType.parseChartType(comboBoxChartType.getSelectionModel().getSelectedIndex());
                        chart.setChartType(type);
                    }
                });
            }
        });

        vbox.getChildren().addAll(textFieldChartName, comboBoxChartType);

        column.setGraphic(vbox);

        column.setCellFactory(new Callback<TreeTableColumn<JEVisTreeRow, Boolean>, TreeTableCell<JEVisTreeRow, Boolean>>() {

            @Override
            public TreeTableCell<JEVisTreeRow, Boolean> call(TreeTableColumn<JEVisTreeRow, Boolean> param) {

                TreeTableCell<JEVisTreeRow, Boolean> cell = new TreeTableCell<JEVisTreeRow, Boolean>() {


                    @Override
                    public void commitEdit(Boolean newValue) {
                        super.commitEdit(newValue);
                        getTreeTableRow().getItem().getObjectSelectedProperty().setValue(newValue);
                        ChartDataModel data = getData(getTreeTableRow().getItem());
                        data.setSelected(newValue);

                        //String selectedChart = data.getSelectedcharts().get(selectionColumnIndex);
                        if (newValue) {
                            if (!data.getSelectedcharts().contains(chartName.get())) {

                                data.getSelectedcharts().add(chartName.get());
                            }
                        } else {
                            data.getSelectedcharts().remove(chartName.get());
                        }

                    }

                    @Override
                    protected void updateItem(Boolean item, boolean empty) {
                        super.updateItem(item, empty);

                        setText(null);
                        setGraphic(null);

                        if (!empty) {
                            try {
                                if (getTreeTableRow().getItem() != null
                                        && tree != null
                                        && tree.getFilter().showCell(column, getTreeTableRow().getItem())) {

                                    CheckBox cbox = new CheckBox();
                                    StackPane stackPane = new StackPane();
                                    Tooltip debugTT = new Tooltip("ID: " + cbox.getId());//Debug, remove

                                    ChartDataModel data = getData(getTreeTableRow().getItem());

                                    stackPane.getChildren().setAll(cbox);
                                    StackPane.setAlignment(stackPane, Pos.CENTER_LEFT);

                                    cbox.setOnAction(event -> {
                                        try {
                                            commitEdit(cbox.isSelected());

                                            if (cbox.isSelected()) {
                                                /**
                                                 * if the choicebox is selected, get a color for it
                                                 */
                                                for (Color c : colorColumn.getColorList()) {
                                                    if (!colorColumn.getUsedColors().contains(c)) {
                                                        data.setColor(c);
                                                        colorColumn.getUsedColors().add(c);
                                                        Platform.runLater(() -> {
                                                            JEVisTreeRow sobj = new JEVisTreeRow(getTreeTableRow().getTreeItem().getValue().getJEVisObject());
                                                            getTreeTableRow().getTreeItem().setValue(sobj);
                                                        });
                                                        break;
                                                    }
                                                }
                                            } else {
                                                /**
                                                 * if the box is unselected, remove the color
                                                 */
                                                colorColumn.getUsedColors().remove(data.getColor());
                                                data.setColor(Color.LIGHTBLUE);
                                                Platform.runLater(() -> {
                                                    JEVisTreeRow sobj = new JEVisTreeRow(getTreeTableRow().getTreeItem().getValue().getJEVisObject());
                                                    getTreeTableRow().getTreeItem().setValue(sobj);
                                                });

                                                if (getData().getChartsList().contains(chartName.get())) {
                                                    /**
                                                     * check for other data rows within this chart
                                                     */
                                                    AtomicReference<Boolean> foundOther = new AtomicReference<>(false);
                                                    getData().getSelectedData().forEach(chartDataModel -> {
                                                        if (chartDataModel.getSelectedcharts().contains(chartName.get()))
                                                            foundOther.set(true);
                                                    });

                                                    if (!foundOther.get()) {
                                                        if (getData().getCharts().size() > 1) {
                                                            getTableColumn().setVisible(false);
                                                            getTableColumn().getParentColumn().getColumns().remove(getTableColumn());

                                                            List<ChartSettings> toBeRemovedCharts = new ArrayList<>();
                                                            getData().getCharts().forEach(chartSettings -> {
                                                                if (chartSettings.getName().equals(chartName.get()))
                                                                    toBeRemovedCharts.add(chartSettings);
                                                            });
                                                            getData().getCharts().removeAll(toBeRemovedCharts);
                                                            getData().getChartsList().remove(chartName.get());
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }
                                    });

                                    cbox.setTooltip(debugTT);
                                    cbox.setSelected(item);
                                    setFieldsEditable(textFieldChartName, comboBoxChartType, cbox.isSelected());

                                    cbox.setDisable(!data.isSelectable());

                                    setText(null);
                                    setGraphic(stackPane);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    private void setFieldsEditable(TextField textFieldChartName, DisabledItemsComboBox<String> comboBoxChartType, Boolean item) {
                        if (item) {
                            textFieldChartName.setEditable(true);
                            textFieldChartName.setDisable(false);
                            comboBoxChartType.setDisable(false);
                        } else {
                            AtomicReference<Boolean> foundSelected = new AtomicReference<>(false);
                            getData().getSelectedData().forEach(mdl -> {
                                if (mdl.getSelected() && mdl.getSelectedcharts().contains(textFieldChartName.getText())) {
                                    foundSelected.set(true);
                                }
                            });
                            if (foundSelected.get()) {
                                textFieldChartName.setEditable(true);
                                textFieldChartName.setDisable(false);
                                comboBoxChartType.setDisable(false);
                            } else {
                                textFieldChartName.setEditable(false);
                                textFieldChartName.setDisable(true);
                                comboBoxChartType.setDisable(true);
                            }
                        }
                    }
                };
                return cell;
            }
        });

        this.selectionColumn = column;
    }

    @Override
    public GraphDataModel getData() {
        return this.data;
    }

    @Override
    public void update(Observable o, Object arg) {
        update();
    }
}
