package org.jevis.jeconfig.application.tools;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.commons.i18n.I18n;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.application.control.ToolTipDocu;
import org.jevis.jeconfig.tool.ScreenSize;

import java.util.*;

public class JEVisHelp {

    private static JEVisHelp jevisHelp;
    private static final Logger logger = LogManager.getLogger(ToolTipDocu.class);
    private Map<String, Set<ToolTipElement>> controlsMap = new HashMap<>();
    private BooleanProperty isHelpShowing = new SimpleBooleanProperty(false);
    private BooleanProperty isInfoShowing = new SimpleBooleanProperty(false);
    private String activePlugin = "";
    private String activeSubModule = "";
    private final KeyCombination help = new KeyCodeCombination(KeyCode.F1);

    public enum LAYOUT {
        HORIZONTAL_TOP_LEFT,
        VERTICAL_BOT_CENTER,
    }

    public JEVisHelp() {
    }

    public void deactivatePlugin(String plugin) {
        activeSubModule = "";
        update();
    }

    public void setActivePlugin(String plugin) {
        activePlugin = plugin;
        activeSubModule = "";
        update();
    }

    public void setActiveSubModule(String subModule) {
        this.activeSubModule = subModule;
        update();
    }


    public static synchronized JEVisHelp getInstance() {
        if (jevisHelp == null) {
            jevisHelp = new JEVisHelp();
        }
        return jevisHelp;
    }

    public void update() {
        logger.debug("Set ActivePlugin: {}", activePlugin);
        boolean isShowingNow = isHelpShowing.get();
        showTooltips(false);
        if (isShowingNow) showTooltips(true);
    }


    public void showTooltips(boolean show) {
        logger.debug("Show tooltips: {},{}", show, activePlugin);

        controlsMap.forEach((s, controls1) -> {
            logger.debug("controlsMap: {}", s, activePlugin);
            //if (!s.startsWith(activePlugin)) return; // if we want plugin and subModule visible at the same time
            String key = toKey(activePlugin, activeSubModule);
            if (!key.equals(s)) return;

            logger.debug("Is active: {}", s);
            controls1.forEach(obj -> {
                try {
                    obj.show(show);
                } catch (Exception ex) {
                    logger.warn(ex, ex);
                }
            });
        });

        isHelpShowing.setValue(show);
    }

    public void toggle() {
        showTooltips(!isHelpShowing.get());
    }

    public ObservableBooleanValue isHelpShowingProperty() {
        return isHelpShowing;
    }

    public ObservableBooleanValue isInfoShowingProperty() {
        return isInfoShowing;
    }

    public void addItems(String plugin, String subModule, LAYOUT layout, List<Node> nodes) {
        logger.error("Add Help items for: {}.{}", plugin, subModule);
        for (Node node : nodes) {
            try {
                if (node instanceof Control) {
                    addControl(plugin, subModule, layout, (Control) node);
                }
            } catch (Exception ex) {
                logger.warn(ex);
            }
        }
    }

    public void addControl(String plugin, String subModule, LAYOUT layout, Control... elements) {
        String key = toKey(plugin, subModule);
        logger.debug("AddControls: {},{}", key, elements.length);
        if (controlsMap.get(key) == null || controlsMap.get(key) == null) {
            controlsMap.put(key, new HashSet<>());
        }

        for (Control element : elements) {
            controlsMap.get(key).add(new ToolTipElement(layout, element));
        }


    }

    private String toKey(String plugin, String subModule) {
        return plugin + "." + subModule;
    }

    public void removeAll(String plugin) {
        controlsMap.forEach((s, controls1) -> {
            if (s.startsWith(plugin)) {
                if (controls1 != null) {
                    controls1.clear();
                }
            }
        });
    }

    public void removeAll(String plugin, String subModule) {
        Set<ToolTipElement> set = controlsMap.get(toKey(plugin, subModule));
        if (set != null) {
            set.clear();
        }
    }

    public void registerHotKey(Stage dialog) {
        dialog.getScene().setOnKeyPressed(ke -> {
            if (help.match(ke)) {
                JEVisHelp.getInstance().toggle();
                ke.consume();
            }
        });
    }

    public void registerHotKey(Dialog dialog) {
        dialog.getDialogPane().setOnKeyPressed(ke -> {
            if (help.match(ke)) {
                JEVisHelp.getInstance().toggle();
                ke.consume();
            }
        });
    }

    public Node buildSpacerNode() {
        Region spacerForRightSide = new Region();
        HBox.setHgrow(spacerForRightSide, Priority.ALWAYS);
        return spacerForRightSide;
    }

    public ToggleButton buildHelpButtons(double width, double height) {
        ToggleButton helpButton = new ToggleButton("", JEConfig.getImage("1404161580_help_blue.png", height, width));
        helpButton.setTooltip(new Tooltip(I18n.getInstance().getString("plugin.toolbar.tip.help")));
        helpButton.setOnAction(event -> JEVisHelp.getInstance().toggle());


        isHelpShowing.addListener((observable, oldValue, newValue) -> {
            helpButton.setSelected(newValue);
        });

        return helpButton;
    }

    public ToggleButton buildInfoButtons(double width, double height) {
        ToggleButton infoButton = new ToggleButton("", JEConfig.getImage("1404337146_info.png", height, width));
        isInfoShowing.addListener((observable, oldValue, newValue) -> {
            infoButton.setSelected(newValue);
        });
        return infoButton;
    }


    public class ToolTipElement {

        private LAYOUT layout = LAYOUT.VERTICAL_BOT_CENTER;
        private Control control;

        public ToolTipElement(LAYOUT layout, Control control) {
            this.layout = layout;
            this.control = control;
        }

        public void show(boolean show) {
            Platform.runLater(() -> {
                try {
                    Tooltip tooltip = control.getTooltip();
                    if (tooltip != null && !tooltip.getText().isEmpty()) {
                        logger.debug("Show tt: {}", tooltip.getText());
                        if (tooltip.getGraphic() == null) tooltip.setGraphic(new Region());
                        if (tooltip.isShowing() != show) {
                            if (tooltip.isShowing()) Platform.runLater(() -> {
                                tooltip.hide();
                                Label parent = (Label) tooltip.getGraphic().getParent();
                                parent.getTransforms().clear();
                            });
                            else {
                                double[] pos = ScreenSize.getAbsoluteScreenPostion(control);
                                double xPos = pos[0];
                                double yPos = pos[1];

                                tooltip.show(control, xPos, yPos);
                                switch (layout) {
                                    case VERTICAL_BOT_CENTER:
                                        Node parent = (Node) tooltip.getGraphic().getParent();
                                        parent.getTransforms().add(new Rotate(90));
                                        System.out.println();
                                        xPos += -control.getHeight();
                                        xPos += (control.getWidth() / 2);// + (control.getHeight() / 2);
                                        yPos += control.getHeight();
                                        break;
                                    case HORIZONTAL_TOP_LEFT:
                                        yPos += -36;//-tooltip.getHeight();
                                        xPos += -8;// magic number
                                        break;
                                }

                                tooltip.setX(xPos);
                                tooltip.setY(yPos);

                                logger.debug("done show: {}", control);

                            }
                        }
                    }

                } catch (Exception ex) {
                    logger.warn(ex, ex);
                    ex.getStackTrace();
                }
            });
        }

    }

}
