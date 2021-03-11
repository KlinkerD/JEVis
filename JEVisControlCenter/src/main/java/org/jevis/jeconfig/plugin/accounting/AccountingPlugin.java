package org.jevis.jeconfig.plugin.accounting;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ibm.icu.text.NumberFormat;
import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jevis.api.*;
import org.jevis.commons.JEVisFileImp;
import org.jevis.commons.i18n.I18n;
import org.jevis.commons.object.plugin.TargetHelper;
import org.jevis.jeconfig.Constants;
import org.jevis.jeconfig.GlobalToolBar;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.application.application.I18nWS;
import org.jevis.jeconfig.application.control.SaveUnderDialog;
import org.jevis.jeconfig.application.tools.JEVisHelp;
import org.jevis.jeconfig.dialog.RenameDialog;
import org.jevis.jeconfig.plugin.TablePlugin;
import org.jevis.jeconfig.plugin.dtrc.*;
import org.jevis.jeconfig.plugin.meters.RegisterTableRow;
import org.jevis.jeconfig.plugin.object.attribute.AttributeEditor;
import org.jevis.jeconfig.plugin.object.attribute.PeriodEditor;
import org.jevis.jeconfig.plugin.object.extension.GenericAttributeExtension;
import org.joda.time.DateTime;
import org.mariuszgromada.math.mxparser.Expression;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.prefs.Preferences;

import static org.jevis.jeconfig.plugin.dtrc.TRCPlugin.TEMPLATE_CLASS;

public class AccountingPlugin extends TablePlugin {
    public static final String ACCOUNTING_CLASS = "Energy Contracting Directory";
    private static final String PLUGIN_CLASS_NAME = "Accounting Plugin";
    private static final Insets INSETS = new Insets(12);
    private static final double EDITOR_MAX_HEIGHT = 50;
    private static final String ACCOUNTING_CONFIGURATION = "Accounting Configuration";
    private static final String ACCOUNTING_CONFIGURATION_DIRECTORY = "Accounting Configuration Directory";
    private static final String DATA_MODEL_ATTRIBUTE = "Template File";
    public static String PLUGIN_NAME = "Accounting Plugin";

