/**
 * Copyright (C) 2014 Envidatec GmbH <info@envidatec.com>
 * <p>
 * This file is part of JEConfig.
 * <p>
 * JEConfig is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation in version 3.
 * <p>
 * JEConfig is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * JEConfig. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * JEConfig is part of the OpenJEVis project, further project information are
 * published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.jeconfig.sample;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisConstants;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisSample;
import org.jevis.commons.dataprocessing.CleanDataObject;
import org.jevis.commons.i18n.I18n;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.application.Chart.ChartPluginElements.DataPointNoteDialog;
import org.jevis.jeconfig.dialog.ConfirmDialog;
import org.jevis.jeconfig.dialog.ProgressForm;
import org.jevis.jeconfig.sample.tableview.SampleTable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Florian Simon <florian.simon@envidatec.com>
 */
public class SampleTableExtension implements SampleEditorExtension {
    private static final Logger logger = LogManager.getLogger(SampleTableExtension.class);

    private final static String TITLE = "Editor";
    private final BorderPane borderPane = new BorderPane();
    private final StackPane dialogContainer = new StackPane(borderPane);
    private final Window owner;
    private DateTimeZone dateTimeZone;
    private JEVisAttribute _att;
    private List<JEVisSample> _samples = new ArrayList<>();
    private boolean _dataChanged = true;
    private final BooleanProperty disableEditing = new SimpleBooleanProperty(false);

    public SampleTableExtension(JEVisAttribute att, Window owner) {
        _att = att;
        this.owner = owner;
        buildGui(att, new ArrayList<>());
    }

