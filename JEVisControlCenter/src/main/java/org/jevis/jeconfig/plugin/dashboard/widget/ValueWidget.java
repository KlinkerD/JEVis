package org.jevis.jeconfig.plugin.dashboard.widget;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.AtomicDouble;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisObject;
import org.jevis.api.JEVisSample;
import org.jevis.commons.calculation.CalcInputObject;
import org.jevis.commons.calculation.CalcJob;
import org.jevis.commons.calculation.CalcJobFactory;
import org.jevis.commons.database.SampleHandler;
import org.jevis.commons.i18n.I18n;
import org.jevis.commons.unit.UnitManager;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.TopMenu;
import org.jevis.jeconfig.application.Chart.data.ChartDataRow;
import org.jevis.jeconfig.application.jevistree.methods.CommonMethods;
import org.jevis.jeconfig.application.tools.ColorHelper;
import org.jevis.jeconfig.plugin.dashboard.DashboardControl;
import org.jevis.jeconfig.plugin.dashboard.config.WidgetConfig;
import org.jevis.jeconfig.plugin.dashboard.config2.*;
import org.jevis.jeconfig.plugin.dashboard.datahandler.DataModelDataHandler;
import org.jevis.jeconfig.plugin.dashboard.datahandler.DataModelWidget;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ValueWidget extends Widget implements DataModelWidget {

    private static final Logger logger = LogManager.getLogger(ValueWidget.class);
    public static String WIDGET_ID = "Value";
    private final Label label = new Label();
    private final NumberFormat nf = NumberFormat.getInstance();
    private DataModelDataHandler sampleHandler;
    private final DoubleProperty displayedSample = new SimpleDoubleProperty(Double.NaN);
    private Limit limit;
    public static String PERCENT_NODE_NAME = "percent";
    private Interval lastInterval = null;


    public static String LIMIT_NODE_NAME = "limit";
    private Percent percent;

    public ValueWidget(DashboardControl control, WidgetPojo config) {
        super(control, config);
        setId(WIDGET_ID);
    }

    public ValueWidget(DashboardControl control) {
        super(control);
    }

    @Override
    public WidgetPojo createDefaultConfig() {
        WidgetPojo widgetPojo = new WidgetPojo();
        widgetPojo.setTitle(I18n.getInstance().getString("plugin.dashboard.valuewidget.newname"));
        widgetPojo.setType(typeID());
        widgetPojo.setSize(new Size(control.getActiveDashboard().yGridInterval * 1, control.getActiveDashboard().xGridInterval * 4));


        return widgetPojo;
    }


    @Override
    public void updateData(Interval interval) {
        logger.debug("Value.Update: {}", interval);
        lastInterval = interval;
        Platform.runLater(() -> {
            showAlertOverview(false, "");
        });

        if (sampleHandler == null) {
            return;
        } else {
            showProgressIndicator(true);
        }


        Platform.runLater(() -> {
            this.label.setText(I18n.getInstance().getString("plugin.dashboard.loading"));
        });

        String widgetUUID = "-1";

        this.nf.setMinimumFractionDigits(this.config.getDecimals());
        this.nf.setMaximumFractionDigits(this.config.getDecimals());

        AtomicDouble total = new AtomicDouble(Double.MIN_VALUE);
        try {
            widgetUUID = getConfig().getUuid() + "";
            this.sampleHandler.setAutoAggregation(true);
            this.sampleHandler.setInterval(interval);
            this.sampleHandler.update();
            if (!this.sampleHandler.getDataModel().isEmpty()) {
                ChartDataRow dataModel = this.sampleHandler.getDataModel().get(0);
                List<JEVisSample> results;
                boolean isQuantity = true;

                String unit = dataModel.getUnitLabel();

                results = dataModel.getSamples();
                if (!results.isEmpty()) {
                    total.set(DataModelDataHandler.getManipulatedData(this.sampleHandler.getDateNode(), results, dataModel));

                    displayedSample.setValue(total.get());
                    String valueText = null;
                    if (percent != null && percent.getPercentWidgetID() > 0) {
                        for (Widget sourceWidget : ValueWidget.this.control.getWidgets()) {
                            if (sourceWidget.getConfig().getUuid() == (percent.getPercentWidgetID())) {
                                Double reference = ((ValueWidget) sourceWidget).displayedSample.get();
                                Double value = this.displayedSample.get();
                                Double result = value / reference * 100;
                                if (!result.isNaN()) {
                                    if (value >= 0.01) {
                                        valueText = this.nf.format(value) + " " + unit + " (" + this.nf.format(result) + "%)";
                                    } else {
                                        valueText = this.nf.format(value) + " " + unit + " (<" + this.nf.format(0.01) + "%)";
                                    }
                                } else {
                                    valueText = this.nf.format(total.get()) + " " + unit;
                                }
                                break;
                            }
                        }
                    } else {
                        valueText = this.nf.format(total.get()) + " " + unit;
                    }

                    String finalValueText = valueText;
                    Platform.runLater(() -> this.label.setText(finalValueText));
                    checkLimit();
                } else {
                    Platform.runLater(() -> {
                        this.label.setText("-");
                    });

                    displayedSample.set(Double.NaN);//or NaN?
                }

            } else {
                Platform.runLater(() -> {
                    this.label.setText("");
                });
                displayedSample.set(Double.NaN);
                logger.warn("ValueWidget is missing SampleHandler.datamodel: [ID:{}]", widgetUUID);
            }

        } catch (Exception ex) {
            logger.error("Error while updating ValueWidget: [ID:{}]:{}", widgetUUID, ex);
            Platform.runLater(() -> {
                this.label.setText("error");
                showAlertOverview(true, ex.getMessage());
            });
        }

//        showProgressIndicator(false);

        Platform.runLater(() -> {
            showProgressIndicator(false);
        });


    }


    @Override
    public DataModelDataHandler getDataHandler() {
        return this.sampleHandler;
    }

    @Override
    public void setDataHandler(DataModelDataHandler dataHandler) {
        this.sampleHandler = dataHandler;
    }

    private void checkLimit() {
        Platform.runLater(() -> {
            try {
                logger.debug("checkLimit: {}", config.getUuid());
//                this.label.setText(this.labelText.getValue());
                Color fontColor = this.config.getFontColor();
                this.label.setFont(new Font(this.config.getFontSize()));

                if (limit != null) {
//                    this.label.setTextFill(limit.getExceedsLimitColor(fontColor, displayedSample.get()));
                    this.label.setStyle("-fx-text-fill: " + ColorHelper.toRGBCode(limit.getExceedsLimitColor(fontColor, displayedSample.get())) + " !important;");
                } else {
//                    this.label.setTextFill(fontColor);
                    this.label.setStyle("-fx-text-fill: " + ColorHelper.toRGBCode(fontColor) + " !important;");
                }


            } catch (Exception ex) {
                logger.error(ex);
            }
        });
    }

    @Override
    public void debug() {
        this.sampleHandler.debug();
    }

    @Override
    public void updateLayout() {

    }

    @Override
    public void openConfig() {
//        System.out.println("The Thread name is0 " + Thread.currentThread().getName());
        WidgetConfigDialog widgetConfigDialog = new WidgetConfigDialog(this);
        widgetConfigDialog.addGeneralTabsDataModel(this.sampleHandler);
        sampleHandler.setAutoAggregation(true);

        logger.error("Value.openConfig() [{}] limit ={}", config.getUuid(), limit);
        if (limit != null) {
            widgetConfigDialog.addTab(limit.getConfigTab());
        }

        if (percent != null) {
            widgetConfigDialog.addTab(percent.getConfigTab());
        }

        widgetConfigDialog.requestFirstTabFocus();

        Optional<ButtonType> result = widgetConfigDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                widgetConfigDialog.commitSettings();
                control.updateWidget(this);
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }


    @Override
    public void updateConfig() {
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                Background bgColor = new Background(new BackgroundFill(this.config.getBackgroundColor(), CornerRadii.EMPTY, Insets.EMPTY));
                this.label.setBackground(bgColor);
                this.label.setTextFill(this.config.getFontColor());
                this.label.setContentDisplay(ContentDisplay.CENTER);
                this.label.setAlignment(this.config.getTitlePosition());
            });
        });


        try {
            if (limit != null && limit.getLimitWidgetID() > 0) {
                for (Widget sourceWidget : ValueWidget.this.control.getWidgets()) {
                    if (sourceWidget.getConfig().getUuid() == (limit.getLimitWidgetID())) {
                        ((ValueWidget) sourceWidget).getDisplayedSampleProperty().addListener((observable, oldValue, newValue) -> {
                            limit.setLowerLimitDynamic(newValue.doubleValue());
                            limit.setUpperLimitDynamic(newValue.doubleValue());
                            checkLimit();
                        });
                        break;
                    }

                }
                if (percent != null && percent.getPercentWidgetID() > 0) {
                    String unit = sampleHandler.getDataModel().get(0).getUnitLabel();
                    for (Widget sourceWidget : ValueWidget.this.control.getWidgets()) {
                        if (sourceWidget.getConfig().getUuid() == (percent.getPercentWidgetID())) {
                            ((ValueWidget) sourceWidget).getDisplayedSampleProperty().addListener((observable, oldValue, newValue) -> {
                                Double reference = ((ValueWidget) sourceWidget).displayedSample.get();
                                Double value = this.displayedSample.get();
                                Double result = value / reference * 100;
                                if (!result.isNaN()) {
                                    if (result >= 0.01) {
                                        this.label.setText(this.nf.format(result) + " " + unit + " (" + this.nf.format(result) + "%)");
                                    } else {
                                        this.label.setText(this.nf.format(value) + " " + unit + " (<" + this.nf.format(0.01) + "%)");
                                    }
                                } else {
                                    this.label.setText(this.nf.format(value) + " " + unit);
                                }
                            });
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error while update config: {}|{}", ex.getStackTrace()[0].getLineNumber(), ex);
        }
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public List<DateTime> getMaxTimeStamps() {
        if (sampleHandler != null) {
            return sampleHandler.getMaxTimeStamps();
        } else {
            return new ArrayList<>();
        }
    }


    @Override
    public void init() {
        logger.debug("init Value Widget: " + getConfig().getUuid());

        this.sampleHandler = new DataModelDataHandler(getDataSource(), this.config.getConfigNode(WidgetConfig.DATA_HANDLER_NODE));
        this.sampleHandler.setMultiSelect(false);

        logger.debug("Value.init() [{}] {}", config.getUuid(), this.config.getConfigNode(LIMIT_NODE_NAME));
        try {
            this.limit = new Limit(this.control, this.config.getConfigNode(LIMIT_NODE_NAME));
        } catch (Exception ex) {
            logger.error(ex);
            ex.printStackTrace();
        }
        if (limit == null) {
            logger.error("Limit is null make new: " + config.getUuid());
            this.limit = new Limit(this.control);
        }

        try {
            this.percent = new Percent(this.control, this.config.getConfigNode(PERCENT_NODE_NAME));
        } catch (Exception ex) {
            logger.error(ex);
            ex.printStackTrace();
        }
        if (percent == null) {
            logger.error("Percent is null make new: " + config.getUuid());
            this.percent = new Percent(this.control);
        }

        this.label.setPadding(new Insets(0, 8, 0, 8));
        setGraphic(this.label);


        setOnMouseClicked(event -> {
            if (!control.editableProperty.get() && event.getButton().equals(MouseButton.PRIMARY)
                    && event.getClickCount() == 1 && !event.isShiftDown()) {
                int row = 0;

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                GridPane gp = new GridPane();
                gp.setHgap(4);
                gp.setVgap(8);

                for (ChartDataRow chartDataRow : sampleHandler.getDataModel()) {
                    if (chartDataRow.getEnPI()) {
                        try {
                            CalcJobFactory calcJobCreator = new CalcJobFactory();

                            CalcJob calcJob = calcJobCreator.getCalcJobForTimeFrame(new SampleHandler(), chartDataRow.getObject().getDataSource(), chartDataRow.getCalculationObject(),
                                    this.getDataHandler().getDurationProperty().getStart(), this.getDataHandler().getDurationProperty().getEnd(), true);
                            alert.setHeaderText(getTranslatedFormula(calcJob.getCalcInputObjects(), calcJob.getExpression()));

                            for (CalcInputObject calcInputObject : calcJob.getCalcInputObjects()) {

                                Label objectName = new Label();
                                if (calcInputObject.getValueAttribute().getObject().getJEVisClassName().equals("Clean Data")) {
                                    JEVisObject parent = CommonMethods.getFirstParentalDataObject(calcInputObject.getValueAttribute().getObject());
                                    if (parent != null) {
                                        objectName.setText(parent.getName());
                                    }
                                } else if (calcInputObject.getValueAttribute().getObject().getJEVisClassName().equals("Data")) {
                                    objectName.setText(calcInputObject.getValueAttribute().getObject().getName());
                                }

                                JFXTextField value = new JFXTextField(calcInputObject.getSamples().get(0).getValueAsString() + " " +
                                        UnitManager.getInstance().format(calcInputObject.getValueAttribute().getDisplayUnit()));

                                gp.addRow(row, objectName, value);
                                row++;
                            }
                        } catch (Exception e) {
                            logger.error("Error while loading calculation", e);
                        }
                    }

                }
                if (!gp.getChildren().isEmpty()) {
                    alert.getDialogPane().setContent(gp);
                    TopMenu.applyActiveTheme(alert.getDialogPane().getScene());
                    alert.showAndWait();
                }

            } else if (!control.editableProperty.get() && event.getButton().equals(MouseButton.PRIMARY)
                    && event.getClickCount() == 1 && event.isShiftDown()) {
                debug();
            }
        });
    }

    public String getTranslatedFormula(List<CalcInputObject> calcInputObjects, String expression) {
        try {
            for (CalcInputObject calcInputObject : calcInputObjects) {
                String name = "";
                if (calcInputObject.getValueAttribute().getObject().getJEVisClassName().equals("Clean Data")) {
                    JEVisObject parent = CommonMethods.getFirstParentalDataObject(calcInputObject.getValueAttribute().getObject());
                    if (parent != null) {
                        name = parent.getName();
                    }
                } else if (calcInputObject.getValueAttribute().getObject().getJEVisClassName().equals("Data")) {
                    name = calcInputObject.getValueAttribute().getObject().getName();
                }

                if (!name.equals("")) {
                    expression = expression.replace(calcInputObject.getIdentifier(), name);
                }
            }

            expression = expression.replace("#", "");
            expression = expression.replace("{", "");
            expression = expression.replace("}", "");
        } catch (Exception e) {

        }

        return expression;
    }


    @Override
    public String typeID() {
        return WIDGET_ID;
    }

    @Override
    public ObjectNode toNode() {

        ObjectNode dashBoardNode = super.createDefaultNode();
        dashBoardNode
                .set(JsonNames.Widget.DATA_HANDLER_NODE, this.sampleHandler.toJsonNode());


        if (limit != null) {
            dashBoardNode
                    .set(LIMIT_NODE_NAME, limit.toJSON());
        }

        if (percent != null) {
            dashBoardNode
                    .set(PERCENT_NODE_NAME, percent.toJSON());
        }


        return dashBoardNode;
    }

    @Override
    public ImageView getImagePreview() {
        return JEConfig.getImage("widget/ValueWidget.png", this.previewSize.getHeight(), this.previewSize.getWidth());
    }


    public DoubleProperty getDisplayedSampleProperty() {
        return displayedSample;
    }


}
