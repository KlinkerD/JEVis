package org.jevis.jeconfig.plugin.alarms;

import com.jfoenix.controls.JFXDatePicker;
import com.sun.javafx.scene.control.skin.TableViewSkin;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.dialog.ProgressDialog;
import org.jevis.api.*;
import org.jevis.commons.alarm.Alarm;
import org.jevis.commons.alarm.AlarmConfiguration;
import org.jevis.commons.alarm.AlarmType;
import org.jevis.commons.datetime.DateHelper;
import org.jevis.commons.json.JsonAlarm;
import org.jevis.commons.json.JsonTools;
import org.jevis.commons.unit.UnitManager;
import org.jevis.jeconfig.GlobalToolBar;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.Plugin;
import org.jevis.jeconfig.application.Chart.TimeFrame;
import org.jevis.jeconfig.tool.I18n;
import org.joda.time.DateTime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmPlugin implements Plugin {
    private static final Logger logger = LogManager.getLogger(AlarmPlugin.class);
    public static String PLUGIN_NAME = "Alarm Plugin";
    public static String ALARM_CONFIG_CLASS = "Alarm Configuration";
    private final JEVisDataSource ds;
    private final String title;
    private final BorderPane borderPane = new BorderPane();
    private final ToolBar toolBar = new ToolBar();
    private final int iconSize = 24;
    private boolean initialized = false;
    private static Method columnToFitMethod;
    private DateHelper dateHelper = new DateHelper(DateHelper.TransformType.TODAY);
    private ComboBox<TimeFrame> timeFrameComboBox;
    private SimpleBooleanProperty hasAlarms = new SimpleBooleanProperty(false);
    private ObservableMap<DateTime, Boolean> activeAlarms = FXCollections.observableHashMap();
    private ExecutorService executor = Executors.newFixedThreadPool(1);

    static {
        try {
            columnToFitMethod = TableViewSkin.class.getDeclaredMethod("resizeColumnToFitContent", TableColumn.class, int.class);
            columnToFitMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private TableView<AlarmRow> tableView = new TableView<>();
    private NumberFormat numberFormat = NumberFormat.getNumberInstance(I18n.getInstance().getLocale());
    private DateTime start;
    private DateTime end;
    private TimeFrame timeFrame;
    private ObservableList<AlarmRow> alarmRows = FXCollections.observableArrayList();

    public AlarmPlugin(JEVisDataSource ds, String title) {
        this.ds = ds;
        this.title = title;

        this.borderPane.setCenter(tableView);
        this.tableView.setItems(alarmRows);

        this.numberFormat.setMinimumFractionDigits(2);
        this.numberFormat.setMaximumFractionDigits(2);

        createColumns();

        initToolBar();

        this.activeAlarms.addListener((MapChangeListener<? super DateTime, ? super Boolean>) change -> {
            if (activeAlarms.isEmpty()) {
                hasAlarms.set(false);
            } else {
                hasAlarms.set(true);
            }
        });

    }

    public static void autoFitTable(TableView tableView) {
        tableView.getItems().addListener(new ListChangeListener<Object>() {
            @Override
            public void onChanged(Change<?> c) {
                for (Object column : tableView.getColumns()) {
                    try {
                        columnToFitMethod.invoke(tableView.getSkin(), column, -1);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void createColumns() {
        TableColumn<AlarmRow, DateTime> dateColumn = new TableColumn<>(I18n.getInstance().getString("plugin.alarm.table.date"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<AlarmRow, DateTime>("date"));
        dateColumn.setStyle("-fx-alignment: CENTER;");
        dateColumn.setSortable(true);
//        dateColumn.setPrefWidth(160);
        dateColumn.setMinWidth(100);

        dateColumn.setCellValueFactory(param -> {
            if (param != null && param.getValue() != null && param.getValue().getTimeStamp() != null)
                return new SimpleObjectProperty<>(param.getValue().getTimeStamp());
            else return new SimpleObjectProperty<>();
        });

        dateColumn.setCellFactory(new Callback<TableColumn<AlarmRow, DateTime>, TableCell<AlarmRow, DateTime>>() {
            @Override
            public TableCell<AlarmRow, DateTime> call(TableColumn<AlarmRow, DateTime> param) {
                return new TableCell<AlarmRow, DateTime>() {
                    @Override
                    protected void updateItem(DateTime item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setText(item.toString("yyyy-MM-dd HH:mm:ss"));

                            if (getTableRow() != null && getTableRow().getItem() != null) {
                                AlarmRow alarmRow = (AlarmRow) getTableRow().getItem();
                                if (!alarmRow.getAlarmConfiguration().isChecked()) {
                                    setTextFill(Color.RED);
                                    activeAlarms.put(alarmRow.getTimeStamp(), true);
                                } else {
                                    setTextFill(Color.BLACK);
                                    activeAlarms.remove(alarmRow.getTimeStamp());
                                }
                            }
                        }
                    }
                };
            }
        });


        TableColumn<AlarmRow, String> nameColumn = new TableColumn<>(I18n.getInstance().getString("plugin.alarm.table.name"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<AlarmRow, String>("name"));
        nameColumn.setStyle("-fx-alignment: CENTER;");
        nameColumn.setSortable(true);
//        nameColumn.setPrefWidth(500);
        nameColumn.setMinWidth(100);

        nameColumn.setCellValueFactory(param -> {
            if (param != null && param.getValue() != null && param.getValue().getTimeStamp() != null)
                return new SimpleObjectProperty<>(param.getValue().getAlarmConfiguration().getName());
            else return new SimpleObjectProperty<>();
        });

        nameColumn.setCellFactory(new Callback<TableColumn<AlarmRow, String>, TableCell<AlarmRow, String>>() {
            @Override
            public TableCell<AlarmRow, String> call(TableColumn<AlarmRow, String> param) {
                return new TableCell<AlarmRow, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setText(item);
                        }
                    }
                };
            }
        });

        TableColumn<AlarmRow, Double> isValueColumn = new TableColumn<>(I18n.getInstance().getString("plugin.alarm.table.isValue"));
        isValueColumn.setCellValueFactory(new PropertyValueFactory<AlarmRow, Double>("isValue"));
        isValueColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        isValueColumn.setSortable(false);
//        isValueColumn.setPrefWidth(500);
        isValueColumn.setMinWidth(100);

        isValueColumn.setCellValueFactory(param -> {
            if (param != null && param.getValue() != null && param.getValue().getAlarm() != null && param.getValue().getAlarm().getIsValue() != null)
                return new SimpleObjectProperty<>(param.getValue().getAlarm().getIsValue());
            else return new SimpleObjectProperty<>();
        });

        isValueColumn.setCellFactory(new Callback<TableColumn<AlarmRow, Double>, TableCell<AlarmRow, Double>>() {
            @Override
            public TableCell<AlarmRow, Double> call(TableColumn<AlarmRow, Double> param) {
                return new TableCell<AlarmRow, Double>() {
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            String text = "";
                            text += numberFormat.format(item);
                            if (getTableRow() != null && getTableRow().getItem() != null) {
                                AlarmRow alarmRow = (AlarmRow) getTableRow().getItem();
                                try {
                                    text += " " + UnitManager.getInstance().format(alarmRow.getAlarm().getAttribute().getDisplayUnit());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            setText(text);
                        }
                    }
                };
            }
        });

        TableColumn<AlarmRow, Double> shouldBeValueColumn = new TableColumn<>(I18n.getInstance().getString("plugin.alarm.table.shouldBeValue"));
        shouldBeValueColumn.setCellValueFactory(new PropertyValueFactory<AlarmRow, Double>("shouldBeValue"));
        shouldBeValueColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        shouldBeValueColumn.setSortable(false);
//        shouldBeValueColumn.setPrefWidth(500);
        shouldBeValueColumn.setMinWidth(100);

        shouldBeValueColumn.setCellValueFactory(param -> {
            if (param != null && param.getValue() != null && param.getValue().getAlarm() != null && param.getValue().getAlarm().getShouldBeValue() != null)
                return new SimpleObjectProperty<>(param.getValue().getAlarm().getShouldBeValue());
            else return new SimpleObjectProperty<>();
        });

        shouldBeValueColumn.setCellFactory(new Callback<TableColumn<AlarmRow, Double>, TableCell<AlarmRow, Double>>() {
            @Override
            public TableCell<AlarmRow, Double> call(TableColumn<AlarmRow, Double> param) {
                return new TableCell<AlarmRow, Double>() {
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            String text = "";
                            text += numberFormat.format(item);
                            if (getTableRow() != null && getTableRow().getItem() != null) {
                                AlarmRow alarmRow = (AlarmRow) getTableRow().getItem();
                                try {
                                    text += " " + UnitManager.getInstance().format(alarmRow.getAlarm().getAttribute().getDisplayUnit());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            setText(text);
                        }
                    }
                };
            }
        });

        TableColumn<AlarmRow, Double> toleranceColumn = new TableColumn<>(I18n.getInstance().getString("plugin.alarm.table.tolerance"));
        toleranceColumn.setCellValueFactory(new PropertyValueFactory<AlarmRow, Double>("tolerance"));
        toleranceColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        toleranceColumn.setSortable(false);
//        toleranceColumn.setPrefWidth(500);
        toleranceColumn.setMinWidth(100);

        toleranceColumn.setCellValueFactory(param -> {
            if (param != null && param.getValue() != null && param.getValue().getAlarm() != null && param.getValue().getAlarm().getTolerance() != null)
                return new SimpleObjectProperty<>(param.getValue().getAlarm().getTolerance());
            else return new SimpleObjectProperty<>();
        });

        toleranceColumn.setCellFactory(new Callback<TableColumn<AlarmRow, Double>, TableCell<AlarmRow, Double>>() {
            @Override
            public TableCell<AlarmRow, Double> call(TableColumn<AlarmRow, Double> param) {
                return new TableCell<AlarmRow, Double>() {
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setText(numberFormat.format(item) + "%");
                        }
                    }
                };
            }
        });

        TableColumn<AlarmRow, AlarmType> alarmTypeColumn = new TableColumn<>(I18n.getInstance().getString("plugin.alarm.table.alarmType"));
        alarmTypeColumn.setCellValueFactory(new PropertyValueFactory<AlarmRow, AlarmType>("alarmType"));
        alarmTypeColumn.setStyle("-fx-alignment: CENTER;");
        alarmTypeColumn.setSortable(false);
//        alarmTypeColumn.setPrefWidth(500);
        alarmTypeColumn.setMinWidth(100);

        alarmTypeColumn.setCellValueFactory(param -> {
            if (param != null && param.getValue() != null && param.getValue().getAlarm() != null && param.getValue().getAlarm().getAlarmType() != null)
                return new SimpleObjectProperty<>(param.getValue().getAlarm().getAlarmType());
            else return new SimpleObjectProperty<>();
        });

        alarmTypeColumn.setCellFactory(new Callback<TableColumn<AlarmRow, AlarmType>, TableCell<AlarmRow, AlarmType>>() {
            @Override
            public TableCell<AlarmRow, AlarmType> call(TableColumn<AlarmRow, AlarmType> param) {
                return new TableCell<AlarmRow, AlarmType>() {
                    @Override
                    protected void updateItem(AlarmType item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setText(getTranslatedTypeName(item.toString()));
                        }
                    }
                };
            }
        });

        TableColumn<AlarmRow, Boolean> confirmationColumn = new TableColumn<>(I18n.getInstance().getString("plugin.alarm.table.confirmation"));
        confirmationColumn.setCellValueFactory(new PropertyValueFactory<AlarmRow, Boolean>("confirmation"));
        confirmationColumn.setStyle("-fx-alignment: CENTER;");
        confirmationColumn.setSortable(false);
//        alarmTypeColumn.setPrefWidth(500);
        confirmationColumn.setMinWidth(100);

        confirmationColumn.setCellValueFactory(param -> {
            if (param != null && param.getValue() != null && param.getValue().getAlarm() != null && param.getValue().getAlarm().getAlarmType() != null)
                return new SimpleObjectProperty<>(param.getValue().getAlarmConfiguration().isChecked());
            else return new SimpleObjectProperty<>();
        });

        confirmationColumn.setCellFactory(new Callback<TableColumn<AlarmRow, Boolean>, TableCell<AlarmRow, Boolean>>() {
            @Override
            public TableCell<AlarmRow, Boolean> call(TableColumn<AlarmRow, Boolean> param) {
                return new TableCell<AlarmRow, Boolean>() {
                    @Override
                    protected void updateItem(Boolean item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            ImageView checked = JEConfig.getImage("1404237035_Valid.png", iconSize, iconSize);
                            ImageView notChecked = JEConfig.getImage("1404237042_Error.png", iconSize, iconSize);

                            ToggleButton checkedButton = new ToggleButton("");
                            if (item) {
                                checkedButton.setGraphic(checked);
                            } else {
                                checkedButton.setGraphic(notChecked);
                            }
                            Tooltip checkedTooltip = new Tooltip(I18n.getInstance().getString("plugin.alarms.tooltip.checked"));
                            checkedButton.setTooltip(checkedTooltip);
                            GlobalToolBar.changeBackgroundOnHoverUsingBinding(checkedButton);
                            checkedButton.setSelected(item);
                            checkedButton.styleProperty().bind(
                                    Bindings
                                            .when(checkedButton.hoverProperty())
                                            .then(
                                                    new SimpleStringProperty("-fx-background-insets: 1 1 1;"))
                                            .otherwise(Bindings
                                                    .when(checkedButton.selectedProperty())
                                                    .then("-fx-background-insets: 1 1 1;")
                                                    .otherwise(
                                                            new SimpleStringProperty("-fx-background-color: transparent;-fx-background-insets: 0 0 0;"))));

                            checkedButton.setOnAction(action -> {
                                if (checkedButton.isSelected()) {
                                    checkedButton.setGraphic(checked);
                                    AlarmRow alarmRow = (AlarmRow) getTableRow().getItem();
                                    alarmRow.getAlarmConfiguration().setChecked(true);
                                } else {
                                    checkedButton.setGraphic(notChecked);
                                    AlarmRow alarmRow = (AlarmRow) getTableRow().getItem();
                                    alarmRow.getAlarmConfiguration().setChecked(false);
                                }
                                Platform.runLater(() -> tableView.refresh());
                            });

                            setGraphic(checkedButton);
                        }
                    }
                };
            }
        });

        tableView.getColumns().setAll(dateColumn, nameColumn, isValueColumn, shouldBeValueColumn, toleranceColumn, alarmTypeColumn, confirmationColumn);
    }

    private String getTranslatedTypeName(String item) {
        switch (item) {
            case ("L1 "):
                return I18n.getInstance().getString("plugin.alarm.table.translation.l1");
            case ("L2"):
                return I18n.getInstance().getString("plugin.alarm.table.translation.l2");
            case ("DYNAMIC"):
                return I18n.getInstance().getString("plugin.alarm.table.translation.dynamic");
            case ("STATIC"):
                return I18n.getInstance().getString("plugin.alarm.table.translation.static");
            case ("NONE"):
            default:
                return I18n.getInstance().getString("plugin.alarm.table.translation.none");
        }
    }

    private void initToolBar() {
        ToggleButton reload = new ToggleButton("", JEConfig.getImage("1403018303_Refresh.png", iconSize, iconSize));
        Tooltip reloadTooltip = new Tooltip(I18n.getInstance().getString("plugin.alarms.reload.progress.tooltip"));
        reload.setTooltip(reloadTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(reload);

        reload.setOnAction(event -> {
            initialized = false;

            final String loading = I18n.getInstance().getString("plugin.alarms.reload.progress.message");
            Service<Void> service = new Service<Void>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<Void>() {
                        @Override
                        protected Void call() {
                            updateMessage(loading);
                            try {
                                ds.clearCache();
                                ds.preload();

                                updateList(start, end);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    };
                }
            };
            ProgressDialog pd = new ProgressDialog(service);
            pd.setHeaderText(I18n.getInstance().getString("plugin.reports.reload.progress.header"));
            pd.setTitle(I18n.getInstance().getString("plugin.reports.reload.progress.title"));
            pd.getDialogPane().setContent(null);

            service.start();

        });

        Separator sep1 = new Separator(Orientation.VERTICAL);
        Separator sep2 = new Separator(Orientation.VERTICAL);

        timeFrameComboBox = getTimeFrameComboBox();

        JFXDatePicker startDatePicker = null;
        if (start != null) {
            startDatePicker = new JFXDatePicker(LocalDate.of(start.getYear(), start.getMonthOfYear(), start.getDayOfMonth()));
        } else {
            startDatePicker = new JFXDatePicker();
        }

        JFXDatePicker endDatePicker = null;
        if (end != null) {
            endDatePicker = new JFXDatePicker(LocalDate.of(end.getYear(), end.getMonthOfYear(), end.getDayOfMonth()));
        } else {
            endDatePicker = new JFXDatePicker();
        }

        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            start = new DateTime(newValue.getYear(), newValue.getMonthValue(), newValue.getDayOfMonth(), 0, 0, 0);
            timeFrame = TimeFrame.CUSTOM;
            if (end != null) {
                updateList(start, end);
            }
        });

        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            end = new DateTime(newValue.getYear(), newValue.getMonthValue(), newValue.getDayOfMonth(), 23, 59, 59);
            timeFrame = TimeFrame.CUSTOM;
            if (start != null) {
                updateList(start, end);
            }
        });

        startDatePicker.setPrefWidth(120d);
        endDatePicker.setPrefWidth(120d);

        toolBar.getItems().setAll(reload, sep1, timeFrameComboBox, sep2, startDatePicker, endDatePicker);
    }

    private ComboBox<TimeFrame> getTimeFrameComboBox() {
        ComboBox<TimeFrame> box = new ComboBox<>();

        final String today = I18n.getInstance().getString("plugin.graph.changedate.buttontoday");
        final String yesterday = I18n.getInstance().getString("plugin.graph.changedate.buttonyesterday");
        final String last7Days = I18n.getInstance().getString("plugin.graph.changedate.buttonlast7days");
        final String thisWeek = I18n.getInstance().getString("plugin.graph.changedate.buttonthisweek");
        final String lastWeek = I18n.getInstance().getString("plugin.graph.changedate.buttonlastweek");
        final String last30Days = I18n.getInstance().getString("plugin.graph.changedate.buttonlast30days");
        final String thisMonth = I18n.getInstance().getString("plugin.graph.changedate.buttonthismonth");
        final String lastMonth = I18n.getInstance().getString("plugin.graph.changedate.buttonlastmonth");
        final String thisYear = I18n.getInstance().getString("plugin.graph.changedate.buttonthisyear");
        final String lastYear = I18n.getInstance().getString("plugin.graph.changedate.buttonlastyear");

        ObservableList<TimeFrame> timeFrames = FXCollections.observableArrayList(TimeFrame.values());
        timeFrames.remove(TimeFrame.values().length - 2, TimeFrame.values().length);
        timeFrames.remove(0, 1);
        box.setItems(timeFrames);

        Callback<ListView<TimeFrame>, ListCell<TimeFrame>> cellFactory = new Callback<javafx.scene.control.ListView<TimeFrame>, ListCell<TimeFrame>>() {
            @Override
            public ListCell<TimeFrame> call(javafx.scene.control.ListView<TimeFrame> param) {
                return new ListCell<TimeFrame>() {
                    @Override
                    protected void updateItem(TimeFrame timeFrame, boolean empty) {
                        super.updateItem(timeFrame, empty);
                        setText(null);
                        setGraphic(null);

                        if (timeFrame != null && !empty) {
                            String text = "";
                            switch (timeFrame) {
                                case TODAY:
                                    text = today;
                                    break;
                                case YESTERDAY:
                                    text = yesterday;
                                    break;
                                case LAST_7_DAYS:
                                    text = last7Days;
                                    break;
                                case THIS_WEEK:
                                    text = thisWeek;
                                    break;
                                case LAST_WEEK:
                                    text = lastWeek;
                                    break;
                                case LAST_30_DAYS:
                                    text = last30Days;
                                    break;
                                case THIS_MONTH:
                                    text = thisMonth;
                                    break;
                                case LAST_MONTH:
                                    text = lastMonth;
                                    break;
                                case THIS_YEAR:
                                    text = thisYear;
                                    break;
                                case LAST_YEAR:
                                    text = lastYear;
                                    break;
                            }
                            setText(text);
                        }
                    }
                };
            }
        };
        box.setCellFactory(cellFactory);
        box.setButtonCell(cellFactory.call(null));
        box.getSelectionModel().select(timeFrame);

        box.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                switch (newValue) {
                    case CUSTOM:
                        break;
                    case TODAY:
                        dateHelper.setType(DateHelper.TransformType.TODAY);
                        break;
                    case YESTERDAY:
                        dateHelper.setType(DateHelper.TransformType.YESTERDAY);
                        break;
                    case LAST_7_DAYS:
                        dateHelper.setType(DateHelper.TransformType.LAST7DAYS);
                        break;
                    case THIS_WEEK:
                        dateHelper.setType(DateHelper.TransformType.THISWEEK);
                        break;
                    case LAST_WEEK:
                        dateHelper.setType(DateHelper.TransformType.LASTWEEK);
                        break;
                    case LAST_30_DAYS:
                        dateHelper.setType(DateHelper.TransformType.LAST30DAYS);
                        break;
                    case THIS_MONTH:
                        dateHelper.setType(DateHelper.TransformType.THISMONTH);
                        break;
                    case LAST_MONTH:
                        dateHelper.setType(DateHelper.TransformType.LASTMONTH);
                        break;
                    case THIS_YEAR:
                        dateHelper.setType(DateHelper.TransformType.THISYEAR);
                        break;
                    case LAST_YEAR:
                        dateHelper.setType(DateHelper.TransformType.LASTYEAR);
                        break;
                }

                if (newValue != TimeFrame.CUSTOM) {
                    timeFrame = newValue;
                    start = dateHelper.getStartDate();
                    end = dateHelper.getEndDate();
                    updateList(dateHelper.getStartDate(), dateHelper.getEndDate());
                    Platform.runLater(this::initToolBar);
                }
            }
        });

        return box;
    }

    @Override
    public String getClassName() {
        return "Alarm Plugin";
    }

    @Override
    public String getName() {
        return title;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public StringProperty nameProperty() {
        return null;
    }

    @Override
    public String getUUID() {
        return null;
    }

    @Override
    public void setUUID(String id) {

    }

    @Override
    public String getToolTip() {
        return I18n.getInstance().getString("plugin.alarms.tooltip");
    }

    @Override
    public StringProperty uuidProperty() {
        return null;
    }

    @Override
    public Node getMenu() {
        return null;
    }

    @Override
    public boolean supportsRequest(int cmdType) {
        return false;
    }

    @Override
    public Node getToolbar() {
        return toolBar;
    }

    @Override
    public void updateToolbar() {

    }

    @Override
    public JEVisDataSource getDataSource() {
        return ds;
    }

    @Override
    public void setDataSource(JEVisDataSource ds) {

    }

    @Override
    public void handleRequest(int cmdType) {

    }

    @Override
    public Node getContentNode() {
        return borderPane;
    }

    private void updateList(DateTime start, DateTime end) {

        alarmRows.clear();

        autoFitTable(tableView);

        executor.execute(() -> {
            try {
                List<AlarmConfiguration> alarms = getAllAlarmConfigs();
                int size = alarms.size();
                JEConfig.getStatusBar().startProgressJob("AlarmConfigs", size, "Loading alarm configurations");
                for (AlarmConfiguration alarmConfiguration : alarms) {
                    JEVisAttribute logAttribute = alarmConfiguration.getLogAttribute();
                    List<AlarmRow> acList = new ArrayList<>();
                    if (logAttribute.hasSample()) {
                        for (JEVisSample jeVisSample : logAttribute.getSamples(start, end)) {
                            try {
                                acList.add(new AlarmRow(jeVisSample.getTimestamp(), alarmConfiguration, null));
                                if (!alarmConfiguration.isChecked()) {
                                    this.hasAlarms.set(true);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }


                    JEVisAttribute fileLog = alarmConfiguration.getFileLogAttribute();
                    List<AlarmRow> newList = new ArrayList<>();
                    if (fileLog.hasSample())
                        for (JEVisSample jeVisSample : fileLog.getSamples(start, end)) {
                            List<Alarm> alarmList = new ArrayList<>();

                            try {
                                JsonAlarm[] jsonAlarmList = JsonTools.objectMapper().readValue(jeVisSample.getValueAsFile().getBytes(), JsonAlarm[].class);
                                for (JsonAlarm jsonAlarm : jsonAlarmList) {
                                    JEVisObject object = ds.getObject(jsonAlarm.getObject());
                                    JEVisAttribute attribute = object.getAttribute(jsonAlarm.getAttribute());
                                    DateTime dateTime = new DateTime(jsonAlarm.getTimeStamp());
                                    JEVisSample sample = attribute.getSamples(dateTime, dateTime).get(0);
                                    alarmList.add(new Alarm(object, attribute, sample, dateTime, jsonAlarm.getIsValue(), jsonAlarm.getShouldBeValue(), jsonAlarm.getAlarmType(), jsonAlarm.getLogValue()));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            for (Alarm alarm : alarmList) {
                                newList.add(new AlarmRow(alarm.getTimeStamp(), alarmConfiguration, alarm));

                                if (!alarmConfiguration.isChecked()) {
                                    this.hasAlarms.set(true);
                                }
                            }
                        }

                    Platform.runLater(() -> {
                        alarmRows.addAll(acList);
                        alarmRows.addAll(newList);

                        alarmRows.sort(Comparator.comparing(AlarmRow::getTimeStamp).reversed());

                        JEConfig.getStatusBar().progressProgressJob("AlarmConfigs", alarms.indexOf(alarmConfiguration), "AlarmConfig " + alarmConfiguration.getName() + " done.");

                        if (alarms.indexOf(alarmConfiguration) == size - 1) {
                            JEConfig.getStatusBar().finishProgressJob("AlarmConfigs", "Done.");
                            Platform.runLater(() -> autoFitTable(tableView));
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private List<AlarmConfiguration> getAllAlarmConfigs() {
        List<AlarmConfiguration> list = new ArrayList<>();
        JEVisClass alarmConfigClass = null;
        try {
            alarmConfigClass = ds.getJEVisClass(ALARM_CONFIG_CLASS);
            List<JEVisObject> allObjects = ds.getObjects(alarmConfigClass, true);
            for (JEVisObject object : allObjects) {
                AlarmConfiguration alarmConfiguration = new AlarmConfiguration(ds, object);
                list.add(alarmConfiguration);
            }
        } catch (JEVisException e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public ImageView getIcon() {
        return JEConfig.getImage("Warning-icon.png", 20, 20);
    }

    @Override
    public void fireCloseEvent() {

    }

    @Override
    public void setHasFocus() {
        timeFrameComboBox.getSelectionModel().select(TimeFrame.TODAY);
    }

    @Override
    public void openObject(Object object) {

    }

    @Override
    public int getPrefTapPos() {
        return 4;
    }

    public SimpleBooleanProperty hasAlarmsProperty() {
        return hasAlarms;
    }
}
