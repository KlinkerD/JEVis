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
package org.jevis.commons.database;

import org.apache.logging.log4j.LogManager;
import org.jevis.api.*;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

/**
 * @author broder
 */
public class SampleHandler {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(SampleHandler.class);

    //    private JEVisObject object;
//    public SampleHandler(JEVisObject object) {
//        this.object = object;
//    }
    public SampleHandler() {
    }

    public Period getInputSampleRate(JEVisObject object, String attributeName) {
        Period period = null;
        try {
            period = object.getAttribute(attributeName).getInputSampleRate();
        } catch (JEVisException ex) {
            logger.error(ex);
        }
        return period;
    }

    public Boolean getLastSample(JEVisObject object, String attributeName, boolean defaultValue) {
        boolean lastBoolean = defaultValue;
        try {
            JEVisAttribute attribute = object.getAttribute(attributeName);
            if (attribute != null) {
                JEVisSample lastSample = attribute.getLatestSample();
                if (lastSample != null) {
                    lastBoolean = getValue(lastSample, defaultValue);
                }
            }
        } catch (JEVisException ex) {
            logger.error(ex);
        }
        return lastBoolean;
    }

    public Double getLastSample(JEVisObject object, String attributeName, Double defaultValue) {
        Double lastValue = defaultValue;
        try {
            JEVisAttribute attribute = object.getAttribute(attributeName);
            if (attribute != null) {
                JEVisSample lastSample = attribute.getLatestSample();
                if (lastSample != null) {
                    lastValue = getValue(lastSample, defaultValue);
                }
            }
        } catch (JEVisException ex) {
            logger.error(ex);
        }
        return lastValue;
    }

    public String getLastSample(JEVisObject object, String attributeName, String defaultValue) {
        String lastString = defaultValue;
        try {
            JEVisAttribute attribute = object.getAttribute(attributeName);
            if (attribute != null) {
                JEVisSample lastSample = attribute.getLatestSample();
                if (lastSample != null) {
                    lastString = getValue(lastSample, defaultValue);
                }
            }
        } catch (JEVisException ex) {
            logger.error(ex);
        }
        return lastString;
    }

    public Long getLastSample(JEVisObject object, String attributeName, Long defaultValue) {
        Long lastValue = defaultValue;
        try {
            JEVisAttribute attribute = object.getAttribute(attributeName);
            if (attribute != null) {
                JEVisSample lastSample = attribute.getLatestSample();
                if (lastSample != null) {
                    lastValue = getValue(lastSample, defaultValue);
                }
            }
        } catch (JEVisException ex) {
            logger.error(ex);
        }
        return lastValue;
    }

    public Object getLastSample(JEVisObject object, String attributeName, Object defaultValue) {
        Object lastValue = defaultValue;
        try {
            JEVisAttribute attribute = object.getAttribute(attributeName);
            if (attribute != null) {
                JEVisSample lastSample = attribute.getLatestSample();
                if (lastSample != null) {
                    lastValue = getValue(lastSample, defaultValue);
                }
            }
        } catch (JEVisException ex) {
            logger.error(ex);
        }
        return lastValue;
    }

    public JEVisFile getLastSample(JEVisObject object, String attributeName, JEVisFile defaultValue) {
        JEVisFile lastValue = defaultValue;
        try {
            JEVisAttribute attribute = object.getAttribute(attributeName);
            if (attribute != null) {
                JEVisSample lastSample = attribute.getLatestSample();
                if (lastSample != null) {
                    lastValue = getValue(lastSample, defaultValue);
                }
            }
        } catch (JEVisException ex) {
            logger.error(ex);
        }
        return lastValue;
    }

    private String getValue(JEVisSample lastSample, String defaultValue) {
        try {
            return lastSample.getValueAsString();
        } catch (JEVisException e) {
            e.printStackTrace();
            logger.error(e);
        }
        return null;
    }

    private Boolean getValue(JEVisSample lastSample, Boolean defaultValue) {
        try {
            return lastSample.getValueAsBoolean();
        } catch (JEVisException e) {
            e.printStackTrace();
            logger.error(e);
        }
        return false;
    }

    private Double getValue(JEVisSample lastSample, Double defaultValue) {
        try {
            return lastSample.getValueAsDouble();
        } catch (JEVisException e) {
            e.printStackTrace();
            logger.error(e);
        }
        return null;
    }

