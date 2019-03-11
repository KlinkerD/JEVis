package org.jevis.jestatus;

import org.apache.logging.log4j.LogManager;
import org.jevis.api.*;
import org.jevis.commons.alarm.AlarmTable;
import org.jevis.commons.object.plugin.TargetHelper;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.*;

public class CalculationTable extends AlarmTable {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(CalculationTable.class);
    private final JEVisDataSource ds;
    private final Alarm alarm;
    private final List<JEVisObject> dataServerObjects;

    public CalculationTable(JEVisDataSource ds, Alarm alarm, List<JEVisObject> dataServerObjects) {
        this.ds = ds;
        this.alarm = alarm;
        this.dataServerObjects = dataServerObjects;

        try {
            createTableString();
        } catch (JEVisException e) {
            logger.error("Could not initialize.");
        }
    }

    private void createTableString() throws JEVisException {
        StringBuilder sb = new StringBuilder();
        sb.append("<br>");
        sb.append("<br>");

        sb.append("<h2>Calculations</h2>");

        /**
         * Start of Table
         */
        sb.append("<table style=\"");
        sb.append(tableCSS);
        sb.append("\" border=\"1\" >");
        sb.append("<tr style=\"");
        sb.append(headerCSS);
        sb.append("\" >");
        sb.append("    <th>Organisation</th>");
        sb.append("    <th>Building</th>");
        sb.append("    <th>Calculation</th>");
        sb.append("    <th>Result Target Data Point</th>");
        sb.append("    <th>Last Time Stamp</th>");
        sb.append("  </tr>");//border=\"0\"


        List<JEVisObject> calcObjects = getCalcObjects();

        Map<JEVisObject, JEVisObject> calcAndTarget = new HashMap<>();
        JEVisClass outputClass = ds.getJEVisClass("Output");

        for (JEVisObject calcObject : calcObjects) {
            List<JEVisObject> results = new ArrayList<>(calcObject.getChildren(outputClass, true));
            if (!results.isEmpty()) {
                calcAndTarget.put(calcObject, results.get(0));
            }
        }

        DateTime now = new DateTime();
        DateTime ignoreTS = now.minus(Period.hours(alarm.getIgnoreOld()));
        DateTime limit = now.minus(Period.hours(alarm.getTimeLimit()));
        Map<JEVisObject, JEVisObject> calcAndResult = new HashMap<>();
        List<JEVisObject> outOfBounds = new ArrayList<>();

        for (JEVisObject calculation : calcObjects) {
            JEVisObject result = calcAndTarget.get(calculation);
            if (result != null) {
                JEVisAttribute lastAtt = result.getAttribute("Output");
                if (lastAtt != null) {
                    JEVisSample lastSampleOutput = lastAtt.getLatestSample();
                    TargetHelper th = null;
                    if (lastSampleOutput != null) {
                        th = new TargetHelper(ds, lastSampleOutput.getValueAsString());
                        JEVisObject target = null;
                        if (!th.getObject().isEmpty()) {
                            target = th.getObject().get(0);
                        }
                        if (target != null) {
                            getListCheckedData().add(target);

                            calcAndResult.put(calculation, target);

                            JEVisAttribute resultAtt = target.getAttribute("Value");
                            if (resultAtt != null) {
                                if (resultAtt.hasSample()) {
                                    JEVisSample lastSample = resultAtt.getLatestSample();
                                    if (lastSample != null) {
                                        if (lastSample.getTimestamp().isBefore(limit) && lastSample.getTimestamp().isAfter(ignoreTS)) {
                                            outOfBounds.add(calculation);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Map<JEVisObject, JEVisObject> allInputs = new HashMap<>();
        JEVisClass inputClass = ds.getJEVisClass("Input");
        JEVisClass rawDataClass = ds.getJEVisClass("Data");
        JEVisClass cleanDataClass = ds.getJEVisClass("Clean Data");
        for (JEVisObject calcObject : outOfBounds) {
            for (JEVisObject input : calcObject.getChildren(inputClass, true)) {
                JEVisAttribute lastAtt = input.getAttribute("Input Data");
                if (lastAtt != null) {
                    JEVisSample lastSampleOutput = lastAtt.getLatestSample();
                    TargetHelper th = null;
                    if (lastSampleOutput != null) {
                        th = new TargetHelper(ds, lastSampleOutput.getValueAsString());
                        JEVisObject target = null;
                        if (!th.getObject().isEmpty()) {
                            target = th.getObject().get(0);
                        }
                        if (target != null) {
                            if (target.getJEVisClass().equals(rawDataClass)) {
                                allInputs.put(target, calcObject);
                            } else if (target.getJEVisClass().equals(cleanDataClass)) {
                                for (JEVisObject parent : target.getParents()) {
                                    allInputs.put(parent, calcObject);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        for (JEVisObject dataServerObject : dataServerObjects) {
            if (allInputs.containsKey(dataServerObject)) {
                outOfBounds.remove(allInputs.get(dataServerObject));
            }
        }

        outOfBounds.sort(new Comparator<JEVisObject>() {
            @Override
            public int compare(JEVisObject o1, JEVisObject o2) {
                DateTime o1ts = null;
                try {
                    JEVisObject o1tar = calcAndResult.get(o1);
                    if (o1tar != null) {
                        JEVisAttribute o1att = o1tar.getAttribute("Value");
                        if (o1att != null) {
                            o1ts = o1att.getTimestampFromLastSample();
                        }
                    }
                } catch (JEVisException e) {
                    e.printStackTrace();
                }
                DateTime o2ts = null;
                try {
                    JEVisObject o2tar = calcAndResult.get(o2);
                    if (o2tar != null) {
                        JEVisAttribute o2att = o2tar.getAttribute("Value");
                        if (o2att != null) {
                            o2ts = o2att.getTimestampFromLastSample();
                        }
                    }
                } catch (JEVisException e) {
                    e.printStackTrace();
                }

                if ((o1ts != null && o2ts != null && o1ts.isBefore(o2ts))) return -1;
                else if ((o1ts != null && o2ts != null && o1ts.isAfter(o2ts))) return 1;
                else return 0;
            }
        });

        JEVisClass organizationClass = ds.getJEVisClass("Organization");
        JEVisClass buildingClass = ds.getJEVisClass("Monitored Object");

        boolean odd = false;
        for (JEVisObject currentCalculation : outOfBounds) {
            String css = rowCss;
            if (odd) {
                css += highlight;
            }
            odd = !odd;

            String name = currentCalculation.getName() + ":" + currentCalculation.getID().toString();
            String nameResult = "";
            JEVisObject resultObject = null;
            try {
                resultObject = calcAndResult.get(currentCalculation);
            } catch (Exception e) {
            }
            if (resultObject != null) nameResult = resultObject.getName() + ":" + resultObject.getID().toString();

            sb.append("<tr>");
            /**
             * Organisation Column
             */
            sb.append("<td style=\"");
            sb.append(css);
            sb.append("\">");
            sb.append(getParentName(currentCalculation, organizationClass));
            sb.append("</td>");
            /**
             * Building Column
             */
            sb.append("<td style=\"");
            sb.append(css);
            sb.append("\">");
            sb.append(getParentName(currentCalculation, buildingClass));
            sb.append("</td>");
            /**
             * Calculation
             */
            sb.append("<td style=\"");
            sb.append(css);
            sb.append("\">");
            sb.append(name);
            sb.append("</td>");
            /**
             * Result Target Data Point
             */
            sb.append("<td style=\"");
            sb.append(css);
            sb.append("\">");
            sb.append(nameResult);
            sb.append("</td>");
            /**
             * Last Time Stamp
             */
            sb.append("<td style=\"");
            sb.append(css);
            sb.append("\">");
            if (resultObject != null) {
                JEVisAttribute resultAtt = resultObject.getAttribute("Value");
                if (resultAtt != null) {
                    if (resultAtt.hasSample()) {
                        sb.append(dtf.print(resultAtt.getLatestSample().getTimestamp()));
                    }
                }
            }
            sb.append("</td>");

            sb.append("</tr>");
        }

        sb.append("</tr>");
        sb.append("</tr>");
        sb.append("</table>");
        sb.append("<br>");
        sb.append("<br>");

        setTableString(sb.toString());
    }

    private List<JEVisObject> getCalcObjects() throws JEVisException {
        JEVisClass calculationClass = ds.getJEVisClass("Calculation");
        return new ArrayList<>(ds.getObjects(calculationClass, false));
    }
}
