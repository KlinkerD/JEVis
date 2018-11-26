/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.application.jevistree.plugin;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisObject;
import org.jevis.application.jevistree.JEVisTree;
import org.jevis.application.jevistree.JEVisTreeRow;
import org.jevis.application.jevistree.TreePlugin;
import org.jevis.application.jevistree.UserSelection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author fs
 */
public class SimpleTargetPlugin implements TreePlugin {
    private static final Logger logger = LogManager.getLogger(SimpleTargetPlugin.class);
    public static String TARGET_COLUMN_ID = "targetcolumn";
    private JEVisTree _tree;
    private List<UserSelection> _preselect = new ArrayList<>();
    private List<SimpleTargetPluginData> _data = new ArrayList<>();
    private boolean allowMultySelection = false;
    private BooleanProperty validProperty = new SimpleBooleanProperty(false);
    private MODE mode = MODE.OBJECT;
    private SimpleFilter filter = null;


    @Override
    public void setTree(JEVisTree tree) {
        _tree = tree;


    }

    public void setModus(MODE mode, SimpleFilter filter) {
        this.filter = filter;
//        this.mode = mode;
    }

    @Override
    public List<TreeTableColumn<JEVisTreeRow, Long>> getColumns() {
        List<TreeTableColumn<JEVisTreeRow, Long>> list = new ArrayList<>();

        TreeTableColumn<JEVisTreeRow, Long> pluginHeader = new TreeTableColumn<>("Target");

        TreeTableColumn<JEVisTreeRow, Boolean> selectColumn = buildSelectionColumn(_tree, "");
        selectColumn.setEditable(true);
        pluginHeader.getColumns().add(selectColumn);

        list.add(pluginHeader);


        return list;
    }

    public BooleanProperty getValidProperty() {
        return validProperty;
    }

    public void setAllowMultySelection(boolean selection) {
        allowMultySelection = selection;
    }

    @Override
    public void selectionFinished() {
    }

    @Override
    public String getTitle() {
        return "Selection";
    }

    private SimpleTargetPluginData getData(JEVisTreeRow row) {
        for (SimpleTargetPluginData data : _data) {
            if (row.getID().equals(data.getRow().getID())) {
                return data;
            }
        }
        SimpleTargetPluginData data = new SimpleTargetPluginData(row);
        data.setSelected(false);
        _data.add(data);
        return data;
    }

