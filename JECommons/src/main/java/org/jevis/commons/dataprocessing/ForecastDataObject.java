/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.commons.dataprocessing;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.*;
import org.jevis.commons.constants.JEDataProcessorConstants;
import org.jevis.commons.database.ObjectHandler;
import org.jevis.commons.database.SampleHandler;
import org.jevis.commons.json.JsonGapFillingConfig;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jevis.commons.dataprocessing.ForecastDataObject.AttributeName.*;

/**
 * @author broder
 */
public class ForecastDataObject {

    public static final String CLASS_NAME = "Forecast Data";
    public static final String VALUE_ATTRIBUTE_NAME = "Value";
    private static final Logger logger = LogManager.getLogger(ForecastDataObject.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JEVisObject forecastDataObject;
    private JEVisObject parentDataObject;
    private Boolean enabled;
    //additional attributes
    private SampleHandler sampleHandler;
    private JEVisAttribute inputAttribute;

    private JEVisAttribute valueAttribute;
    private JEVisAttribute enabledAttribute;
    private JEVisAttribute typeAttribute;
    private JEVisAttribute referencePeriodAttribute;
    private JEVisAttribute referencePeriodCountAttribute;
    private JEVisAttribute bindToSpecificAttribute;
    private JEVisAttribute forecastDurationAttribute;
    private JEVisAttribute forecastDurationCountAttribute;
    private DateTime lastRawDate;
    private int processingSize = 10000;
    private List<JEVisSample> sampleCache;

    private Period inputDataPeriod;

    public ForecastDataObject(JEVisObject forecastObject, ObjectHandler objectHandler) {
        forecastDataObject = forecastObject;
        parentDataObject = objectHandler.getFirstParent(forecastObject);
        sampleHandler = new SampleHandler();
    }

    public void getAttributes() throws JEVisException {
        if (enabledAttribute == null) {
            enabledAttribute = getForecastDataObject().getAttribute(ENABLED.getAttributeName());
        }

        if (valueAttribute == null) {
            valueAttribute = getForecastDataObject().getAttribute(VALUE.getAttributeName());
        }

        if (typeAttribute == null) {
            typeAttribute = getForecastDataObject().getAttribute(TYPE.getAttributeName());
        }

        if (referencePeriodAttribute == null) {
            referencePeriodAttribute = getForecastDataObject().getAttribute(REFERENCE_PERIOD.getAttributeName());
        }

        if (referencePeriodCountAttribute == null) {
            referencePeriodCountAttribute = getForecastDataObject().getAttribute(REFERENCE_PERIOD_COUNT.getAttributeName());
        }

        if (bindToSpecificAttribute == null) {
            bindToSpecificAttribute = getForecastDataObject().getAttribute(BIND_TO_SPECIFIC.getAttributeName());
        }

        if (forecastDurationAttribute == null) {
            forecastDurationAttribute = getForecastDataObject().getAttribute(FORECAST_DURATION.getAttributeName());
        }

        if (forecastDurationCountAttribute == null) {
            forecastDurationCountAttribute = getForecastDataObject().getAttribute(FORECAST_DURATION_COUNT.getAttributeName());
        }
    }

    public void reloadAttributes() throws JEVisException {
        getForecastDataObject().getDataSource().reloadAttribute(enabledAttribute);
        getForecastDataObject().getDataSource().reloadAttribute(valueAttribute);
        getForecastDataObject().getDataSource().reloadAttribute(typeAttribute);
        getForecastDataObject().getDataSource().reloadAttribute(referencePeriodAttribute);
        getForecastDataObject().getDataSource().reloadAttribute(referencePeriodCountAttribute);
        getForecastDataObject().getDataSource().reloadAttribute(bindToSpecificAttribute);
        getForecastDataObject().getDataSource().reloadAttribute(forecastDurationAttribute);
        getForecastDataObject().getDataSource().reloadAttribute(forecastDurationCountAttribute);
    }

    public Boolean getEnabled() {
        if (enabled == null)
            enabled = sampleHandler.getLastSample(getForecastDataObject(), ENABLED.getAttributeName(), false);
        return enabled;
    }

    public JEVisObject getForecastDataObject() {
        return forecastDataObject;
    }

    public JEVisObject getParentDataObject() {
        return parentDataObject;
    }

    public Map<DateTime, JEVisSample> getNotesMap() {
        Map<DateTime, JEVisSample> notesMap = new HashMap<>();
        try {
            final JEVisClass dataNoteClass = parentDataObject.getDataSource().getJEVisClass("Data Notes");
            for (JEVisObject obj : forecastDataObject.getParents().get(0).getChildren(dataNoteClass, true)) {
                if (obj.getName().contains(forecastDataObject.getName())) {
                    JEVisAttribute userNoteAttribute = obj.getAttribute("User Notes");
                    if (userNoteAttribute.hasSample()) {
                        for (JEVisSample smp : userNoteAttribute.getAllSamples()) {
                            notesMap.put(smp.getTimestamp(), smp);
                        }
                    }
                }
            }
        } catch (JEVisException e) {
        }
        return notesMap;
    }

    public String getName() {
        return forecastDataObject.getName() + ":" + forecastDataObject.getID();
    }

    public JEVisAttribute getValueAttribute() throws JEVisException {
        if (valueAttribute == null)
            valueAttribute = getForecastDataObject().getAttribute(VALUE.getAttributeName());
        return valueAttribute;
    }

    public JEVisAttribute getInputAttribute() throws JEVisException {
        if (inputAttribute == null)
            inputAttribute = getParentDataObject().getAttribute(VALUE.getAttributeName());
        return inputAttribute;
    }

    public void setInputAttribute(JEVisAttribute inputAttribute) {
        this.valueAttribute = inputAttribute;
    }

    public JEVisAttribute getTypeAttribute() throws JEVisException {
        if (typeAttribute == null)
            typeAttribute = getForecastDataObject().getAttribute(TYPE.getAttributeName());
        return typeAttribute;
    }

    public JEVisAttribute getReferencePeriodAttribute() throws JEVisException {
        if (referencePeriodAttribute == null)
            referencePeriodAttribute = getForecastDataObject().getAttribute(REFERENCE_PERIOD.getAttributeName());
        return referencePeriodAttribute;
    }

    public JEVisAttribute getReferencePeriodCountAttribute() throws JEVisException {
        if (referencePeriodCountAttribute == null)
            referencePeriodCountAttribute = getForecastDataObject().getAttribute(REFERENCE_PERIOD_COUNT.getAttributeName());
        return referencePeriodCountAttribute;
    }

    public JEVisAttribute getBindToSpecificAttribute() throws JEVisException {
        if (bindToSpecificAttribute == null)
            bindToSpecificAttribute = getForecastDataObject().getAttribute(BIND_TO_SPECIFIC.getAttributeName());
        return bindToSpecificAttribute;
    }

    public JEVisAttribute getForecastDurationAttribute() throws JEVisException {
        if (forecastDurationAttribute == null)
            forecastDurationAttribute = getForecastDataObject().getAttribute(FORECAST_DURATION.getAttributeName());
        return forecastDurationAttribute;
    }

    public JEVisAttribute getForecastDurationCountAttribute() throws JEVisException {
        if (forecastDurationCountAttribute == null)
            forecastDurationCountAttribute = getForecastDataObject().getAttribute(FORECAST_DURATION_COUNT.getAttributeName());
        return forecastDurationCountAttribute;
    }

    public Period getInputDataPeriod() {
        if (inputDataPeriod == null) {
            try {
                inputDataPeriod = getInputAttribute().getInputSampleRate();
            } catch (Exception e) {
                logger.error("Could not get input data period for object {}:{}", getParentDataObject().getName(), getParentDataObject().getID(), e);
            }
        }
        return inputDataPeriod;
    }

    public JEVisAttribute getEnabledAttribute() {
        return enabledAttribute;
    }

    public void setProcessingSize(int processingSize) {
        this.processingSize = processingSize;
    }

    public DateTime getStartDate() {
        return getLastRun(this.getForecastDataObject());
    }

    public List<JEVisSample> getSampleCache() {
        if (this.sampleCache == null || this.sampleCache.isEmpty()) {
            String referencePeriod = null;
            Long referencePeriodCount = null;
            try {
                if (getReferencePeriodAttribute().hasSample()) {
                    referencePeriod = getReferencePeriodAttribute().getLatestSample().getValueAsString();
                }
            } catch (JEVisException e) {
                logger.error("Could not get reference period from {}:{}, assuming default value of month", getForecastDataObject().getName(), getForecastDataObject().getID(), e);
                referencePeriod = "month";
            }

            try {
                if (getReferencePeriodCountAttribute().hasSample()) {
                    referencePeriodCount = getReferencePeriodCountAttribute().getLatestSample().getValueAsLong();
                }
            } catch (JEVisException e) {
                logger.error("Could not get reference period count from {}:{}, assuming default value of 6", getForecastDataObject().getName(), getForecastDataObject().getID(), e);
                referencePeriodCount = 6L;
            }

            long duration = 0L;
            if (referencePeriod != null && referencePeriodCount != null) {
                switch (referencePeriod.toLowerCase()) {
                    case (JEDataProcessorConstants.GapFillingReferencePeriod.DAY):
                        duration = 2 * 24L * 60L * 60L * 1000L;
                        break;
                    case (JEDataProcessorConstants.GapFillingReferencePeriod.MONTH):
                        duration = 2 * 4L * 7L * 24L * 60L * 60L * 1000L;
                        break;
                    case (JEDataProcessorConstants.GapFillingReferencePeriod.WEEK):
                        duration = 2 * 7L * 24L * 60L * 60L * 1000L;
                        break;
                    case (JEDataProcessorConstants.GapFillingReferencePeriod.YEAR):
                        duration = 2 * 52L * 4L * 7L * 24L * 60L * 60L * 1000L;
                        break;
                    case (JEDataProcessorConstants.GapFillingReferencePeriod.ALL):
                        try {
                            sampleCache = getInputAttribute().getAllSamples();
                            return sampleCache;
                        } catch (JEVisException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                duration *= referencePeriodCount;
                DateTime endDate = getStartDate();
                DateTime startDate = endDate.minus(duration);

                try {
                    sampleCache = getInputAttribute().getSamples(startDate, endDate);
                    return sampleCache;
                } catch (JEVisException e) {
                    logger.error("Could not get samples from {}:{} in the interval {} to {}",
                            getForecastDataObject().getName(), getForecastDataObject().getID(), startDate, endDate, e);
                }
            }
            try {
                sampleCache = getInputAttribute().getAllSamples();
                return sampleCache;
            } catch (JEVisException ex) {
                ex.printStackTrace();
            }
            sampleCache = new ArrayList<>();
        }
        return sampleCache;
    }

    public DateTime getEndDate() throws JEVisException {
        if (getForecastDurationAttribute().hasSample()) {
            String forecastDuration = getForecastDurationAttribute().getLatestSample().getValueAsString();
            long forecastDurationCount = 1L;
            if (getForecastDurationCountAttribute().hasSample()) {
                forecastDurationCount = getForecastDurationCountAttribute().getLatestSample().getValueAsLong();
            }
            long duration = 0L;

            switch (forecastDuration) {
                case "MINUTES":
                    duration = 60L * 1000L;
                    break;
                case "HOURS":
                    duration = 60L * 60L * 1000L;
                    break;
                case "DAYS":
                    duration = 24L * 60L * 60L * 1000L;
                    break;
                case "WEEKS":
                    duration = 7L * 24L * 60L * 60L * 1000L;
                    break;
                case "MONTHS":
                    duration = 4L * 7L * 24L * 60L * 60L * 1000L;
                    break;
            }

            duration *= forecastDurationCount;
            return getStartDate().plus(duration);
        }
        return null;
    }

    public boolean isReady(JEVisObject object) {
        DateTime lastRun = getLastRun(object);
        Long cycleTime = getCycleTime(object);
        DateTime nextRun = lastRun.plusMillis(cycleTime.intValue());
        return DateTime.now().withZone(getTimeZone(object)).equals(nextRun) || DateTime.now().isAfter(nextRun);
    }

    private DateTimeZone getTimeZone(JEVisObject object) {
        DateTimeZone zone = DateTimeZone.UTC;

        JEVisAttribute timeZoneAttribute = null;
        try {
            timeZoneAttribute = object.getAttribute("Timezone");
            if (timeZoneAttribute != null) {
                JEVisSample lastTimeZoneSample = timeZoneAttribute.getLatestSample();
                if (lastTimeZoneSample != null) {
                    zone = DateTimeZone.forID(lastTimeZoneSample.getValueAsString());
                }
            }
        } catch (JEVisException e) {
            e.printStackTrace();
        }
        return zone;
    }

    private DateTime getLastRun(JEVisObject object) {
        DateTime dateTime = new DateTime(2001, 1, 1, 0, 0, 0).withZone(getTimeZone(object));

        try {
            JEVisAttribute lastRunAttribute = object.getAttribute("Last Run");
            if (lastRunAttribute != null) {
                JEVisSample lastSample = lastRunAttribute.getLatestSample();
                if (lastSample != null) {
                    dateTime = new DateTime(lastSample.getValueAsString());
                }
            }

        } catch (JEVisException e) {
            logger.error("Could not get data source last run time: " + e);
        }

        return dateTime;
    }

    private Long getCycleTime(JEVisObject object) {
        Long aLong = null;

        try {
            JEVisAttribute lastRunAttribute = object.getAttribute("Cycle Time");
            if (lastRunAttribute != null) {
                JEVisSample lastSample = lastRunAttribute.getLatestSample();
                if (lastSample != null) {
                    aLong = lastSample.getValueAsLong();
                }
            }

        } catch (JEVisException e) {
            logger.error("Could not get data source cycle time: " + e);
        }

        return aLong;
    }

    public void finishCurrentRun(JEVisObject object) {
        Long cycleTime = getCycleTime(object);
        DateTime lastRun = getLastRun(object);
        try {
            JEVisAttribute lastRunAttribute = object.getAttribute("Last Run");
            if (lastRunAttribute != null) {
                DateTime dateTime = lastRun.plusMillis(cycleTime.intValue());
                JEVisSample newSample = lastRunAttribute.buildSample(DateTime.now(), dateTime);
                newSample.commit();
            }

        } catch (JEVisException e) {
            logger.error("Could not get data source last run time: " + e);
        }
    }

    public JsonGapFillingConfig getJsonGapFillingConfig() throws JEVisException {
        JsonGapFillingConfig jsonGapFillingConfig = new JsonGapFillingConfig();

        String type = null;
        String referencePeriod = null;
        String referencePeriodCount = null;
        String bindToSpecific = null;
        if (getTypeAttribute().hasSample()) {
            type = getTypeAttribute().getLatestSample().getValueAsString();
        }

        if (getReferencePeriodAttribute().hasSample()) {
            referencePeriod = getReferencePeriodAttribute().getLatestSample().getValueAsString();
        }

        if (getReferencePeriodCountAttribute().hasSample()) {
            referencePeriodCount = getReferencePeriodCountAttribute().getLatestSample().getValueAsString();
        }

        if (getBindToSpecificAttribute().hasSample()) {
            bindToSpecific = getBindToSpecificAttribute().getLatestSample().getValueAsString();
        }

        jsonGapFillingConfig.setType(type);
        jsonGapFillingConfig.setReferenceperiod(referencePeriod);
        jsonGapFillingConfig.setReferenceperiodcount(referencePeriodCount);
        jsonGapFillingConfig.setBindtospecific(bindToSpecific);

        return jsonGapFillingConfig;
    }

    public enum AttributeName {
        VALUE("Value"),
        ENABLED("Enabled"),
        TYPE("Type"),
        REFERENCE_PERIOD("Reference Period"),
        REFERENCE_PERIOD_COUNT("Reference Period Count"),
        BIND_TO_SPECIFIC("Bind To Specific"),
        FORECAST_DURATION("Forecast Duration"),
        FORECAST_DURATION_COUNT("Forecast Duration Count");

        private final String attributeName;

        AttributeName(String attributeName) {
            this.attributeName = attributeName;
        }

        public String getAttributeName() {
            return attributeName;
        }
    }
}