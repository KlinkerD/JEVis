/**
 * Copyright (C) 2009 - 2015 Envidatec GmbH <info@envidatec.com>
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
package org.jevis.jeconfig.plugin.object;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.HiddenSidesPane;
import org.jevis.api.JEVisObject;
import org.jevis.api.JEVisType;
import org.jevis.commons.i18n.I18n;
import org.jevis.jeconfig.Constants;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.application.application.I18nWS;
import org.jevis.jeconfig.application.jevistree.JEVisTree;
import org.jevis.jeconfig.plugin.object.extension.*;
import org.jevis.jeconfig.tool.ImageConverter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This Edior is used to configure the Attributes of an Objects. Its used in the
 * right side next to the Objects Tree.
 *
 * @author Florian Simon <florian.simon@envidatec.com>
 */
public class ObjectEditor {

    private JEVisObject _currentObject = null;
    private List<ObjectEditorExtension> installedExtensions = new LinkedList<>();
    private List<ObjectEditorExtension> activeExtensions = new ArrayList<>();
    private boolean _hasChanged = true;
    private String _lastOpenEditor = "";
    private static final Logger logger = LogManager.getLogger(ObjectEditor.class);
    private JEVisTree tree;

    private HiddenSidesPane _view;

    public ObjectEditor() {
        _view = new HiddenSidesPane();
        _view.setId("objecteditorpane");
        _view.getStylesheets().add("/styles/Styles.css");
        _view.setStyle("-fx-background-color: " + Constants.Color.LIGHT_GREY2);

    }

    public void commitAll() {
        for (ObjectEditorExtension extension : activeExtensions) {
            logger.debug("ObjectEditor.comitall: {}", extension.getClass());
            extension.save();
        }
    }

