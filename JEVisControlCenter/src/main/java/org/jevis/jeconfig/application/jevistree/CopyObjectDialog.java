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
package org.jevis.jeconfig.application.jevistree;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisObject;
import org.jevis.commons.i18n.I18n;
import org.jevis.jeconfig.application.resource.ResourceLoader;
import org.jevis.jeconfig.application.tools.NumberSpinner;

import java.math.BigDecimal;

/**
 * Dialog to prompt the user about the copy/clone/move action
 *
 * @author fs
 */
public class CopyObjectDialog {
    private static final Logger logger = LogManager.getLogger(CopyObjectDialog.class);

    public static String ICON = "1403555565_stock_folder-move.png";

    private TextField nameField = new TextField();
    private boolean recursionAllowed = false;
    private boolean includeDataAllowed = true;
    private final CheckBox includeValues = new CheckBox(I18n.getInstance().getString("jevistree.dialog.copy.addvalues"));
    private final Button ok = new Button(I18n.getInstance().getString("jevistree.dialog.copy.ok"));

    private final RadioButton move = new RadioButton(I18n.getInstance().getString("jevistree.dialog.copy.move"));
    private final RadioButton link = new RadioButton(I18n.getInstance().getString("jevistree.dialog.copy.link"));
    private final RadioButton copy = new RadioButton(I18n.getInstance().getString("jevistree.dialog.copy.copy"));
    private final CheckBox recursion = new CheckBox(I18n.getInstance().getString("jevistree.dialog.copy.substructure"));
    private final CheckBox includeSamples = new CheckBox(I18n.getInstance().getString("jevistree.dialog.copy.adddata"));
    private boolean includeValuesAllowed = true;
    private final NumberSpinner count = new NumberSpinner(BigDecimal.valueOf(1), BigDecimal.valueOf(1));

    /**
     * @param owner
     * @param object
     * @param newParent
     * @return
     */
    public Response show(Stage owner, final JEVisObject object, final JEVisObject newParent, DefaultAction defaultAction) {

//        boolean linkOK = false;
        try {

            if (!object.getDataSource().getCurrentUser().canCreate(object.getID())) {
                showError(I18n.getInstance().getString("jevistree.dialog.copy.permission.denied"), I18n.getInstance().getString("jevistree.dialog.copy.permission.denied.message"));
                return Response.CANCEL;
            }

//            if (newParent.getJEVisClassName().equals("Link")
//                    || newParent.getJEVisClassName().equals("View Directory")) {
//                linkOK = true;
//            }
            /** links are now ok everywhere **/
//            linkOK = true;


            if (!object.getJEVisClassName().equals("Link") && !object.isAllowedUnder(newParent)) {
                showError(I18n.getInstance().getString("jevistree.dialog.copy.rules.error"),
                        String.format(I18n.getInstance().getString("jevistree.dialog.copy.rules.error.message"), object.getJEVisClass().getName(),
                                newParent.getJEVisClass().getName()));
                return Response.CANCEL;
            }
            //Don't allow recursion if the process failed the recursion check
            this.recursionAllowed = !TreeHelper.isOwnParentCheck(object, newParent);
            if (!recursionAllowed) {
                this.recursion.setSelected(false);
            }

            this.recursion.setDisable(!this.recursionAllowed);
            this.move.setDisable(!this.recursionAllowed);

        } catch (JEVisException ex) {
            logger.fatal(ex);
            showError(ex.getMessage(), ex.getCause().getMessage());
            return Response.CANCEL;
        }

        final Stage stage = new Stage();

        final BooleanProperty isOK = new SimpleBooleanProperty(false);

        stage.setTitle(I18n.getInstance().getString("jevistree.dialog.copy.title"));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);

        VBox root = new VBox();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setWidth(450);
        stage.setHeight(400);
        stage.initStyle(StageStyle.UTILITY);
        stage.setResizable(true);
        scene.setCursor(Cursor.DEFAULT);

        BorderPane header = new BorderPane();
        header.setStyle("-fx-background-color: linear-gradient(#e2e2e2,#eeeeee);");
        header.setPadding(new Insets(10, 10, 10, 10));

        Label topTitle = new Label(I18n.getInstance().getString("jevistree.dialog.copy.chooseaction"));
        topTitle.setTextFill(Color.web("#0076a3"));
        topTitle.setFont(Font.font("Cambria", 25));

        ImageView imageView = ResourceLoader.getImage(ICON, 64, 64);

        stage.getIcons().add(imageView.getImage());

        VBox vboxLeft = new VBox();
        VBox vboxRight = new VBox();
        vboxLeft.getChildren().add(topTitle);
        vboxLeft.setAlignment(Pos.CENTER_LEFT);
        vboxRight.setAlignment(Pos.CENTER_LEFT);
        vboxRight.getChildren().add(imageView);

        header.setLeft(vboxLeft);

        header.setRight(vboxRight);

        HBox buttonPanel = new HBox();

        this.ok.setDefaultButton(true);
        this.ok.setDisable(true);