    private Long getValue(JEVisSample lastSample, Long defaultValue) {
        try {
            return lastSample.getValueAsLong();
        } catch (JEVisException e) {
            e.printStackTrace();
            logger.error(e);
        }
        return null;
    }

    private Object getValue(JEVisSample lastSample, Object defaultValue) {
        try {
            return lastSample.getValue();
        } catch (JEVisException e) {
            e.printStackTrace();
            logger.error(e);
        }
        return null;
    }

    private JEVisFile getValue(JEVisSample lastSample, JEVisFile defaultValue) {
        try {
            return lastSample.getValueAsFile();
        } catch (JEVisException e) {
            e.printStackTrace();
            logger.error(e);
        }
        return null;
    }

    public DateTime getTimeStampFromLastSample(JEVisObject object, String attributeName) {
        DateTime lastDate = null;
        try {
            JEVisAttribute attribute = object.getAttribute(attributeName);
            if (attribute != null) {
                JEVisSample smp = attribute.getLatestSample();
                if (smp != null) lastDate = smp.getTimestamp();
            }
        } catch (JEVisException ex) {
            logger.error(ex);
        }
        return lastDate;
    }

    public DateTime getTimestampFromFirstSample(JEVisObject object, String attributeName) {
        DateTime firstDate = null;
        try {
            JEVisAttribute attribute = object.getAttribute(attributeName);
            if (attribute != null) {
                List<JEVisSample> sampleList = attribute.getAllSamples();
                if (sampleList.size() > 0) {
                    JEVisSample smp = sampleList.get(0);
                    firstDate = smp.getTimestamp();
                }
                firstDate = attribute.getTimestampFromFirstSample();

            }
        } catch (JEVisException ex) {
            logger.error(ex);
        }
        return firstDate;
    }

    public JEVisSample getFirstSample(JEVisObject object, String attributeName) {
        JEVisSample firstSample = null;
        try {
            JEVisAttribute attribute = object.getAttribute(attributeName);
            if (attribute != null) {
                List<JEVisSample> sampleList = attribute.getAllSamples();
                if (sampleList.size() > 0) {
                    firstSample = sampleList.get(0);
                }

            }
        } catch (JEVisException ex) {
            logger.error(ex);
        }
        return firstSample;
    }

    public List<JEVisSample> getSamplesInPeriod(JEVisObject object, String attributeName, DateTime firstDate, DateTime lastDate) {
        List<JEVisSample> samples = new ArrayList<>();
        try {
            JEVisAttribute attribute = object.getAttribute(attributeName);
            if (attribute != null) {
                samples = attribute.getSamples(firstDate, lastDate);
            }
        } catch (JEVisException ex) {
            logger.error(ex);
        }
        return samples;
    }

    public void buildSample(JEVisObject object, String attribute, Object value) {
        try {
            object.getAttribute(attribute).buildSample(new DateTime(), value).commit();
            object.commit();
        } catch (JEVisException ex) {
            logger.error(ex);
        }
    }

    public List<JEVisSample> getAllSamples(JEVisObject object, String attributeName) {
        List<JEVisSample> samples = new ArrayList<>();
        try {
            JEVisAttribute attribute = object.getAttribute(attributeName);
            if (attribute != null) {
                samples = attribute.getAllSamples();
            }
        } catch (JEVisException ex) {
            logger.error(ex);
        }
        return samples;
    }

    public void importData(List<JEVisSample> aggregatedData, JEVisAttribute attribute) {
        try {
            attribute.addSamples(aggregatedData);
        } catch (JEVisException ex) {
            logger.error(ex);
        }
    }

    public void importDataAndReplaceSorted(List<JEVisSample> aggregatedData, JEVisAttribute attribute) {
        if (aggregatedData.isEmpty()) {
            return;
        }
        try {
            DateTime from = aggregatedData.get(0).getTimestamp();
            DateTime to = aggregatedData.get(aggregatedData.size() - 1).getTimestamp();
            attribute.deleteSamplesBetween(from, to);
            attribute.addSamples(aggregatedData);
        } catch (JEVisException ex) {
            logger.error(ex);
        }
    }
}
