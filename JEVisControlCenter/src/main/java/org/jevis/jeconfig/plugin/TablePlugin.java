package org.jevis.jeconfig.plugin;

import com.jfoenix.controls.JFXDatePicker;
import com.jfoenix.controls.JFXTextField;
import com.sun.javafx.scene.control.skin.TableViewSkin;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.*;
import org.jevis.commons.JEVisFileImp;
import org.jevis.commons.dataprocessing.CleanDataObject;
import org.jevis.commons.i18n.I18n;
import org.jevis.commons.object.plugin.TargetHelper;
import org.jevis.commons.relationship.ObjectRelations;
import org.jevis.commons.utils.AlphanumComparator;
import org.jevis.commons.utils.JEVisDates;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.application.jevistree.UserSelection;
import org.jevis.jeconfig.application.jevistree.filter.JEVisTreeFilter;
import org.jevis.jeconfig.dialog.EnterDataDialog;
import org.jevis.jeconfig.dialog.ImageViewerDialog;
import org.jevis.jeconfig.dialog.PDFViewerDialog;
import org.jevis.jeconfig.dialog.SelectTargetDialog;
import org.jevis.jeconfig.plugin.meters.AttributeValueChange;
import org.jevis.jeconfig.plugin.meters.MeterPlugin;
import org.jevis.jeconfig.plugin.meters.RegisterTableRow;
import org.jevis.jeconfig.plugin.object.ObjectPlugin;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class TablePlugin {
    protected static final Logger logger = LogManager.getLogger(TablePlugin.class);
    private static Method columnToFitMethod;

    static {
        try {
            columnToFitMethod = TableViewSkin.class.getDeclaredMethod("resizeColumnToFitContent", TableColumn.class, int.class);
            columnToFitMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    protected final JEVisDataSource ds;
    protected final int toolBarIconSize = 20;
    protected final int tableIconSize = 18;
    protected final SimpleBooleanProperty openedDataDialog = new SimpleBooleanProperty(false);
    protected final Map<JEVisAttribute, AttributeValueChange> changeMap = new HashMap<>();
    protected final ObjectRelations objectRelations;
    protected final String title;
    protected final AlphanumComparator alphanumComparator = new AlphanumComparator();

    public TablePlugin(JEVisDataSource ds, String title) {
        this.ds = ds;
        this.objectRelations = new ObjectRelations(ds);
        this.title = title;
    }

    public static void autoFitTable(TableView<RegisterTableRow> tableView) {
        for (TableColumn<RegisterTableRow, ?> column : tableView.getColumns()) {
//            if (column.isVisible()) {
            try {
                if (tableView.getSkin() != null) {
                    TablePlugin.columnToFitMethod.invoke(tableView.getSkin(), column, -1);
                }
            } catch (Exception e) {
            }
//            }
        }
    }

    protected Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>> valueCellDateTime() {
        return new Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>>() {
            @Override
            public TableCell<RegisterTableRow, JEVisAttribute> call(TableColumn<RegisterTableRow, JEVisAttribute> param) {
                return new TableCell<RegisterTableRow, JEVisAttribute>() {
                    @Override
                    protected void updateItem(JEVisAttribute item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty || getTableRow() == null || getTableRow().getItem() == null) {
                            setText(null);
                            setGraphic(null);
                        } else {

                            JFXDatePicker pickerDate = new JFXDatePicker();

                            if (getTableRow().getIndex() % 2 == 0) {
                                pickerDate.setStyle("-fx-text-fill: white;");
                            } else {
                                pickerDate.setStyle("-fx-text-fill: black;");
                            }

                            if (item.hasSample()) {
                                try {
                                    DateTime date = JEVisDates.parseDefaultDate(item);
                                    LocalDateTime lDate = LocalDateTime.of(
                                            date.get(DateTimeFieldType.year()), date.get(DateTimeFieldType.monthOfYear()), date.get(DateTimeFieldType.dayOfMonth()),
                                            date.get(DateTimeFieldType.hourOfDay()), date.get(DateTimeFieldType.minuteOfHour()), date.get(DateTimeFieldType.secondOfMinute()));
                                    lDate.atZone(ZoneId.of(date.getZone().getID()));
                                    pickerDate.valueProperty().setValue(lDate.toLocalDate());

                                    if (item.getName().equals("Verification Date")) {
                                        if (date.isBefore(new DateTime())) {
                                            pickerDate.setDefaultColor(Color.RED);
                                        }
                                    }
                                } catch (Exception ex) {
                                    logger.catching(ex);
                                }
                            }

                            pickerDate.valueProperty().addListener((observable, oldValue, newValue) -> {
                                if (newValue != oldValue) {
                                    try {
                                        updateDate(item, getCurrentDate(pickerDate));
                                    } catch (Exception e) {
                                        logger.error(e);
                                    }
                                }
                            });

                            setGraphic(pickerDate);
                        }
                    }

                    private DateTime getCurrentDate(JFXDatePicker pickerDate) {
                        return new DateTime(
                                pickerDate.valueProperty().get().getYear(), pickerDate.valueProperty().get().getMonthValue(), pickerDate.valueProperty().get().getDayOfMonth(),
                                0, 0, 0,
                                DateTimeZone.getDefault());
                    }

                    private void updateDate(JEVisAttribute item, DateTime datetime) throws JEVisException {
                        AttributeValueChange attributeValueChange = changeMap.get(item);
                        if (attributeValueChange != null) {
                            attributeValueChange.setDateTime(datetime);
                        } else {
                            AttributeValueChange valueChange = new AttributeValueChange(item.getPrimitiveType(), item.getType().getGUIDisplayType(), item, datetime);
                            changeMap.put(item, valueChange);
                        }
                    }
                };
            }
        };
    }

    protected Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>> valueCellTargetSelection() {
        return new Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>>() {
            @Override
            public TableCell<RegisterTableRow, JEVisAttribute> call(TableColumn<RegisterTableRow, JEVisAttribute> param) {
                return new TableCell<RegisterTableRow, JEVisAttribute>() {
                    @Override
                    protected void updateItem(JEVisAttribute item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty || getTableRow() == null || getTableRow().getItem() == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            RegisterTableRow registerTableRow = (RegisterTableRow) getTableRow().getItem();

                            Button manSampleButton = new Button("", JEConfig.getImage("if_textfield_add_64870.png", tableIconSize, tableIconSize));
                            manSampleButton.setDisable(true);
                            manSampleButton.setTooltip(new Tooltip(I18n.getInstance().getString("plugin.meters.table.mansample")));
                            Button treeButton = new Button("",
                                    JEConfig.getImage("folders_explorer.png", tableIconSize, tableIconSize));
                            treeButton.wrapTextProperty().setValue(true);

                            Button gotoButton = new Button("",
                                    JEConfig.getImage("1476393792_Gnome-Go-Jump-32.png", tableIconSize, tableIconSize));//icon
                            gotoButton.setTooltip(new Tooltip(I18n.getInstance().getString("plugin.object.attribute.target.goto.tooltip")));

                            try {
                                if (item.hasSample()) {
                                    addEventManSampleAction(item.getLatestSample(), manSampleButton, registerTableRow.getName());
                                    Platform.runLater(() -> manSampleButton.setDisable(false));
                                }

                            } catch (Exception ex) {
                                logger.catching(ex);
                            }

                            gotoButton.setOnAction(event -> {
                                try {
                                    TargetHelper th = new TargetHelper(ds, item);
                                    if (th.isValid() && th.targetAccessible()) {
                                        JEVisObject findObj = ds.getObject(th.getObject().get(0).getID());
                                        JEConfig.openObjectInPlugin(ObjectPlugin.PLUGIN_NAME, findObj);
                                    }
                                } catch (Exception ex) {
                                    logger.catching(ex);
                                }
                            });

                            treeButton.setOnAction(event -> {
                                try {
                                    SelectTargetDialog selectTargetDialog = null;
                                    JEVisSample latestSample = item.getLatestSample();
                                    TargetHelper th = null;
                                    if (latestSample != null) {
                                        th = new TargetHelper(item.getDataSource(), latestSample.getValueAsString());
                                        if (th.isValid() && th.targetAccessible()) {
                                            logger.info("Target Is valid");
                                            setToolTipText(treeButton, item);
                                        }
                                    }

                                    List<JEVisTreeFilter> allFilter = new ArrayList<>();
                                    JEVisTreeFilter allDataFilter = SelectTargetDialog.buildAllDataFilter();
                                    allFilter.add(allDataFilter);

                                    selectTargetDialog = new SelectTargetDialog(allFilter, allDataFilter, null, SelectionMode.SINGLE);
                                    selectTargetDialog.setInitOwner(treeButton.getScene().getWindow());

                                    List<UserSelection> openList = new ArrayList<>();

                                    if (th != null && !th.getObject().isEmpty()) {
                                        for (JEVisObject obj : th.getObject()) {
                                            openList.add(new UserSelection(UserSelection.SelectionType.Object, obj));
                                        }
                                    }

                                    if (selectTargetDialog.show(
                                            ds,
                                            I18n.getInstance().getString("dialog.target.data.title"),
                                            openList
                                    ) == SelectTargetDialog.Response.OK) {
                                        logger.trace("Selection Done");

                                        String newTarget = "";
                                        List<UserSelection> selections = selectTargetDialog.getUserSelection();
                                        for (UserSelection us : selections) {
                                            int index = selections.indexOf(us);
                                            if (index > 0) newTarget += ";";

                                            newTarget += us.getSelectedObject().getID();
                                            if (us.getSelectedAttribute() != null) {
                                                newTarget += ":" + us.getSelectedAttribute().getName();

                                            } else {
                                                newTarget += ":Value";
                                            }
                                        }


                                        JEVisSample newTargetSample = item.buildSample(new DateTime(), newTarget);
                                        newTargetSample.commit();
                                        try {
                                            addEventManSampleAction(newTargetSample, manSampleButton, registerTableRow.getName());
                                            manSampleButton.setDisable(false);
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }

                                    }
                                    setToolTipText(treeButton, item);

                                } catch (Exception ex) {
                                    logger.catching(ex);
                                }
                            });


                            HBox hBox = new HBox(treeButton, manSampleButton);
                            hBox.setAlignment(Pos.CENTER);
                            hBox.setSpacing(4);

                            hBox.getChildren().add(gotoButton);
                            gotoButton.setDisable(!setToolTipText(treeButton, item));

                            VBox vBox = new VBox(hBox);
                            vBox.setAlignment(Pos.CENTER);
                            setGraphic(vBox);
                        }

                    }
                };
            }

        };
    }

    protected void addEventManSampleAction(JEVisSample targetSample, Button buttonToAddEvent, String headerText) {

        buttonToAddEvent.setOnAction(event -> {
            if (!openedDataDialog.get()) {
                openedDataDialog.set(true);

                if (targetSample != null) {
                    try {
                        TargetHelper th = new TargetHelper(getDataSource(), targetSample.getValueAsString());
                        if (th.isValid() && th.targetAccessible() && !th.getAttribute().isEmpty()) {
                            JEVisSample lastValue = th.getAttribute().get(0).getLatestSample();

                            EnterDataDialog enterDataDialog = new EnterDataDialog(getDataSource());
                            enterDataDialog.setTarget(false, th.getAttribute().get(0));
                            enterDataDialog.setSample(lastValue);
                            enterDataDialog.setShowValuePrompt(true);

                            enterDataDialog.setOnCloseRequest(event1 -> openedDataDialog.set(false));
                            enterDataDialog.showPopup(buttonToAddEvent, headerText);
                        } else {
                            openedDataDialog.set(false);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    private boolean setToolTipText(Button treeButton, JEVisAttribute att) {
        boolean foundTarget = false;
        try {
            TargetHelper th = new TargetHelper(ds, att);

            if (th.isValid() && th.targetAccessible()) {

                StringBuilder bText = new StringBuilder();

                JEVisClass cleanData = ds.getJEVisClass("Clean Data");

                for (JEVisObject obj : th.getObject()) {
                    int index = th.getObject().indexOf(obj);
                    if (index > 0) bText.append("; ");

                    if (obj.getJEVisClass().equals(cleanData)) {
                        List<JEVisObject> parents = obj.getParents();
                        if (!parents.isEmpty()) {
                            for (JEVisObject parent : parents) {
                                bText.append("[");
                                bText.append(parent.getID());
                                bText.append("] ");
                                bText.append(parent.getName());
                                bText.append(" / ");
                            }
                        }
                    }

                    bText.append("[");
                    bText.append(obj.getID());
                    bText.append("] ");
                    bText.append(obj.getName());

                    if (th.hasAttribute()) {

                        bText.append(" - ");
                        bText.append(th.getAttribute().get(index).getName());

                    }

                    foundTarget = true;
                }

                Platform.runLater(() -> treeButton.setTooltip(new Tooltip(bText.toString())));
            }

        } catch (Exception ex) {
            logger.catching(ex);
        }
        return foundTarget;
    }

    protected Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>> valueCellString() {
        return new Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>>() {
            @Override
            public TableCell<RegisterTableRow, JEVisAttribute> call(TableColumn<RegisterTableRow, JEVisAttribute> param) {
                return new TableCell<RegisterTableRow, JEVisAttribute>() {
                    @Override
                    protected void updateItem(JEVisAttribute item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty || getTableRow() == null || getTableRow().getItem() == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            RegisterTableRow registerTableRow = (RegisterTableRow) getTableRow().getItem();

                            JFXTextField textField = new JFXTextField();

                            if (getTableRow().getIndex() % 2 == 0) {
                                textField.setStyle("-fx-text-fill: white;");
                            } else {
                                textField.setStyle("-fx-text-fill: black;");
                            }

                            try {
                                JEVisAttribute attribute = registerTableRow.getAttributeMap().get(item.getType());
                                if (attribute != null && attribute.hasSample()) {
                                    textField.setText(attribute.getLatestSample().getValueAsString());
                                }
                            } catch (JEVisException e) {
                                e.printStackTrace();
                            }

                            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                                try {
                                    AttributeValueChange attributeValueChange = changeMap.get(item);
                                    if (attributeValueChange != null) {
                                        attributeValueChange.setStringValue(newValue);
                                    } else {
                                        AttributeValueChange valueChange = new AttributeValueChange(item.getPrimitiveType(), item.getType().getGUIDisplayType(), item, newValue);
                                        changeMap.put(item, valueChange);
                                    }
                                } catch (Exception ex) {
                                    logger.error("Error in string text", ex);
                                }
                            });

                            setGraphic(textField);
                        }

                    }
                };
            }

        };
    }

    protected Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>> valueCellStringPassword() {
        return null;
    }

    protected Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>> valueCellBoolean() {
        return null;
    }

    protected Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>> valueCellFile() {
        return new Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>>() {

            @Override
            public TableCell<RegisterTableRow, JEVisAttribute> call(TableColumn<RegisterTableRow, JEVisAttribute> param) {
                return new TableCell<RegisterTableRow, JEVisAttribute>() {

                    @Override
                    protected void updateItem(JEVisAttribute item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty || getTableRow() == null || getTableRow().getItem() == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            Button downloadButton = new Button("", JEConfig.getImage("698925-icon-92-inbox-download-48.png", tableIconSize, tableIconSize));
                            Button previewButton = new Button("", JEConfig.getImage("eye_visible.png", tableIconSize, tableIconSize));
                            Button uploadButton = new Button("", JEConfig.getImage("1429894158_698394-icon-130-cloud-upload-48.png", tableIconSize, tableIconSize));

                            downloadButton.setTooltip(new Tooltip(I18n.getInstance().getString("plugin.meters.table.download")));
                            previewButton.setTooltip(new Tooltip(I18n.getInstance().getString("plugin.meters.table.preview")));
                            uploadButton.setTooltip(new Tooltip(I18n.getInstance().getString("plugin.meters.table.upload")));

                            AttributeValueChange valueChange;
                            if (changeMap.get(item) == null) {
                                valueChange = new AttributeValueChange();
                                try {
                                    valueChange.setPrimitiveType(item.getPrimitiveType());
                                    valueChange.setGuiDisplayType(item.getType().getGUIDisplayType());
                                    valueChange.setAttribute(item);
                                } catch (JEVisException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                valueChange = changeMap.get(item);
                            }

                            previewButton.setDisable(true);
                            try {
                                downloadButton.setDisable(!item.hasSample());
                                if (item.hasSample()) {
                                    setPreviewButton(previewButton, valueChange);
                                    previewButton.setDisable(false);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            HBox hBox = new HBox();
                            hBox.setAlignment(Pos.CENTER);
                            hBox.setSpacing(4);

                            downloadButton.setOnAction(event -> {
                                try {
                                    if (item.hasSample()) {
                                        JEVisFile file = item.getLatestSample().getValueAsFile();
                                        if (file != null) {
                                            FileChooser fileChooser = new FileChooser();
                                            fileChooser.setInitialFileName(file.getFilename());
                                            fileChooser.setTitle(I18n.getInstance().getString("plugin.object.attribute.file.download.title"));
                                            fileChooser.getExtensionFilters().addAll(
                                                    new FileChooser.ExtensionFilter("All Files", "*.*"));
                                            File selectedFile = fileChooser.showSaveDialog(null);
                                            if (selectedFile != null) {
                                                JEConfig.setLastPath(selectedFile);
                                                file.saveToFile(selectedFile);
                                            }
                                        }
                                    }
                                } catch (Exception ex) {
                                    logger.error(ex);
                                }

                            });

                            uploadButton.setOnAction(event -> {
                                try {
                                    FileChooser fileChooser = new FileChooser();
                                    fileChooser.setInitialDirectory(JEConfig.getLastPath());
                                    fileChooser.setTitle(I18n.getInstance().getString("plugin.object.attribute.file.upload"));
                                    fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
                                    File selectedFile = fileChooser.showOpenDialog(JEConfig.getStage());
                                    if (selectedFile != null) {
                                        try {
                                            JEConfig.setLastPath(selectedFile);
                                            JEVisFile jfile = new JEVisFileImp(selectedFile.getName(), selectedFile);
                                            JEVisSample fileSample = item.buildSample(new DateTime(), jfile);
                                            fileSample.commit();
                                            valueChange.setJeVisFile(jfile);

                                            downloadButton.setDisable(false);
                                            setPreviewButton(previewButton, valueChange);
                                            previewButton.setDisable(false);

                                        } catch (Exception ex) {
                                            logger.catching(ex);
                                        }
                                    }
                                } catch (Exception ex) {
                                    logger.error("Error in string text", ex);
                                }
                            });


                            hBox.getChildren().addAll(uploadButton, downloadButton, previewButton);

                            VBox vBox = new VBox(hBox);
                            vBox.setAlignment(Pos.CENTER);

                            setGraphic(vBox);
                        }
                    }
                };
            }

        };
    }

    private void setPreviewButton(Button button, AttributeValueChange valueChange) {

        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    Task clearCacheTask = new Task() {
                        @Override
                        protected Object call() throws Exception {
                            try {
                                this.updateTitle(I18n.getInstance().getString("plugin.meters.download"));
                                boolean isPDF = false;
                                JEVisFile file = valueChange.getAttribute().getLatestSample().getValueAsFile();
                                String fileName = file.getFilename();

                                String s = FilenameUtils.getExtension(fileName);
                                switch (s) {
                                    case "pdf":
                                        isPDF = true;
                                        break;
                                    case "png":
                                    case "jpg":
                                    case "jpeg":
                                    case "gif":
                                        isPDF = false;
                                        break;
                                }


                                if (isPDF) {
                                    Platform.runLater(() -> {
                                        PDFViewerDialog pdfViewerDialog = new PDFViewerDialog();
                                        pdfViewerDialog.show(valueChange.getAttribute(), file, JEConfig.getStage());
                                    });

                                } else {
                                    Platform.runLater(() -> {
                                        ImageViewerDialog imageViewerDialog = new ImageViewerDialog();
                                        imageViewerDialog.show(valueChange.getAttribute(), file, JEConfig.getStage());
                                    });

                                }
                            } catch (Exception ex) {
                                failed();
                            } finally {
                                done();
                            }
                            return null;
                        }
                    };
                    JEConfig.getStatusBar().addTask(MeterPlugin.PLUGIN_NAME, clearCacheTask, JEConfig.getImage("measurement_instrument.png"), true);


                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    protected Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>> valueCellDouble() {
        return new Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>>() {
            @Override
            public TableCell<RegisterTableRow, JEVisAttribute> call(TableColumn<RegisterTableRow, JEVisAttribute> param) {
                return new TableCell<RegisterTableRow, JEVisAttribute>() {

                    @Override
                    protected void updateItem(JEVisAttribute item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty || getTableRow() == null || getTableRow().getItem() == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            RegisterTableRow registerTableRow = (RegisterTableRow) getTableRow().getItem();

                            JFXTextField textField = new JFXTextField();

                            if (getTableRow().getIndex() % 2 == 0) {
                                textField.setStyle("-fx-text-fill: white;");
                            } else {
                                textField.setStyle("-fx-text-fill: black;");
                            }

                            try {
                                JEVisAttribute attribute = registerTableRow.getAttributeMap().get(item.getType());
                                if (attribute != null && attribute.hasSample()) {
                                    textField.setText(attribute.getLatestSample().getValueAsDouble().toString());
                                }
                            } catch (JEVisException e) {
                                e.printStackTrace();
                            }

                            NumberFormat numberFormat = NumberFormat.getNumberInstance(I18n.getInstance().getLocale());
                            UnaryOperator<TextFormatter.Change> filter = t -> {
                                if (t.getText().length() > 1) {/** Copy&paste case **/
                                    try {
                                        Number newNumber = numberFormat.parse(t.getText());
                                        t.setText(String.valueOf(newNumber.doubleValue()));
                                    } catch (Exception ex) {
                                        t.setText("");
                                    }
                                } else if (t.getText().matches(",")) {/** to be use the Double.parse **/
                                    t.setText(".");
                                }

                                try {
                                    /** We don't use the NumberFormat to validate, because it is not strict enough **/
                                    Double parse = Double.parseDouble(t.getControlNewText());
                                } catch (Exception ex) {
                                    t.setText("");
                                }
                                return t;
                            };

                            textField.setTextFormatter(new TextFormatter<>(filter));

                            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                                try {
                                    Double value = Double.parseDouble(newValue);
                                    AttributeValueChange attributeValueChange = changeMap.get(item);
                                    if (attributeValueChange != null) {
                                        attributeValueChange.setDoubleValue(value);
                                    } else {
                                        AttributeValueChange valueChange = new AttributeValueChange(item.getPrimitiveType(), item.getType().getGUIDisplayType(), item, value);
                                        changeMap.put(item, valueChange);
                                    }
                                } catch (Exception ex) {
                                    logger.error("Error in double text", ex);
                                }
                            });


                            setGraphic(textField);

                        }

                    }
                };
            }

        };
    }

    protected Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>> valueCellInteger() {
        return new Callback<TableColumn<RegisterTableRow, JEVisAttribute>, TableCell<RegisterTableRow, JEVisAttribute>>() {
            @Override
            public TableCell<RegisterTableRow, JEVisAttribute> call(TableColumn<RegisterTableRow, JEVisAttribute> param) {
                return new TableCell<RegisterTableRow, JEVisAttribute>() {
                    @Override
                    protected void updateItem(JEVisAttribute item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty || getTableRow() == null || getTableRow().getItem() == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            RegisterTableRow registerTableRow = (RegisterTableRow) getTableRow().getItem();

                            JFXTextField textField = new JFXTextField();

                            if (getTableRow().getIndex() % 2 == 0) {
                                textField.setStyle("-fx-text-fill: white;");
                            } else {
                                textField.setStyle("-fx-text-fill: black;");
                            }

                            try {
                                JEVisAttribute attribute = registerTableRow.getAttributeMap().get(item.getType());
                                if (attribute != null && attribute.hasSample()) {
                                    textField.setText(attribute.getLatestSample().getValueAsDouble().toString());
                                }
                            } catch (JEVisException e) {
                                e.printStackTrace();
                            }

                            UnaryOperator<TextFormatter.Change> filter = t -> {

                                if (t.getControlNewText().isEmpty()) {
                                    t.setText("0");
                                } else {
                                    try {
                                        Long bewValue = Long.parseLong(t.getControlNewText());
                                    } catch (Exception ex) {
                                        t.setText("");
                                    }
                                }

                                return t;
                            };

                            textField.setTextFormatter(new TextFormatter<>(filter));

                            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                                try {
                                    Long value = Long.parseLong(newValue);
                                    AttributeValueChange attributeValueChange = changeMap.get(item);
                                    if (attributeValueChange != null) {
                                        attributeValueChange.setLongValue(value);
                                    } else {
                                        AttributeValueChange valueChange = new AttributeValueChange(item.getPrimitiveType(), item.getType().getGUIDisplayType(), item, value);
                                        changeMap.put(item, valueChange);
                                    }
                                } catch (Exception ex) {
                                    logger.error("Error in double text", ex);
                                }
                            });


                            setGraphic(textField);

                        }

                    }
                };
            }

        };
    }

    public JEVisDataSource getDataSource() {
        return ds;
    }

    public Map<JEVisAttribute, AttributeValueChange> getChangeMap() {
        return changeMap;
    }

    protected boolean isCounter(JEVisObject object, JEVisSample latestSample) {
        boolean isCounter = false;
        try {
            JEVisClass cleanDataClass = ds.getJEVisClass("Clean Data");
            if (object.getJEVisClassName().equals("Data")) {
                JEVisObject cleanDataObject = object.getChildren(cleanDataClass, true).get(0);
                JEVisAttribute conversionToDiffAttribute = cleanDataObject.getAttribute(CleanDataObject.AttributeName.CONVERSION_DIFFERENTIAL.getAttributeName());

                if (conversionToDiffAttribute != null) {
                    List<JEVisSample> conversionDifferential = conversionToDiffAttribute.getAllSamples();

                    for (int i = 0; i < conversionDifferential.size(); i++) {
                        JEVisSample cd = conversionDifferential.get(i);

                        DateTime timeStampOfConversion = cd.getTimestamp();

                        DateTime nextTimeStampOfConversion = null;
                        Boolean conversionToDifferential = cd.getValueAsBoolean();
                        if (conversionDifferential.size() > (i + 1)) {
                            nextTimeStampOfConversion = (conversionDifferential.get(i + 1)).getTimestamp();
                        }

                        if (conversionToDifferential) {
                            if (latestSample.getTimestamp().equals(timeStampOfConversion)
                                    || latestSample.getTimestamp().isAfter(timeStampOfConversion)
                                    && ((nextTimeStampOfConversion == null) || latestSample.getTimestamp().isBefore(nextTimeStampOfConversion))) {
                                isCounter = true;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Could not determine diff or not", e);
        }

        return isCounter;
    }
}
