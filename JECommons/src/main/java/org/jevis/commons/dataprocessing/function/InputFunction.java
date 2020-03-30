/**
 * Copyright (C) 2015 Envidatec GmbH <info@envidatec.com>
 * <p>
 * This file is part of JECommons.
 * <p>
 * JECommons is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation in version 3.
 * <p>
 * JECommons is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * JECommons. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * JECommons is part of the OpenJEVis project, further project information are
 * published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.commons.dataprocessing.function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.*;
import org.jevis.commons.dataprocessing.Process;
import org.jevis.commons.dataprocessing.*;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.jevis.commons.constants.NoteConstants.User.USER_VALUE;

/**
 * @author Florian Simon <florian.simon@envidatec.com>
 */
public class InputFunction implements ProcessFunction {
    private static final Logger logger = LogManager.getLogger(InputFunction.class);

    public final static String NAME = "Input";
    public final static String OBJECT_ID = "object-id";
    public final static String ATTRIBUTE_ID = "attribute-id";
    private List<JEVisSample> _result = null;

    public InputFunction() {
    }

    public InputFunction(List<JEVisSample> resultSamples) {
        _result = resultSamples;
    }

    @Override
    public void resetResult() {
        _result = null;
    }

    @Override
    public List<JEVisSample> getResult(Process task) {
        if (_result != null) {
            return _result;
        } else {
            _result = new ArrayList<>();

            JEVisObject object = null;
            if (ProcessOptions.ContainsOption(task, OBJECT_ID)) {
                long oid = Long.parseLong((ProcessOptions.GetLatestOption(task, OBJECT_ID, new BasicProcessOption(OBJECT_ID, "")).getValue()));
                try {
                    object = task.getJEVisDataSource().getObject(oid);
                } catch (JEVisException ex) {
                    logger.fatal(ex);
                }
            } else if (task.getObject() != null) {
                try {
                    object = task.getObject().getParents().get(0);//TODO make save
                } catch (JEVisException ex) {
                    logger.error(ex);
                }
            }

            if (object != null && ProcessOptions.ContainsOption(task, ATTRIBUTE_ID)) {

                try {
                    logger.info("Parent object: " + object);
//                    long oid = Long.valueOf(task.getOptions().get(OBJECT_ID));
//                    JEVisObject object = task.getJEVisDataSource().getObject(oid);

                    JEVisAttribute att = object.getAttribute(ProcessOptions.GetLatestOption(task, ATTRIBUTE_ID, new BasicProcessOption(ATTRIBUTE_ID, "")).getValue());

                    JEVisObject correspondingUserDataObject = null;
                    boolean foundUserDataObject = false;
                    final JEVisClass userDataClass = object.getDataSource().getJEVisClass("User Data");
                    for (JEVisObject parent : object.getParents()) {
                        for (JEVisObject child : parent.getChildren()) {
                            if (child.getJEVisClass().equals(userDataClass)) {
                                correspondingUserDataObject = child;
                                foundUserDataObject = true;
                                break;
                            }
                        }
                    }

                    DateTime[] startEnd = ProcessOptions.getStartAndEnd(task);
                    logger.info("start: " + startEnd[0] + " end: " + startEnd[1]);

                    if (foundUserDataObject) {
                        SortedMap<DateTime, JEVisSample> map = new TreeMap<>();
                        for (JEVisSample jeVisSample : att.getSamples(startEnd[0], startEnd[1])) {
                            map.put(jeVisSample.getTimestamp(), jeVisSample);
                        }

                        JEVisAttribute userDataValueAttribute = correspondingUserDataObject.getAttribute("Value");
                        List<JEVisSample> userValues = userDataValueAttribute.getSamples(startEnd[0], startEnd[1]);

                        for (JEVisSample userValue : userValues) {
                            String note = map.get(userValue.getTimestamp()).getNote();
                            VirtualSample virtualSample = new VirtualSample(userValue.getTimestamp(), userValue.getValueAsDouble(), att.getDisplayUnit());
                            virtualSample.setNote(note + "," + USER_VALUE);
                            virtualSample.setAttribute(map.get(userValue.getTimestamp()).getAttribute());

                            map.remove(userValue.getTimestamp());
                            map.put(virtualSample.getTimestamp(), virtualSample);
                        }

                        _result = new ArrayList<>(map.values());
                    } else {
                        _result = att.getSamples(startEnd[0], startEnd[1]);
                    }

                    logger.info("Input result: " + _result.size());
                } catch (JEVisException ex) {
                    logger.fatal(ex);
                }
            } else {
                logger.warn("Missing options " + OBJECT_ID + " and " + ATTRIBUTE_ID);
            }
        }
        return _result;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ProcessOption> getAvailableOptions() {
        List<ProcessOption> options = new ArrayList<>();

        options.add(new BasicProcessOption("Object"));
        options.add(new BasicProcessOption("Attribute"));
        options.add(new BasicProcessOption("Workflow"));

        return options;
    }
}
