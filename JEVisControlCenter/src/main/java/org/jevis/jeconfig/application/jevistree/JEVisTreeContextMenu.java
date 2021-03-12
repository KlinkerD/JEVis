/**
 * Copyright (C) 2009 - 2014 Envidatec GmbH <info@envidatec.com>
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
package org.jevis.jeconfig.application.jevistree;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.*;
import org.jevis.commons.export.TreeExporterDelux;
import org.jevis.commons.i18n.I18n;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.application.resource.ResourceLoader;
import org.jevis.jeconfig.application.tools.ImageConverter;
import org.jevis.jeconfig.dialog.EnterDataDialog;
import org.jevis.jeconfig.dialog.KPIWizard;
import org.jevis.jeconfig.dialog.LocalNameDialog;
import org.jevis.jeconfig.plugin.object.extension.OPC.OPCBrowser;
import org.jevis.jeconfig.tool.AttributeCopy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Florian Simon <florian.simon@envidatec.com>
 */
public class JEVisTreeContextMenu extends ContextMenu {
    private static final Logger logger = LogManager.getLogger(JEVisTreeContextMenu.class);
    private final StackPane dialogContainer;

    private JEVisObject obj;
    private JEVisTree tree;

    public JEVisTreeContextMenu(StackPane dialogContainer) {
        super();
        this.dialogContainer = dialogContainer;
    }

    public void setTree(JEVisTree tree) {
        this.tree = tree;
        tree.setOnMouseClicked(event -> {
            obj = getObject();
            getItems().setAll(
                    buildNew2(),
                    buildReload(),
                    new SeparatorMenuItem(),
                    buildDelete(),
                    //buildRename(),
                    buildMenuLocalize(),
                    buildCopy(),
                    buildCut(),
                    buildPaste(),
                    new SeparatorMenuItem(),
                    buildCopyFormat(),
                    buildParsedFormat(),
                    new SeparatorMenuItem(),
                    buildExport(),
                    buildImport()
            );

            try {
                if (obj.getJEVisClassName().equals("Calculation")) {
                    getItems().add(new SeparatorMenuItem());
                    getItems().add(buildMenuAddInput());
                } else if (obj.getJEVisClassName().equals("OPC UA Server")) {
                    getItems().add(new SeparatorMenuItem());
                    getItems().add(buildOCP());
                } else if (obj.getAttribute("Value") != null) {
                    getItems().add(buildManualSample());
                } else if (JEConfig.getExpert() && obj.getJEVisClassName().equals("Data Directory")) {
                    getItems().addAll(new SeparatorMenuItem(),
                            buildKPIWizard());
                }


            } catch (Exception ex) {
                logger.fatal(ex);
            }
        });
    }

    private JEVisObject getObject() {
        return ((JEVisTreeItem) tree.getSelectionModel().getSelectedItem()).getValue().getJEVisObject();
    }


    private MenuItem buildOCP() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.opc"), ResourceLoader.getImage("17_Paste_48x48.png", 20, 20));