        Button cancel = new Button(I18n.getInstance().getString("jevistree.dialog.copy.cancel"));
        cancel.setCancelButton(true);

        buttonPanel.getChildren().addAll(this.ok, cancel);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);
        buttonPanel.setPadding(new Insets(10, 10, 10, 10));
        buttonPanel.setSpacing(10);
        buttonPanel.setMaxHeight(25);

        GridPane gp = new GridPane();
        gp.setPadding(new Insets(10));
        gp.setHgap(10);
        gp.setVgap(8);

        final ToggleGroup group = new ToggleGroup();

        this.move.setMaxWidth(Double.MAX_VALUE);
        this.move.setMinWidth(120);
        this.link.setMaxWidth(Double.MAX_VALUE);
        this.copy.setMaxWidth(Double.MAX_VALUE);

        this.link.setToggleGroup(group);
        this.move.setToggleGroup(group);
        this.copy.setToggleGroup(group);

        this.nameField.setPrefWidth(250);
        this.nameField.setPromptText(I18n.getInstance().getString("jevistree.dialog.copy.name.prompt"));

        final Label nameLabel = new Label(I18n.getInstance().getString("jevistree.dialog.copy.name"));
        final Label countLabel = new Label(I18n.getInstance().getString("jevistree.dialog.copy.amount"));


        group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {

            @Override
            public void changed(ObservableValue<? extends Toggle> ov, Toggle t, Toggle t1) {

                if (t1 != null) {
                    if (t1.equals(CopyObjectDialog.this.move)) {
                        CopyObjectDialog.this.ok.setDisable(false);
                        CopyObjectDialog.this.nameField.setDisable(true);
                        nameLabel.setDisable(true);
                        countLabel.setDisable(true);
                        CopyObjectDialog.this.count.setDisable(true);

                        CopyObjectDialog.this.includeSamples.setDisable(true);
                        CopyObjectDialog.this.includeSamples.setSelected(true);
                        CopyObjectDialog.this.includeValues.setDisable(true);
                        CopyObjectDialog.this.includeValues.setSelected(true);
                        CopyObjectDialog.this.recursion.setDisable(true);
                        CopyObjectDialog.this.recursion.setSelected(true);

                    } else if (t1.equals(CopyObjectDialog.this.link)) {
                        CopyObjectDialog.this.nameField.setDisable(false);
                        CopyObjectDialog.this.count.setDisable(true);
                        nameLabel.setDisable(false);
                        countLabel.setDisable(true);
                        CopyObjectDialog.this.includeSamples.setDisable(true);
                        CopyObjectDialog.this.includeSamples.setSelected(false);
                        CopyObjectDialog.this.includeValues.setDisable(true);
                        CopyObjectDialog.this.includeValues.setSelected(false);
                        CopyObjectDialog.this.recursion.setDisable(true);
                        CopyObjectDialog.this.recursion.setSelected(false);

                        checkName();

                    } else if (t1.equals(CopyObjectDialog.this.copy)) {
                        CopyObjectDialog.this.nameField.setDisable(false);
                        CopyObjectDialog.this.count.setDisable(false);
                        nameLabel.setDisable(false);
                        countLabel.setDisable(false);
                        CopyObjectDialog.this.nameField.setText(object.getName());

                        CopyObjectDialog.this.includeSamples.setDisable(!CopyObjectDialog.this.includeDataAllowed);
                        CopyObjectDialog.this.includeSamples.setSelected(true);
                        CopyObjectDialog.this.includeValues.setDisable(!CopyObjectDialog.this.includeValuesAllowed);
                        CopyObjectDialog.this.includeValues.setSelected(true);
                        CopyObjectDialog.this.recursion.setDisable(!CopyObjectDialog.this.recursionAllowed);
                        CopyObjectDialog.this.recursion.setSelected(true);

                        checkName();
                    }


                }

                if (!recursionAllowed) {
                    CopyObjectDialog.this.recursion.setSelected(false);
                }

            }
        });

        try {
//            this.link.setDisable(!linkOK);

            if (object.isAllowedUnder(newParent)) {
                if (recursionAllowed) {
                    this.move.setDisable(false);
                }
                this.copy.setDisable(false);
//                clone.setDisable(false);
            } else {
                this.move.setDisable(true);
                this.copy.setDisable(true);
//                clone.setDisable(true);
            }

            if (!this.link.isDisable()) {
                group.selectToggle(this.link);
                this.nameField.setText(object.getName());
                this.ok.setDisable(false);
            } else if (!this.move.isDisable()) {
                group.selectToggle(this.move);
            } else if (!this.copy.isDisable()) {
                group.selectToggle(this.copy);
            }

        } catch (JEVisException ex) {
            logger.fatal(ex);
        }

        HBox nameBox = new HBox(5);
        nameBox.getChildren().setAll(nameLabel, this.nameField);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        final HBox countBox = new HBox(5);
        countBox.getChildren().setAll(countLabel, this.count);
        countBox.setAlignment(Pos.CENTER_LEFT);

        Separator s1 = new Separator(Orientation.HORIZONTAL);
        GridPane.setMargin(s1, new Insets(5, 0, 10, 0));

        //check allowed
        int yAxis = 0;

        gp.add(this.link, 0, yAxis);
        gp.add(this.move, 0, ++yAxis);

        gp.add(this.copy, 0, ++yAxis);
        gp.add(s1, 0, ++yAxis, 3, 1);

        gp.add(this.recursion, 0, ++yAxis, 3, 1);//new
        gp.add(this.includeSamples, 0, ++yAxis, 3, 1);//new
        gp.add(this.includeValues, 0, ++yAxis, 3, 1);//new
        gp.add(countBox, 0, ++yAxis, 3, 1);
        gp.add(new Separator(Orientation.HORIZONTAL), 0, ++yAxis, 3, 1);
        gp.add(nameBox, 0, ++yAxis, 3, 1);


        Separator sep = new Separator(Orientation.HORIZONTAL);
        sep.setMinHeight(10);

        root.getChildren().addAll(header, new Separator(Orientation.HORIZONTAL), gp, buttonPanel);
        VBox.setVgrow(gp, Priority.ALWAYS);
        VBox.setVgrow(buttonPanel, Priority.NEVER);
        VBox.setVgrow(header, Priority.NEVER);

        this.ok.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                stage.close();

                if (group.getSelectedToggle().equals(CopyObjectDialog.this.move)) {
                    CopyObjectDialog.this.response = Response.MOVE;
                } else if (group.getSelectedToggle().equals(CopyObjectDialog.this.link)) {
                    CopyObjectDialog.this.response = Response.LINK;
                } else if (group.getSelectedToggle().equals(CopyObjectDialog.this.copy)) {
                    CopyObjectDialog.this.response = Response.COPY;
                }
