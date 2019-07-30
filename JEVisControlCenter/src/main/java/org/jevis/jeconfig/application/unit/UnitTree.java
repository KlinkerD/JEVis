/**
 * Copyright (C) 2014-2015 Envidatec GmbH <info@envidatec.com>
 *
 * This file is part of JEConfig.
 *
 * JEConfig is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation in version 3.
 *
 * JEConfig is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * JEConfig. If not, see <http://www.gnu.org/licenses/>.
 *
 * JEConfig is part of the OpenJEVis project, further project information are
 * published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.jeconfig.application.unit;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Callback;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisUnit;
import org.jevis.commons.unit.JEVisUnitImp;
import org.jevis.commons.unit.UnitManager;

import javax.measure.unit.Unit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

/**
 * TreeView to display JEVIsUnits in JEConfig
 *
 * @author Florian Simon <florian.simon@envidatec.com>
 */
public class UnitTree extends TreeView<UnitObject> {

//    private UnitEditor _editor = new UnitEditor();
    private JEVisDataSource _ds;

    private HashMap<String, TreeItem<UnitObject>> itemCache;
    private HashMap<String, UnitGraphic> graphicsCache;
    private HashMap<TreeItem<UnitObject>, ObservableList<TreeItem<UnitObject>>> itemChildren;
    private ObservableList<TreeItem<UnitObject>> emptyList = FXCollections.emptyObservableList();

    private UnitObject _dragObj;

    public UnitTree() {

    }

