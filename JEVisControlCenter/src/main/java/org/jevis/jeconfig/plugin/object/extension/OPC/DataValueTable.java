package org.jevis.jeconfig.plugin.object.extension.OPC;


import com.jfoenix.controls.JFXDatePicker;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import org.apache.logging.log4j.LogManager;
import org.controlsfx.control.HiddenSidesPane;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadResult;
import org.jevis.commons.i18n.I18n;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeopc.OPCClient;
import org.joda.time.DateTime;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class DataValueTable {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(DataValueTable.class);
    private GridPane view = new GridPane();
    private TableView<DataValueRow> tableView = new TableView<>();
    private OPCClient opcClient;
    private HiddenSidesPane hiddenSidesPane = new HiddenSidesPane();
    private ObservableList<DataValueRow> list = FXCollections.observableArrayList();
    private ObservableList<DataValueRow> filteredList = FXCollections.observableArrayList();
    private CheckBox filterTrends = new CheckBox();
    private TextField filterFieldGroup= new TextField();
    private ObservableList<DataValueRow> nodeObservableList = FXCollections.observableArrayList();
    private final Image taskIcon = JEConfig.getImage("if_dashboard_46791.png");
    private Label fromLabel = new Label("From:");
    private Label untilLabel = new Label("Until:");
    private JFXDatePicker fromDatePicker = new JFXDatePicker();
    private JFXDatePicker untilDatePicker = new JFXDatePicker();
    private NodeId selectedtNodeId =null;
    private Task runningTask=null;

    public DataValueTable(OPCClient opcClient) {
        this.opcClient= opcClient;
        filterFieldGroup.setPromptText(I18n.getInstance().getString("plugin.object.role.filterprompt"));
        filterFieldGroup.textProperty().addListener((observable, oldValue, newValue) -> updateFilteredData());

        TableColumn<DataValueRow, String> dateCol = new TableColumn("Timestamp");//I18n.getInstance().getString("plugin.object.role.table.read"
        TableColumn<DataValueRow, String> valueCol = new TableColumn("Value");
        TableColumn<DataValueRow, String> qualityCol = new TableColumn("Quality");
        dateCol.setCellValueFactory(param -> param.getValue().tsProperty);
        valueCol.setCellValueFactory(param -> param.getValue().valueProperty);
        qualityCol.setCellValueFactory(param -> param.getValue().qualityProperty);

        dateCol.setPrefWidth(100);
        valueCol.setPrefWidth(300);


        tableView.getColumns().addAll(valueCol,dateCol,qualityCol);
        //tableView.setPrefSize(Double.MAX_VALUE,Double.MAX_VALUE);
        tableView.getSortOrder().add(valueCol);
        tableView.setMinSize(600,900);
        tableView.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        //hiddenSidesPane.setContent(tableView);
        view.addRow(0,filterTrends,filterFieldGroup);
        view.addRow(1,fromLabel,fromDatePicker);
        view.addRow(2,untilLabel,untilDatePicker);
        //view.setStyle("-fx-background-color:orangered;");


        //view.setGridLinesVisible(true);
        view.setPadding(new Insets(8));
        view.setHgap(8);
        view.add(tableView,0,3,3,1);
        GridPane.setFillWidth(tableView,true);
        GridPane.setFillHeight(tableView,true);
        //_view.setPinnedSide(Side.RIGHT)
        //_view.setRight(help);


        //readCol.setCellFactory(param -> new CheckBoxTableCell<>());





        filteredList.addAll(list);
        System.out.println("filteredList; "+filteredList.size());

        tableView.setItems(filteredList);

        list.addListener((ListChangeListener<DataValueRow>) change -> updateFilteredData());

        fromDatePicker.valueProperty().set(LocalDate.now());
        untilDatePicker.valueProperty().set(LocalDate.now());

        fromDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {updateTable(selectedtNodeId);});
        untilDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {updateTable(selectedtNodeId);});

        filterTrends.selectedProperty().addListener((observable, oldValue, newValue) -> {updateFilteredData();});
        filterTrends.setSelected(true);


        updateFilteredData();

    }

    public void updateTable(NodeId nodeID){
        System.out.println("updateTable: "+nodeID);
        selectedtNodeId=nodeID;
        if(selectedtNodeId!=null){
            if(runningTask!=null){
                runningTask.cancel(true);
                list.clear();
            }

            Task task = new Task() {
                @Override
                protected Object call() throws Exception {

                    DateTime fromts = new DateTime(fromDatePicker.valueProperty().get().getYear(), fromDatePicker.valueProperty().get().getMonthValue(), fromDatePicker.valueProperty().get().getDayOfMonth(), 0, 0, 0);
                    DateTime untilts = new DateTime(untilDatePicker.valueProperty().get().getYear(), untilDatePicker.valueProperty().get().getMonthValue(), untilDatePicker.valueProperty().get().getDayOfMonth(), 23, 59, 59);

                    logger.error("historyReadResult: id={} from={} until={},",selectedtNodeId.getIdentifier(),fromts,untilts);
                    HistoryReadResult historyReadResult = opcClient.getHistory(selectedtNodeId,fromts,untilts);
                    List<DataValue> valueList = opcClient.getDateValues(historyReadResult);
                    valueList.forEach(dataValue -> {
                        try {
                            System.out.println("sample: " + dataValue);
                            list.add(new DataValueRow(dataValue));
                        }catch (Exception ex){
                            logger.error("DataValue error; {}",ex);
                        }
                    });
                    System.out.println("done");
                    super.done();
                    return null;
                }
            };
            runningTask = task;
            JEConfig.getStatusBar().addTask(OPCBrowser.class.getName(),task, taskIcon,true);
        }

    }


    public GridPane getView(){
        return view;
    }

    private void updateFilteredData() {
        Platform.runLater(()->{
            filteredList.clear();

            try {
                for (DataValueRow p : list) {
                    boolean isFilerMatch = matchesFilter(p);
                    if (filterTrends.isSelected()) {
                        //filteredList.add(p);

                        if (isFilerMatch) {
                            filteredList.add(p);
                        }

                    } else if (isFilerMatch) {
                        filteredList.add(p);
                    }

                }

            }catch (Exception ex){
                /** todo: fix ConcurrentModificationException **/
            }
            // Must re-sort table after items changed
            reapplyTableSortOrder();
        });

    }

    private void reapplyTableSortOrder() {
        ArrayList<TableColumn<DataValueRow, ?>> sortOrder = new ArrayList<>(tableView.getSortOrder());
        tableView.getSortOrder().clear();
        tableView.getSortOrder().addAll(sortOrder);
    }

    private boolean matchesFilter(DataValueRow node) {
        String filterString = filterFieldGroup.getText();
        if (filterString == null || filterString.isEmpty()) {
            // No filter --> Add all.
            return true;
        }
        String lowerCaseFilterString = filterString.toLowerCase();


        if (node.valueProperty.getValue().toLowerCase().indexOf(lowerCaseFilterString) != -1) {
            return true;
        } else if (node.tsProperty.getValue().toLowerCase().indexOf(lowerCaseFilterString) != -1) {
            return true;
        }


        return false; // Does not match
    }

    public void setData(){

    }

}