    private static final Logger logger = LogManager.getLogger(AccountingPlugin.class);
    private final PseudoClass header = PseudoClass.getPseudoClass("section-header");
    private final Preferences pref = Preferences.userRoot().node("JEVis.JEConfig.AccountingPlugin");
    private final Image taskImage = JEConfig.getImage("accounting.png");
    private final ToolBar toolBar = new ToolBar();
    private final AccountingDirectories accountingDirectories;
    private final AccountingTemplateHandler ath = new AccountingTemplateHandler();
    private final NumberFormat nf = NumberFormat.getInstance(I18n.getInstance().getLocale());
    private final ObjectMapper mapper = new ObjectMapper();
    private final ToggleButton newButton = new ToggleButton("", JEConfig.getImage("list-add.png", toolBarIconSize, toolBarIconSize));
    private final ToggleButton reload = new ToggleButton("", JEConfig.getImage("1403018303_Refresh.png", toolBarIconSize, toolBarIconSize));
    private final ToggleButton save = new ToggleButton("", JEConfig.getImage("save.gif", toolBarIconSize, toolBarIconSize));
    private final ToggleButton delete = new ToggleButton("", JEConfig.getImage("if_trash_(delete)_16x16_10030.gif", toolBarIconSize, toolBarIconSize));
    private final ToggleButton xlsxButton = new ToggleButton("", JEConfig.getImage("xlsx_315594.png", toolBarIconSize, toolBarIconSize));
    private final ToggleButton printButton = new ToggleButton("", JEConfig.getImage("Print_1493286.png", toolBarIconSize, toolBarIconSize));
    private final ToggleButton infoButton = JEVisHelp.getInstance().buildInfoButtons(toolBarIconSize, toolBarIconSize);
    private final ToggleButton helpButton = JEVisHelp.getInstance().buildHelpButtons(toolBarIconSize, toolBarIconSize);
    private final BorderPane borderPane = new BorderPane();
    private final TabPane motherTabPane = new TabPane();
    private final TabPane enterDataTabPane = new TabPane();
    private final TabPane configTabPane = new TabPane();
    private final JFXComboBox<JEVisObject> configComboBox = new JFXComboBox<>();
    private final StackPane enterDataStackPane = new StackPane(enterDataTabPane);
    private final StackPane configStackPane = new StackPane(configTabPane);
    private final Tab enterDataTab = new Tab(I18n.getInstance().getString("plugin.accounting.tab.enterdata"));
    private final Tab energySupplierTab = new Tab();
    private final Tab energyMeteringOperatorsTab = new Tab();
    private final Tab energyGridOperatorsTab = new Tab();
    private final Tab energyContractorTab = new Tab();
    private final Tab governmentalDuesTab = new Tab();
    private final JFXComboBox<JEVisObject> trcs = new JFXComboBox<>();
    private final Callback<ListView<JEVisClass>, ListCell<JEVisClass>> classNameCellFactory = new Callback<ListView<JEVisClass>, ListCell<JEVisClass>>() {
        @Override
        public ListCell<JEVisClass> call(ListView<JEVisClass> param) {
            return new JFXListCell<JEVisClass>() {
                @Override
                protected void updateItem(JEVisClass obj, boolean empty) {
                    super.updateItem(obj, empty);
                    if (obj == null || empty) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        try {
                            setText(I18nWS.getInstance().getClassName(obj.getName()));
                        } catch (JEVisException e) {
                            logger.error("Could not get name of class {}", obj, e);
                        }
                    }
                }
            };
        }
    };

    private final Callback<ListView<JEVisObject>, ListCell<JEVisObject>> objectNameCellFactory = new Callback<ListView<JEVisObject>, ListCell<JEVisObject>>() {
        @Override
        public ListCell<JEVisObject> call(ListView<JEVisObject> param) {
            return new JFXListCell<JEVisObject>() {
                @Override
                protected void updateItem(JEVisObject obj, boolean empty) {
                    super.updateItem(obj, empty);
                    if (obj == null || empty) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        setText(obj.getName());
                    }
                }
            };
        }
    };
    private final JFXComboBox<JEVisObject> energySupplierBox = new JFXComboBox<>();

    private final Callback<ListView<ComboBoxItem>, ListCell<ComboBoxItem>> comboBoxItemCellFactory = new Callback<ListView<ComboBoxItem>, ListCell<ComboBoxItem>>() {
        @Override
        public ListCell<ComboBoxItem> call(ListView<ComboBoxItem> param) {
            return new JFXListCell<ComboBoxItem>() {
                @Override
                protected void updateItem(ComboBoxItem obj, boolean empty) {
                    super.updateItem(obj, empty);
                    if (empty) {
                        setText(null);
                        setDisable(false);
                        pseudoClassStateChanged(header, false);
                    } else {
                        setText(obj.toString());
                        setDisable(!obj.isSelectable());
                        pseudoClassStateChanged(header, !obj.isSelectable());
                    }
                }
            };
        }
    };
    private final JFXComboBox<JEVisObject> energyMeteringOperatorBox = new JFXComboBox<>();
    private final JFXComboBox<JEVisObject> energyGridOperatorBox = new JFXComboBox<>();
    private final JFXComboBox<ComboBoxItem> energyContractorBox = new JFXComboBox<>();
    private final JFXComboBox<JEVisObject> governmentalDuesBox = new JFXComboBox<>();
    private final List<AttributeEditor> attributeEditors = new ArrayList<>();
    private boolean initialized = false;
    private boolean guiUpdate = false;
    private final TemplateHandler templateHandler = new TemplateHandler();
    private final OutputView viewTab;
    private final Tab configTab = new Tab(I18n.getInstance().getString("plugin.accounting.tab.config"));

    public AccountingPlugin(JEVisDataSource ds, String title) {
        super(ds, title);

        accountingDirectories = new AccountingDirectories(ds);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        viewTab = new OutputView(I18n.getInstance().getString("plugin.accounting.tab.view"), ds, templateHandler);

        enterDataTab.setContent(enterDataStackPane);

        enterDataTab.setClosable(false);
        energySupplierTab.setClosable(false);
        energyMeteringOperatorsTab.setClosable(false);
        energyGridOperatorsTab.setClosable(false);
        energyContractorTab.setClosable(false);
        governmentalDuesTab.setClosable(false);

        configTab.setContent(configStackPane);
        configTab.setClosable(false);

        Tab inputs = new Tab(I18n.getInstance().getString("plugin.dtrc.dialog.inputslabel"));
        inputs.setClosable(false);

        Label trcsLabel = new Label(I18nWS.getInstance().getClassName(TEMPLATE_CLASS));
        HBox trcsBox = new HBox(6, trcsLabel, trcs);

        VBox configVBox = new VBox(6, trcsBox, viewTab.getViewInputs());
        configVBox.setPadding(INSETS);

        inputs.setContent(configVBox);
        configTabPane.getTabs().add(inputs);

        configComboBox.setCellFactory(objectNameCellFactory);
        configComboBox.setButtonCell(objectNameCellFactory.call(null));

        try {
            energySupplierTab.setText(I18nWS.getInstance().getClassName(accountingDirectories.getEnergySupplierClass()));
            energyMeteringOperatorsTab.setText(I18nWS.getInstance().getClassName(accountingDirectories.getEnergyMeteringOperatorClass()));
            energyGridOperatorsTab.setText(I18nWS.getInstance().getClassName(accountingDirectories.getEnergyGridOperatorClass()));
            energyContractorTab.setText(I18nWS.getInstance().getClassName(accountingDirectories.getEnergyContractorClass()));
            governmentalDuesTab.setText(I18nWS.getInstance().getClassName(accountingDirectories.getGovernmentalDuesClass()));
        } catch (JEVisException e) {
            logger.error("Could not get class name for tabs", e);
        }

        energySupplierBox.setCellFactory(objectNameCellFactory);
        energySupplierBox.setButtonCell(objectNameCellFactory.call(null));

        energyMeteringOperatorBox.setCellFactory(objectNameCellFactory);
        energyMeteringOperatorBox.setButtonCell(objectNameCellFactory.call(null));

        energyGridOperatorBox.setCellFactory(objectNameCellFactory);
        energyGridOperatorBox.setButtonCell(objectNameCellFactory.call(null));

        energyContractorBox.setCellFactory(comboBoxItemCellFactory);
        energyContractorBox.setButtonCell(comboBoxItemCellFactory.call(null));

        governmentalDuesBox.setCellFactory(objectNameCellFactory);
        governmentalDuesBox.setButtonCell(objectNameCellFactory.call(null));

        reload.setTooltip(new Tooltip(I18n.getInstance().getString("plugin.accounting.toolbar.reload.tooltip")));
        save.setTooltip(new Tooltip(I18n.getInstance().getString("plugin.accounting.toolbar.save.tooltip")));
        newButton.setTooltip(new Tooltip(I18n.getInstance().getString("plugin.accounting.new.tooltip")));
        printButton.setTooltip(new Tooltip(I18n.getInstance().getString("plugin.accounting.toolbar.tooltip.print")));

        motherTabPane.getTabs().addAll(viewTab, enterDataTab, configTab);

        motherTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.equals(enterDataTab)) {
                Platform.runLater(() -> {
                    newButton.setDisable(false);
                    delete.setDisable(false);
                });
            } else {
                Platform.runLater(() -> {
                    newButton.setDisable(true);
                    delete.setDisable(true);
                });
            }
        });

        this.borderPane.setCenter(motherTabPane);

        initToolBar();
    }

    private void initToolBar() {

        GlobalToolBar.changeBackgroundOnHoverUsingBinding(reload);
        reload.setOnAction(event -> handleRequest(Constants.Plugin.Command.RELOAD));

        Separator sep1 = new Separator(Orientation.VERTICAL);

        GlobalToolBar.changeBackgroundOnHoverUsingBinding(save);
        save.setOnAction(event -> handleRequest(Constants.Plugin.Command.SAVE));

        Separator sep2 = new Separator(Orientation.VERTICAL);

        GlobalToolBar.changeBackgroundOnHoverUsingBinding(newButton);
        newButton.setOnAction(event -> handleRequest(Constants.Plugin.Command.NEW));

        GlobalToolBar.changeBackgroundOnHoverUsingBinding(delete);
        delete.setOnAction(event -> handleRequest(Constants.Plugin.Command.DELETE));

        Separator sep3 = new Separator(Orientation.VERTICAL);


        Tooltip xlsxTooltip = new Tooltip(I18n.getInstance().getString("plugin.reports.toolbar.tooltip.xlsx"));
        xlsxButton.setTooltip(xlsxTooltip);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(xlsxButton);

        xlsxButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("XLSX File Destination");
            FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", ".xlsx");
            fileChooser.getExtensionFilters().addAll(pdfFilter);
            fileChooser.setSelectedExtensionFilter(pdfFilter);

            File selectedFile = fileChooser.showSaveDialog(JEConfig.getStage());
            if (selectedFile != null) {
                JEConfig.setLastPath(selectedFile);
                createExcelFile(selectedFile);
            }
        });

        GlobalToolBar.changeBackgroundOnHoverUsingBinding(printButton);
        printButton.setDisable(true);
        printButton.setOnAction(event -> {
            Tab selectedItem = motherTabPane.getSelectionModel().getSelectedItem();
            TableView<RegisterTableRow> tableView = (TableView<RegisterTableRow>) selectedItem.getContent();

            Printer printer = null;
            ObservableSet<Printer> printers = Printer.getAllPrinters();
            printer = printers.stream().findFirst().orElse(printer);

            if (printer != null) {
                PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.LANDSCAPE, Printer.MarginType.DEFAULT);
                PrinterJob job = PrinterJob.createPrinterJob(printer);

                if (job.showPrintDialog(JEConfig.getStage().getOwner())) {
                    double pagePrintableWidth = job.getJobSettings().getPageLayout().getPrintableWidth();
                    double pagePrintableHeight = job.getJobSettings().getPageLayout().getPrintableHeight();

                    double prefHeight = tableView.getPrefHeight();
                    double minHeight = tableView.getMinHeight();
                    double maxHeight = tableView.getMaxHeight();

                    tableView.prefHeightProperty().bind(Bindings.size(tableView.getItems()).multiply(EDITOR_MAX_HEIGHT));
                    tableView.minHeightProperty().bind(tableView.prefHeightProperty());
                    tableView.maxHeightProperty().bind(tableView.prefHeightProperty());

                    double scaleX = pagePrintableWidth / tableView.getBoundsInParent().getWidth();
                    double scaleY = scaleX;
                    double localScale = scaleX;

                    double numberOfPages = Math.ceil((tableView.getPrefHeight() * localScale) / pagePrintableHeight);

                    tableView.getTransforms().add(new Scale(scaleX, (scaleY)));
                    tableView.getTransforms().add(new Translate(0, 0));

                    Translate gridTransform = new Translate();
                    tableView.getTransforms().add(gridTransform);

                    for (int i = 0; i < numberOfPages; i++) {
                        gridTransform.setY(-i * (pagePrintableHeight / localScale));
                        job.printPage(pageLayout, tableView);
                    }

                    job.endJob();

                    tableView.prefHeightProperty().unbind();
                    tableView.minHeightProperty().unbind();
                    tableView.maxHeightProperty().unbind();
                    tableView.getTransforms().clear();

                    tableView.setMinHeight(minHeight);
                    tableView.setMaxHeight(maxHeight);
                    tableView.setPrefHeight(prefHeight);
                }
            }
        });

        toolBar.getItems().setAll(configComboBox, sep1, reload, sep2, save, newButton, delete, sep3, xlsxButton, printButton);
        toolBar.getItems().addAll(JEVisHelp.getInstance().buildSpacerNode(), helpButton, infoButton);
        JEVisHelp.getInstance().addHelpItems(AccountingPlugin.class.getSimpleName(), "", JEVisHelp.LAYOUT.VERTICAL_BOT_CENTER, toolBar.getItems());
    }

    private void createExcelFile(File destinationFile) {

        XSSFWorkbook workbook = new XSSFWorkbook(); //create workbook

        XSSFDataFormat dataFormatDates = workbook.createDataFormat();
        dataFormatDates.putFormat((short) 165, "YYYY-MM-dd HH:MM:ss");
        CellStyle cellStyleDateTime = workbook.createCellStyle();
        cellStyleDateTime.setDataFormat((short) 165);

        CellStyle cellStyleValues = workbook.createCellStyle();
        cellStyleValues.setDataFormat((short) 4);

        Sheet sheet = workbook.createSheet(I18n.getInstance().getString("plugin.dtrc.view.output"));

        List<TemplateOutput> templateOutputs = templateHandler.getRcTemplate().getTemplateOutputs();
        templateOutputs.sort(viewTab::compareTemplateOutputs);
        Map<String, Double> resultMap = new HashMap<>();

        DateTime start = viewTab.getStart();
        DateTime end = viewTab.getEnd();

        for (TemplateOutput templateOutput : templateOutputs) {
            if (!templateOutput.getSeparator()) {
                Cell cell = getOrCreateCell(sheet, templateOutput.getRow(), templateOutput.getColumn());
                boolean hasLabel = false;
                if (templateOutput.getName() != null && !templateOutput.getName().equals("")) {
                    cell.setCellValue(templateOutput.getName());
                    hasLabel = true;
                }
                TemplateFormula formula = templateHandler.getRcTemplate().getTemplateFormulas().stream().filter(templateFormula -> templateFormula.getOutput().equals(templateOutput.getId())).findFirst().orElse(null);
                String result = "";
                if (formula != null) {
                    String formulaString = formula.getFormula();
                    boolean isText = false;
                    for (TemplateInput templateInput : templateHandler.getRcTemplate().getTemplateInputs()) {
                        if (formula.getInputIds().contains(templateInput.getId())) {
                            try {
                                if (templateInput.getVariableType().equals(InputVariableType.STRING.toString())) {
                                    isText = true;
                                }

                                if (!templateInput.getVariableType().equals(InputVariableType.FORMULA.toString())) {
                                    formulaString = formulaString.replace(templateInput.getVariableName(), templateInput.getValue(ds, start, end));
                                } else {
                                    Double d = resultMap.get(templateInput.getVariableName());
                                    if (d != null) {
                                        formulaString = formulaString.replace(templateInput.getVariableName(), d.toString());
                                    }
                                }

                            } catch (JEVisException e) {
                                logger.error("Could not get template input value for {}", templateInput.getVariableName(), e);
                            }
                        }
                    }

                    if (!isText) {
                        try {
                            Expression expression = new Expression(formulaString);
                            Double calculate = expression.calculate();
                            if (!calculate.isNaN()) {
                                resultMap.put(formula.getName(), calculate);
                            }
                            result = nf.format(calculate) + " " + templateOutput.getUnit();
                        } catch (Exception e) {
                            logger.error("Error in formula {}", formula.getName(), e);
                        }
                    } else {
                        result = formulaString;
                    }
                } else result = "";


                if (hasLabel) {
                    cell.setCellValue(cell.getStringCellValue() + ": " + result);
                } else {
                    cell.setCellValue(result);
                }
            }
        }

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(destinationFile);
            workbook.write(fileOutputStream);
            workbook.close();
            fileOutputStream.close();
        } catch (IOException e) {
            logger.error("Could not save file {}", destinationFile, e);
        }
    }

    @Override
    public String getClassName() {
        return AccountingPlugin.PLUGIN_CLASS_NAME;
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
        return I18n.getInstance().getString("plugin.accounting.tooltip");
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
        switch (cmdType) {
            case Constants.Plugin.Command.SAVE:
                return true;
            case Constants.Plugin.Command.DELETE:
                return true;
            case Constants.Plugin.Command.EXPAND:
                return false;
            case Constants.Plugin.Command.NEW:
                return true;
            case Constants.Plugin.Command.RELOAD:
                return true;
            case Constants.Plugin.Command.ADD_TABLE:
                return false;
            case Constants.Plugin.Command.EDIT_TABLE:
                return false;
            case Constants.Plugin.Command.CREATE_WIZARD:
                return false;
            case Constants.Plugin.Command.FIND_OBJECT:
                return false;
            case Constants.Plugin.Command.PASTE:
                return false;
            case Constants.Plugin.Command.COPY:
                return false;
            case Constants.Plugin.Command.CUT:
                return false;
            case Constants.Plugin.Command.FIND_AGAIN:
                return false;
            default:
                return false;
        }
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
        switch (cmdType) {
            case Constants.Plugin.Command.SAVE:
                if (motherTabPane.getSelectionModel().getSelectedItem().equals(enterDataTab)) {
                    for (AttributeEditor attributeEditor : attributeEditors) {
                        try {
                            attributeEditor.commit();
                        } catch (Exception e) {
                            logger.error("Could not save {}", attributeEditor, e);
                        }
                    }
                    try {
                        attributeEditors.clear();
                        updateGUI();
                    } catch (Exception e) {
                        logger.error("Error while updating GUI", e);
                    }
                } else if (motherTabPane.getSelectionModel().getSelectedItem().equals(configTab)) {
                    try {
                        JEVisClass templateClass = ds.getJEVisClass(ACCOUNTING_CONFIGURATION);

                        SaveUnderDialog saveUnderDialog = new SaveUnderDialog(configStackPane, ds, ACCOUNTING_CONFIGURATION_DIRECTORY, ath.getTemplateObject(), templateClass, ath.getTitle(), (target, sameObject) -> {

                            try {
                                ath.setTitle(target.getName());

                                JEVisAttribute dataModel = target.getAttribute(DATA_MODEL_ATTRIBUTE);

                                JEVisFileImp jsonFile = new JEVisFileImp(
                                        ath.getTitle() + "_" + DateTime.now().toString("yyyyMMddHHmm") + ".json"
                                        , this.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ath.toJsonNode()).getBytes(StandardCharsets.UTF_8));
                                JEVisSample newSample = dataModel.buildSample(new DateTime(), jsonFile);
                                newSample.commit();
                            } catch (Exception e) {
                                logger.error("Could not save template", e);
                            }
                            return true;
                        });
                        saveUnderDialog.show();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                break;
            case Constants.Plugin.Command.DELETE:
                if (motherTabPane.getSelectionModel().getSelectedItem().equals(enterDataTab)) {
                    JEVisObject objectToDelete = null;
                    if (enterDataTabPane.getSelectionModel().getSelectedItem().equals(energySupplierTab)) {
                        objectToDelete = energySupplierBox.getSelectionModel().getSelectedItem();
                    } else if (enterDataTabPane.getSelectionModel().getSelectedItem().equals(energyMeteringOperatorsTab)) {
                        objectToDelete = energyMeteringOperatorBox.getSelectionModel().getSelectedItem();
                    } else if (enterDataTabPane.getSelectionModel().getSelectedItem().equals(energyGridOperatorsTab)) {
                        objectToDelete = energyGridOperatorBox.getSelectionModel().getSelectedItem();
                    } else if (enterDataTabPane.getSelectionModel().getSelectedItem().equals(energyContractorTab)) {
                        objectToDelete = energyContractorBox.getSelectionModel().getSelectedItem().getObject();
                    } else if (enterDataTabPane.getSelectionModel().getSelectedItem().equals(governmentalDuesTab)) {
                        objectToDelete = governmentalDuesBox.getSelectionModel().getSelectedItem();
                    }

                    Label really = new Label(I18n.getInstance().getString("jevistree.dialog.delete.message"));

                    JFXButton ok = new JFXButton(I18n.getInstance().getString("newobject.ok"));
                    ok.setDefaultButton(true);
                    JFXButton cancel = new JFXButton(I18n.getInstance().getString("newobject.cancel"));
                    cancel.setCancelButton(true);

                    JFXDialog dialog = new JFXDialog(enterDataStackPane, new VBox(12, really, new HBox(6, cancel, ok)), JFXDialog.DialogTransition.CENTER);

                    cancel.setOnAction(event -> dialog.close());
                    JEVisObject finalObjectToDelete = objectToDelete;
                    ok.setOnAction(event -> {
                        try {
                            if (finalObjectToDelete != null) {
                                ds.deleteObject(finalObjectToDelete.getID());
                                updateGUI();
                            }
                        } catch (JEVisException e) {
                            logger.error("Could not delete object {}:{}", finalObjectToDelete.getName(), finalObjectToDelete.getID(), e);
                        }
                        dialog.close();
                    });

                    dialog.show();
                }
                break;
            case Constants.Plugin.Command.EXPAND:
                break;
            case Constants.Plugin.Command.NEW:
                if (motherTabPane.getSelectionModel().getSelectedItem().equals(enterDataTab)) {
                    JEVisObject directory = null;
                    List<JEVisClass> newObjectClasses = new ArrayList<>();
                    JFXComboBox<JEVisObject> selected = null;
                    JFXComboBox<ComboBoxItem> selectedOther = null;

                    if (enterDataTabPane.getSelectionModel().getSelectedItem().equals(energySupplierTab)) {
                        directory = accountingDirectories.getEnergySupplyDir();
                        newObjectClasses.add(accountingDirectories.getElectricitySupplyContractorClass());
                        newObjectClasses.add(accountingDirectories.getGasSupplyContractorClass());
                        newObjectClasses.add(accountingDirectories.getCommunityHeatingSupplyContractorClass());
                        selected = energySupplierBox;
                    } else if (enterDataTabPane.getSelectionModel().getSelectedItem().equals(energyMeteringOperatorsTab)) {
                        directory = accountingDirectories.getEnergyMeteringPointOperationDir();
                        newObjectClasses.add(accountingDirectories.getElectricityMeteringPointOperatorClass());
                        newObjectClasses.add(accountingDirectories.getGasMeteringPointOperatorClass());
                        newObjectClasses.add(accountingDirectories.getCommunityHeatingMeteringPointOperatorClass());
                        selected = energyMeteringOperatorBox;
                    } else if (enterDataTabPane.getSelectionModel().getSelectedItem().equals(energyGridOperatorsTab)) {
                        directory = accountingDirectories.getEnergyGridOperationDir();
                        newObjectClasses.add(accountingDirectories.getElectricityGridOperatorClass());
                        newObjectClasses.add(accountingDirectories.getGasGridOperatorClass());
                        newObjectClasses.add(accountingDirectories.getCommunityHeatingGridOperatorClass());
                        selected = energyGridOperatorBox;
                    } else if (enterDataTabPane.getSelectionModel().getSelectedItem().equals(energyContractorTab)) {
                        directory = accountingDirectories.getEnergyContractorDir();
                        newObjectClasses.add(accountingDirectories.getEnergySupplyContractorClass());
                        newObjectClasses.add(accountingDirectories.getEnergyMeteringPointOperationContractorClass());
                        newObjectClasses.add(accountingDirectories.getEnergyGridOperationContractorClass());
                        selectedOther = energyContractorBox;
                    } else if (enterDataTabPane.getSelectionModel().getSelectedItem().equals(governmentalDuesTab)) {
                        directory = accountingDirectories.getEnergyGovernmentalDuesDir();
                        newObjectClasses.add(accountingDirectories.getGovernmentalDuesClass());
                        selected = governmentalDuesBox;
                    }

                    JFXComboBox<JEVisClass> box = new JFXComboBox<>();
                    box.setCellFactory(classNameCellFactory);
                    box.setButtonCell(classNameCellFactory.call(null));
                    box.getItems().addAll(newObjectClasses);
                    box.getSelectionModel().selectFirst();

                    JFXTextField nameField = new JFXTextField();
                    nameField.setPromptText(I18n.getInstance().getString("newobject.name.prompt"));

                    JFXButton ok = new JFXButton(I18n.getInstance().getString("newobject.ok"));
                    ok.setDefaultButton(true);
                    JFXButton cancel = new JFXButton(I18n.getInstance().getString("newobject.cancel"));
                    cancel.setCancelButton(true);

                    VBox vBox = new VBox(12, nameField, box, new HBox(6, cancel, ok));
                    vBox.setPadding(INSETS);

                    JFXDialog dialog = new JFXDialog(enterDataStackPane, vBox, JFXDialog.DialogTransition.CENTER);

                    cancel.setOnAction(event -> dialog.close());
                    JEVisObject finalDirectory = directory;
                    JFXComboBox<JEVisObject> finalSelected = selected;
                    JFXComboBox<ComboBoxItem> finalSelectedOther = selectedOther;
                    ok.setOnAction(event -> {
                        try {
                            JEVisObject jeVisObject = finalDirectory.buildObject(nameField.getText(), box.getSelectionModel().getSelectedItem());
                            jeVisObject.commit();
                            updateGUI();
                            if (finalSelected != null) {
                                Platform.runLater(() -> finalSelected.getSelectionModel().select(jeVisObject));
                            } else if (finalSelectedOther != null) {
                                Platform.runLater(() -> finalSelectedOther.getSelectionModel().select(new ComboBoxItem(jeVisObject, true)));
                            }
                        } catch (Exception e) {
                            logger.error("Could not create object {} under directory {}:{}", nameField.getText(), finalDirectory.getName(), finalDirectory.getID(), e);
                        }
                        dialog.close();
                    });

                    dialog.show();
                } else if (motherTabPane.getSelectionModel().getSelectedItem().equals(configTab)) {

                }
                break;
            case Constants.Plugin.Command.RELOAD:

                Task clearCacheTask = new Task() {
                    @Override
                    protected Object call() throws Exception {
                        try {
                            this.updateTitle(I18n.getInstance().getString("Clear Cache"));
                            if (initialized) {
                                ds.clearCache();
                                ds.preload();
                            } else {
                                initialized = true;

                                updateGUI();
                            }

                            succeeded();
                        } catch (Exception ex) {
                            failed();
                        } finally {
                            done();
                        }
                        return null;
                    }
                };
                JEConfig.getStatusBar().addTask(PLUGIN_NAME, clearCacheTask, JEConfig.getImage("accounting.png"), true);
                break;
            case Constants.Plugin.Command.ADD_TABLE:
                break;
            case Constants.Plugin.Command.EDIT_TABLE:
                break;
            case Constants.Plugin.Command.CREATE_WIZARD:
                break;
            case Constants.Plugin.Command.FIND_OBJECT:
                break;
            case Constants.Plugin.Command.PASTE:
                break;
            case Constants.Plugin.Command.COPY:
                break;
            case Constants.Plugin.Command.CUT:
                break;
            case Constants.Plugin.Command.FIND_AGAIN:
                break;
        }
    }

    @Override
    public Node getContentNode() {
        return borderPane;
    }


    @Override
    public ImageView getIcon() {
        return JEConfig.getImage("accounting.png", 20, 20);
    }

    @Override
    public void fireCloseEvent() {

    }

    @Override
    public void setHasFocus() {

        try {
            initialized = true;
            boolean isMultiSite = isMultiSite(AccountingPlugin.ACCOUNTING_CLASS);

            initGUI();

            viewTab.showDatePicker(false);
            viewTab.showInputs(false);
            viewTab.getIntervalSelector().getTimeFactoryBox().getItems().remove(0, 2);
            viewTab.getIntervalSelector().getTimeFactoryBox().getItems().remove(2, viewTab.getIntervalSelector().getTimeFactoryBox().getItems().size());

            List<JEVisObject> allAccountingConfigurations = getAllAccountingConfigurations();
            if (allAccountingConfigurations.isEmpty()) {
                SelectionTemplate selectionTemplate = new SelectionTemplate();
                ath.setSelectionTemplate(selectionTemplate);
                viewTab.setSelectionTemplate(selectionTemplate);
            } else {
                configComboBox.getItems().clear();
                configComboBox.getItems().addAll(allAccountingConfigurations);
                configComboBox.getSelectionModel().selectFirst();
            }

        } catch (JEVisException e) {
            e.printStackTrace();
        }

    }

    public void initGUI() throws JEVisException {

        Callback<ListView<JEVisObject>, ListCell<JEVisObject>> attributeCellFactory = new Callback<ListView<JEVisObject>, ListCell<JEVisObject>>() {
            @Override
            public ListCell<JEVisObject> call(ListView<JEVisObject> param) {
                return new JFXListCell<JEVisObject>() {
                    @Override
                    protected void updateItem(JEVisObject obj, boolean empty) {
                        super.updateItem(obj, empty);
                        if (obj == null || empty) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setText(obj.getName());
                        }
                    }
                };
            }
        };

        trcs.setCellFactory(attributeCellFactory);
        trcs.setButtonCell(attributeCellFactory.call(null));

        trcs.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                templateHandler.setTemplateObject(newValue);
                ath.getSelectionTemplate().setTemplateSelection(newValue.getID());

                viewTab.updateViewInputFlowPane();
                viewTab.requestUpdate();
            }
        });

        configComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                ath.setTemplateObject(newValue);

                try {
                    JEVisObject selectedObject = ds.getObject(ath.getSelectionTemplate().getTemplateSelection());
                    viewTab.setSelectionTemplate(ath.getSelectionTemplate());
                    trcs.getSelectionModel().select(null);
                    trcs.getSelectionModel().select(selectedObject);
                } catch (Exception e) {
                    logger.error("Could not get selected object from selection template {}", ath.getSelectionTemplate().getTemplateSelection(), e);
                }
            }
        });

        GridPane esGP = new GridPane();
        esGP.setPadding(INSETS);
        esGP.setHgap(6);
        esGP.setVgap(6);

        Separator separator1 = new Separator(Orientation.HORIZONTAL);
        separator1.setPadding(new Insets(8, 0, 8, 0));

        JFXButton esRename = new JFXButton(I18n.getInstance().getString("plugin.meters.button.rename"));
        esRename.setOnAction(event -> {
            if (energySupplierBox.getSelectionModel().getSelectedItem() != null) {
                RenameDialog renameDialog = new RenameDialog(enterDataStackPane, energySupplierBox.getSelectionModel().getSelectedItem());
                renameDialog.show();
            }
        });
        VBox es0VBox = new VBox(esRename);
        es0VBox.setAlignment(Pos.CENTER);
        HBox esHBox = new HBox(6, energySupplierBox, es0VBox);

        VBox esVBox = new VBox(6, esHBox, separator1, esGP);
        energySupplierTab.setContent(esVBox);

        energySupplierBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateWithChangeCheck(esGP, newValue);
        });

        GridPane emoGP = new GridPane();
        emoGP.setPadding(INSETS);
        emoGP.setHgap(6);
        emoGP.setVgap(6);

        Separator separator2 = new Separator(Orientation.HORIZONTAL);
        separator2.setPadding(new Insets(8, 0, 8, 0));

        JFXButton emoRename = new JFXButton(I18n.getInstance().getString("plugin.meters.button.rename"));
        emoRename.setOnAction(event -> {
            if (energyMeteringOperatorBox.getSelectionModel().getSelectedItem() != null) {
                RenameDialog renameDialog = new RenameDialog(enterDataStackPane, energyMeteringOperatorBox.getSelectionModel().getSelectedItem());
                renameDialog.show();
            }
        });
        VBox emo0VBox = new VBox(emoRename);
        emo0VBox.setAlignment(Pos.CENTER);
        HBox emoHBox = new HBox(6, energyMeteringOperatorBox, emo0VBox);

        VBox emoVBox = new VBox(6, emoHBox, separator2, emoGP);
        energyMeteringOperatorsTab.setContent(emoVBox);

        energyMeteringOperatorBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateWithChangeCheck(emoGP, newValue);
        });

        GridPane egoGP = new GridPane();
        egoGP.setPadding(INSETS);
        egoGP.setHgap(6);
        egoGP.setVgap(6);

        Separator separator3 = new Separator(Orientation.HORIZONTAL);
        separator3.setPadding(new Insets(8, 0, 8, 0));

        JFXButton egoRename = new JFXButton(I18n.getInstance().getString("plugin.meters.button.rename"));
        egoRename.setOnAction(event -> {
            if (energyGridOperatorBox.getSelectionModel().getSelectedItem() != null) {
                RenameDialog renameDialog = new RenameDialog(enterDataStackPane, energyGridOperatorBox.getSelectionModel().getSelectedItem());
                renameDialog.show();
            }
        });
        VBox ego0VBox = new VBox(egoRename);
        ego0VBox.setAlignment(Pos.CENTER);
        HBox egoHBox = new HBox(6, energyGridOperatorBox, ego0VBox);

        VBox egoVBox = new VBox(6, egoHBox, separator3, egoGP);
        energyGridOperatorsTab.setContent(egoVBox);

        energyGridOperatorBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateWithChangeCheck(egoGP, newValue);
        });

        GridPane cGP = new GridPane();
        cGP.setPadding(INSETS);
        cGP.setHgap(6);
        cGP.setVgap(6);

        Separator separator4 = new Separator(Orientation.HORIZONTAL);
        separator4.setPadding(new Insets(8, 0, 8, 0));

        JFXButton cvRename = new JFXButton(I18n.getInstance().getString("plugin.meters.button.rename"));
        cvRename.setOnAction(event -> {
            if (energyContractorBox.getSelectionModel().getSelectedItem() != null) {
                RenameDialog renameDialog = new RenameDialog(enterDataStackPane, energyContractorBox.getSelectionModel().getSelectedItem().getObject());
                renameDialog.show();
            }
        });
        VBox cv0VBox = new VBox(cvRename);
        cv0VBox.setAlignment(Pos.CENTER);
        HBox cvHBox = new HBox(6, energyContractorBox, cv0VBox);

        VBox cVBox = new VBox(6, cvHBox, separator4, cGP);
        energyContractorTab.setContent(cVBox);

        energyContractorBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateWithChangeCheck(cGP, newValue.getObject());
        });

        GridPane gdGP = new GridPane();
        gdGP.setPadding(INSETS);
        gdGP.setHgap(6);
        gdGP.setVgap(6);

        Separator separator5 = new Separator(Orientation.HORIZONTAL);
        separator5.setPadding(new Insets(8, 0, 8, 0));

        JFXButton gdRename = new JFXButton(I18n.getInstance().getString("plugin.meters.button.rename"));
        gdRename.setOnAction(event -> {
            if (governmentalDuesBox.getSelectionModel().getSelectedItem() != null) {
                RenameDialog renameDialog = new RenameDialog(enterDataStackPane, governmentalDuesBox.getSelectionModel().getSelectedItem());
                renameDialog.show();
            }
        });
        VBox gd0VBox = new VBox(gdRename);
        gd0VBox.setAlignment(Pos.CENTER);

        HBox gdHBox = new HBox(6, governmentalDuesBox, gd0VBox);

        VBox gdVBox = new VBox(6, gdHBox, separator5, gdGP);
        governmentalDuesTab.setContent(gdVBox);

        governmentalDuesBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateWithChangeCheck(gdGP, newValue);
        });

        Platform.runLater(() -> enterDataTabPane.getTabs().addAll(energySupplierTab, energyMeteringOperatorsTab, energyGridOperatorsTab, energyContractorTab, governmentalDuesTab));

        updateGUI();
    }

    private void updateWithChangeCheck(GridPane esGP, JEVisObject newValue) {
        boolean changed = attributeEditors.stream().anyMatch(AttributeEditor::hasChanged);

        if (changed && !guiUpdate) {
            Label saved = new Label(I18n.getInstance().getString("plugin.dashboard.dialog.changed.text"));
            JFXButton ok = new JFXButton(I18n.getInstance().getString("graph.dialog.ok"));
            ok.setDefaultButton(true);
            JFXButton cancel = new JFXButton(I18n.getInstance().getString("graph.dialog.cancel"));
            cancel.setCancelButton(true);

            HBox buttonBox = new HBox(6, cancel, ok);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);

            Separator separator = new Separator(Orientation.HORIZONTAL);
            separator.setPadding(new Insets(8, 0, 8, 0));

            VBox vBox = new VBox(6, saved, separator, buttonBox);
            vBox.setPadding(INSETS);

            JFXDialog dialog = new JFXDialog(enterDataStackPane, vBox, JFXDialog.DialogTransition.CENTER);

            cancel.setOnAction(event -> {
                dialog.close();
                updateGrid(esGP, newValue);
            });

            ok.setOnAction(event -> {
                for (AttributeEditor attributeEditor : attributeEditors) {
                    try {
                        attributeEditor.commit();
                    } catch (JEVisException e) {
                        logger.error("Could not save attribute editor {}", attributeEditor, e);
                    }
                }
                attributeEditors.clear();
                dialog.close();
                updateGrid(esGP, newValue);
            });

            dialog.show();
        } else {
            attributeEditors.clear();
            updateGrid(esGP, newValue);
        }
    }

    private void updateGUI() throws JEVisException {
        guiUpdate = true;

        JEVisObject configComboBoxSelectedItem = configComboBox.getSelectionModel().getSelectedItem();
        JEVisObject energySupplierBoxSelectedItem = energySupplierBox.getSelectionModel().getSelectedItem();
        JEVisObject energyMeteringOperatorBoxSelectedItem = energyMeteringOperatorBox.getSelectionModel().getSelectedItem();
        JEVisObject energyGridOperatorBoxSelectedItem = energyGridOperatorBox.getSelectionModel().getSelectedItem();
        ComboBoxItem energyContractorBoxSelectedItem = energyContractorBox.getSelectionModel().getSelectedItem();
        JEVisObject governmentalDuesBoxSelectedItem = governmentalDuesBox.getSelectionModel().getSelectedItem();

        configComboBox.getItems().clear();
        energySupplierBox.getItems().clear();
        energyMeteringOperatorBox.getItems().clear();
        energyGridOperatorBox.getItems().clear();
        energyContractorBox.getItems().clear();
        governmentalDuesBox.getItems().clear();
        trcs.getItems().clear();

        trcs.getItems().addAll(getAllTemplateCalculations());

        configComboBox.getItems().addAll(getAllAccountingConfigurations());

        try {
            if (configComboBoxSelectedItem != null && configComboBox.getItems().contains(configComboBoxSelectedItem)) {
                configComboBox.getSelectionModel().select(configComboBoxSelectedItem);
            } else {
                configComboBox.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
        }

        List<JEVisObject> allEnergySupplier = ds.getObjects(accountingDirectories.getEnergySupplierClass(), true);
        allEnergySupplier.sort((o1, o2) -> alphanumComparator.compare(o1.getName(), o2.getName()));
        energySupplierBox.getItems().addAll(allEnergySupplier);

        if (energySupplierBoxSelectedItem != null && energySupplierBox.getItems().contains(energySupplierBoxSelectedItem)) {
            energySupplierBox.getSelectionModel().select(energySupplierBoxSelectedItem);
        } else {
            energySupplierBox.getSelectionModel().selectFirst();
        }

        List<JEVisObject> allEnergyMeteringOperators = ds.getObjects(accountingDirectories.getEnergyMeteringOperatorClass(), true);
        allEnergyMeteringOperators.sort((o1, o2) -> alphanumComparator.compare(o1.getName(), o2.getName()));
        energyMeteringOperatorBox.getItems().addAll(allEnergyMeteringOperators);

        if (energyMeteringOperatorBoxSelectedItem != null && energyMeteringOperatorBox.getItems().contains(energyMeteringOperatorBoxSelectedItem)) {
            energyMeteringOperatorBox.getSelectionModel().select(energyMeteringOperatorBoxSelectedItem);
        } else {
            energyMeteringOperatorBox.getSelectionModel().selectFirst();
        }

        List<JEVisObject> allEnergyGridOperators = ds.getObjects(accountingDirectories.getEnergyGridOperatorClass(), true);
        allEnergyGridOperators.sort((o1, o2) -> alphanumComparator.compare(o1.getName(), o2.getName()));
        energyGridOperatorBox.getItems().addAll(allEnergyGridOperators);

        if (energyGridOperatorBoxSelectedItem != null && energyGridOperatorBox.getItems().contains(energyGridOperatorBoxSelectedItem)) {
            energyGridOperatorBox.getSelectionModel().select(energyGridOperatorBoxSelectedItem);
        } else {
            energyGridOperatorBox.getSelectionModel().selectFirst();
        }

        List<JEVisObject> energySupplyContractors = ds.getObjects(accountingDirectories.getEnergySupplyContractorClass(), false);
        energySupplyContractors.sort((o1, o2) -> alphanumComparator.compare(o1.getName(), o2.getName()));
        List<JEVisObject> energyMeteringPointContractors = ds.getObjects(accountingDirectories.getEnergyMeteringPointOperationContractorClass(), false);
        energyMeteringPointContractors.sort((o1, o2) -> alphanumComparator.compare(o1.getName(), o2.getName()));
        List<JEVisObject> energyGridOperationContractors = ds.getObjects(accountingDirectories.getEnergyGridOperationContractorClass(), false);
        energyGridOperationContractors.sort((o1, o2) -> alphanumComparator.compare(o1.getName(), o2.getName()));

        List<ComboBoxItem> allContractors = new ArrayList<>();
        allContractors.add(new ComboBoxItem(I18nWS.getInstance().getClassName(accountingDirectories.getEnergySupplyContractorClass().getName()), false));
        energySupplyContractors.forEach(jeVisObject -> allContractors.add(new ComboBoxItem(jeVisObject, true)));
        allContractors.add(new ComboBoxItem("", false));
        allContractors.add(new ComboBoxItem(I18nWS.getInstance().getClassName(accountingDirectories.getEnergyMeteringPointOperationContractorClass().getName()), false));
        energyMeteringPointContractors.forEach(jeVisObject -> allContractors.add(new ComboBoxItem(jeVisObject, true)));
        allContractors.add(new ComboBoxItem("", false));
        allContractors.add(new ComboBoxItem(I18nWS.getInstance().getClassName(accountingDirectories.getEnergyGridOperationContractorClass().getName()), false));
        energyGridOperationContractors.forEach(jeVisObject -> allContractors.add(new ComboBoxItem(jeVisObject, true)));

        energyContractorBox.getItems().addAll(allContractors);

        if (energyContractorBoxSelectedItem != null && energyContractorBox.getItems().contains(energyContractorBoxSelectedItem)) {
            energyContractorBox.getSelectionModel().select(energyContractorBoxSelectedItem);
        } else {
            if (!energySupplyContractors.isEmpty()) {
                energyContractorBox.getSelectionModel().select(new ComboBoxItem(energySupplyContractors.get(0), true));
            } else if (!energyMeteringPointContractors.isEmpty()) {
                energyContractorBox.getSelectionModel().select(new ComboBoxItem(energyMeteringPointContractors.get(0), true));
            } else if (!energyGridOperationContractors.isEmpty()) {
                energyContractorBox.getSelectionModel().select(new ComboBoxItem(energyGridOperationContractors.get(0), true));
            }
        }

        List<JEVisObject> allGovernmentalDues = ds.getObjects(accountingDirectories.getGovernmentalDuesClass(), true);
        allGovernmentalDues.sort((o1, o2) -> alphanumComparator.compare(o1.getName(), o2.getName()));
        governmentalDuesBox.getItems().addAll(allGovernmentalDues);

        if (governmentalDuesBoxSelectedItem != null && governmentalDuesBox.getItems().contains(governmentalDuesBoxSelectedItem)) {
            governmentalDuesBox.getSelectionModel().select(governmentalDuesBoxSelectedItem);
        } else {
            governmentalDuesBox.getSelectionModel().selectFirst();
        }

        guiUpdate = false;
    }

    private List<JEVisObject> getAllAccountingConfigurations() {
        List<JEVisObject> objects = new ArrayList<>();

        try {
            JEVisClass accountingConfigurationClass = ds.getJEVisClass("Accounting Configuration");
            objects.addAll(ds.getObjects(accountingConfigurationClass, false));
        } catch (JEVisException e) {
            logger.error("Could not get any accounting configuration", e);
        }

        return objects;
    }


    private void updateGrid(GridPane gp, JEVisObject selectedObject) {
        if (selectedObject != null) {
            gp.getChildren().clear();

            try {
                List<JEVisAttribute> attributes = selectedObject.getAttributes();
                attributes.sort(Comparator.comparingInt(o -> {
                    try {
                        return o.getType().getGUIPosition();
                    } catch (JEVisException e) {
                        e.printStackTrace();
                    }
                    return -1;
                }));

                int halfAttributesSize = attributes.size() / 2;
                if (attributes.size() % 2 != 0) {
                    halfAttributesSize++;
                }
                int row = 0;
                int column = 0;
                for (int i = 0, size = attributes.size(); i < size; i++) {
                    JEVisAttribute attribute = attributes.get(i);
                    JEVisObject contractor = null;
                    boolean isContractorAttribute = false;
                    if (attribute.getName().equals("Contractor")) {
                        isContractorAttribute = true;
                        if (attribute.hasSample()) {
                            JEVisSample latestSample = attribute.getLatestSample();
                            if (latestSample != null) {
                                TargetHelper th = new TargetHelper(ds, latestSample.getValueAsString());
                                if (th.isValid() && th.targetAccessible()) {
                                    contractor = th.getObject().get(0);
                                }
                            }
                        }
                    }

                    if (i == halfAttributesSize) {
                        row = 0;
                        column += 3;
                    }

                    Label typeName = new Label(I18nWS.getInstance().getTypeName(attribute.getType()));
                    VBox typeBox = new VBox(typeName);
                    typeBox.setAlignment(Pos.CENTER_LEFT);
                    VBox editorBox = new VBox();

                    if (!isContractorAttribute) {
                        AttributeEditor attributeEditor = GenericAttributeExtension.getEditor(enterDataStackPane, attribute.getType(), attribute);
                        attributeEditor.setReadOnly(false);
                        if (attribute.getType().getGUIDisplayType().equals("Period")) {
                            PeriodEditor periodEditor = (PeriodEditor) attributeEditor;
                            periodEditor.showTs(false);
                        }
                        attributeEditors.add(attributeEditor);
                        editorBox.getChildren().setAll(attributeEditor.getEditor());
                        editorBox.setAlignment(Pos.CENTER);
                    } else {
                        JFXComboBox<JEVisObject> contractorBox = new JFXComboBox<>();
                        contractorBox.setCellFactory(objectNameCellFactory);
                        contractorBox.setButtonCell(objectNameCellFactory.call(null));

                        List<JEVisObject> allContractors = new ArrayList<>();
                        String jeVisClassName = selectedObject.getJEVisClass().getInheritance().getName();
                        if (accountingDirectories.getEnergySupplierClass().getName().equals(jeVisClassName)) {
                            allContractors.addAll(ds.getObjects(accountingDirectories.getEnergySupplyContractorClass(), false));
                        } else if (accountingDirectories.getEnergyMeteringOperatorClass().getName().equals(jeVisClassName)) {
                            allContractors.addAll(ds.getObjects(accountingDirectories.getEnergyMeteringPointOperationContractorClass(), false));
                        } else if (accountingDirectories.getEnergyGridOperatorClass().getName().equals(jeVisClassName)) {
                            allContractors.addAll(ds.getObjects(accountingDirectories.getEnergyGridOperationContractorClass(), false));
                        }

                        contractorBox.setItems(FXCollections.observableArrayList(allContractors));

                        if (contractor != null) {
                            contractorBox.getSelectionModel().select(contractor);
                        }

                        contractorBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                            try {
                                JEVisSample newSample = attribute.buildSample(new DateTime(), newValue.getID());
                                newSample.commit();
                            } catch (JEVisException e) {
                                e.printStackTrace();
                            }
                        });

                        editorBox.getChildren().setAll(contractorBox);
                        editorBox.setAlignment(Pos.CENTER_LEFT);
                    }

                    int finalColumn = column;
                    int finalRow = row;
                    Platform.runLater(() -> {
                        gp.add(typeBox, finalColumn, finalRow);
                        gp.add(editorBox, finalColumn + 1, finalRow);
                    });
                    row++;
                }

                Separator separator = new Separator(Orientation.VERTICAL);
                separator.setPadding(new Insets(0, 8, 0, 8));
                int finalHalfAttributesSize = halfAttributesSize;
                Platform.runLater(() -> gp.add(separator, 2, 0, 1, finalHalfAttributesSize + 1));


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private List<JEVisObject> getAllTemplateCalculations() {
        List<JEVisObject> list = new ArrayList<>();
        try {
            JEVisClass templateClass = getDataSource().getJEVisClass(TEMPLATE_CLASS);
            list = getDataSource().getObjects(templateClass, true);
            list.sort((o1, o2) -> alphanumComparator.compare(o1.getName(), o2.getName()));
        } catch (JEVisException e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public void lostFocus() {

    }

    @Override
    public void openObject(Object object) {

    }

    @Override
    public int getPrefTapPos() {
        return 8;
    }

    private org.apache.poi.ss.usermodel.Cell getOrCreateCell(Sheet sheet, int rowIdx, int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }

        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }

        return cell;
    }
}
