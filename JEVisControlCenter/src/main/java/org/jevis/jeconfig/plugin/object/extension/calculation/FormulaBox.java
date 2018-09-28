package org.jevis.jeconfig.plugin.object.extension.calculation;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import net.sourceforge.jeval.Evaluator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.*;
import org.jevis.application.dialog.SelectTargetDialog2;
import org.jevis.application.jevistree.UserSelection;
import org.jevis.application.tools.CalculationNameFormater;
import org.jevis.commons.object.plugin.TargetHelper;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.tool.I18n;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class FormulaBox extends HBox {

    private static final Logger logger = LogManager.getLogger(FormulaBox.class);
    //    TextArea textArea = new TextArea();
    TextArea textArea = new TextArea();
    Label errorArea = new Label();
    private List<String> variables = new ArrayList<>();
    private JEVisObject calcObj;
    private Button outputButton;

    public FormulaBox() {
        super();
        setSpacing(5);

//        textArea.setPrefHeight(200);
//        errorArea.setPrefWidth(200);
        errorArea.setTextFill(Color.FIREBRICK);
        textArea.setWrapText(true);
        getChildren().addAll(textArea);
        HBox.setHgrow(textArea, Priority.ALWAYS);
        textArea.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
//                eval();
            }
        });

    }


    public String getExpression() {
        return textArea.getText();
    }

    public void setExpression(String expression) {
        textArea.setText(expression);
    }

    public void backspaceExpression() {
        int caret = textArea.getCaretPosition();
        String beforeText = textArea.getText().substring(0, caret - 1);
        Platform.runLater(() -> {
            textArea.setText(beforeText);
            textArea.positionCaret(caret - 1);

        });

    }

    public void addExpression(String expression) {
        if (expression != null && !expression.isEmpty()) {
            int caret = textArea.getCaretPosition();
            String beforeText = textArea.getText().substring(0, caret);
            String afterText = textArea.getText().substring(caret);
            String newText = beforeText + expression + afterText;
            Platform.runLater(() -> {
                textArea.setText(newText);
                textArea.positionCaret(caret + expression.length());
                textArea.selectPositionCaret(caret + expression.length());

            });
        }

    }

    public void eval() {
        Evaluator eval = new Evaluator();
        try {
            eval.parse(textArea.getText());

            for (String var : variables) {
                eval.putVariable(var, "1");
            }

            String value = eval.evaluate();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Erfolg");
            alert.setHeaderText(null);
            alert.setContentText("Keinen Fehler gefunden.");

            alert.showAndWait();

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText(getErrorMessage(ex));
            alert.showAndWait();


            ex.printStackTrace();
        }

    }

    private String getErrorMessage(Exception ex) {
        String message = "Unknows error";
        try {
            message = ex.getCause().toString();
        } catch (NullPointerException np) {
            message = ex.getMessage();
        }


        return message;
    }

    public void updateVariables() throws JEVisException {
        JEVisClass input = calcObj.getDataSource().getJEVisClass("Input");
        for (JEVisObject inpuObject : calcObj.getChildren()) {
            try {
                JEVisAttribute id = inpuObject.getAttribute("Identifier");
                if (id != null) {
                    JEVisSample value = id.getLatestSample();
                    if (value != null && !value.getValueAsString().isEmpty()) {
                        variables.add(value.getValueAsString());
                    }
                }
            } catch (Exception inputEx) {
                inputEx.printStackTrace();
            }
        }
    }

    public void setOutputButton(Button butttonOutput) {
        outputButton = butttonOutput;
        try {
            JEVisClass outputClass = this.calcObj.getDataSource().getJEVisClass("Output");
            List<JEVisObject> outputs = this.calcObj.getChildren(outputClass, true);
            if (!outputs.isEmpty()) {//there can only be one output
                JEVisObject outputObj = outputs.get(0);
                outputButton.setText(outputObj.getName());
                Tooltip tt = new Tooltip();
                tt.setText("ID: " + outputObj.getID());
                outputButton.setTooltip(tt);
            } else {
                outputButton.setText("Select Output");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        logger.info("Button.text: " + butttonOutput.getText());
        outputButton.textProperty().addListener((observable, oldValue, newValue) -> {
            logger.info("Button text changed: " + oldValue + " new: " + newValue);
        });
    }

    public void setOnOutputAction() {

        try {
            JEVisClass outputClass = this.calcObj.getDataSource().getJEVisClass("Output");
            List<JEVisObject> outputs = this.calcObj.getChildren(outputClass, true);
            List<UserSelection> openList = new ArrayList<>();
            JEVisObject outputObj = null;

            if (!outputs.isEmpty()) {//there can only be one output
                outputObj = outputs.get(0);
                JEVisAttribute targetAttribute = outputObj.getAttribute("Output");
                JEVisSample targetSample = targetAttribute.getLatestSample();

                if (targetSample != null) {
                    TargetHelper th = new TargetHelper(this.calcObj.getDataSource(), targetSample.getValueAsString());
                    openList.add(new UserSelection(UserSelection.SelectionType.Attribute, th.getAttribute(), null, null));
                }
            }

            SelectTargetDialog2 selectionDialog = new SelectTargetDialog2();


            if (selectionDialog.show(
                    JEConfig.getStage(),
                    this.calcObj.getDataSource(),
                    I18n.getInstance().getString("plugin.object.attribute.target.selection"),
                    openList,
                    SelectTargetDialog2.MODE.ATTRIBUTE
            ) == SelectTargetDialog2.Response.OK) {
                for (UserSelection us : selectionDialog.getUserSelection()) {

                    TargetHelper th = new TargetHelper(this.calcObj.getDataSource(), us.getSelectedObject(), us.getSelectedAttribute());

                    if (th.isValid() && th.targetAccessable()) {

                        JEVisAttribute targetAtt = null;

                        if (outputObj != null) {
                            targetAtt = outputObj.getAttribute("Output");

                        } else {
                            JEVisObject outputObject = this.calcObj.buildObject(CalculationNameFormater.crateVarName(targetAtt), outputClass);
                            outputObject.commit();
                            targetAtt = outputObject.getAttribute("Output");
                        }

                        JEVisSample newSample = targetAtt.buildSample(new DateTime(), th.getSourceString());
                        newSample.commit();
                        outputButton.setText(th.getObject().getName() + "." + th.getAttribute().getName());

                    }
                }

            }


        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    public void setCalculation(JEVisObject calcObj) {
        this.calcObj = calcObj;
        try {
            JEVisAttribute expression = calcObj.getAttribute("Expression");

            JEVisSample expVal = expression.getLatestSample();
            if (expVal != null) {

                textArea.setText(expVal.getValueAsString());
            }
            updateVariables();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


}