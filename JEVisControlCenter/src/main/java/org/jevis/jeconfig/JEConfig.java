/**
 * Copyright (C) 2009 - 2018 Envidatec GmbH <info@envidatec.com>
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
package org.jevis.jeconfig;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.log4j.BasicConfigurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisSample;
import org.jevis.commons.application.ApplicationInfo;
import org.jevis.commons.i18n.I18n;
import org.jevis.jeapi.ws.JEVisDataSourceWS;
import org.jevis.jeconfig.application.application.I18nWS;
import org.jevis.jeconfig.application.application.JavaVersionCheck;
import org.jevis.jeconfig.application.login.FXLogin;
import org.jevis.jeconfig.application.statusbar.Statusbar;
import org.jevis.jeconfig.application.tools.Holidays;
import org.jevis.jeconfig.dialog.HiddenConfig;
import org.jevis.jeconfig.tool.PatchNotesPage;
import org.jevis.jeconfig.tool.WelcomePage;
import org.joda.time.DateTime;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

/**
 * This is the main class of the JEConfig. The JEConfig is an JAVAFX programm,
 * the early version will need the MAVEN javafx 2.0 plugin to be build for java
 * 1.8
 *
 * @author Florian Simon <florian.simon@envidatec.com>
 */
public class JEConfig extends Application {

    /*
    TODO: Make the config into an singleton
     */
    private final static Configuration _config = new Configuration();
    private static final Logger logger = LogManager.getLogger(JEConfig.class);

    /**
     * Dangerous workaround to get the password to the ISOBrowser Plugin.
     */
    public static String userpassword;
    static ApplicationInfo PROGRAM_INFO = new ApplicationInfo("JEVis Control Center", JEConfig.class.getPackage().getImplementationVersion());//can be ignored
    private static Preferences pref = Preferences.userRoot().node("JEVis.JEConfig");
    private static Stage _primaryStage;
    private static JEVisDataSource _mainDS;
    private static PluginManager pluginManager;
    private static Statusbar statusBar = new Statusbar();
    private static ExecutorService taskExecutor = Executors.newFixedThreadPool(10);
    private TopMenu menu;

    public static boolean getExpert() {
        final Preferences prefExpert = Preferences.userRoot().node("JEVis.JEConfig.Expert");
        return prefExpert.getBoolean("show", false);
    }

    /**
     * Returns the last path the local user selected
     *
     * @return
     * @deprecated Will be moved into the Configuration -> user settings
     */
    public static File getLastPath() {
        File result;

//        if (OsUtils.isWindows()) {//Pref is not working under windows 8+
//            result = new File("/");
//        } else {
        final Preferences lastPath = Preferences.userRoot().node("JEVis.JEConfig");
        result = new File(lastPath.get("lastPath", System.getProperty("user.home")));
//        }

        if (result.canRead()) {
            if (result.isFile()) {
                logger.info("Is folder: " + result.getParentFile().getAbsoluteFile());
                return result.getParentFile();
            } else {
                return result;
            }
        } else {
            return new File(System.getProperty("user.home"));
        }
    }

    /**
     * Set the last path the user selected for an file operation
     *
     * @param file
     * @deprecated Will be moved into the Configuration -> user settings
     */
    public static void setLastPath(File file) {
        final Preferences lastPath = Preferences.userRoot().node("JEVis.JEConfig");
        lastPath.put("lastPath", file.getAbsolutePath());
    }