    private void buildGui(final JEVisAttribute att, final List<JEVisSample> samples) {
        HBox deleteBox = new HBox(10);
        deleteBox.setAlignment(Pos.CENTER);

        HBox userBox = new HBox(10);
        userBox.setAlignment(Pos.CENTER);

        VBox motherBox = new VBox();
        motherBox.setAlignment(Pos.CENTER);

        final SampleTable table = new SampleTable(att, dateTimeZone, samples);
        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        boolean canDelete = false;
        boolean canWrite = false;
        JFXButton deleteAll = new JFXButton(I18n.getInstance().getString("sampleeditor.confirmationdialog.deleteall.titlelong"));

        try {
            canDelete = att.getObject().getDataSource().getCurrentUser().canDelete(att.getObject().getID());
            canWrite = att.getObject().getDataSource().getCurrentUser().canWrite(att.getObject().getID());
        } catch (Exception ex) {
            logger.error(ex);
        }
        deleteAll.setDisable(!canDelete);

        deleteAll.setOnAction(event -> {
            try {
                ConfirmDialog dia = new ConfirmDialog();
                if (dia.show(this.owner, I18n.getInstance().getString("sampleeditor.confirmationdialog.deleteall.title"),
                        I18n.getInstance().getString("sampleeditor.confirmationdialog.deleteall.titlelong"),
                        I18n.getInstance().getString("sampleeditor.confirmationdialog.deleteall.message")) == ConfirmDialog.Response.YES) {

                    taskWithAnimation(new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            att.deleteAllSample();
                            setSamples(att, att.getAllSamples());
                            update();
                            return null;
                        }
                    });

                    logger.info("Deleted all Samples of Attribute " + att.getName() +
                            " of Object " + att.getObject().getName() + " of ID " + att.getObject().getID());
                }
            } catch (Exception ex) {
                logger.fatal(ex);
            }
        });

        JFXButton deleteSelected = new JFXButton(I18n.getInstance().getString("sampleeditor.confirmationdialog.deleteselected.titlelong"));
        deleteSelected.setDisable(!table.deleteSelectedProperty().getValue());
        deleteSelected.disableProperty().bind(table.deleteSelectedProperty().not());

        deleteSelected.setOnAction(event -> {
                    try {

                        ConfirmDialog dia = new ConfirmDialog();

                        if (dia.show(this.owner, I18n.getInstance().getString("sampleeditor.confirmationdialog.deleteselected.title"),
                                I18n.getInstance().getString("sampleeditor.confirmationdialog.deleteselected.titlelong"),
                                I18n.getInstance().getString("sampleeditor.confirmationdialog.deleteselected.message")) == ConfirmDialog.Response.YES) {
                            taskWithAnimation(new Task<Void>() {
                                @Override
                                protected Void call() throws Exception {
                                    table.deleteSelectedData();
                                    update();
                                    return null;
                                }
                            });


                        }

                    } catch (Exception ex) {
                        logger.fatal(ex);
                    }

                }
        );
        JFXButton saveButton = new JFXButton(I18n.getInstance().getString("sampleeditor.confirmationdialog.save"));
        saveButton.disableProperty().bind(table.needSaveProperty().not());
        saveButton.setOnAction(event -> {
            table.commitChanges();
            update();
        });
        saveButton.setDefaultButton(true);

        JFXButton addNewSample = new JFXButton(null, JEConfig.getImage("list-add.png", 17, 17));
        addNewSample.setDisable(!canWrite);

        addNewSample.setOnAction(event -> {
            /** TODO: implement missing PrimitiveTypes **/
            try {
                Object value;
                switch (att.getPrimitiveType()) {
                    case JEVisConstants.PrimitiveType.DOUBLE:
                        value = 1.0d;
                        break;
                    case JEVisConstants.PrimitiveType.LONG:
                        value = 1L;
                        break;
                    case JEVisConstants.PrimitiveType.BOOLEAN:
                        value = true;
                        break;
                    default:
                        value = "1";
                        break;
                }
                table.addNewSample(new DateTime().withField(DateTimeFieldType.millisOfSecond(), 0), value, "Manual Sample");
            } catch (Exception ex) {
                logger.error(ex);
            }
        });

        JFXButton deleteInBetween = new JFXButton(I18n.getInstance().getString("sampleeditor.confirmationdialog.deleteinbetween.titlelong"));
        deleteInBetween.setDisable(table.deleteInBetweenProperty().getValue());
        deleteInBetween.disableProperty().bind(table.deleteInBetweenProperty().not());

        deleteInBetween.setOnAction(event -> {
                    try {

                        ConfirmDialog dia = new ConfirmDialog();

                        DateTime[] minMax = table.findSelectedMinMaxDate();
                        DateTime firstDate = minMax[0];
                        DateTime endDate = minMax[1];
                        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
                        String message = String.format(I18n.getInstance().getString("sampleeditor.confirmationdialog.deleteinbetween.message1"), fmt.print(firstDate), fmt.print(endDate));

                        if (dia.show(this.owner, I18n.getInstance().getString("sampleeditor.confirmationdialog.deleteinbetween.title"),
                                I18n.getInstance().getString("sampleeditor.confirmationdialog.deleteinbetween.titlelong"),
                                message) == ConfirmDialog.Response.YES) {

                            taskWithAnimation(new Task<Void>() {
                                @Override
                                protected Void call() throws Exception {
                                    table.deleteInBetween();
                                    update();
                                    return null;
                                }
                            });
                        }

                    } catch (Exception ex) {
                        logger.fatal(ex);
                    }

                }
        );

        deleteBox.getChildren().setAll(addNewSample, deleteAll, deleteSelected, deleteInBetween, saveButton);

        JFXButton addNewUserValue = new JFXButton(I18n.getInstance().getString("sampleeditor.confirmationdialog.addnewuservalue.titlelong"));
        addNewUserValue.setDisable(!table.deleteSelectedProperty().getValue());
        addNewUserValue.disableProperty().bind(table.deleteSelectedProperty().not());

        addNewUserValue.setOnAction(event -> {
            try {
                if (att.getPrimitiveType() == JEVisConstants.PrimitiveType.DOUBLE) {
                    new DataPointNoteDialog(att, table);
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        });

        JFXButton addUserValuesInBetween = new JFXButton(I18n.getInstance().getString("sampleeditor.confirmationdialog.adduservalueinbetwen.titlelong"));
        addUserValuesInBetween.setDisable(table.deleteInBetweenProperty().getValue());
        addUserValuesInBetween.disableProperty().bind(table.deleteInBetweenProperty().not());

        addUserValuesInBetween.setOnAction(event -> {
            DateTime[] minMax = table.findSelectedMinMaxDate();
            DateTime firstDate = minMax[0];
            DateTime endDate = minMax[1];
            try {
                new DataPointNoteDialog(att, minMax);
            } catch (Exception ex) {
                logger.error(ex);
            }
        });

        JFXButton addValuesInBetween = new JFXButton(I18n.getInstance().getString("sampleeditor.confirmationdialog.addvalueinbetwen.titlelong"));
        addValuesInBetween.setDisable(table.deleteInBetweenProperty().getValue());
        addValuesInBetween.disableProperty().bind(table.deleteInBetweenProperty().not());

        addValuesInBetween.setOnAction(event -> {
            DateTime[] minMax = table.findSelectedMinMaxDate();
            DateTime firstDate = minMax[0];
            DateTime endDate = minMax[1];
            try {
                JFXDialog dialog = new JFXDialog();
                dialog.setDialogContainer(dialogContainer);
                dialog.setTransitionType(JFXDialog.DialogTransition.NONE);
                dialog.setOverlayClose(false);

                Label valueLabel = new Label("Wert:");
                JFXTextField valueField = new JFXTextField();

                Label noteLabel = new Label("Notiz:");
                JFXTextField noteField = new JFXTextField();

                GridPane gridPane = new GridPane();
                gridPane.setPadding(new Insets(10));
                gridPane.setHgap(10);
                gridPane.setVgap(5);

                int row = 0;
                gridPane.add(valueLabel, 0, row);
                gridPane.add(valueField, 1, row);
                row++;

                gridPane.add(noteLabel, 0, row);
                gridPane.add(noteField, 1, row);

                final JFXButton ok = new JFXButton(I18n.getInstance().getString("newobject.ok"));
                ok.setDefaultButton(true);
                final JFXButton cancel = new JFXButton(I18n.getInstance().getString("newobject.cancel"));
                cancel.setCancelButton(true);

                HBox buttonBar = new HBox(6, cancel, ok);
                buttonBar.setAlignment(Pos.CENTER_RIGHT);
                buttonBar.setPadding(new Insets(12));

                Separator separator = new Separator(Orientation.HORIZONTAL);
                separator.setPadding(new Insets(8, 0, 8, 0));

                VBox vBox = new VBox(6, gridPane, separator, buttonBar);
                dialog.setContent(vBox);

                ok.setOnAction(evt -> {
                    try {
                        BigDecimal d = new BigDecimal(valueField.getText());


                        final ProgressForm pForm = new ProgressForm("Setting values...");

                        Task<List<JEVisSample>> set = new Task<List<JEVisSample>>() {
                            @Override
                            protected List<JEVisSample> call() throws JEVisException {
                                List<JEVisSample> sampleList = new ArrayList<>();
                                Period periodForDate = CleanDataObject.getPeriodForDate(att.getObject(), minMax[0]);
                                DateTime date = minMax[0].plus(periodForDate);

                                while (date.isBefore(minMax[1])) {
                                    sampleList.add(att.buildSample(date, d, noteField.getText()));
                                    pForm.addMessage("Created sample " + date + " - " + d + " - " + noteField.getText());

                                    CleanDataObject.getPeriodForDate(att.getObject(), date);
                                    date = date.plus(periodForDate);
                                }

                                return sampleList;
                            }
                        };
                        set.setOnSucceeded(task -> {
                            try {
                                pForm.addMessage("Adding " + set.getValue().size() + " samples.");
                                att.addSamples(set.getValue());
                                pForm.addMessage("Finished importing samples.");
                            } catch (JEVisException e) {
                                e.printStackTrace();
                            }
                            pForm.getDialogStage().close();
                        });

                        set.setOnCancelled(task -> {
                            logger.debug("Creating samples cancelled");
                            pForm.getDialogStage().hide();
                        });

                        set.setOnFailed(task -> {
                            logger.debug("Creating samples failed");
                            pForm.getDialogStage().hide();
                        });

                        pForm.activateProgressBar(set);
                        pForm.getDialogStage().show();

                        new Thread(set).start();

                    } catch (Exception e) {
                        logger.error("Could not write value config to JEVis System: " + e);
                    }
                    dialog.close();
                });

                cancel.setOnAction(e -> dialog.close());

                dialog.show();

            } catch (Exception ex) {
                logger.error(ex);
            }
        });

        userBox.getChildren().setAll(addNewUserValue, addUserValuesInBetween, addValuesInBetween);

        borderPane.setPadding(new Insets(10, 0, 10, 0));
        deleteBox.setPadding(new Insets(10, 0, 10, 0));
        userBox.setPadding(new Insets(10, 0, 10, 0));

        try {
            if (att.getObject().getJEVisClassName().equals("Data") || att.getObject().getJEVisClassName().equals("Clean Data")) {
                motherBox.getChildren().setAll(deleteBox, userBox);
            } else {
                motherBox.getChildren().setAll(deleteBox);
            }
        } catch (JEVisException e) {
            e.printStackTrace();
        }

        borderPane.setCenter(table);
        borderPane.setBottom(motherBox);
    }

    public void taskWithAnimation(Task<Void> task) {

        final ProgressForm pForm = new ProgressForm(I18n.getInstance().getString("plugin.object.waitsave"));

        task.setOnSucceeded(event -> pForm.getDialogStage().close());

        task.setOnCancelled(event -> {
            logger.error(I18n.getInstance().getString("plugin.object.waitsave.canceled"));
            pForm.getDialogStage().hide();
        });

        task.setOnFailed(event -> {
            logger.error(I18n.getInstance().getString("plugin.object.waitsave.failed"));
            pForm.getDialogStage().hide();
        });

        pForm.activateProgressBar(task);
        pForm.getDialogStage().show();

        new Thread(task).start();

    }

    @Override
    public boolean isForAttribute(JEVisAttribute obj) {
        return true;
    }

    @Override
    public Node getView() {
        return dialogContainer;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void setSamples(final JEVisAttribute att, final List<JEVisSample> samples) {
        _samples = samples;
        _att = att;
        _dataChanged = true;
    }

    @Override
    public void setDateTimeZone(DateTimeZone dateTimeZone) {
        this.dateTimeZone = dateTimeZone;
    }

    @Override
    public void disableEditing(boolean disable) {
        disableEditing.setValue(disable);

    }

    @Override
    public void update() {
        Platform.runLater(() -> {
            if (_dataChanged) {
                buildGui(_att, _samples);
                _dataChanged = false;
            }
        });
    }

    @Override
    public boolean sendOKAction() {
        return false;
    }

}
