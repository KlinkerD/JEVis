package org.jevis.jeconfig.plugin.Dashboard;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.jevis.api.JEVisClass;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisObject;
import org.jevis.jeconfig.GlobalToolBar;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.plugin.Dashboard.config.DashBordModel;
import org.jevis.jeconfig.plugin.Dashboard.timeframe.ToolBarIntervalSelector;
import org.jevis.jeconfig.plugin.Dashboard.widget.Widget;
import org.jevis.jeconfig.plugin.Dashboard.wizzard.Wizard;
import org.jevis.jeconfig.tool.I18n;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class DashBoardToolbar extends ToolBar {

    private final ComboBox<JEVisObject> listAnalysesComboBox = new ComboBox();
    private final JEVisDataSource dataSource;
    private final DashBordPlugIn dashBordPlugIn;
    private double iconSize = 20;

    public DashBoardToolbar(JEVisDataSource dataSource, DashBordPlugIn dashBordPlugIn) {
        this.dataSource = dataSource;
        this.dashBordPlugIn = dashBordPlugIn;

        listAnalysesComboBox.setPrefWidth(300);
        listAnalysesComboBox.setMinWidth(300);

        try {
            JEVisClass sadaAnalyses = dataSource.getJEVisClass(DashBordPlugIn.CLASS_ANALYSIS);
            List<JEVisObject> allAnalisis = dataSource.getObjects(sadaAnalyses, false);
            listAnalysesComboBox.getItems().addAll(allAnalisis);
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        Callback<ListView<JEVisObject>, ListCell<JEVisObject>> cellFactory = new Callback<ListView<JEVisObject>, ListCell<JEVisObject>>() {
            @Override
            public ListCell<JEVisObject> call(ListView<JEVisObject> param) {
                final ListCell<JEVisObject> cell = new ListCell<JEVisObject>() {

//                    {
//                        super.setPrefWidth(300);
//                    }

                    @Override
                    protected void updateItem(JEVisObject item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && !empty) {
                            setText(item.getName());
                            setGraphic(null);
                            Tooltip tt = new Tooltip();
                            tt.setText("ID: " + item.getID());
                            setTooltip(tt);
                        }


                    }
                };

                return cell;
            }
        };

        listAnalysesComboBox.setCellFactory(cellFactory);
        listAnalysesComboBox.setButtonCell(cellFactory.call(null));
        listAnalysesComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            try {
                DashBordModel analysis = new DashBordModel(newValue);

                dashBordPlugIn.loadAnalysis(analysis);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }


    public void updateToolbar(final DashBordModel analyses) {
        Label analysisLabel = new Label(I18n.getInstance().getString("plugin.scada.analysis"));

        try {
            if (analyses != null) {
//                listAnalysesComboBox.getSelectionModel().select(analyses.getAnalysisObject());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        ToggleButton treeButton = new ToggleButton("", JEConfig.getImage("Data.png", iconSize, iconSize));
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(treeButton);

        ToggleButton settingsButton = new ToggleButton("", JEConfig.getImage("Service Manager.png", iconSize, iconSize));
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(settingsButton);

        ToggleButton save = new ToggleButton("", JEConfig.getImage("save.gif", iconSize, iconSize));
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(save);

        ToggleButton exportPDF = new ToggleButton("", JEConfig.getImage("pdf_32_32.png", iconSize, iconSize));
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(exportPDF);


        ToggleButton newButton = new ToggleButton("", JEConfig.getImage("1390343812_folder-open.png", iconSize, iconSize));
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(newButton);

        ToggleButton delete = new ToggleButton("", JEConfig.getImage("if_trash_(delete)_16x16_10030.gif", iconSize, iconSize));
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(delete);

        ToggleButton zoomIn = new ToggleButton("", JEConfig.getImage("zoomIn_32.png", iconSize, iconSize));
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(zoomIn);

        ToggleButton zoomOut = new ToggleButton("", JEConfig.getImage("zoomOut_32.png", iconSize, iconSize));
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(zoomOut);

        ToggleButton enlarge = new ToggleButton("", JEConfig.getImage("enlarge_32.png", iconSize, iconSize));
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(enlarge);


        final ImageView lockIcon = JEConfig.getImage("if_lock_blue_68757.png", iconSize, iconSize);
        final ImageView unlockIcon = JEConfig.getImage("if_lock-unlock_blue_68758.png", iconSize, iconSize);

        final ToggleButton unlockB = new ToggleButton("", lockIcon);
//        unlockB.setSelected(analyses.editProperty.get());
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(unlockB);
        analyses.editProperty.addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                if (newValue) {
                    unlockB.setGraphic(unlockIcon);
                } else {
                    unlockB.setGraphic(lockIcon);
                }
            }

        });

        exportPDF.setOnAction(event -> {
            dashBordPlugIn.toPDF();
        });

        save.setOnAction(event -> {

            if (analyses.isNew()) {
                NewAnalyseDialog newAnalyseDialog = new NewAnalyseDialog();
                try {

                    NewAnalyseDialog.Response response = newAnalyseDialog.show((Stage) this.getScene().getWindow(), dataSource);
                    if (response == NewAnalyseDialog.Response.YES) {
                        JEVisClass analisisDirClass = dataSource.getJEVisClass(DashBordPlugIn.CLASS_ANALYSIS_DIR);
                        List<JEVisObject> analisisDir = dataSource.getObjects(analisisDirClass, true);
                        JEVisClass analisisClass = dataSource.getJEVisClass(DashBordPlugIn.CLASS_ANALYSIS);


                        JEVisObject newObject = newAnalyseDialog.getParent().buildObject(newAnalyseDialog.getCreateName(), analisisClass);
                        newObject.commit();
                        analyses.save(newObject);
                        listAnalysesComboBox.getItems().add(newObject);
                        listAnalysesComboBox.getSelectionModel().select(newObject);

                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                analyses.save();
            }


        });

        unlockB.onActionProperty().addListener((observable, oldValue, newValue) -> {
            analyses.editProperty.setValue(!analyses.editProperty.getValue());
        });

        ImageView pauseIcon = JEConfig.getImage("pause_32.png", iconSize, iconSize);
        ImageView playIcon = JEConfig.getImage("play_32.png", iconSize, iconSize);

        ToggleButton runUpdateButton = new ToggleButton("", playIcon);
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(runUpdateButton);

        analyses.updateIsRunningProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                runUpdateButton.setGraphic(pauseIcon);
            } else {
                runUpdateButton.setGraphic(playIcon);
            }
        });

        runUpdateButton.setOnAction(event -> {
            analyses.updateIsRunningProperty.setValue(!analyses.updateIsRunningProperty.getValue());
        });

        ToggleButton backgroundButton = new ToggleButton("", JEConfig.getImage("if_32_171485.png", iconSize, iconSize));
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(backgroundButton);

        ToggleButton newWidgetButton = new ToggleButton("", JEConfig.getImage("Data.png", iconSize, iconSize));
        GlobalToolBar.changeBackgroundOnHoverUsingBinding(newWidgetButton);

        newWidgetButton.setOnAction(event -> {
            Wizard wizzard = new Wizard(JEConfig.getDataSource());
            Optional<Widget> newWidget = wizzard.show(null);

            if (newWidget.isPresent()) {
                dashBordPlugIn.addWidget(newWidget.get().getConfig());
            }

        });

        backgroundButton.setOnAction(event -> {


            FileChooser fileChooser = new FileChooser();
            fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Pictures", "*.png", "*.gif", "*.jpg", "*.bmp"));
            File newBackground = fileChooser.showOpenDialog(JEConfig.getStage());
            if (newBackground != null) {
                try {
                    BufferedImage bufferedImage = ImageIO.read(newBackground);
                    javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                    analyses.imageBoardBackground.setValue(fxImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        });

        settingsButton.setOnAction(event -> {
            analyses.openConfig();
        });

        unlockB.setOnAction(event -> {
            analyses.editProperty.setValue(!analyses.editProperty.getValue());
        });

        zoomIn.setOnAction(event -> {
            analyses.zoomIn();
        });

        zoomOut.setOnAction(event -> {
            analyses.zoomOut();
        });

        ToolBarIntervalSelector toolBarIntervalSelector = new ToolBarIntervalSelector(analyses, iconSize, new Interval(new DateTime(), new DateTime()));

        toolBarIntervalSelector.getIntervalProperty().addListener((observable, oldValue, newValue) -> {

            analyses.updateIsRunningProperty.setValue(false);
            analyses.dataPeriodProperty.setValue(newValue.toPeriod());
            analyses.displayedIntervalProperty.setValue(newValue);

        });

        Separator sep1 = new Separator();
        Separator sep2 = new Separator();
        Separator sep3 = new Separator();
        Separator sep4 = new Separator();


        newButton.setDisable(true);
        delete.setDisable(true);
        save.setDisable(true);
        exportPDF.setVisible(false);

        getItems().clear();
        getItems().addAll(
                listAnalysesComboBox
                , sep3, toolBarIntervalSelector
                , sep1, zoomOut, zoomIn
                , sep4, newButton, save, delete, newWidgetButton, settingsButton, backgroundButton, exportPDF
                , sep2, runUpdateButton, unlockB);


    }


}