    /**
     * maximized the given stage
     *
     * @param primaryStage
     * @deprecated
     */
    public static void maximize(Stage primaryStage) {
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());
    }

    /**
     * Get the configuration for the app
     *
     * @return
     * @deprecated will be replaced by an singleton
     */
    public static Configuration getConfig() {
        return _config;
    }

    /**
     * Open an object in an plugin of choice
     * <p>
     * TODO: replace this with an generic function and datatype
     *
     * @param pluginName
     * @param object
     */
    public static void openObjectInPlugin(String pluginName, Object object) {
        if (pluginManager != null) {
            pluginManager.openInPlugin(pluginName, object);
        }
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Returns the main JEVis Datasource of this JEConfig Try not to use this
     * because it may disapear
     *
     * @return
     * @deprecated
     */
    public static JEVisDataSource getDataSource() {
        return _mainDS;
    }

    public static Stage getStage() {
        return _primaryStage;
    }

    public static Statusbar getStatusBar() {
        return statusBar;
    }

    /**
     * Return an common resource
     *
     * @param file
     * @return
     */
    public static String getResource(String file) {
        //        scene.getStylesheets().addAll(this.getClass().getResource("/org/jevis/jeconfig/css/main.css").toExternalForm());

//        logger.info("get Resouce: " + file);
        return JEConfig.class.getResource("/styles/" + file).toExternalForm();
//        return JEConfig.class.getResource("/org/jevis/jeconfig/css/" + file).toExternalForm();

    }

    /**
     * Fet an image out of the common resources
     *
     * @param icon
     * @return
     */
    public static Image getImage(String icon) {
        try {
            return new Image(JEConfig.class.getResourceAsStream("/icons/" + icon));
//            return new Image(JEConfig.class.getResourceAsStream("/org/jevis/jeconfig/image/" + icon));
        } catch (Exception ex) {
            logger.error("Could not load icon: " + "/icons/" + icon + ": ", ex);
            return new Image(JEConfig.class.getResourceAsStream("/icons/1393355905_image-missing.png"));
        }
    }

    public static ExecutorService executor() {
        return taskExecutor;
    }

    /**
     * Get an imge in the given size from the common
     *
     * @param icon
     * @param height
     * @param width
     * @return
     */
    public static ImageView getImage(String icon, double height, double width) {
        ImageView image = new ImageView(JEConfig.getImage(icon));
        image.fitHeightProperty().set(height);
        image.fitWidthProperty().set(width);
        return image;
    }

    /**
     * Inform the user the some precess is working
     *
     * @param working
     */
    public static void loadNotification(final boolean working) {
        Platform.runLater(() -> {
            if (working) {
                getStage().getScene().setCursor(Cursor.WAIT);
            } else {
                getStage().getScene().setCursor(Cursor.DEFAULT);
            }
        });

    }

    @Override
    public void init() throws Exception {
        super.init();
        BasicConfigurator.configure();//Load an default log4j config
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.ERROR);
        Parameters parameters = getParameters();
        _config.parseParameters(parameters);
//        PROGRAM_INFO.setName(I18n.getInstance().getString("app.name"));
        PROGRAM_INFO.addLibrary(org.jevis.commons.application.Info.INFO);
        PROGRAM_INFO.addLibrary(org.jevis.jeapi.ws.Info.INFO);

    }


    /**
     * Build an new JEConfig Login and main frame/stage
     *
     * @param primaryStage
     */
    private void initGUI(Stage primaryStage) {
        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });


        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            try {
                java.awt.Toolkit xToolkit = java.awt.Toolkit.getDefaultToolkit();
                Field awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName");
                awtAppClassNameField.setAccessible(true);
                awtAppClassNameField.set(xToolkit, "JEVis Control Center");

            } catch (Exception e) {
                // TODO
            }
        }

        final AnchorPane jeconfigRoot = new AnchorPane();

        Scene scene = new Scene(jeconfigRoot);
        primaryStage.setScene(scene);

        Date start = new Date();
        final FXLogin login = new FXLogin(primaryStage, getParameters(), PROGRAM_INFO);

        AnchorPane.setTopAnchor(jeconfigRoot, 0.0);
        AnchorPane.setRightAnchor(jeconfigRoot, 0.0);
        AnchorPane.setLeftAnchor(jeconfigRoot, 0.0);
        AnchorPane.setBottomAnchor(jeconfigRoot, 0.0);

        login.getLoginStatus().addListener((observable, oldValue, newValue) -> {
            if (newValue) {

                logger.debug("Start JEVis Control Center");
                _mainDS = login.getDataSource();

                JEConfig.userpassword = login.getUserPassword();
                I18n.getInstance().selectBundle(login.getSelectedLocale());
                Locale.setDefault(login.getSelectedLocale());
                I18nWS.setDataSource((JEVisDataSourceWS) _mainDS);
                I18nWS.getInstance().setLocale(login.getSelectedLocale());

                _config.setLocale(login.getSelectedLocale());

                try {
                    _mainDS.preload();
                    logger.error("done preloading");
//                    logger.error("-------test\n {}", _mainDS.getObject(9485l).getChildren());

                    Holidays.setDataSource(_mainDS);
                } catch (Exception ex) {
                    logger.error("Error while preloading datasource", ex);
                    ex.printStackTrace();
                }


                logger.error("start GUI");

                PROGRAM_INFO.setJEVisAPI(_mainDS.getInfo());
                PROGRAM_INFO.setName(I18n.getInstance().getString("app.name"));
                Platform.runLater(() -> {
                    primaryStage.setTitle(I18n.getInstance().getString("app.name"));
//                    try {
//                        java.awt.Toolkit xToolkit = java.awt.Toolkit.getDefaultToolkit();
//                        Field awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName");
//                        awtAppClassNameField.setAccessible(true);
//                        awtAppClassNameField.set(xToolkit, I18n.getInstance().getString("app.name"));
//                    } catch (NoSuchFieldException | IllegalAccessException e) {
//                        e.printStackTrace();
//                    }
                });

                ExecutorService exe = Executors.newSingleThreadExecutor();
                exe.submit(() -> {
                    try {
                        JEVisAttribute activities = getDataSource().getCurrentUser().getUserObject().getAttribute("Activities");
                        if (activities != null) {
                            JEVisSample log = activities.buildSample(new DateTime(), "Login: " + PROGRAM_INFO.getName() + " Version: " + PROGRAM_INFO.getVersion());
                            log.commit();
                        } else {
                            logger.warn("Missing activities attribute for user");
                        }


                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                pluginManager = new PluginManager(_mainDS);
                menu = new TopMenu();
                pluginManager.setMenuBar(menu);

                final KeyCombination saveCombo = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
                final KeyCombination deleteCombo = new KeyCodeCombination(KeyCode.DELETE);
                final KeyCombination reloadF5 = new KeyCodeCombination(KeyCode.F5);
                final KeyCombination newCombo = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
                final KeyCombination hiddenSettings = new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN);
                scene.setOnKeyPressed(ke -> {
//                    Platform.runLater(() -> pluginManager.getToolbar().requestFocus());//the most attribute will validate if the lose focus so we do
                    if (saveCombo.match(ke)) {
                        pluginManager.getSelectedPlugin().handleRequest(Constants.Plugin.Command.SAVE);
                        ke.consume();
                    } else if (reloadF5.match(ke)) {
                        pluginManager.getSelectedPlugin().handleRequest(Constants.Plugin.Command.RELOAD);
                        ke.consume();
                    } else if (deleteCombo.match(ke)) {
                        pluginManager.getSelectedPlugin().handleRequest(Constants.Plugin.Command.DELETE);
                        ke.consume();
                    } else if (newCombo.match(ke)) {
                        pluginManager.getSelectedPlugin().handleRequest(Constants.Plugin.Command.NEW);
                        ke.consume();
                    } else if (hiddenSettings.match(ke)) {
                        HiddenConfig.showHiddenConfig();
                        ke.consume();
                    }
                });

//                GlobalToolBar toolbar = new GlobalToolBar(pluginManager);
                try {
                    pluginManager.addPluginsByUserSetting(_mainDS.getCurrentUser());
                } catch (JEVisException jex) {
                    logger.error(jex);
                }

                BorderPane border = new BorderPane();
                VBox vbox = new VBox();
                vbox.setStyle("-fx-background-color: black;");
                vbox.getChildren().addAll(menu, pluginManager.getToolbar());
                border.setTop(vbox);
                border.setCenter(pluginManager.getView());

//                statusBar = new Statusbar(_mainDS);
                statusBar.setDataSource(_mainDS);
                statusBar.initView();

                border.setBottom(statusBar);

                //Disable GUI is StatusBar note an disconnect
                border.disableProperty().bind(statusBar.connectedProperty.not());

                Platform.runLater(() -> {

                    AnchorPane.setTopAnchor(border, 0.0);
                    AnchorPane.setRightAnchor(border, 0.0);
                    AnchorPane.setLeftAnchor(border, 0.0);
                    AnchorPane.setBottomAnchor(border, 0.0);

                    jeconfigRoot.getChildren().setAll(border);
                    try {
                        WelcomePage welcome = new WelcomePage();
                        welcome.show(primaryStage, _config.getWelcomeURL());
                    } catch (URISyntaxException ex) {
                        logger.fatal(ex);
                    }

                    PatchNotesPage patchNotesPage = new PatchNotesPage();
                    patchNotesPage.show(primaryStage);

                    logger.info("Time to start: {}ms", ((new Date()).getTime() - start.getTime()));
                });

            } else {
                System.exit(0);
            }

        });

        AnchorPane.setTopAnchor(login, 0.0);
        AnchorPane.setRightAnchor(login, 0.0);
        AnchorPane.setLeftAnchor(login, 0.0);
        AnchorPane.setBottomAnchor(login, 0.0);

        scene.getStylesheets().add("/styles/Styles.css");
        scene.getStylesheets().add("/styles/charts.css");
        primaryStage.getIcons().add(getImage("JEVisIconBlue.png"));
        primaryStage.setTitle("JEVis Control Center");

        primaryStage.setMaximized(true);
        primaryStage.show();

        jeconfigRoot.getChildren().setAll(login);

        primaryStage.onCloseRequestProperty().addListener((ov, t, t1) -> {
            try {
                logger.info("Disconnect");
                try {
                    JEVisAttribute activities = _mainDS.getCurrentUser().getUserObject().getAttribute("Activities");
                    JEVisSample log = activities.buildSample(new DateTime(), "Logout: " + PROGRAM_INFO.getName() + " Version: " + PROGRAM_INFO.getVersion());
                    log.commit();
                } catch (Exception ex) {
                    logger.error("Could not write logout to activities log:", ex);
                }

                _mainDS.disconnect();
            } catch (JEVisException ex) {
                logger.fatal(ex);
            }
        });


    }


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(PROGRAM_INFO.getName());
        JavaVersionCheck checkVersion = new JavaVersionCheck();
        if (!checkVersion.isVersionOK()) {
            System.exit(1);
        }

        _primaryStage = primaryStage;
        initGUI(primaryStage);
    }

    public static final class OsUtils {
        private static String OS = null;

        public static String getOsName() {
            if (OS == null) {
                OS = System.getProperty("os.name");
            }
            return OS;
        }

        public static boolean isWindows() {
            return getOsName().startsWith("Windows");
        }

        //TODO stuff for recognizing different os
//        public static boolean isUnix()
//        {
//            return false;
//        }
    }

//    /**
//     * Get the static list of preloader JEVisClasses
//     *
//     * @return
//     */
//    static public List<JEVisClass> getPreLodedClasses() {
//        return preLodedClasses;
//    }
//
//    /**
//     * Get the static list of all root objects for this user
//     *
//     * @return
//     */
//    static public List<JEVisObject> getPreLodedRootObjects() {
//        return preLodedRootObjects;
//    }


    public TopMenu getMenu() {
        return menu;
    }
}
