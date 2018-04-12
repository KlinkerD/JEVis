/**
 * Copyright (C) 2014 Envidatec GmbH <info@envidatec.com>
 *
 * This file is part of JEApplication.
 *
 * JEApplication is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation in version 3.
 *
 * JEApplication is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * JEApplication. If not, see <http://www.gnu.org/licenses/>.
 *
 * JEApplication is part of the OpenJEVis project, further project information
 * are published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.application.dialog;

import java.awt.Desktop;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jevis.application.resource.ResourceLoader;
import org.jevis.commons.application.ApplicationInfo;

/**
 *
 * @author fs
 */
public class ExceptionDialog {

    //https://www.iconfinder.com/icons/68795/blue_question_icon#size=64
    public static String ICON_WARNING = "1401136217_exclamation-diamond_red.png";

    private static String MAIL_TO = "info@envidatec.com";
    private static String MAIL_SUBJECT = "Error Report";

    public static enum Response {

        RETRY, CANCEL
    };

    private Response response = Response.CANCEL;

    public Response show(Stage owner, String title, String titleLong, Exception ex, ApplicationInfo info) {
        return show(owner, title, titleLong, ex.toString(), ex, info);
    }

    public Response show(Stage owner, String title, String titleLong, String message, final Exception ex, final ApplicationInfo info) {
        final Stage stage = new Stage();

        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);

//        BorderPane root = new BorderPane();
        VBox root = new VBox();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setWidth(500);
        stage.setHeight(250);
        stage.initStyle(StageStyle.UTILITY);
        stage.setResizable(false);

        BorderPane header = new BorderPane();
        header.setStyle("-fx-background-color: linear-gradient(#e2e2e2,#eeeeee);");
        header.setPadding(new Insets(10, 10, 10, 10));

        Label topTitle = new Label(titleLong);
        topTitle.setTextFill(Color.web("#0076a3"));
        topTitle.setFont(Font.font("Cambria", 25));

        ImageView imageView = ResourceLoader.getImage(ICON_WARNING, 65, 65);

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

        Button ok = new Button("OK");
        ok.setDefaultButton(true);

        Button cancel = new Button("Cancel");
        cancel.setCancelButton(true);

//        Button exit = new Button("Exit");
        Button send = new Button("Report");

        Button details = new Button("Details");
        Button copyAll = new Button("Copy all");

        buttonPanel.getChildren().addAll(details, copyAll, send, ok, cancel);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);
        buttonPanel.setPadding(new Insets(10, 10, 10, 10));
        buttonPanel.setSpacing(10);
        buttonPanel.setMaxHeight(25);

        HBox messagePanel = new HBox();
        messagePanel.setPadding(new Insets(30, 30, 30, 30));

        Label mewssage = new Label(message);
        messagePanel.getChildren().add(mewssage);
        mewssage.setWrapText(true);
        mewssage.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator(Orientation.HORIZONTAL);
        sep.setMinHeight(10);

        root.getChildren().addAll(header, new Separator(Orientation.HORIZONTAL), messagePanel, buttonPanel);
        VBox.setVgrow(messagePanel, Priority.ALWAYS);
        VBox.setVgrow(buttonPanel, Priority.NEVER);
        VBox.setVgrow(header, Priority.NEVER);

        ok.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
//                System.out.println("Size: h:" + stage.getHeight() + " w:" + stage.getWidth());
                stage.close();
//                isOK.setValue(true);
                response = Response.CANCEL;

            }
        });

        cancel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
//                stage.close();
//                response = Response.CANCEL;
                System.exit(1);

            }
        });

        details.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                showDetails(stage, ex, info);
            }
        });

//        exit.setOnAction(new EventHandler<ActionEvent>() {
//
//            @Override
//            public void handle(ActionEvent t) {
//                System.exit(-1);
//            }
//        });
        send.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                report(getMessage(info, ex));
            }
        });

        copyAll.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                toClipbord(info, ex);
            }
        });

        stage.showAndWait();

        return response;
    }

    private void showDetails(Stage owner, final Exception ex, final ApplicationInfo pInfo) {
        final Stage stage = new Stage();

        final BooleanProperty isOK = new SimpleBooleanProperty(false);

        stage.setTitle("Details");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);

//        BorderPane root = new BorderPane();
        VBox root = new VBox();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setWidth(600);
        stage.setHeight(350);
        stage.initStyle(StageStyle.UTILITY);
        stage.setResizable(false);

        HBox infoBox = new HBox();
        TextArea info = new TextArea(exceptionToString(ex));

        info.setWrapText(false);
        info.setPrefColumnCount(50);
        info.setPrefRowCount(20);
        infoBox.getChildren().add(info);

        HBox buttonPanel = new HBox();

        Button ok = new Button("Close");
        ok.setCancelButton(true);

        Button exit = new Button("Exit");
        Button send = new Button("Report");

        send.setDefaultButton(true);

        Button copyAll = new Button("Copy All");

        buttonPanel.getChildren().addAll(copyAll, send, ok, exit);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);
        buttonPanel.setPadding(new Insets(10, 10, 10, 10));
        buttonPanel.setSpacing(10);
        buttonPanel.setMaxHeight(25);

        root.getChildren().addAll(infoBox, buttonPanel);

        ok.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                stage.close();
            }
        });

        send.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                report(getMessage(pInfo, ex));
            }
        });

        copyAll.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                toClipbord(pInfo, ex);
            }
        });

        exit.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                System.exit(1);
            }
        });

        stage.show();
    }

    private void toClipbord(ApplicationInfo info, Exception ex) {
        Clipboard clip = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();

        content.putString(getMessage(info, ex));
        clip.setContent(content);
    }

    private String exceptionToString(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

    private void report(String message) {
        String uri = String.format("mailto:%s?subject=%s&body=%s", MAIL_TO, MAIL_SUBJECT, message);

        uri = uri.replaceAll(" ", "%20");
        uri = uri.replaceAll("\t", "%20");
        uri = uri.replaceAll("\n", "%0D%0A");

        System.out.println("Send :\n" + uri);
        try {
//            URI open = new URI(uri);

            if (Desktop.isDesktopSupported()) {
                System.out.println("Desktop is supportet");
                Desktop.getDesktop().mail(new URI(uri));
            } else {
                //TODO: maybe disable send button if not suppotret at all
                System.out.println("Desktop is not Supportet");
                if (System.getProperty("os.name").equals("Linux")) {
                    System.out.println("is limux using xdg-open");
                    Runtime.getRuntime().exec("xdg-open " + uri);
                } else if (System.getProperty("os.name").equals("Windows")) {
                    Runtime.getRuntime().exec(uri);//TODo check if this is right
                } else {
                    //TODO: implement MAC and show also messeage to user
                    System.out.println("Cannot send");
                }
            }

            //does not work an linux
            //
        } catch (Exception ex1) {
            Logger.getLogger(ExceptionDialog.class.getName()).log(Level.SEVERE, null, ex1);
        }
    }

    private String getMessage(final ApplicationInfo info, final Exception ex) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String message = String.format("%s\nDate: %s\nOS: %s\nJAVA: %s\nException:\n\n%s",
                info.toString(),
                sdf.format(new Date()),
                System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"),
                System.getProperty("java.vendor") + " " + System.getProperty("java.version"),
                exceptionToString(ex)
        );

        return message;

    }

}
