package org.jevis.jeconfig.dialog;

import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.*;
import org.jevis.commons.i18n.I18n;
import org.jevis.commons.utils.AlphanumComparator;
import org.jevis.commons.utils.CommonMethods;
import org.jevis.jeconfig.application.application.I18nWS;
import org.jevis.jeconfig.plugin.dtrc.InputVariableType;
import org.jevis.jeconfig.plugin.dtrc.JEVisNameType;
import org.jevis.jeconfig.plugin.dtrc.TRCPlugin;
import org.jevis.jeconfig.plugin.dtrc.TemplateInput;

import java.util.ArrayList;
import java.util.List;

public class TemplateCalculationInputDialog extends JFXDialog {
    private static final Logger logger = LogManager.getLogger(TemplateCalculationInputDialog.class);
    private final String ICON = "1404313956_evolution-tasks.png";
    private final AlphanumComparator ac = new AlphanumComparator();
    private Response response = Response.CANCEL;

    public TemplateCalculationInputDialog(StackPane dialogContainer, JEVisDataSource ds, TemplateInput templateInput) {
        super();

        setDialogContainer(dialogContainer);

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(6));
        gridPane.setVgap(4);
        gridPane.setHgap(4);

        ColumnConstraints labelWidth = new ColumnConstraints(80);
        gridPane.getColumnConstraints().add(0, labelWidth);
        ColumnConstraints fieldWidth = new ColumnConstraints(150);
        gridPane.getColumnConstraints().add(1, fieldWidth);

        Label classesLabel = new Label(I18n.getInstance().getString("plugin.dtrc.dialog.classlabel"));
        gridPane.add(classesLabel, 0, 0);
        GridPane.setHgrow(classesLabel, Priority.ALWAYS);

        Label attributeLabel = new Label(I18n.getInstance().getString("plugin.dtrc.dialog.attributelabel"));
        gridPane.add(attributeLabel, 0, 1);
        GridPane.setHgrow(attributeLabel, Priority.ALWAYS);

        Label typeLabel = new Label(I18n.getInstance().getString("plugin.dtrc.dialog.typelabel"));
        gridPane.add(typeLabel, 0, 2);
        GridPane.setHgrow(typeLabel, Priority.ALWAYS);

