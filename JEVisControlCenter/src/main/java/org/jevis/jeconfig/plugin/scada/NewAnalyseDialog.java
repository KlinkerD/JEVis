/**
 * Copyright (C) 2014 Envidatec GmbH <info@envidatec.com>
 * <p>
 * This file is part of JEApplication.
 * <p>
 * JEApplication is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation in version 3.
 * <p>
 * JEApplication is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * JEApplication. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * JEApplication is part of the OpenJEVis project, further project information
 * are published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.jeconfig.plugin.scada;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.jevis.api.JEVisClass;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisObject;
import org.jevis.application.application.AppLocale;
import org.jevis.application.application.SaveResourceBundle;
import org.jevis.application.resource.ImageConverter;
import org.jevis.application.resource.ResourceLoader;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author fs
 */
public class NewAnalyseDialog {

    public static String ICON = "1403104602_brick_add.png";

    private JEVisObject selectedParent;
    private String createName = "No Name";
    private SaveResourceBundle rb = new SaveResourceBundle("jeapplication", AppLocale.getInstance().getLocale());
    private Response response = Response.CANCEL;
    private ObjectProperty<Response> responseProperty = new SimpleObjectProperty<>(response);

    /**
     * @param owner
     * @return
     */
    public Response show(Stage owner, JEVisDataSource ds) throws JEVisException {
        JEVisClass analysesClass = ds.getJEVisClass("Analyses Directory");
        List<JEVisObject> anaylsesDirs = ds.getObjects(analysesClass, true);
        boolean canWrite = true;

        if (anaylsesDirs.isEmpty()) {
            //Error Missing Analyse Directory
        }


        Dialog<ButtonType> dialog = new Dialog();
        dialog.setTitle(rb.getString("dialog.analyses.title"));
        dialog.setHeaderText(rb.getString("dialog.analyses.header"));
        dialog.getDialogPane().getButtonTypes().setAll();
        dialog.setGraphic(ResourceLoader.getImage(ICON, 50, 50));
        VBox root = new VBox();

        dialog.getDialogPane().setContent(root);


        GridPane gp = new GridPane();
        gp.setPadding(new Insets(10));
        gp.setHgap(10);
        gp.setVgap(5);
        int x = 0;

        Label lName = new Label(rb.getString("jevistree.dialog.new.name"));
        final TextField fName = new TextField();
        fName.setPromptText(rb.getString("jevistree.dialog.new.name.prompt"));


        Label lClass = new Label(rb.getString("dialog.analyses.saveplace"));

        ObservableList<JEVisObject> optionsParents = FXCollections.observableArrayList(anaylsesDirs);


        Callback<ListView<JEVisObject>, ListCell<JEVisObject>> cellFactory = new Callback<ListView<JEVisObject>, ListCell<JEVisObject>>() {
            @Override
            public ListCell<JEVisObject> call(ListView<JEVisObject> param) {
                final ListCell<JEVisObject> cell = new ListCell<JEVisObject>() {
                    {
                        super.setPrefWidth(260);
                    }

                    @Override
                    public void updateItem(JEVisObject item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && !empty) {
                            HBox box = new HBox(5);
                            box.setAlignment(Pos.CENTER_LEFT);
                            try {
                                ImageView icon = ImageConverter.convertToImageView(analysesClass.getIcon(), 15, 15);

                                String parentName = "";
                                try {
                                    JEVisObject parent = item.getParents().get(0);//not save
                                    parentName = parent.getName();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }


                                Label cName = new Label(parentName + "/" + item.getName());
                                cName.setTextFill(Color.BLACK);
                                box.getChildren().setAll(icon, cName);

                                //TODO: set canWrite
                            } catch (JEVisException ex) {
                                Logger.getLogger(NewAnalyseDialog.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            setGraphic(box);

                        }
                    }
                };
                return cell;
            }
        };

        final ComboBox<JEVisObject> comboBox = new ComboBox<>(optionsParents);
        comboBox.setCellFactory(cellFactory);
        comboBox.setButtonCell(cellFactory.call(null));

        comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedParent = newValue;
        });
        comboBox.getSelectionModel().selectFirst();

        comboBox.setMinWidth(250);
        comboBox.setMaxWidth(Integer.MAX_VALUE);//workaround


        gp.add(lName, 0, x);
        gp.add(fName, 1, x);


        gp.add(lClass, 0, ++x, 1, 1);
        gp.add(comboBox, 1, x, 1, 1);

        Separator sep = new Separator(Orientation.HORIZONTAL);
        sep.setMinHeight(10);

        root.getChildren().addAll(gp);
        VBox.setVgrow(gp, Priority.ALWAYS);

        if (anaylsesDirs.size() == 1) {
            comboBox.setDisable(true);
        }


        fName.onKeyPressedProperty().addListener((observable, oldValue, newValue) -> {
            createName = fName.getText();
        });

        final ButtonType ok = new ButtonType(rb.getString("jevistree.dialog.new.ok"), ButtonBar.ButtonData.FINISH);
        final ButtonType cancel = new ButtonType(rb.getString("jevistree.dialog.new.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, cancel);

        Platform.runLater(() -> fName.requestFocus());
        dialog.showAndWait()
                .ifPresent(response -> {
                    if (response.getButtonData().getTypeCode() == ButtonType.FINISH.getButtonData().getTypeCode()) {

                        createName = fName.getText();

                        NewAnalyseDialog.this.response = Response.YES;
                    } else {
                        NewAnalyseDialog.this.response = Response.CANCEL;
                    }
                });


        return response;
    }


    public String getCreateName() {
        return createName;
    }

    public JEVisObject getParent() {
        return selectedParent;
    }

    public enum Type {

        NEW, RENAME
    }

    public enum Response {

        NO, YES, CANCEL
    }


}