        menu.setOnAction(t -> {
                    OPCBrowser opcEditor = new OPCBrowser(obj);
                }
        );
        return menu;
    }

    private MenuItem buildPaste() {
        //TODO: disable if not allowed
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.paste"), ResourceLoader.getImage("17_Paste_48x48.png", 20, 20));

        menu.setOnAction(t -> TreeHelper.EventDrop(tree, tree.getCopyObjects(), obj, CopyObjectDialog.DefaultAction.COPY)
        );
        return menu;
    }

    private MenuItem buildCopy() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.copy"), ResourceLoader.getImage("16_Copy_48x48.png", 20, 20));
        menu.setOnAction(t -> tree.setCopyObjectsBySelection(false)
        );
        return menu;
    }

    private MenuItem buildCut() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.cut"), ResourceLoader.getImage("16_Copy_48x48.png", 20, 20));
        menu.setOnAction(t -> tree.setCopyObjectsBySelection(true)
        );
        return menu;
    }

    private MenuItem buildKPIWizard() {
        MenuItem menu = new MenuItem("KPI Wizard", ResourceLoader.getImage("Startup Wizard_18228.png", 20, 20));
        menu.setOnAction(t -> {
                    KPIWizard wizard = new KPIWizard(dialogContainer, obj);
                    wizard.show();
                }
        );
        return menu;
    }

    private MenuItem buildExport() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.export"), ResourceLoader.getImage("1401894975_Export.png", 20, 20));
        menu.setOnAction(t -> {
                    TreeExporterDelux exportMaster = new TreeExporterDelux();

                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Save");
                    fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JEVis Export", "*.jex"));
                    File file = fileChooser.showSaveDialog(JEConfig.getStage());

                    if (file != null) {
                        List<JEVisObject> objects = new ArrayList<>();
                        tree.getSelectionModel().getSelectedItems().forEach(o -> {
                            JEVisTreeItem jeVisTreeItem = (JEVisTreeItem) o;
                            objects.add(jeVisTreeItem.getValue().getJEVisObject());
                        });

                        Task exportTask = exportMaster.exportToFileTask(file, objects);
                        JEConfig.getStatusBar().addTask("Tree Exporter", exportTask, JEConfig.getImage("save.gif"), true);

                    }
                    // JsonExportDialog dia = new JsonExportDialog("Import", obj);


                }
        );
        return menu;
    }

    private MenuItem buildImport() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.import"), ResourceLoader.getImage("1401894975_Export.png", 20, 20));
        menu.setOnAction(new EventHandler<ActionEvent>() {
                             @Override
                             public void handle(ActionEvent t) {

                                 FileChooser fileChooser = new FileChooser();
                                 fileChooser.setTitle("Open JEVis File");
                                 fileChooser.getExtensionFilters().addAll(
                                         new FileChooser.ExtensionFilter("JEVis Export", "*.jex"),
                                         new FileChooser.ExtensionFilter("All Files", "*.*"));
                                 File selectedFile = fileChooser.showOpenDialog(null);
                                 if (selectedFile != null) {
                                     try {
                                         TreeExporterDelux exportMaster = new TreeExporterDelux();
                                         Task exportTask = exportMaster.importFromFile(selectedFile, obj);
                                         JEConfig.getStatusBar().addTask("Tree Importer", exportTask, JEConfig.getImage("save.gif"), true);
                                         //List<DimpexObject> objects = DimpEX.readFile(selectedFile);
                                         //DimpEX.importALL(obj.getDataSource(), objects, obj);
                                     } catch (Exception ex) {
                                         logger.fatal(ex);
                                     }
                                 }

                             }
                         }
        );
        return menu;
    }

    public List<MenuItem> buildMenuNewContent() {

        logger.debug("buildMenuNewContent()");
        Object obj2 = this.getUserData();
        logger.debug("obj2: " + obj2);
        Object obj3 = this.getOwnerNode();
        logger.debug("obj3: " + obj3);

        List<MenuItem> newContent = new ArrayList<>();
        try {
            for (JEVisClass jlass : obj.getAllowedChildrenClasses()) {
                MenuItem classItem;

                classItem = new CheckMenuItem(jlass.getName(), getIcon(jlass));
                classItem.setOnAction(new EventHandler<ActionEvent>() {

                                          @Override
                                          public void handle(ActionEvent t) {
                                              TreeHelper.EventNew(dialogContainer, tree, obj);
                                          }
                                      }
                );
                newContent.add(classItem);
            }
        } catch (JEVisException ex) {
            logger.fatal(ex);
        }

        return newContent;
    }

    public MenuItem buildMenuAddInput() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.addinput"), ResourceLoader.getImage("1401894975_Export.png", 20, 20));
        menu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    TreeHelper.createCalcInput(dialogContainer, obj, null);
                } catch (JEVisException ex) {
                    logger.fatal(ex);
                }
            }
        });

        return menu;
    }

    public MenuItem buildMenuLocalize() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.localename"), ResourceLoader.getImage("translate.png", 20, 20));
        menu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    LocalNameDialog localNameDialog = new LocalNameDialog(obj);
                    localNameDialog.show();
                } catch (Exception ex) {
                    logger.fatal(ex);
                }
            }
        });

        return menu;
    }

    public MenuItem buildManualSample() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("menu.file.import.manual"), ResourceLoader.getImage("if_textfield_add_64870.png", 20, 20));
        menu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    JEVisSample lastValue = obj.getAttribute("Value").getLatestSample();
                    EnterDataDialog enterDataDialog = new EnterDataDialog(dialogContainer, obj.getDataSource());
                    enterDataDialog.setTarget(false, obj.getAttribute("Value"));
                    enterDataDialog.setSample(lastValue);
                    enterDataDialog.setShowValuePrompt(true);

                    enterDataDialog.show();
                } catch (Exception ex) {
                    logger.fatal(ex);
                }
            }
        });

        return menu;
    }


    public MenuItem buildMenuExport() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.export"), ResourceLoader.getImage("1401894975_Export.png", 20, 20));
        menu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                Platform.runLater(() -> {
                    try {
                        TreeHelper.EventExportTree(dialogContainer, obj);
                    } catch (JEVisException ex) {
                        logger.fatal(ex);
                    }
                });


            }
        });

        return menu;
    }


    private Menu buildMenuNew() {
        Menu addMenu = new Menu(I18n.getInstance().getString("jevistree.menu.new"), ResourceLoader.getImage("list-add.png", 20, 20));
        addMenu.getItems().addAll(buildMenuNewContent());

        return addMenu;

    }

    private ImageView getIcon(JEVisClass jclass) {
        try {
            return ImageConverter.convertToImageView(jclass.getIcon(), 20, 20);
        } catch (Exception ex) {
            return ResourceLoader.getImage("1393615831_unknown2.png", 20, 20);
        }
    }

    private MenuItem buildProperties() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.expand")
        );//shoud be edit but i use it for expand for the time
        menu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
