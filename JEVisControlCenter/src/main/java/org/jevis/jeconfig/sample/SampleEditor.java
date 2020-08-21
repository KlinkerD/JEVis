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

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.*;
import org.jevis.commons.i18n.I18n;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.dialog.DialogHeader;
import org.jevis.jeconfig.tool.ScreenSize;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI Dialog to configure attributes and there sample.
 *
 * @author Florian Simon <florian.simon@envidatec.com>
 * @TODO: rename it to Attribute editor or something?!
 */
public class SampleEditor {
    private static final Logger logger = LogManager.getLogger(SampleEditor.class);
    public static String ICON = "1415314386_Graph.png";

    private final List<SampleEditorExtension> extensions = new ArrayList<>();
    private JEVisAttribute _attribute;
    private Response response = Response.CANCEL;
    private final BooleanProperty disableEditing = new SimpleBooleanProperty(false);
    Node header;//new Separator(Orientation.HORIZONTAL),

    private SampleEditorExtension activExtensions;


    /**
     * @param owner
     * @param attribute
     * @return
     */
    public Response show(Window owner, final JEVisAttribute attribute) {
        final Stage stage = new Stage();

        _attribute = attribute;
        try {
            _attribute.getDataSource().reloadAttribute(_attribute);
        } catch (Exception ex) {
            logger.error("Update failed", ex);
        }

//        String headerString = I18n.getInstance().getString("attribute.editor.title");
        String headerString = "";
        try {
            JEVisClass objectClass = attribute.getObject().getJEVisClass();
            if (objectClass.getName().equals("Clean Data")) {
                headerString += attribute.getObject().getParents().get(0).getName() + " / " + attribute.getObject().getName();
            } else {
                headerString += attribute.getObject().getName();
            }
        } catch (JEVisException e) {
            logger.error("Could not get class", e);
        }

        header = DialogHeader.getDialogHeader(ICON, headerString);

        stage.setTitle(I18n.getInstance().getString("attribute.editor.title"));
        stage.initModality(Modality.NONE);
        stage.initOwner(owner);

        VBox root = new VBox();
        root.setMaxWidth(2000);

        final Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setWidth(ScreenSize.fitScreenHeight(800));
        stage.setHeight(ScreenSize.fitScreenHeight(800));
        stage.setMaxWidth(ScreenSize.fitScreenHeight(1500));
        stage.initStyle(StageStyle.UTILITY);
        stage.setResizable(true);


        HBox bottomBox = new HBox();
        bottomBox.setPadding(new Insets(10));


        /** ------------------------------------------- Processor -------------------------------------------------**/


        Button config = new Button();
        config.setGraphic(JEConfig.getImage("Service Manager.png", 16, 16));


        SampleTableExtension sampleTableExtension = new SampleTableExtension(attribute, stage);
        extensions.add(sampleTableExtension);
        activExtensions = sampleTableExtension;

        /** graph makes only if the data are numbers **/
        try {
            if (attribute.getPrimitiveType() == JEVisConstants.PrimitiveType.LONG || attribute.getPrimitiveType() == JEVisConstants.PrimitiveType.DOUBLE) {
                extensions.add(new SampleGraphExtension(attribute));
            }
        } catch (Exception ex) {
            logger.error(ex);
        }

        extensions.add(new AttributeStatesExtension(attribute));
        extensions.add(new SampleExportExtension(attribute));
        extensions.add(new AttributeUnitExtension(attribute));

        final List<Tab> tabs = new ArrayList<>();

        for (SampleEditorExtension ex : extensions) {
            Tab tabEditor = new Tab();
            tabEditor.setText(ex.getTitle());
            tabEditor.setContent(ex.getView());
            tabs.add(tabEditor);
        }

        disableEditing.addListener((observable, oldValue, newValue) -> {
            extensions.forEach(sampleEditorExtension -> {
                logger.info("Disabled editing in: " + sampleEditorExtension.getTitle());
                sampleEditorExtension.disableEditing(newValue);
            });
        });


        ControlPane controlPane = new ControlPane(_attribute);


        final TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabPane.getTabs().addAll(tabs);


        root.getChildren().addAll(header, tabPane, new Separator(Orientation.HORIZONTAL), controlPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        VBox.setVgrow(controlPane, Priority.NEVER);
        VBox.setVgrow(header, Priority.NEVER);

        controlPane.setOnOK(t -> {
            stage.close();
            for (SampleEditorExtension ex : extensions) {
                ex.sendOKAction();
            }
        });

        controlPane.setOnCancel(t -> {
            stage.close();
            response = Response.CANCEL;
            stage.close();

        });

        controlPane.setOnTimeRangeChange(event -> {
            List<JEVisSample> samples = controlPane.getSamples();
            for (SampleEditorExtension extension : extensions) {
                Platform.runLater(() -> {
                    try {
                        extension.setDateTimeZone(controlPane.getDateTimeZone());
                        extension.setSamples(controlPane.getAttribute(), samples);
                    } catch (Exception excp) {
                        logger.error(extension);
                    }
                });
            }
            if (activExtensions != null) {
                activExtensions.update();
            }
        });


        tabPane.getSelectionModel().selectedItemProperty().addListener((ov, t, t1) -> {
            for (SampleEditorExtension ex : extensions) {
                if (ex.getTitle().equals(t1.getText())) {
                    logger.info("Tab changed: " + ex.getClass());
                    activExtensions = ex;
                    ex.update();
                }
            }
        });

        if (attribute.hasSample()) {

            DateTime start = attribute.getTimestampFromLastSample().minus(Period.days(7));
            DateTime end = attribute.getTimestampFromLastSample();

            Period displaySampleRate = attribute.getDisplaySampleRate();

            if (displaySampleRate.equals(Period.years(1))) {
                start = attribute.getTimestampFromLastSample().minusYears(10);
            } else if (displaySampleRate.equals(Period.months(1))) {
                start = attribute.getTimestampFromLastSample().minusMonths(12);
            } else if (displaySampleRate.equals(Period.weeks(1))) {
                start = attribute.getTimestampFromLastSample().minusWeeks(10);
            } else if (displaySampleRate.equals(Period.days(1))) {
                start = attribute.getTimestampFromLastSample().minusDays(14);
            } else if (displaySampleRate.equals(Period.hours(1))) {
                start = attribute.getTimestampFromLastSample().minusDays(2);
            } else if (displaySampleRate.equals(Period.minutes(15))) {
                start = attribute.getTimestampFromLastSample().minusHours(24);
            } else if (displaySampleRate.equals(Period.minutes(1))) {
                start = attribute.getTimestampFromLastSample().minusHours(6);
            }

            controlPane.initTimeRange(start, end);
        }

        stage.showAndWait();

        return response;
    }

    private void disableEditing(boolean disable) {
        disableEditing.setValue(true);
    }


    public enum Response {

        YES, CANCEL
    }
}