        JFXComboBox<InputVariableType> inputVariableTypeJFXComboBox = new JFXComboBox<>(FXCollections.observableArrayList(InputVariableType.values()));
        Callback<ListView<InputVariableType>, ListCell<InputVariableType>> inputVariableTypeJFXComboBoxCellFactory = new Callback<ListView<InputVariableType>, ListCell<InputVariableType>>() {
            @Override
            public ListCell<InputVariableType> call(ListView<InputVariableType> param) {
                return new JFXListCell<InputVariableType>() {
                    @Override
                    protected void updateItem(InputVariableType obj, boolean empty) {
                        super.updateItem(obj, empty);
                        if (obj == null || empty) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            switch (obj) {
                                case SUM:
                                    setText(I18n.getInstance().getString("plugin.dtrc.dialog.type.sum"));
                                    break;
                                case AVG:
                                    setText(I18n.getInstance().getString("plugin.dtrc.dialog.type.avg"));
                                    break;
                                case LAST:
                                    setText(I18n.getInstance().getString("plugin.dtrc.dialog.type.last"));
                                    break;
                                case STRING:
                                    setText(I18n.getInstance().getString("plugin.dtrc.dialog.type.string"));
                                    break;
                            }
                        }
                    }
                };
            }
        };

        inputVariableTypeJFXComboBox.setCellFactory(inputVariableTypeJFXComboBoxCellFactory);
        inputVariableTypeJFXComboBox.setButtonCell(inputVariableTypeJFXComboBoxCellFactory.call(null));

        gridPane.add(inputVariableTypeJFXComboBox, 1, 2);
        GridPane.setHgrow(inputVariableTypeJFXComboBox, Priority.ALWAYS);

        inputVariableTypeJFXComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> templateInput.setVariableType(newValue.toString()));

        if (templateInput.getVariableType() != null) {
            inputVariableTypeJFXComboBox.getSelectionModel().select(InputVariableType.valueOf(templateInput.getVariableType()));
        } else {
            inputVariableTypeJFXComboBox.getSelectionModel().selectFirst();
        }

        Label limiterLabel = new Label(I18n.getInstance().getString("plugin.dtrc.dialog.limiterlabel"));
        gridPane.add(limiterLabel, 0, 3);
        GridPane.setHgrow(limiterLabel, Priority.ALWAYS);

        JFXTextField filterField = new JFXTextField();
        filterField.setPromptText(I18n.getInstance().getString("searchbar.filterinput.prompttext"));
        gridPane.add(filterField, 1, 3);
        GridPane.setHgrow(filterField, Priority.ALWAYS);

        if (templateInput.getFilter() != null) {
            filterField.setText(templateInput.getFilter());
        }

        JFXCheckBox groupCheckBox = new JFXCheckBox(I18n.getInstance().getString("plugin.dtrc.dialog.grouplabel"));
        gridPane.add(groupCheckBox, 0, 4, 2, 1);
        GridPane.setHgrow(groupCheckBox, Priority.ALWAYS);

        if (templateInput.getGroup() != null) {
            groupCheckBox.setSelected(templateInput.getGroup());
        }

        groupCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> templateInput.setGroup(newValue));

        JFXComboBox<JEVisClass> classSelector = new JFXComboBox<>();

        JFXListView<JEVisObject> listView = new JFXListView<>();
        Callback<ListView<JEVisObject>, ListCell<JEVisObject>> listViewCellFactory = new Callback<ListView<JEVisObject>, ListCell<JEVisObject>>() {
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
                            try {
                                if (classSelector.getSelectionModel().getSelectedItem() != null &&
                                        classSelector.getSelectionModel().getSelectedItem().getName().equals("Clean Data")) {
                                    setText(CommonMethods.getFirstParentalDataObject(obj).getName() + " : " + obj.getID());
                                } else {
                                    setText(obj.getName() + " : " + obj.getID());
                                }
                            } catch (JEVisException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
            }
        };
        listView.setCellFactory(listViewCellFactory);

        gridPane.add(listView, 2, 0, 1, 5);
        GridPane.setHgrow(listView, Priority.ALWAYS);

        try {
            List<JEVisClass> allClasses = ds.getJEVisClasses();
            List<JEVisClass> classesWithoutDirectories = new ArrayList<>();
            JEVisClass dirClass = ds.getJEVisClass("Directory");
            for (JEVisClass jeVisClass : allClasses) {
                boolean isDir = false;
                for (JEVisClassRelationship jeVisClassRelationship : jeVisClass.getRelationships()) {
                    if (jeVisClassRelationship.getType() == 0 && jeVisClassRelationship.getEnd() != null && jeVisClassRelationship.getEnd().equals(dirClass)) {
                        isDir = true;
                        break;
                    }
                }
                if (!isDir) classesWithoutDirectories.add(jeVisClass);
            }

            ObservableList<JEVisClass> jeVisClasses = FXCollections.observableArrayList(classesWithoutDirectories);
            jeVisClasses.sort((o1, o2) -> {
                try {
                    return ac.compare(I18nWS.getInstance().getClassName(o1.getName()), I18nWS.getInstance().getClassName(o2.getName()));
                } catch (JEVisException e) {
                    logger.error("Could not sort object {} and object {}", o1, o2, e);
                }
                return 0;
            });

            classSelector.setItems(jeVisClasses);

            Callback<ListView<JEVisClass>, ListCell<JEVisClass>> classCellFactory = new Callback<ListView<JEVisClass>, ListCell<JEVisClass>>() {
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
                                    logger.error("Could not get class name", e);
                                }
                            }
                        }
                    };
                }
            };

            classSelector.setCellFactory(classCellFactory);
            classSelector.setButtonCell(classCellFactory.call(null));

            if (templateInput.getObjectClass() != null) {
                JEVisClass selectedClass = ds.getJEVisClass(templateInput.getObjectClass());
                classSelector.getSelectionModel().select(selectedClass);
            } else {
                classSelector.getSelectionModel().selectFirst();
            }

            JEVisClass firstClass = classSelector.getSelectionModel().getSelectedItem();
            List<JEVisObject> objectsOfFirstClass = ds.getObjects(firstClass, false);
            objectsOfFirstClass.sort((o1, o2) -> ac.compare(o1.getName(), o2.getName()));

            ObservableList<JEVisObject> objects = FXCollections.observableArrayList(objectsOfFirstClass);
            createFilterList(templateInput, filterField, listView, objects);

            List<JEVisType> types = firstClass.getTypes();
            types.sort((o1, o2) -> {
                try {
                    return ac.compare(o1.getName(), o2.getName());
                } catch (JEVisException e) {
                    logger.error("Could not sort object {} and object {}", o1, o2, e);
                }
                return 0;
            });
            types.add(new JEVisNameType(ds, firstClass));

            JFXComboBox<JEVisType> attributeSelector = new JFXComboBox<>(FXCollections.observableArrayList(types));
            Callback<ListView<JEVisType>, ListCell<JEVisType>> attributeCellFactory = new Callback<ListView<JEVisType>, ListCell<JEVisType>>() {
                @Override
                public ListCell<JEVisType> call(ListView<JEVisType> param) {
                    return new JFXListCell<JEVisType>() {
                        @Override
                        protected void updateItem(JEVisType obj, boolean empty) {
                            super.updateItem(obj, empty);
                            if (obj == null || empty) {
                                setGraphic(null);
                                setText(null);
                            } else {
                                try {
                                    if (!obj.getName().equals("name")) {
                                        setText(I18nWS.getInstance().getTypeName(classSelector.getSelectionModel().getSelectedItem().getName(), obj.getName()));
                                    } else {
                                        setText(I18n.getInstance().getString("plugin.graph.table.name"));
                                    }
                                } catch (JEVisException e) {
                                    logger.error("Could not get type name", e);
                                }
                            }
                        }
                    };
                }
            };

            attributeSelector.setCellFactory(attributeCellFactory);
            attributeSelector.setButtonCell(attributeCellFactory.call(null));

            if (templateInput.getAttributeName() != null) {
                JEVisType selectedType = firstClass.getType(templateInput.getAttributeName());
                attributeSelector.getSelectionModel().select(selectedType);
            } else {
                attributeSelector.getSelectionModel().selectFirst();
            }

            classSelector.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.equals(oldValue)) {
                    try {
                        templateInput.setObjectClass(newValue.getName());
                        attributeSelector.getSelectionModel().selectFirst();

                        List<JEVisType> newTypes = newValue.getTypes();
                        newTypes.sort((o1, o2) -> {
                            try {
                                return ac.compare(o1.getName(), o2.getName());
                            } catch (JEVisException e) {
                                logger.error("Could not sort object {} and object {}", o1, o2, e);
                            }
                            return 0;
                        });
                        newTypes.add(new JEVisNameType(ds, newValue));

                        List<JEVisObject> newObjects = ds.getObjects(newValue, false);

                        if (newValue.getName().equals("Clean Data")) {
                            newObjects.sort((o1, o2) -> {
                                JEVisObject firstParentalDataObjectO1 = null;
                                try {
                                    firstParentalDataObjectO1 = CommonMethods.getFirstParentalDataObject(o1);
                                    JEVisObject firstParentalDataObjectO2 = CommonMethods.getFirstParentalDataObject(o2);
                                    return ac.compare(firstParentalDataObjectO1.getName(), firstParentalDataObjectO2.getName());
                                } catch (JEVisException e) {
                                    e.printStackTrace();
                                }
                                return -1;
                            });
                        } else {
                            newObjects.sort((o1, o2) -> ac.compare(o1.getName(), o2.getName()));
                        }

                        Platform.runLater(() -> {
                            createFilterList(templateInput, filterField, listView, FXCollections.observableArrayList(newObjects));
                            attributeSelector.setItems(FXCollections.observableArrayList(newTypes));
                            attributeSelector.getSelectionModel().selectFirst();
                        });
                    } catch (JEVisException e) {
                        logger.error("Could not set new class name", e);
                    }
                }
            });

            attributeSelector.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.equals(oldValue)) {
                    try {
                        templateInput.setAttributeName(newValue.getName());
                        templateInput.buildVariableName(classSelector.getSelectionModel().getSelectedItem(), newValue);
                    } catch (JEVisException e) {
                        logger.error("Could not set new attribute name", e);
                    }
                }
            });

            gridPane.add(classSelector, 1, 0);
            gridPane.add(attributeSelector, 1, 1);
        } catch (JEVisException e) {
            logger.error("Could not load JEVisClasses", e);
        }

        JFXButton ok = new JFXButton(I18n.getInstance().getString("graph.dialog.ok"));
        ok.setOnAction(event -> {
            response = Response.OK;
            this.close();
        });

        JFXButton cancel = new JFXButton(I18n.getInstance().getString("graph.dialog.cancel"));
        cancel.setOnAction(event -> this.close());

        JFXButton delete = new JFXButton(I18n.getInstance().getString("jevistree.menu.delete"));
        delete.setOnAction(event -> {
            response = Response.DELETE;
            this.close();
        });

        HBox buttonBar = new HBox(8, delete, cancel, ok);
        gridPane.add(buttonBar, 1, 5, 2, 1);

        setContent(gridPane);
    }

    private void createFilterList(TemplateInput templateInput, JFXTextField limiterField, JFXListView<JEVisObject> listView, ObservableList<JEVisObject> objects) {
        FilteredList<JEVisObject> filteredList = new FilteredList<>(objects, s -> true);

        limiterField.textProperty().addListener(obs -> {
            String filter = limiterField.getText();
            templateInput.setFilter(filter);
            if (filter == null || filter.length() == 0) {
                filteredList.setPredicate(s -> true);
            } else {
                if (filter.contains(" ")) {
                    String[] result = filter.split(" ");
                    filteredList.setPredicate(s -> {
                        String name = TRCPlugin.getRealName(s);
                        boolean match = false;
                        String string = name.toLowerCase();
                        for (String value : result) {
                            String subString = value.toLowerCase();
                            if (!string.contains(subString))
                                return false;
                            else match = true;
                        }
                        return match;
                    });
                } else {
                    filteredList.setPredicate(s -> {
                        String name = TRCPlugin.getRealName(s);

                        String string = name.toLowerCase();
                        return string.contains(filter.toLowerCase());
                    });
                }
            }
        });

        listView.setItems(filteredList);
    }

    public Response getResponse() {
        return response;
    }
}