    private void unselectAllBut(JEVisTreeRow row) {
        for (SimpleTargetPluginData data : _data) {
            if (data.getRow().getID().equals(row.getID())) {
                continue;
            }

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    data.setSelected(false);
                    data.getBox().setSelected(false);
                }
            });

            logger.info(data.getBox().isSelected());
        }
    }

    private TreeTableColumn<JEVisTreeRow, Boolean> buildSelectionColumn(JEVisTree tree, String columnName) {
        TreeTableColumn<JEVisTreeRow, Boolean> column = new TreeTableColumn(columnName);
        column.setId(TARGET_COLUMN_ID);
        column.setPrefWidth(90);
        column.setEditable(true);

        column.setText(columnName);
        column.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<JEVisTreeRow, Boolean>, ObservableValue<Boolean>>() {

            @Override
            public ObservableValue<Boolean> call(TreeTableColumn.CellDataFeatures<JEVisTreeRow, Boolean> param) {
                return new ReadOnlyObjectWrapper<>(getData(param.getValue().getValue()).isSelected());
            }
        });

        column.setCellFactory(new Callback<TreeTableColumn<JEVisTreeRow, Boolean>, TreeTableCell<JEVisTreeRow, Boolean>>() {

            @Override
            public TreeTableCell<JEVisTreeRow, Boolean> call(TreeTableColumn<JEVisTreeRow, Boolean> param) {

                TreeTableCell<JEVisTreeRow, Boolean> cell = new TreeTableCell<JEVisTreeRow, Boolean>() {

                    @Override
                    public void commitEdit(Boolean newValue) {
                        super.commitEdit(newValue);
                    }

                    @Override
                    protected void updateItem(Boolean item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(null);
                        setGraphic(null);

                        if (!empty) {

                            try {
                                if (getTreeTableRow() != null && getTreeTableRow().getItem() != null && tree != null) {

                                    boolean show = _tree.getFilter().showCell(column, getTreeTableRow().getItem());
                                    if (show) {

                                        StackPane hbox = new StackPane();
                                        StackPane.setAlignment(hbox, Pos.CENTER_LEFT);
                                        CheckBox box = new CheckBox();
                                        hbox.getChildren().setAll(box);
                                        setGraphic(hbox);

                                        box.selectedProperty().addListener((observable, oldValue, newValue) -> {
                                            if (!allowMultySelection && box.isSelected()) {
                                                unselectAllBut(getTreeTableRow().getItem());
                                                commitEdit(box.isSelected());
                                                for (SimpleTargetPluginData data : _data) {
                                                    if (data.isSelected()) {
                                                        validProperty.setValue(true);
                                                    }
                                                }

                                            }
                                            logger.error("Selection: {}", box.isSelected());
                                            getData(getTreeTableRow().getItem()).setSelected(box.isSelected());
                                        });

                                        if (isPreselected(getTreeTableRow().getItem())) {
                                            box.setSelected(true);
                                        }
                                    }


                                }
                            } catch (Exception ex) {
                                logger.error(ex);
                                ex.printStackTrace();
                            }

                        }
                    }

                };

                return cell;
            }
        });

        return column;

    }

    private boolean isPreselected(JEVisTreeRow row) {
//        for (UserSelection us : _preselect) {
//            if (mode == MODE.OBJECT) {
//                if (us.getSelectedObject().equals(row.getJEVisObject())) {
//                    return true;
//                }
//            } else if (mode == MODE.ATTRIBUTE) {
//                if (us.getSelectedAttribute().equals(row.getJEVisAttribute())) {
//                    return true;
//                }
//            }
//
//        }
        return false;
    }

    private boolean isPreselected(JEVisObject obj) {
        for (UserSelection us : _preselect) {
            if (us.getSelectedObject().equals(obj)) {
                return true;
            }
        }
        return false;
    }

    public List<UserSelection> getUserSelection() {
        List<UserSelection> result = new ArrayList<>();
        for (SimpleTargetPluginData data : _data) {
            if (data.isSelected()) {
//                _preselect.add(new UserSelection(UserSelection.SelectionType.Object, data.getObj()));
                if (mode == MODE.OBJECT) {
                    result.add(new UserSelection(UserSelection.SelectionType.Object, data.getObj()));
                } else {
                    result.add(new UserSelection(UserSelection.SelectionType.Attribute, data.getAtt(), null, null));
                }
            }
        }

        return result;
    }

    public void setUserSelection(List<UserSelection> list) {
        _preselect = list;
    }


    public enum MODE {
        OBJECT, ATTRIBUTE, FILTER
    }

    private class AttributeFilter {
        private List<String> attributes = new ArrayList<>();

        public AttributeFilter(String... attributes) {
            this.attributes = Arrays.asList(attributes);
        }

        public List<String> getAttributes() {
            return attributes;
        }
    }

    public class ObjectFilter {
        public final String ALL = "*";
        public final String NONE = "NONE";
        private boolean includeInheritance = false;
        private List<AttributeFilter> attributeFilter = new ArrayList<>();
        private boolean objectFilter = false;
        private String className = "";

        public ObjectFilter(String className, boolean includeInheritance, boolean objectFilter, List<AttributeFilter> attributeFilter) {
            this.includeInheritance = includeInheritance;
            this.attributeFilter = attributeFilter;
            this.objectFilter = objectFilter;
            this.className = className;
        }


        public boolean isIncludeInheritance() {
            return includeInheritance;
        }

        public List<AttributeFilter> getAttributeFilter() {
            return attributeFilter;
        }

        public boolean isObjectFilter() {
            return objectFilter;
        }

        public void setObjectFilter(boolean objectFilter) {
            this.objectFilter = objectFilter;
        }

        public boolean match(JEVisObject obj) {
            try {
                return obj.getJEVisClassName().equals(className);
            } catch (Exception ex) {
                return false;
            }
        }
    }

    public class SimpleFilter {

        List<ObjectFilter> filter = new ArrayList<>();


        public SimpleFilter(List<ObjectFilter> filter) {
            this.filter = filter;
        }

        public List<ObjectFilter> getFilter() {
            return filter;
        }

        public boolean show(Object object) {
            if (object instanceof JEVisObject) {
                return showObject((JEVisObject) object);
            } else if (object instanceof JEVisAttribute) {
                return showAttribute((JEVisAttribute) object);
            }
            return false;
        }

        public boolean showObject(JEVisObject jevisClass) {

            for (ObjectFilter oFilter : filter) {
                if (oFilter.isObjectFilter()
                        && oFilter.match(jevisClass)) {
                    return true;
                }
            }

            return false;
        }

        public boolean showAttribute(JEVisAttribute jevisClass) {
            for (ObjectFilter oFilter : filter) {
                if (!oFilter.isObjectFilter()) {
                    for (AttributeFilter aFilter : oFilter.getAttributeFilter()) {
                        if (aFilter.getAttributes().contains(jevisClass.getName())) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }


    }


}
