package org.jevis.jeconfig.plugin.accounting;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.*;
import org.jevis.commons.JEVisFileImp;
import org.jevis.commons.i18n.I18n;
import org.jevis.commons.object.plugin.TargetHelper;
import org.jevis.jeconfig.Constants;
import org.jevis.jeconfig.GlobalToolBar;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.Plugin;
import org.jevis.jeconfig.application.application.I18nWS;
import org.jevis.jeconfig.application.control.SaveUnderDialog;
import org.jevis.jeconfig.application.tools.JEVisHelp;
import org.jevis.jeconfig.plugin.TablePlugin;
import org.jevis.jeconfig.plugin.dtrc.OutputView;
import org.jevis.jeconfig.plugin.dtrc.TemplateHandler;
import org.jevis.jeconfig.plugin.meters.RegisterTableRow;
import org.jevis.jeconfig.plugin.object.attribute.AttributeEditor;
import org.jevis.jeconfig.plugin.object.attribute.PeriodEditor;
import org.jevis.jeconfig.plugin.object.extension.GenericAttributeExtension;
import org.joda.time.DateTime;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;

import static org.jevis.jeconfig.plugin.dtrc.TRCPlugin.TEMPLATE_CLASS;

public class AccountingPlugin extends TablePlugin implements Plugin {
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
    private final ObjectMapper mapper = new ObjectMapper();
    private final ToggleButton newButton = new ToggleButton("", JEConfig.getImage("list-add.png", toolBarIconSize, toolBarIconSize));
    private final ToggleButton reload = new ToggleButton("", JEConfig.getImage("1403018303_Refresh.png", toolBarIconSize, toolBarIconSize));
    private final ToggleButton save = new ToggleButton("", JEConfig.getImage("save.gif", toolBarIconSize, toolBarIconSize));
    private final ToggleButton delete = new ToggleButton("", JEConfig.getImage("if_trash_(delete)_16x16_10030.gif", toolBarIconSize, toolBarIconSize));
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
        viewTab.getViewInputs().setOrientation(Orientation.VERTICAL);

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


