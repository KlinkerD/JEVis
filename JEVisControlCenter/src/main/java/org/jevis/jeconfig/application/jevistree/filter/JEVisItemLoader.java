package org.jevis.jeconfig.application.jevistree.filter;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.TreeItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisObject;
import org.jevis.jeconfig.application.jevistree.JEVisTree;
import org.jevis.jeconfig.application.jevistree.JEVisTreeItem;
import org.jevis.jeconfig.application.jevistree.JEVisTreeRow;

import java.util.*;

/**
 * The JEVisItemLoader creates the items for an JEVisTree. It also handel the filtering for the tree.
 *
 * @author florian.simon@envidatec.com
 */
public class JEVisItemLoader {

    private static final Logger logger = LogManager.getLogger(JEVisItemLoader.class);
    private final JEVisTree jeVisTree;
    private final List<JEVisObject> roots;
    private final List<JEVisTreeItem> treeObjectItems = Collections.synchronizedList(new ArrayList<>());
    private final List<JEVisTreeItem> treeAttributeItems = Collections.synchronizedList(new ArrayList<>());
    private final Map<JEVisObject, JEVisTreeItem> itemObjectLinker = Collections.synchronizedMap(new TreeMap<>());
    private final Map<String, JEVisTreeItem> itemAttributeLinker = Collections.synchronizedMap(new TreeMap<>());


    /**
     * Create an new TreeItem loader.
     *
     * @param jeVisTree
     * @param objects
     * @param roots
     */
    public JEVisItemLoader(JEVisTree jeVisTree, List<JEVisObject> objects, List<JEVisObject> roots) {
        this.jeVisTree = jeVisTree;
        this.roots = roots;

        buildItems(objects);
        buildItems(this.roots);
    }

    /**
     * Reset items
     */
    private void cleanFilter() {
        for (JEVisTreeItem item : treeObjectItems) {
            try {
                item.getChildren().clear();
                item.setParentForFilter(false);
                item.setFiltered(false);
            } catch (Exception ex) {
            }
        }

        for (JEVisTreeItem item : treeAttributeItems) {
            try {
                item.getChildren().clear();
                item.setParentForFilter(false);
                item.setFiltered(false);
            } catch (Exception ex) {
            }
        }
    }

    public List<JEVisObject> getVisibleObjects() {
        List<JEVisObject> result = new ArrayList<>();
        for (JEVisTreeItem item : treeObjectItems) {
            if (item.isFiltered() || item.isParentForFilter()) {
                result.add(item.getValue().getJEVisObject());
            }
        }
        return result;
    }

    /**
     * Clear all items for the garbage collector
     */
    public void clearForGC() {
        for (JEVisTreeItem item : treeObjectItems) {
            item = null;
        }
        treeObjectItems.clear();
        itemObjectLinker.clear();
        treeAttributeItems.clear();
        itemAttributeLinker.clear();

    }


    /**
     * Create JEVisTreeItem all objects and attributes.
     * NOTE: loading all attributes at this point will take some time if we do not
     * optimize the Webservice for it
     *
     * @param objects
     */
    private void buildItems(List<JEVisObject> objects) {

        objects.parallelStream().forEach(object -> {
            try {
//                logger.error("Create item for object: {}", object.getName());
                JEVisTreeItem item = new JEVisTreeItem(object);
                registerEventHandler(object);
                treeObjectItems.add(item);
                itemObjectLinker.put(object, item);
                for (JEVisAttribute attribute : object.getAttributes()) {
                    try {
//                        logger.error("Create item for attribute: {}", attribute.getName());
                        JEVisTreeItem attributeItem = new JEVisTreeItem(attribute);
                        treeAttributeItems.add(attributeItem);
                        itemAttributeLinker.put(attributeKey(attribute), attributeItem);
//                        System.out.println("#### " + attributeKey(attribute) + " " + attribute);
                    } catch (Exception aex) {
                        logger.error("Error while loading type {}", attribute.getName(), aex);
                    }
                }
            } catch (Exception ex) {
                logger.error("Error while loading object {}", object.getID(), ex);
            }
        });
//        for (JEVisObject object : objects) {
//            try {
////                logger.error("Create item for object: {}", object.getName());
//                JEVisTreeItem item = new JEVisTreeItem(jeVisTree, object);
//                registerEventHandler(object);
//                treeObjectItems.add(item);
//                itemObjectLinker.put(object, item);
//                for (JEVisAttribute attribute : object.getAttributes()) {
//                    try {
////                        logger.error("Create item for attribute: {}", attribute.getName());
//                        JEVisTreeItem attributeItem = new JEVisTreeItem(jeVisTree, attribute);
//                        treeAttributeItems.add(attributeItem);
//                        itemAttributeLinker.put(attributeKey(attribute), attributeItem);
////                        System.out.println("#### " + attributeKey(attribute) + " " + attribute);
//                    } catch (Exception aex) {
//                        logger.error("Error while loading type {}", attribute.getName(), aex);
//                    }
//                }
//            } catch (Exception ex) {
//                logger.error("Error while loading object {}", object.getID(), ex);
//            }
//        }
    }

