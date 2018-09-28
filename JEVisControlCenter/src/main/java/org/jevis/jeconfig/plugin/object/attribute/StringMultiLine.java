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
package org.jevis.jeconfig.plugin.object.attribute;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisConstants;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisSample;
import org.jevis.jeconfig.JEConfig;
import org.jevis.jeconfig.plugin.object.extension.GenericAttributeExtension;
import org.joda.time.DateTime;


/**
 *
 * @author Florian Simon <florian.simon@envidatec.com>
 */
public class StringMultiLine implements AttributeEditor {
    private static final Logger logger = LogManager.getLogger(StringMultiLine.class);

    private HBox box = new HBox();
    public JEVisAttribute _attribute;
    private TextArea _field;
    private JEVisSample _newSample;
    private JEVisSample _lastSample;
    private final BooleanProperty _changed = new SimpleBooleanProperty(false);
    private boolean _readOnly = true;

    public StringMultiLine(JEVisAttribute att) {
        _attribute = att;
    }

    @Override
    public boolean hasChanged() {
//        logger.info(_attribute.getName() + " changed: " + _hasChanged);
        return _changed.getValue();
    }

    @Override
    public BooleanProperty getValueChangedProperty() {
        return _changed;
    }

    @Override
    public void setReadOnly(boolean canRead) {
        _readOnly = canRead;
    }

    //    @Override
//    public void setAttribute(JEVisAttribute att) {
//        _attribute = att;
//    }
    @Override
    public void commit() throws JEVisException {
        if (hasChanged() && _newSample != null) {

            //TODO: check if tpye is ok, maybe better at imput time
            _newSample.commit();
        }
    }

    @Override
    public Node getEditor() {
        try {
            buildEditor();
        } catch (Exception ex) {

        }

        return box;
//        return _field;
    }

    private void buildEditor() throws JEVisException {
        if (_field == null) {
            _field = new TextArea();
            _field.setPrefWidth(GenericAttributeExtension.editorWhith.doubleValue());
//            _field.setPrefWidth(500);//TODO: remove this workaround
            _field.setPrefRowCount(10);
            _field.setWrapText(true);
            _field.setDisable(_readOnly);

            if (_attribute.getLatestSample() != null) {
                _field.setText(_attribute.getLatestSample().getValueAsString());
                _lastSample = _attribute.getLatestSample();
            } else {
                _field.setText("");
            }

            _field.setOnKeyReleased(new EventHandler<KeyEvent>() {

                @Override
                public void handle(KeyEvent t) {
                    try {
                        if (_lastSample == null) {
                            _newSample = _attribute.buildSample(new DateTime(), _field.getText());
                            _changed.setValue(true);
                        } else if (!_lastSample.getValueAsString().equals(_field.getText())) {
                            _changed.setValue(true);
                            _newSample = _attribute.buildSample(new DateTime(), _field.getText());
                        } else if (_lastSample.getValueAsString().equals(_field.getText())) {
                            _changed.setValue(false);
                        }
                    } catch (JEVisException ex) {
                        logger.fatal(ex);
                    }

                }
            });

            _field.setId("attributelabel");

            if (_attribute.getType().getDescription() != null && !_attribute.getType().getDescription().isEmpty()) {
                Tooltip tooltip = new Tooltip();
                try {
                    tooltip.setText(_attribute.getType().getDescription());
                    tooltip.setGraphic(JEConfig.getImage("1393862576_info_blue.png", 30, 30));
                    _field.setTooltip(tooltip);
                } catch (JEVisException ex) {
                    logger.fatal(ex);
                }
            }

            box.getChildren().add(_field);
            HBox.setHgrow(_field, Priority.ALWAYS);

            try {
                if (_attribute.getType().getValidity() == JEVisConstants.Validity.AT_DATE) {
                    Button chartView = new Button();
                    chartView.setGraphic(JEConfig.getImage("1394566386_Graph.png", 20, 20));
                    chartView.setStyle("-fx-padding: 0 2 0 2;-fx-background-insets: 0;-fx-background-radius: 0;-fx-background-color: transparent;");

                    chartView.setMaxHeight(_field.getHeight());
                    chartView.setMaxWidth(20);

                    box.getChildren().add(chartView);
                    HBox.setHgrow(chartView, Priority.NEVER);

                }
            } catch (Exception ex) {
                logger.fatal(ex);
            }

        }
    }

    @Override
    public JEVisAttribute getAttribute() {
        return _attribute;
    }

    @Override
    public boolean isValid() {
        //TODO: implement validation
        return true;
    }
}