    private boolean isMultiSite() {

        try {
            JEVisClass equipmentRegisterClass = ds.getJEVisClass(ACCOUNTING_CLASS);
            List<JEVisObject> objects = ds.getObjects(equipmentRegisterClass, false);

            List<JEVisObject> buildingParents = new ArrayList<>();
            for (JEVisObject jeVisObject : objects) {
                JEVisObject buildingParent = objectRelations.getBuildingParent(jeVisObject);
                if (!buildingParents.contains(buildingParent)) {
                    buildingParents.add(buildingParent);

                    if (buildingParents.size() > 1) {
                        return true;
                    }
                }
            }

        } catch (Exception e) {

        }

        return false;
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

        toolBar.getItems().setAll(configComboBox, sep1, reload, sep2, save, newButton, delete, sep3, printButton);
        toolBar.getItems().addAll(JEVisHelp.getInstance().buildSpacerNode(), helpButton, infoButton);
        JEVisHelp.getInstance().addHelpItems(AccountingPlugin.class.getSimpleName(), "", JEVisHelp.LAYOUT.VERTICAL_BOT_CENTER, toolBar.getItems());
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
                        } catch (JEVisException e) {
                            logger.error("Could not save {}", attributeEditor, e);
                        }
                    }
                    try {
                        attributeEditors.clear();
                        updateGUI();
                    } catch (JEVisException e) {
                        logger.error("Error while updating GUI", e);
                    }
                }
                if (motherTabPane.getSelectionModel().getSelectedItem().equals(configTab)) {
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

            initGUI();

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
                viewTab.update();
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
                } catch (JEVisException e) {
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

        VBox esVBox = new VBox(6, energySupplierBox, separator1, esGP);
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

        VBox emoVBox = new VBox(6, energyMeteringOperatorBox, separator2, emoGP);
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

        VBox egoVBox = new VBox(6, energyGridOperatorBox, separator3, egoGP);
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

        VBox cVBox = new VBox(6, energyContractorBox, separator4, cGP);
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

        VBox gdVBox = new VBox(6, governmentalDuesBox, separator5, gdGP);
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

        if (configComboBoxSelectedItem != null) {
            configComboBox.getSelectionModel().select(configComboBoxSelectedItem);
        } else {
            configComboBox.getSelectionModel().selectFirst();
        }

        List<JEVisObject> allEnergySupplier = ds.getObjects(accountingDirectories.getEnergySupplierClass(), true);
        allEnergySupplier.sort((o1, o2) -> alphanumComparator.compare(o1.getName(), o2.getName()));
        energySupplierBox.getItems().addAll(allEnergySupplier);

        if (energySupplierBoxSelectedItem != null) {
            energySupplierBox.getSelectionModel().select(energySupplierBoxSelectedItem);
        } else {
            energySupplierBox.getSelectionModel().selectFirst();
        }

        List<JEVisObject> allEnergyMeteringOperators = ds.getObjects(accountingDirectories.getEnergyMeteringOperatorClass(), true);
        allEnergyMeteringOperators.sort((o1, o2) -> alphanumComparator.compare(o1.getName(), o2.getName()));
        energyMeteringOperatorBox.getItems().addAll(allEnergyMeteringOperators);

        if (energyMeteringOperatorBoxSelectedItem != null) {
            energyMeteringOperatorBox.getSelectionModel().select(energyMeteringOperatorBoxSelectedItem);
        } else {
            energyMeteringOperatorBox.getSelectionModel().selectFirst();
        }

        List<JEVisObject> allEnergyGridOperators = ds.getObjects(accountingDirectories.getEnergyGridOperatorClass(), true);
        allEnergyGridOperators.sort((o1, o2) -> alphanumComparator.compare(o1.getName(), o2.getName()));
        energyGridOperatorBox.getItems().addAll(allEnergyGridOperators);

        if (energyGridOperatorBoxSelectedItem != null) {
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

        if (energyContractorBoxSelectedItem != null) {
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

        if (governmentalDuesBoxSelectedItem != null) {
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
            Platform.runLater(() -> gp.getChildren().clear());

            try {
                int column = 0;
                int row = 0;

                List<JEVisAttribute> attributes = selectedObject.getAttributes();
                attributes.sort(Comparator.comparingInt(o -> {
                    try {
                        return o.getType().getGUIPosition();
                    } catch (JEVisException e) {
                        e.printStackTrace();
                    }
                    return -1;
                }));

                for (JEVisAttribute attribute : attributes) {
                    int index = attributes.indexOf(attribute);
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

                    if (index == 2 || (index > 2 && index % 2 == 0)) {
                        column = 0;
                        row++;
                    }

                    Label typeName = new Label(I18nWS.getInstance().getTypeName(attribute.getType()));
                    VBox typeBox = new VBox(typeName);
                    typeBox.setAlignment(Pos.CENTER_LEFT);
                    VBox editorBox = new VBox();

                    if (!isContractorAttribute) {
                        AttributeEditor attributeEditor = GenericAttributeExtension.getEditor(attribute.getType(), attribute);
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
                    if (column < 2) {
                        Platform.runLater(() -> gp.add(typeBox, finalColumn, finalRow));
                    } else {
                        Platform.runLater(() -> gp.add(typeBox, finalColumn + 1, finalRow));
                    }
                    column++;

                    int finalColumn2 = column;
                    if (column < 2) {
                        Platform.runLater(() -> gp.add(editorBox, finalColumn2, finalRow));
                    } else {
                        Platform.runLater(() -> gp.add(editorBox, finalColumn2 + 1, finalRow));
                    }
                    column++;
                }

                Separator separator = new Separator(Orientation.VERTICAL);
                int finalRow = row;
                Platform.runLater(() -> gp.add(separator, 2, 0, 1, finalRow + 1));

                row++;

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
        return 6;
    }
}