    public boolean needSave() {
        if (_currentObject != null) {
            for (ObjectEditorExtension extension : activeExtensions) {
                if (extension.needSave()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void dismissChanges() {
        if (_currentObject != null) {
            for (ObjectEditorExtension extension : activeExtensions) {
                extension.dismissChanges();
            }
        }
    }

    //    public void checkIfSaved(JEVisObject obj) {
//        logger.info("checkIfSaved: " + obj);
//        if (_currentObject != null && !Objects.equals(obj.getID(), _currentObject.getID())) {
//
//            List<ObjectEditorExtension> needSave = new ArrayList<>();
//
//            _hasChanged = true;
//            for (ObjectEditorExtension extension : installedExtensions) {
//                if (extension.needSave()) {
//                    logger.info("extension need save: " + extension.getTitle());
//                    needSave.add(extension);
//                }
//            }
//            logger.info("needSave.size: " + needSave.size());
//
//            commitAll();
//            if (!needSave.isEmpty()) {
//                Platform.runLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        ConfirmDialog dia = new ConfirmDialog();
//                        ConfirmDialog.Response re = dia.show(JEConfig.getStage(), "Save", "Save Attribute Changes", "Changes will be lost if not saved, do you want to save now?");
//                        if (re == ConfirmDialog.Response.YES) {
//                            commitAll();
//                        } else {
//                            _hasChanged = false;
//                        }
//
//                    }
//                });
//
//            }
//
//        }
//    }
    public Node getView() {
        return _view;
    }

    public void setTree(JEVisTree tree) {
        this.tree = tree;
    }

    public void setObject(final JEVisObject obj) {
        Task<Void> load = new Task<Void>() {
            @Override
            protected Void call() {
                loadObject(obj);
                return null;
            }
        };

        Platform.runLater(() -> JEConfig.getStage().getScene().setCursor(Cursor.WAIT));

        load.setOnSucceeded(event -> Platform.runLater(() -> JEConfig.getStage().getScene().setCursor(Cursor.DEFAULT)));

        new Thread(load).start();

    }


    public void loadObject(final JEVisObject obj) {
        //        checkIfSaved(obj); //TODO reimplement
        _currentObject = obj;

        Platform.runLater(
                () -> {
                    Accordion accordion = new Accordion();
                    accordion.getStylesheets().add("/styles/objecteditor.css");
                    accordion.setStyle("-fx-box-border: transparent;");

                    List<TitledPane> tabs = new ArrayList<>();
                    installedExtensions = new ArrayList<>();

                    installedExtensions.add(new CalculationExtension(obj));
                    installedExtensions.add(new CleanDataExtension(obj));
                    installedExtensions.add(new MemberExtension(obj));

                    //Generic Extensions every Class has
                    //TODO: make an better logic to decide/configure the extension order
                    this.installedExtensions.add(new GenericAttributeExtension(obj, this.tree));
                    if (JEConfig.getExpert()) {
                        this.installedExtensions.add(new ChildrenEditorPlugin(obj));
                    }
                    this.installedExtensions.add(new PermissionExtension(obj));
                    this.installedExtensions.add(new RootExtension(obj));
                    this.installedExtensions.add(new LinkExtension(obj));

                    activeExtensions = new ArrayList<>();

                    for (final ObjectEditorExtension ex : installedExtensions) {
                        try {
                            if (ex.isForObject(obj)) {
                                if (_lastOpenEditor == null) {
                                    _lastOpenEditor = ex.getTitle();
                                }

                                activeExtensions.add(ex);
                                TitledPane newTab = new TitledPane(ex.getTitle(), ex.getView());
                                newTab.getStylesheets().add("/styles/objecteditor.css");

                                newTab.setAnimated(false);
                                tabs.add(newTab);
                                ex.getValueChangedProperty().addListener((ov, t, t1) -> {
                                    if (t1) {
                                        _hasChanged = t1;//TODO: enable/disable the save button
                                    }
                                });

                                newTab.expandedProperty().addListener((ov, t, t1) -> {
                                    if (t1) {
                                        try {
                                            JEConfig.loadNotification(true);
                                            ex.setVisible();
                                            _lastOpenEditor = ex.getTitle();
                                            JEConfig.loadNotification(false);
                                        } catch (Exception ex1) {
                                            logger.error("Could not make tab {} visible for object {}:{}.", ex.getTitle(), obj.getName(), obj.getID(), ex1);
                                        }
                                    }
                                });

                            }
                        } catch (Exception pex) {
                            logger.error("Error while loading extension: ", pex);
                        }
                    }

                    accordion.getPanes().addAll(tabs);

                    //load the last Extensions for the new object
                    boolean foundTab = false;
                    if (!tabs.isEmpty()) {

                        for (ObjectEditorExtension ex : activeExtensions) {
                            if (ex.getTitle().equals(_lastOpenEditor)) {
                                ex.setVisible();
//                            updateView(content, ex);
                            }
                        }
                        for (TitledPane tap : tabs) {
                            if (tap.getText().equals(_lastOpenEditor)) {
                                accordion.setExpandedPane(tap);
                                foundTab = true;
                            }
                        }

                    }
                    if (!foundTab) {

                        //Set the first enabled extension visible, waring the order of installedExtensions an tabs is not the same
                        for (final ObjectEditorExtension ex : activeExtensions) {
                            try {
                                if (ex.isForObject(obj)) {
                                    ex.setVisible();
                                    break;
                                }
                            } catch (Exception nex) {
                                logger.error("Could not set first extension {} visible for object {}:{}", ex.getTitle(), obj.getName(), obj.getID(), nex);
                            }
                        }
//                            installedExtensions.get(0).setVisible();
                        accordion.setExpandedPane(tabs.get(0));
                        tabs.get(0).requestFocus();
                        _lastOpenEditor = installedExtensions.get(0).getTitle();
                    }

                    Button helpButton = new Button("", JEConfig.getImage("1400874302_question_blue.png", 34, 34));
                    helpButton.setStyle("-fx-padding: 0 2 0 2;-fx-background-insets: 0;-fx-background-radius: 0;-fx-background-color: transparent;");

                    //Header
                    ImageView classIcon;
                    try {
                        classIcon = ImageConverter.convertToImageView(obj.getJEVisClass().getIcon(), 60, 60);
                    } catch (Exception ex) {
                        classIcon = JEConfig.getImage("1390343812_folder-open.png", 20, 20);
                    }

                    GridPane header = new GridPane();
                    try {
                        Label nameLabel = new Label(I18n.getInstance().getString("plugin.object.editor.name"));
                        Label objectName = new Label(obj.getName());
                        objectName.setStyle("-fx-font-weight: bold;");
                        Label classlabel = new Label(I18n.getInstance().getString("plugin.object.editor.type"));
                        classlabel.setTooltip(new Tooltip(obj.getJEVisClassName()));
                        //Label className = new Label(obj.getJEVisClass().getName());
                        Label className = new Label(I18nWS.getInstance().getClassName(obj.getJEVisClassName()));

                        Label idlabel = new Label(I18n.getInstance().getString("plugin.object.editor.id"));
                        Label idField = new Label(obj.getID() + "");

                        //TODO: would be nice if the user can copy the ID and name but the layout is broken if i use this textfield code

                        Region spacer = new Region();

                        //                    x  y  xw yh
                        header.add(classIcon, 0, 0, 1, 4);

                        header.add(nameLabel, 1, 0, 1, 1);
                        header.add(classlabel, 1, 1, 1, 1);
                        header.add(idlabel, 1, 2, 1, 1);

                        header.add(objectName, 2, 0, 1, 1);
                        header.add(className, 2, 1, 1, 1);
                        header.add(idField, 2, 2, 1, 1);

                        header.add(spacer, 3, 0, 1, 4);
                        header.add(helpButton, 4, 1, 1, 2);

                        GridPane.setVgrow(spacer, Priority.ALWAYS);
                        GridPane.setFillWidth(spacer, true);
                        GridPane.setHgrow(spacer, Priority.ALWAYS);

                        GridPane.setVgrow(nameLabel, Priority.NEVER);
                        GridPane.setVgrow(classlabel, Priority.NEVER);
                        GridPane.setVgrow(idlabel, Priority.NEVER);
                        GridPane.setVgrow(objectName, Priority.NEVER);
                        GridPane.setVgrow(className, Priority.NEVER);
                        GridPane.setVgrow(idField, Priority.NEVER);
                        GridPane.setVgrow(helpButton, Priority.NEVER);
                        GridPane.setHgrow(helpButton, Priority.NEVER);

                        Separator sep = new Separator(Orientation.HORIZONTAL);
                        GridPane.setVgrow(sep, Priority.ALWAYS);

                        header.setPadding(new Insets(10));
                        header.setVgap(5);
                        header.setHgap(12);
                        header.setPadding(new Insets(10, 0, 20, 10));
                    } catch (Exception ex) {
                        logger.error("Error while creating header.", ex);
                    }


                    BorderPane pane = new BorderPane();
                    pane.setTop(header);
                    pane.setCenter(accordion);

                    try {

                        StringBuilder sb = new StringBuilder();
                        sb.append("<html><body>");
                        sb.append("<p>");
                        sb.append(I18nWS.getInstance().getClassDescription(obj.getJEVisClassName()));
                        sb.append("</p><br>");
                        for (JEVisType type : obj.getJEVisClass().getTypes()) {
                            sb.append("<h4>");
                            sb.append(I18nWS.getInstance().getTypeName(obj.getJEVisClassName(), type.getName()));
                            sb.append("</h4>");
                            sb.append("<p>");
                            sb.append(I18nWS.getInstance().getTypeDescription(obj.getJEVisClassName(), type.getName()));
                            sb.append("</p>");
                        }

                        sb.append("</body></html>");
                        SideNode help = new SideNode(
                                I18nWS.getInstance().getClassName(obj.getJEVisClassName()),
                                sb.toString(),
                                Side.RIGHT, _view);


                        _view.setRight(help);
                        helpButton.setOnAction(event -> _view.setPinnedSide(Side.RIGHT));

                    } catch (Exception ex) {
                        logger.error("Error while creating help view", ex);
                    }

                    _view.setContent(pane);
                }
        );

    }

    static class SideNode extends VBox {

        SideNode(final String headline, final String text, final Side side,
                 final HiddenSidesPane pane) {
            super();

            setPrefSize(300, 200);
            setOpacity(0.7d);
            setSpacing(10d);
            setPadding(new Insets(10));
            setStyle("-fx-background-color: white;");

            Region spacerHeader = new Region();
            spacerHeader.setMinHeight(30d);//120d);

            Label header = new Label(headline);
            header.setFont(Font.font(22));

            WebView helpText = new WebView();

            helpText.getEngine().setUserStyleSheetLocation(ObjectEditor.class.getResource("/styles/help.css").toExternalForm());

            helpText.getEngine().loadContent(text);


            getChildren().addAll(header, spacerHeader, helpText);
            VBox.setVgrow(header, Priority.NEVER);
            VBox.setVgrow(helpText, Priority.ALWAYS);

            setOnMouseClicked(t -> pane.setPinnedSide(null));

        }
    }

}
