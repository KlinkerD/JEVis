package org.jevis.application.Chart.ChartPluginElements;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import org.jevis.application.Chart.ChartDataModel;
import org.jevis.application.Chart.data.GraphDataModel;
import org.jevis.application.application.AppLocale;
import org.jevis.application.application.SaveResourceBundle;
import org.jevis.application.jevistree.JEVisTree;
import org.jevis.application.jevistree.JEVisTreeRow;
import org.jevis.application.jevistree.plugin.ChartPlugin;
import org.jevis.commons.dataprocessing.AggregationPeriod;

/**
 * @author <gerrit.schutz@envidatec.com>Gerrit Schutz</gerrit.schutz@envidatec.com>
 */

public class AggregationColumn extends TreeTableColumn<JEVisTreeRow, AggregationPeriod> implements ChartPluginColumn {
    private final Image imgMarkAll = new Image(ChartPlugin.class.getResourceAsStream("/icons/" + "jetxee-check-sign-and-cross-sign-3.png"));
    private SaveResourceBundle rb = new SaveResourceBundle("jeapplication", AppLocale.getInstance().getLocale());
    private final Tooltip tpMarkAll = new Tooltip(rb.getString("plugin.graph.dialog.changesettings.tooltip.forall"));
    private TreeTableColumn<JEVisTreeRow, AggregationPeriod> aggregationColumn;
    private GraphDataModel data;
    private JEVisTree tree;
    private String columnName;

    public AggregationColumn(JEVisTree tree, String columnName) {
        this.tree = tree;
        this.columnName = columnName;
    }


    public TreeTableColumn<JEVisTreeRow, AggregationPeriod> getAggregationColumn() {
        return aggregationColumn;
    }

    @Override
    public void setGraphDataModel(GraphDataModel graphDataModel) {
        this.data = graphDataModel;

        update();
    }

    @Override
    public void buildColumn() {
        TreeTableColumn<JEVisTreeRow, AggregationPeriod> column = new TreeTableColumn(columnName);
        column.setPrefWidth(120);
        column.setMinWidth(100);

        column.setCellValueFactory(param -> {

            ChartDataModel data = getData(param.getValue().getValue());

            return new ReadOnlyObjectWrapper<>(data.getAggregationPeriod());
        });

        column.setCellFactory(new Callback<TreeTableColumn<JEVisTreeRow, AggregationPeriod>, TreeTableCell<JEVisTreeRow, AggregationPeriod>>() {

            @Override
            public TreeTableCell<JEVisTreeRow, AggregationPeriod> call(TreeTableColumn<JEVisTreeRow, AggregationPeriod> param) {

                TreeTableCell<JEVisTreeRow, AggregationPeriod> cell = new TreeTableCell<JEVisTreeRow, AggregationPeriod>() {

                    @Override
                    public void commitEdit(AggregationPeriod newValue) {
                        super.commitEdit(newValue);
                    }

                    @Override
                    protected void updateItem(AggregationPeriod item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) {
                            StackPane stackPane = new StackPane();

                            if (getTreeTableRow().getItem() != null && tree != null && tree.getFilter().showColumn(getTreeTableRow().getItem(), columnName)) {
                                ChartDataModel data = getData(getTreeTableRow().getItem());

                                AggregationBox aggBox = new AggregationBox(data);

                                ImageView imageMarkAll = new ImageView(imgMarkAll);
                                imageMarkAll.fitHeightProperty().set(12);
                                imageMarkAll.fitWidthProperty().set(12);

                                Button tb = new Button("", imageMarkAll);

                                tb.setTooltip(tpMarkAll);

                                tb.setOnAction(event -> {
                                    AggregationPeriod selection = AggregationPeriod.parseAggregation(aggBox.getAggregationBox().getSelectionModel().getSelectedItem().toString());
                                    getData().getSelectedData().forEach(mdl -> {
                                        if (mdl.getSelected()) {
                                            mdl.setAggregationPeriod(selection);
                                        }
                                    });

                                    tree.refresh();
                                });

                                HBox hbox = new HBox();
                                hbox.getChildren().addAll(aggBox.getAggregationBox(), tb);
                                stackPane.getChildren().add(hbox);
                                StackPane.setAlignment(stackPane, Pos.CENTER_LEFT);

                                aggBox.getAggregationBox().setDisable(!data.isSelectable());

                            }

                            setText(null);
                            setGraphic(stackPane);
                        } else {
                            setText(null);
                            setGraphic(null);
                        }

                    }

                };

                return cell;
            }
        });

        this.aggregationColumn = column;
    }

    @Override
    public GraphDataModel getData() {
        return this.data;
    }
}