    private String attributeKey(JEVisAttribute attribute) {
        return attribute.getObjectID() + ":" + attribute.getName();
    }

    public JEVisTreeItem getItemForObject(JEVisObject object) {
        if (itemObjectLinker.containsKey(object)) {
            return itemObjectLinker.get(object);
        }
        return null;
    }

    /**
     * Build an new single JEVisTreeItem out of an JEVIsoObject and check filter
     * This function will also set filter to true. Use this if after the tree is already filtered an new object is added.
     *
     * @param object
     */
    private void buildItems(JEVisObject object) {
        List<JEVisObject> list = new ArrayList<>();
        list.add(object);
        buildItems(list);

        /** the new item is always visible **/
        JEVisTreeItem newItem = itemObjectLinker.get(object);
        newItem.setFiltered(true);
    }

    /**
     * Travers objects and set parents of filtered object as needed the tree so the user can navigate
     * to the filtered objects or attributes.
     *
     * @param parents
     * @param object
     */
    private void parentCheck(Set<JEVisObject> parents, JEVisObject object) {
        try {
            if (object == null || parents == null) {
                System.out.println("debug anker");
                return;
            }
            for (JEVisObject parent : object.getParents()) {
                /** continue travers if parent is not in list  **/
                if (!parents.contains(parent)) {
                    parents.add(parent);
                    parentCheck(parents, parent);
                }
            }
        } catch (Exception ex) {
            logger.error("Error while finding parents {}", Objects.requireNonNull(object).getID(), ex);
        }

    }

    /**
     * Internal debug help
     */
    private void printItemDebug() {
        for (JEVisTreeItem item : treeObjectItems) {
            if (item.getValue().getType() == JEVisTreeRow.TYPE.OBJECT) {
                System.out.println("item: " + item);
            }
        }
    }