    public UnitTree(JEVisDataSource ds) {
        super();
        try {
            _ds = ds;
            itemCache = new HashMap<>();
            graphicsCache = new HashMap<>();
            itemChildren = new HashMap<>();
//            setStyle("-fx-background-color: white;");
            setMaxHeight(2014);

            UnitObject uo = new UnitObject(UnitObject.Type.FakeRoot, new JEVisUnitImp(Unit.ONE), "Unit");
            TreeItem<UnitObject> rootItem = buildItem(uo);

            setShowRoot(false);

            getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

            getSelectionModel().selectedItemProperty().addListener((ov, t, t1) -> {
                if (t != null) {
//                        _editor.setUnit(t1.getValue());
                }

            });

            setCellFactory(new Callback<TreeView<UnitObject>, TreeCell<UnitObject>>() {
                @Override
                public TreeCell<UnitObject> call(TreeView<UnitObject> p) {
                    return new ObjectCell() {

                        @Override
                        protected void updateItem(UnitObject item, boolean emty) {
                            super.updateItem(item, emty); //To change body of generated methods, choose Tools | Templates.
                            if (!emty) {
                                UnitGraphic gc = getObjectGraphic(item);
//                                setText(item);
                                setGraphic(gc.getGraphic());

                            } else {
                                setText(null);
                                setGraphic(null);
                            }
                        }

                    };
                }
            });

            //TODO: give this its own id
            setId("objecttree");
//            getStylesheets().add("/styles/Styles.css");

            setPrefWidth(500);

            setRoot(rootItem);
            getSelectionModel().select(rootItem);
            setEditable(true);

        } catch (Exception ex) {
//            Logger.getLogger(ObjectTree.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }

    }

    public UnitObject getDragItem() {
        return _dragObj;
    }

    public void setDragItem(UnitObject obj) {
        _dragObj = obj;
    }

    public static void sortList(ObservableList<TreeItem<UnitObject>> list) {
        Comparator<TreeItem<UnitObject>> sort = Comparator.comparing(o -> o.getValue().getName());

        FXCollections.sort(list, sort);

    }

    public TreeItem<UnitObject> getObjectTreeItem(UnitObject object) {
        return buildItem(object);
    }

    public UnitGraphic getObjectGraphic(UnitObject object) {
        if (graphicsCache.containsKey(object.toString())) {
            return graphicsCache.get(object.toString());
        }

//        logger.info("grahic does not exist create for: " + object);
        UnitGraphic graph = new UnitGraphic(object, this);
        graphicsCache.put(object.toString(), graph);

        return graph;
    }

    public TreeItem<UnitObject> buildItem(UnitObject object) {

        if (itemCache.containsKey(object.toString())) {
            return itemCache.get(object.toString());
        }

//        logger.info("buildItem: " + object);
        final TreeItem<UnitObject> newItem = new UnitItem(object, this);
        itemCache.put(object.toString(), newItem);

        return newItem;
    }

    public ObservableList<TreeItem<UnitObject>> getChildrenList(TreeItem<UnitObject> item) {
        if (item == null || item.getValue() == null) {
            return emptyList;
        }

        if (itemChildren.containsKey(item)) {
            return itemChildren.get(item);
        }

        ObservableList<TreeItem<UnitObject>> list = FXCollections.observableArrayList();

//        for (Unit child : UnitManager.getInstance().getCompatibleSIUnit(item.getValue().getUnit())) {
//            TreeItem<UnitObject> newItem = buildItem(child);
//            list.add(newItem);
//        }
//        sortList(list);
        itemChildren.put(item, list);

        return list;

    }

    public void reload() {

    }

    public void expandSelected(boolean expand) {
        TreeItem<UnitObject> item = getSelectionModel().getSelectedItem();
        expand(item, expand);
    }

    private void expand(TreeItem<UnitObject> item, boolean expand) {
        if (!item.isLeaf()) {
            if (item.isExpanded() && !expand) {
                item.setExpanded(expand);
            } else if (!item.isExpanded() && expand) {
                item.setExpanded(expand);
            }

            for (TreeItem<UnitObject> child : item.getChildren()) {
                expand(child, expand);
            }
        }
    }

    public void addChildrenList(TreeItem<UnitObject> item, ObservableList<TreeItem<UnitObject>> list) {
//        logger.info("addChildrenList: " + item);
//        List<JEVisUnit> usedAdditionalUnits = new ArrayList<>();
        itemChildren.put(item, list);

        if (item.getValue().getType() == UnitObject.Type.FakeRoot) {
            for (JEVisUnit child : UnitManager.getInstance().getQuantitiesJunit()) {
//                logger.info("---add quanti: " + child);
                UnitObject quant = new UnitObject(UnitObject.Type.Quantity, child, UnitManager.getInstance().getQuantitiesName(child, Locale.ENGLISH));
                TreeItem<UnitObject> newItem = buildItem(quant);
                list.add(newItem);
            }
            //Quantity for all the rest Unit with are not comatible to anyting
            UnitObject custom = new UnitObject(UnitObject.Type.Quantity, new JEVisUnitImp(Unit.ONE), "Custom");
            TreeItem<UnitObject> customItem = buildItem(custom);
            list.add(customItem);

        } else if (item.getValue().getType() == UnitObject.Type.Quantity) {
            if (item.getValue().getID().equals("Custom")) {
                for (JEVisUnit child : UnitManager.getInstance().getCustomUnits()) {
                    UnitObject customUnit = new UnitObject(UnitObject.Type.NonSIUnit, child, item.getValue().getID() + child.toString());
                    TreeItem<UnitObject> newItem = buildItem(customUnit);
                    list.add(newItem);
                }
            } else {
                for (JEVisUnit child : UnitManager.getInstance().getCompatibleSIUnit(item.getValue().getUnit())) {
//                logger.info("------add SI Unit childList: " + child);
                    UnitObject quant = new UnitObject(UnitObject.Type.SIUnit, child, item.getValue().getID() + child.toString());
                    TreeItem<UnitObject> newItem = buildItem(quant);
                    list.add(newItem);
                }

                for (JEVisUnit child : UnitManager.getInstance().getCompatibleNonSIUnit(item.getValue().getUnit())) {
//                logger.info("------add NonSI Unit childList: " + child);
                    UnitObject quant = new UnitObject(UnitObject.Type.NonSIUnit, child, item.getValue().getID() + child.toString());
                    TreeItem<UnitObject> newItem = buildItem(quant);
                    list.add(newItem);
                }

                //TODO add additional Units from data source
                for (JEVisUnit child : UnitManager.getInstance().getCompatibleAdditionalUnit(item.getValue().getUnit())) {
//                    logger.info("------add Additional Unit childList: " + child);
                    UnitObject quant = new UnitObject(UnitObject.Type.NonSIUnit, child, item.getValue().getID() + child.toString());
                    TreeItem<UnitObject> newItem = buildItem(quant);
                    list.add(newItem);

                }
            }
        } else if (item.getValue().getType() == UnitObject.Type.NonSIUnit || item.getValue().getType() == UnitObject.Type.SIUnit) {
//            for (JEVisUnit child : getLabels(item.getValue().getUnit())) {
////                logger.info("----------add labels Unit childList: " + child);
//                UnitObject quant = new UnitObject(UnitObject.Type.AltSymbol, child, item.getValue().getID() + child.toString());
//                TreeItem<UnitObject> newItem = buildItem(quant);
//                list.add(newItem);
//
//            }
        }

        sortList(list);
    }

    public void fireSaveAttributes(boolean ask) {

//        if (ask) {
//            _editor.checkIfSaved(null);
//        } else {
//            _editor.commitAll();
//        }
    }

    public UnitObject getSelectedObject() {
        return getSelectionModel().getSelectedItem().getValue();
    }

    public void fireDelete(Unit obj) {
//        ConfirmDialog dia = new ConfirmDialog();
//        String question = "Do you want to delete the Class \"" + obj.getName() + "\" ?";
//
//        if (dia.show(JEConfig.getStage(), "Delete Object", "Delete Object?", question) == ConfirmDialog.Response.YES) {
//            try {
//                logger.info("User want to delete: " + obj.getName());
//
//                obj.delete();
//                getObjectTreeItem(obj).getParent().getChildren().remove(getObjectTreeItem(obj));
//                getSelectionModel().select(getObjectTreeItem(obj).getParent());
//
//            } catch (Exception ex) {
//                ex.printStackTrace();
////                    Dialogs.showErrorDialog(JEConfig.getStage(), ex.getMessage(), "Error", "Error", ex);
//
//            }
//        }
    }

    public void fireEventNew(final Unit parent) {

//        NewObjectDialog dia = new NewObjectDialog();
////        JEVisObject currentObject = _cl.getCurrentItem().getValue().getObject();
////        final TreeItem currentItem = _cl.getCurrentItem();
//
//        if (parent != null) {
//            if (dia.show(JEConfig.getStage(), null, parent, false, NewObjectDialog.Type.NEW, null) == NewObjectDialog.Response.YES) {
//                logger.info("create new: " + dia.getCreateName() + " class: " + dia.getCreateClass() + " " + dia.getCreateCount() + " times");
//
//                for (int i = 0; i < dia.getCreateCount(); i++) {
//                    try {
//                        //TODo check for uniq
////                if(!dia.getCreateClass().isUnique()){
////
////                }
//                        String name = dia.getCreateName();
//                        if (dia.getCreateCount() > 1) {
//                            name += " " + i;
//                        }
//
//                        JEVisObject newObject = parent.buildObject(name, dia.getCreateClass());
//                        newObject.commit();
//                        final TreeItem<JEVisObject> newTreeItem = buildItem(newObject);
//                        TreeItem<JEVisObject> parentItem = getObjectTreeItem(parent);
//
//                        parentItem.getChildren().add(newTreeItem);
//
//                        Platform.runLater(new Runnable() {
//                            @Override
//                            public void run() {
//                                getSelectionModel().select(newTreeItem);
//                            }
//                        });
//
//                    } catch (JEVisException ex) {
//                        //TODO: Cancel all if one faild befor he has to see the exeption dia.getCreateCount() times
//                        Logger.getLogger(UnitTree.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//
//            }
//        }
    }

    /**
     *
     */
    public class ObjectCell extends TreeCell<UnitObject> {

        @Override
        protected void updateItem(final UnitObject obj, boolean emty) {
            super.updateItem(obj, emty);
            if (!emty) {
                UnitGraphic grph = getObjectGraphic(obj);
                setText(grph.getText());
                setGraphic(grph.getGraphic());
                setTooltip(grph.getToolTip());
//                setContextMenu(grph.getContexMenu());

                //---------------------- Drag & Drop part --------------
//                setOnDragDetected(new EventHandler<MouseEvent>() {
//
//                    @Override
//                    public void handle(MouseEvent e) {
//                        logger.info("Drag Source: " + obj.toString());
//                        ClipboardContent content = new ClipboardContent();
////                        content.putString(obj.getName());
//                        Dragboard dragBoard = startDragAndDrop(TransferMode.ANY);
//                        content.put(DataFormat.PLAIN_TEXT, obj.toString());
//                        dragBoard.setContent(content);
//
//                        setDragItem(obj);
//                        e.consume();
//                    }
//                });
//
//                setOnDragDone(new EventHandler<DragEvent>() {
//                    @Override
//                    public void handle(DragEvent dragEvent) {
//                        logger.info("Drag done on " + obj.toString());
//                        dragEvent.consume();
//                    }
//                });
//
//                //TODO: ceh if its ok to move the Object here
//                setOnDragOver(new EventHandler<DragEvent>() {
//                    @Override
//                    public void handle(DragEvent dragEvent) {
//                        logger.info("Drag Over: " + obj.toString());
//
//                        try {
//                            if (getDragItem().isAllowedUnder(obj)) {
//                                dragEvent.acceptTransferModes(TransferMode.ANY);
//                            }
//
//                            if (obj.getJEVisClass().getName().equals("Views Directory") || obj.getJEVisClass().getName().equals(CommonClasses.LINK.NAME)) {
//                                dragEvent.acceptTransferModes(TransferMode.ANY);
//                            }
//
//                        } catch (JEVisException ex) {
//                            Logger.getLogger(UnitTree.class.getName()).log(Level.SEVERE, null, ex);
//                        }
//
//                        dragEvent.consume();
//
//                    }
//                });
//
//                setOnDragDropped(new EventHandler<DragEvent>() {
//                    @Override
//                    public void handle(final DragEvent dragEvent) {
//                        logger.info("\nDrag dropped on " + obj.getName());
//                        logger.info("To Drag: " + getDragItem().getName());
//                        dragEvent.consume();//to disable the drag cursor
//
//                        Platform.runLater(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (dragEvent.isAccepted()) {
////                                    JEConfig.getStage().getScene().setCursor(Cursor.DEFAULT);
//                                    showMoveDialog(_dragObj, obj);
//                                }
//
//                            }
//                        });
//
//                    }
//                });
            }

        }
    }

}