//                else if (group.getSelectedToggle().equals(clone)) {
//                    response = Response.CLONE;
//                }

            }
        });

        cancel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                stage.close();
                CopyObjectDialog.this.response = Response.CANCEL;

            }
        });

        this.nameField.setOnKeyPressed(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent t) {
                if (CopyObjectDialog.this.nameField.getText() != null && !CopyObjectDialog.this.nameField.getText().equals("")) {
                    CopyObjectDialog.this.ok.setDisable(false);
                }
            }
        });

        switch (defaultAction) {
            case COPY:
                if (!this.copy.isDisable()) {
                    this.copy.setSelected(true);
                }
                break;
            case MOVE:
                if (!this.move.isDisable()) {
                    this.move.setSelected(true);
                }
                break;
            case LINK:
                if (!this.link.isDisable()) {
                    this.link.setSelected(true);
                }
                break;

        }

        stage.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                if (this.move.isSelected()) {
                    this.ok.fire();
                    ev.consume();
                }
            }
        });

        stage.sizeToScene();
        stage.setAlwaysOnTop(true);
        Platform.runLater(stage::requestFocus);
        Platform.runLater(stage::toFront);
        stage.showAndWait();

        return this.response;
    }

    public enum Response {

        MOVE, LINK, CANCEL, COPY //,CLONE
    }

    private Response response = Response.CANCEL;


    public String getCreateName() {
        return this.nameField.getText();
    }

    public boolean isRecursion() {
        return this.recursion.isSelected();
    }

    public int getCreateCount() {

        if (this.count.getNumber().intValue() > 0 && this.count.getNumber().intValue() < 500) {
            return this.count.getNumber().intValue();
        } else {
            return 1;
        }
    }

    public enum DefaultAction {

        MOVE, LINK, COPY
    }

    public boolean isIncludeData() {
        return this.includeSamples.isSelected();
    }

    public boolean isIncludeValues() {
        return this.includeValues.isSelected();
    }

    private void checkName() {
        if (this.nameField.getText() != null && !this.nameField.getText().isEmpty()) {
            this.ok.setDisable(false);
        } else {
            this.ok.setDisable(true);
        }
    }

    private void showError(String titleLong, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18n.getInstance().getString("jevistree.dialog.copy.recursion.alert.title"));
        alert.setHeaderText(titleLong);
        alert.setContentText(message);
        alert.setResizable(true);
        alert.show();
    }

    public boolean parentCheck(JEVisObject obj, JEVisObject target) {
        try {
//            logger.info("parentCheck: " + obj.getName() + " -> " + target.getName());
            //Check if its the same object
            if (target.equals(obj)) {
//                logger.info("Error 1");
                return false;
            }

            //check if the obj its os own parent
            for (JEVisObject parent : target.getParents()) {
                if (parent.equals(target)) {
                    showError(I18n.getInstance().getString("jevistree.dialog.copy.recursion.error"),
                            I18n.getInstance().getString("jevistree.dialog.copy.recursion.error.message"));
                    return false;
                }
                if (!parentCheck(obj, parent)) {
//                    logger.info("Error 3.2");
                    return false;
                }

            }

        } catch (JEVisException ex) {
            return false;
        }
        return true;
    }
}
