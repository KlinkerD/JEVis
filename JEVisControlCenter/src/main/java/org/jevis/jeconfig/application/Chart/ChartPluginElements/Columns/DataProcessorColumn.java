package org.jevis.jeconfig.application.Chart.ChartPluginElements.Columns;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisObject;
import org.jevis.commons.chart.ChartDataModel;
import org.jevis.jeconfig.application.Chart.ChartPluginElements.Boxes.ProcessorBox;
import org.jevis.jeconfig.application.Chart.data.GraphDataModel;
import org.jevis.jeconfig.application.jevistree.JEVisTree;
import org.jevis.jeconfig.application.jevistree.JEVisTreeRow;

public class DataProcessorColumn extends TreeTableColumn<JEVisTreeRow, JEVisObject> implements ChartPluginColumn {
    public static String COLUMN_ID = "DataProcessorColumn";
    private TreeTableColumn<JEVisTreeRow, JEVisObject> dataProcessorColumn;
    private static final Logger logger = LogManager.getLogger(DataProcessorColumn.class);
    private GraphDataModel data;
    private JEVisTree tree;
    private String columnName;
    private final JEVisDataSource dataSource;

    /**
     * @author <gerrit.schutz@envidatec.com>Gerrit Schutz</gerrit.schutz@envidatec.com>
     */

    public DataProcessorColumn(JEVisTree tree, JEVisDataSource dataSource, String columnName) {
        this.tree = tree;
        this.dataSource = dataSource;
        this.columnName = columnName;
    }

    public TreeTableColumn<JEVisTreeRow, JEVisObject> getDataProcessorColumn() {
        return dataProcessorColumn;
    }

    @Override
    public void setGraphDataModel(GraphDataModel graphDataModel) {
        this.data = graphDataModel;
        update();
    }

    @Override
    public void buildColumn() {
        TreeTableColumn<JEVisTreeRow, JEVisObject> column = new TreeTableColumn(columnName);
        column.setPrefWidth(180);
        column.setEditable(true);
        column.setId(COLUMN_ID);

        column.setCellValueFactory(param -> {
            ChartDataModel data = getData(param.getValue().getValue());
            return new ReadOnlyObjectWrapper<>(data.getDataProcessor());
        });

        column.setCellFactory(new Callback<TreeTableColumn<JEVisTreeRow, JEVisObject>, TreeTableCell<JEVisTreeRow, JEVisObject>>() {

            @Override
            public TreeTableCell<JEVisTreeRow, JEVisObject> call(TreeTableColumn<JEVisTreeRow, JEVisObject> param) {

                TreeTableCell<JEVisTreeRow, JEVisObject> cell = new TreeTableCell<JEVisTreeRow, JEVisObject>() {

                    @Override
                    public void commitEdit(JEVisObject newValue) {
                        super.commitEdit(newValue);

                        ChartDataModel data = getData(getTreeTableRow().getItem());
                        if (newValue.equals(data.getObject())) {
                            data.setDataProcessor(null);
                        } else {
                            data.setDataProcessor(newValue);
                        }
                    }

                    @Override
                    protected void updateItem(JEVisObject item, boolean empty) {
                        super.updateItem(item, empty);

                        setText(null);
                        setGraphic(null);

                        if (!empty) {
                            try {
                                if (getTreeTableRow().getItem() != null && tree != null
                                        && tree.getFilter().showCell(column, getTreeTableRow().getItem())) {
                                    StackPane stackPane = new StackPane();

                                    ChartDataModel data = getData(getTreeTableRow().getItem());
                                    ProcessorBox box = new ProcessorBox(data.getObject(), data.getDataProcessor());

                                    box.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> commitEdit(newValue));

                                    stackPane.getChildren().setAll(box);

                                    StackPane.setAlignment(stackPane, Pos.CENTER_LEFT);

                                    box.setDisable(!data.isSelectable());
                                    setGraphic(stackPane);
                                }
                            } catch (Exception e) {
                                logger.error("could not build column item: " + e);
                            }
                        }

                    }

                };

                return cell;
            }
        });

        this.dataProcessorColumn = column;
    }

    @Override
    public GraphDataModel getData() {
        return this.data;
    }

    @Override
    public JEVisDataSource getDataSource() {
        return dataSource;
    }

}
