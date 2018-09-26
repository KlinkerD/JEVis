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
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisSample;
import org.jevis.api.JEVisUnit;
import org.jevis.commons.dataprocessing.*;
import org.jevis.commons.dataprocessing.Process;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Florian Simon <florian.simon@envidatec.com>
 */
public class MathFunction implements ProcessFunction {
    private static final Logger logger = LogManager.getLogger(MathFunction.class);
    private final String AVERAGE = "average";
    private final String MIN = "min";
    private final String MAX = "max";
    private final String MEDIAN = "median";

    public static final String NAME = "Math Processor";
    private final String mode;

    public MathFunction(String mode) {
        this.mode = mode;
    }

    @Override
    public void resetResult() {
    }

    @Override
    public List<JEVisSample> getResult(Process mainTask) {
        List<JEVisSample> result = new ArrayList<>();

        List<JEVisSample> allSamples = new ArrayList<>();
        for (Process task : mainTask.getSubProcesses()) {
            allSamples.addAll(task.getResult());
        }

        Boolean hasSamples = null;
        BigDecimal value = BigDecimal.ZERO;
        BigDecimal min = BigDecimal.valueOf(Double.MAX_VALUE);
        BigDecimal max = BigDecimal.valueOf(Double.MIN_VALUE);
        List<BigDecimal> listMedian = new ArrayList<>();
        JEVisUnit unit = null;
        DateTime dateTime = null;

        for (JEVisSample smp : allSamples) {
            try {
                BigDecimal currentValue = BigDecimal.valueOf(smp.getValueAsDouble());
                value.add(currentValue);
                min = BigDecimal.valueOf(Math.min(min.doubleValue(), currentValue.doubleValue()));
                max = BigDecimal.valueOf(Math.max(max.doubleValue(), currentValue.doubleValue()));
                listMedian.add(currentValue);
                if (hasSamples == null) hasSamples = true;
                if (unit == null) unit = smp.getUnit();
                if (dateTime == null) dateTime = smp.getTimestamp();
            } catch (JEVisException ex) {
                logger.fatal(ex);
            }
        }

        if (hasSamples) {
            if (mode.equals(AVERAGE)) {
                value = value.divide(BigDecimal.valueOf(allSamples.size()));
            } else if (mode.equals(MIN)) {
                value = min;
            } else if (mode.equals(MAX)) {
                value = max;
            } else if (mode.equals(MEDIAN)) {
                if (listMedian.size() > 1)
                    listMedian.sort(Comparator.naturalOrder());
                value = listMedian.get((listMedian.size() - 1) / 2);
            }
        }

        result.add(new VirtualSample(dateTime, value.doubleValue(), unit, mainTask.getJEVisDataSource(), new VirtualAttribute(null)));


        return result;

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