//                PopOver popup = new PopOver(new HBox());
//                popup.show(_item.getGraphic(), 200d, 200d, Duration.seconds(1));
                //TMP test

//                logger.info("expand all");
//                _item.expandAll(true);
            }
        });
        return menu;
    }

    private MenuItem buildNew2() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.new"), ResourceLoader.getImage("list-add.png", 20, 20));
        menu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Object obj2 = getUserData();
                logger.debug("userdate: " + obj2);
                logger.debug("new event");
                TreeHelper.EventNew(dialogContainer, tree, obj);

            }
        });
        return menu;
    }

    /**
     * private MenuItem buildRename() {
     * MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.rename"), ResourceLoader.getImage("Rename.png", 20, 20));
     * menu.setOnAction(new EventHandler<ActionEvent>() {
     *
     * @Override public void handle(ActionEvent t) {
     * TreeHelper.EventRename(tree, obj);
     * }
     * });
     * return menu;
     * }
     **/

    private MenuItem buildReload() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.reload"), ResourceLoader.getImage("1476369770_Sync.png", 20, 20));
        menu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                TreeHelper.EventReload(obj, ((JEVisTreeItem) tree.getSelectionModel().getSelectedItem()));
            }
        });
        return menu;
    }

    private MenuItem buildDelete() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.delete"), ResourceLoader.getImage("list-remove.png", 20, 20));
        menu.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                TreeHelper.EventDelete(tree);
            }
        });
        return menu;
    }


    private MenuItem buildCopyFormat() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("jevistree.menu.copyformat"), ResourceLoader.getImage("pipette.png", 20, 20));
        menu.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                AttributeCopy attributeCopy = new AttributeCopy();
                attributeCopy.showAttributeSelection(obj);
                tree.setConfigObject(AttributeCopy.CONFIG_NAME, attributeCopy.getSelectedAttributes());

            }
        });
        return menu;
    }

    private MenuItem buildParsedFormat() {
        MenuItem menu = new MenuItem(I18n.getInstance().getString("dialog.attributecopy.paste"), ResourceLoader.getImage("Asset.png", 20, 20));
        menu.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                AttributeCopy attributeCopy = new AttributeCopy();
                List<JEVisAttribute> jeVisAttributes = (List<JEVisAttribute>) tree.getConfigObject(AttributeCopy.CONFIG_NAME);
                attributeCopy.startPaste(jeVisAttributes, tree.getSelectionModel().getSelectedItems());

            }
        });
        return menu;
    }

}
