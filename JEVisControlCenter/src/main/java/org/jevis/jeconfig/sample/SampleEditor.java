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
import javafx.stage.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisConstants;
import org.jevis.api.JEVisObject;
import org.jevis.api.JEVisSample;
import org.jevis.commons.i18n.I18n;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.dialog.DialogHeader;
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
    private List<JEVisSample> samples = new ArrayList<>();
    private JEVisAttribute _attribute;
    private List<JEVisObject> _dataProcessors = new ArrayList<>();
    private Response response = Response.CANCEL;
    private BooleanProperty disableEditing = new SimpleBooleanProperty(false);
    Node header = DialogHeader.getDialogHeader(ICON, I18n.getInstance().getString("attribute.editor.title"));//new Separator(Orientation.HORIZONTAL),

    private int lastDataSettings = 0;
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


        stage.setTitle(I18n.getInstance().getString("attribute.editor.title"));
        stage.initModality(Modality.NONE);
        stage.initOwner(owner);

        VBox root = new VBox();
        root.setMaxWidth(2000);

        final Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setWidth(950);
        stage.setHeight(800);
        stage.setMaxWidth(2000);
        stage.initStyle(StageStyle.UTILITY);
        stage.setResizable(false);

        Screen screen = Screen.getPrimary();
        if (screen.getBounds().getHeight() < 740) {
            stage.setWidth(screen.getBounds().getHeight());
        }



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
            System.out.println("----------1");
            List<JEVisSample> samples =  controlPane.getSamples();
            System.out.println("----------2");
            for (SampleEditorExtension extension : extensions) {
                Platform.runLater(() -> {
                    try {
                        System.out.println("----------2a");
                        extension.setDateTimeZone(controlPane.getDateTimeZone());
                        extension.setSamples(controlPane.getAttribute(),samples);
                    } catch (Exception excp) {
                        logger.error(extension);
                    }
                });
            }
            System.out.println("----------3");
            if (activExtensions != null) {
                activExtensions.update();
            }
            System.out.println("----------4");
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

        controlPane.initTimeRange(attribute.getTimestampFromLastSample().minus(Period.days(7)),attribute.getTimestampFromLastSample());

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