    /**
     * Filter the the Objects list and set the new root item for the tree
     *
     * @param filter
     * @return
     */
    public void filterTree(JEVisTreeFilter filter) {
        JEVisTreeFilter activFilter = filter;
        cleanFilter();
//        Benchmark benchmark = new Benchmark();

        Set<JEVisObject> neededParents = new HashSet<>();

        /** find matching objects **/
        for (JEVisTreeItem item : treeObjectItems) {
            boolean show = filter.showItem(item.getValue().getJEVisObject());
            if (show) {
                item.setFiltered(true);
                parentCheck(neededParents, item.getValue().getJEVisObject());
            }
        }

//        benchmark.printBenchmarkDetail("find matching objects");

        /** find matching attributes **/
        for (JEVisTreeItem item : treeAttributeItems) {
            try {
                boolean show = filter.showItem(item.getValue().getJEVisAttribute());
                if (show) {
                    item.setFiltered(true);
//                item.setParentForFilter(true);
                    parentCheck(neededParents, item.getValue().getJEVisObject());
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
//        benchmark.printBenchmarkDetail("find matching attributes");

        /** set needed parents **/
        for (JEVisObject obj : neededParents) {
            if (itemObjectLinker.containsKey(obj)) {
                itemObjectLinker.get(obj).setParentForFilter(true);
            } else {
                logger.error("Parent item not found: " + obj);
            }
        }


//        benchmark.printBenchmarkDetail("find parents");
        /** build children lists **/
        for (JEVisTreeItem item : treeObjectItems) {
            if ((item.isFiltered() || item.isParentForFilter()) && item.isObject()) {
                try {
                    update(item.getValue().getJEVisObject());
                } catch (Exception ex) {
                    logger.error("Error while adding children {}", item, ex);
                }
            }
        }
//        benchmark.printBenchmarkDetail("build children");


        /** create an fake rootItem and add the root objects ad children if visible **/
        /** TODO: does not always work with all users. bug #455 **/
        JEVisTreeItem rootItem = new JEVisTreeItem();
        rootItem.setExpanded(true);
        for (JEVisObject rooObject : roots) {
            if (itemObjectLinker.containsKey(rooObject)) {
                JEVisTreeItem rootChild = itemObjectLinker.get(rooObject);
                if (rootChild.isFiltered() || rootChild.isParentForFilter()) {
                    rootItem.getChildren().add(rootChild);
                } else {
                    logger.warn("Root is filtered out: {}", rooObject);
                }

            } else {
                logger.error("Root is not build: {}", rooObject);
            }

        }

//        benchmark.printBenchmarkDetail("build root");

        jeVisTree.setRoot(rootItem);
    }

    /**
     * Update an object and its children
     *
     * @param object
     */
    public void update(JEVisObject object) {
        try {
            JEVisTreeItem item = itemObjectLinker.get(object);
            ObservableList<JEVisTreeItem> newChildrenList = FXCollections.observableArrayList();

            for (JEVisObject objChild : object.getChildren()) {
                /** add object children **/
                if (itemObjectLinker.containsKey(objChild)) {
                    JEVisTreeItem itemChild = itemObjectLinker.get(objChild);
                    if (itemChild.isParentForFilter() || itemChild.isFiltered()) {
                        newChildrenList.add(itemChild);
                    }
                }
            }
            /** add attributes children **/
            for (JEVisAttribute attribute : object.getAttributes()) {
//                    System.out.println(" ~~~> " + attribute.getName());
                if (itemAttributeLinker.containsKey(attributeKey(attribute))) {
                    JEVisTreeItem itemChild = itemAttributeLinker.get(attributeKey(attribute));
                    if (itemChild.isParentForFilter() || itemChild.isFiltered()) {
                        newChildrenList.add(itemChild);
                    }
                }
            }
            newChildrenList.sort(TreeItemComparator.getInstance());
            item.getChildren().setAll(newChildrenList);
//            item.getChildren().forEach(jeVisTreeRowTreeItem -> {
//                System.out.println(" +++++> " + jeVisTreeRowTreeItem.getValue().getID());
//            });

        } catch (Exception ex) {
            logger.error(ex);
            ex.printStackTrace();
        }
    }


    /**
     * Add the EventListeners to the JEVisObject to handle delete,update, new and so on
     *
     * @param object
     */
    private void registerEventHandler(JEVisObject object) {
        /** TODO: an weak listener would be better **/
        object.addEventListener(event -> {
            logger.error("Object Event [{}]: object {}", event.getType(), object.getID());
            switch (event.getType()) {
                case OBJECT_DELETE:
                    /** nothing to do, we listen to the parent OBJECT_CHILD_DELETED event **/
                    break;
                case OBJECT_NEW_CHILD:
                    JEVisObject newObject = (JEVisObject) event.getSource();
                    buildItems(newObject);
                    if (newObject != null && itemObjectLinker.containsKey(newObject)) {
                        Platform.runLater(() -> {
                            update(object);
                            JEVisTreeItem newItem = itemObjectLinker.get(object);
                            newItem.setExpanded(true);
                            jeVisTree.getSelectionModel().select(itemObjectLinker.get(newObject));
                        });
                    }
                    break;
                case OBJECT_CHILD_DELETED:
                    update(object);

                    break;
                case OBJECT_UPDATED:
                    JEVisTreeItem parentItem = itemObjectLinker.get(object);
                    TreeItem.TreeModificationEvent<JEVisTreeRow> treeEvent = new TreeItem.TreeModificationEvent<>(JEVisTreeItem.valueChangedEvent(), parentItem);
                    Event.fireEvent(parentItem, treeEvent);

                    break;
                default:
                    break;
            }
        });

    }


}