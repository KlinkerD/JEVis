package org.jevis.jedataprocessor.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisSample;
import org.jevis.commons.constants.JEDataProcessorConstants;
import org.jevis.commons.constants.NoteConstants;
import org.jevis.commons.dataprocessing.VirtualSample;
import org.jevis.commons.json.JsonGapFillingConfig;
import org.jevis.jedataprocessor.data.CleanInterval;
import org.jevis.jedataprocessor.gap.Gap;
import org.jevis.jedataprocessor.limits.LimitBreak;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GapsAndLimits {
    private static final Logger logger = LogManager.getLogger(GapsAndLimits.class);
    private List<CleanInterval> intervals;
    private GapsAndLimitsType gapsAndLimitsType;
    private List<Gap> gapList;
    private List<LimitBreak> limitBreaksList;
    private JsonGapFillingConfig c;
    private List<JEVisSample> sampleCache;

    public GapsAndLimits(List<CleanInterval> intervals, GapsAndLimitsType type,
                         JsonGapFillingConfig c, List<Gap> gapList, List<LimitBreak> limitBreaksList, List<JEVisSample> sampleCache) {
        this.intervals = intervals;
        this.gapsAndLimitsType = type;
        this.gapList = gapList;
        this.limitBreaksList = limitBreaksList;
        this.c = c;
        this.sampleCache = sampleCache;
    }

    public static String getNote(CleanInterval currentInterval) {
        try {
            return currentInterval.getTmpSamples().get(0).getNote();
        } catch (Exception e1) {
            try {
                return currentInterval.getRawSamples().get(0).getNote();
            } catch (Exception e2) {
                return "";
            }
        }
    }

    private Double getSpecificValue(DateTime lastDate) throws JEVisException {

        String bindToSpecificValue = c.getBindtospecific();
        if (Objects.isNull(bindToSpecificValue)) bindToSpecificValue = "";
        List<JEVisSample> boundListSamples = new ArrayList<>();
        DateTime firstDate;

        boundListSamples.clear();
        firstDate = getFirstDate(lastDate);
        List<JEVisSample> listSamplesNew = new ArrayList<>();
        switch (bindToSpecificValue) {
            case (JEDataProcessorConstants.GapFillingBoundToSpecific.WEEKDAY):
                if (sampleCache != null && !sampleCache.isEmpty()) {
                    for (JEVisSample sample : sampleCache) {
                        if (sample.getTimestamp().getDayOfWeek() == lastDate.getDayOfWeek()) {
                            if ((sample.getTimestamp().getHourOfDay() == lastDate.getHourOfDay()) && (sample.getTimestamp().getMinuteOfHour() == lastDate.getMinuteOfHour())) {
                                boundListSamples.add(sample);
                            }
                        }
                    }
                }
                for (CleanInterval ci : intervals) {
                    if (ci.getDate().equals(firstDate) || (ci.getDate().isAfter(firstDate) && ci.getDate().isBefore(lastDate))
                            || ci.getDate().equals(lastDate))
                        for (JEVisSample js : ci.getTmpSamples()) {
                            if (js.getTimestamp().getDayOfWeek() == lastDate.getDayOfWeek()) {
                                if ((js.getTimestamp().getHourOfDay() == lastDate.getHourOfDay()) && (js.getTimestamp().getMinuteOfHour() == lastDate.getMinuteOfHour())) {
                                    boundListSamples.add(js);
                                }
                            }
                        }
                }
                return calcValueWithType(boundListSamples);
            case (JEDataProcessorConstants.GapFillingBoundToSpecific.WEEKOFYEAR):
                if (sampleCache != null && !sampleCache.isEmpty()) {
                    for (JEVisSample sample : sampleCache) {
                        if (sample.getTimestamp().getWeekyear() == lastDate.getWeekyear()) {
                            if ((sample.getTimestamp().getHourOfDay() == lastDate.getHourOfDay()) && (sample.getTimestamp().getMinuteOfHour() == lastDate.getMinuteOfHour())) {
                                boundListSamples.add(sample);
                            }
                        }
                    }
                }
                for (CleanInterval ci : intervals) {
                    if (ci.getDate().equals(firstDate) || (ci.getDate().isAfter(firstDate) && ci.getDate().isBefore(lastDate))
                            || ci.getDate().equals(lastDate))
                        for (JEVisSample js : ci.getTmpSamples()) {
                            if (js.getTimestamp().getWeekyear() == lastDate.getWeekyear()) {
                                if ((js.getTimestamp().getHourOfDay() == lastDate.getHourOfDay()) && (js.getTimestamp().getMinuteOfHour() == lastDate.getMinuteOfHour())) {
                                    boundListSamples.add(js);
                                }
                            }
                        }
                }
                return calcValueWithType(boundListSamples);
            case (JEDataProcessorConstants.GapFillingBoundToSpecific.MONTHOFYEAR):
                if (sampleCache != null && !sampleCache.isEmpty()) {
                    for (JEVisSample sample : sampleCache) {
                        if (sample.getTimestamp().getMonthOfYear() == lastDate.getMonthOfYear()) {
                            if ((sample.getTimestamp().getHourOfDay() == lastDate.getHourOfDay()) && (sample.getTimestamp().getMinuteOfHour() == lastDate.getMinuteOfHour())) {
                                boundListSamples.add(sample);
                            }
                        }
                    }
                }
                for (CleanInterval ci : intervals) {
                    if (ci.getDate().equals(firstDate) || (ci.getDate().isAfter(firstDate) && ci.getDate().isBefore(lastDate))
                            || ci.getDate().equals(lastDate))
                        for (JEVisSample js : ci.getTmpSamples()) {
                            if (js.getTimestamp().getMonthOfYear() == lastDate.getMonthOfYear()) {
                                if ((js.getTimestamp().getHourOfDay() == lastDate.getHourOfDay()) && (js.getTimestamp().getMinuteOfHour() == lastDate.getMinuteOfHour())) {
                                    boundListSamples.add(js);
                                }
                            }
                        }
                }
                return calcValueWithType(boundListSamples);
            default:
                if (sampleCache != null && !sampleCache.isEmpty()) {
                    for (JEVisSample sample : sampleCache) {
                        if ((sample.getTimestamp().getHourOfDay() == lastDate.getHourOfDay()) && (sample.getTimestamp().getMinuteOfHour() == lastDate.getMinuteOfHour())) {
                            listSamplesNew.add(sample);
                        }
                    }
                }
                for (CleanInterval ci : intervals) {
                    if (ci.getDate().equals(firstDate) || (ci.getDate().isAfter(firstDate) && ci.getDate().isBefore(lastDate))
                            || ci.getDate().equals(lastDate))
                        for (JEVisSample js : ci.getTmpSamples()) {
                            if ((js.getTimestamp().getHourOfDay() == lastDate.getHourOfDay()) && (js.getTimestamp().getMinuteOfHour() == lastDate.getMinuteOfHour())) {
                                listSamplesNew.add(js);
                            }
                        }
                }
                return calcValueWithType(listSamplesNew);
        }
    }

    private Double calcValueWithType(List<JEVisSample> listSamples) throws
            JEVisException {
        final String gapFillingType = c.getType();

        if (Objects.nonNull(listSamples) && !listSamples.isEmpty()) {
            switch (gapFillingType) {
                case JEDataProcessorConstants.GapFillingType.MINIMUM:
                    Double minValue = listSamples.get(0).getValueAsDouble();
                    for (JEVisSample sample : listSamples) {
                        minValue = Math.min(minValue, sample.getValueAsDouble());
                    }
                    return minValue;
                case JEDataProcessorConstants.GapFillingType.MAXIMUM:
                    Double maxValue = listSamples.get(0).getValueAsDouble();
                    for (JEVisSample sample : listSamples) {
                        maxValue = Math.max(maxValue, sample.getValueAsDouble());
                    }
                    return maxValue;
                case JEDataProcessorConstants.GapFillingType.MEDIAN:
                    Double medianValue = 0d;
                    List<Double> sortedArray = new ArrayList<>();
                    for (JEVisSample sample : listSamples) {
                        sortedArray.add(sample.getValueAsDouble());
                    }
                    Collections.sort(sortedArray);
                    if (!sortedArray.isEmpty()) {
                        if (sortedArray.size() > 2) medianValue = sortedArray.get(sortedArray.size() / 2);
                        else if (sortedArray.size() == 2) medianValue = (sortedArray.get(0) + sortedArray.get(1)) / 2;
                    }

                    return medianValue;
                case JEDataProcessorConstants.GapFillingType.AVERAGE:
                    Double averageValue = 0d;
                    for (JEVisSample sample : listSamples) {
                        averageValue += sample.getValueAsDouble();
                    }
                    //logger.info("sum: " + averageValue + " listSize: " + listSamples.size());
                    averageValue = averageValue / listSamples.size();
                    return averageValue;
                default:
                    break;
            }
        }
        return 0d;
    }

    public void fillMaximum() throws Exception {
        switch (gapsAndLimitsType) {
            case GAPS_TYPE:
                for (Gap currentGap : gapList) {
                    for (CleanInterval currentInterval : currentGap.getIntervals()) {
                        Double value = getSpecificValue(currentInterval.getDate());
                        JEVisSample sample = new VirtualSample(currentInterval.getDate(), value);
                        String note = "";
                        note += getNote(currentInterval);
                        note += "," + NoteConstants.Gap.GAP_MAX;
                        sample.setNote(note);
                        currentInterval.addTmpSample(sample);
                    }
                }
                break;
            case LIMITS_TYPE:
                for (LimitBreak currentLimitBreak : limitBreaksList) {
                    for (CleanInterval currentInterval : currentLimitBreak.getIntervals()) {
                        Double value = getSpecificValue(currentInterval.getDate());
                        JEVisSample sample = new VirtualSample(currentInterval.getDate(), value);
                        String note = "";
                        note += getNote(currentInterval);
                        note += "," + NoteConstants.Limits.LIMIT_MAX;
                        sample.setNote(note);
                        currentInterval.addTmpSample(sample);
                    }
                }
                break;
        }
    }

    public void fillMedian() throws Exception {
        switch (gapsAndLimitsType) {
            case GAPS_TYPE:
                for (Gap currentGap : gapList) {
                    for (CleanInterval currentInterval : currentGap.getIntervals()) {
                        Double value = getSpecificValue(currentInterval.getDate());
                        JEVisSample sample = new VirtualSample(currentInterval.getDate(), value);
                        String note = "";
                        note += getNote(currentInterval);
                        note += "," + NoteConstants.Gap.GAP_MEDIAN;
                        sample.setNote(note);
                        currentInterval.addTmpSample(sample);

                    }
                }
                break;
            case LIMITS_TYPE:
                for (LimitBreak currentLimitBreak : limitBreaksList) {
                    for (CleanInterval currentInterval : currentLimitBreak.getIntervals()) {
                        Double value = getSpecificValue(currentInterval.getDate());
                        JEVisSample sample = new VirtualSample(currentInterval.getDate(), value);
                        String note = "";
                        note += getNote(currentInterval);
                        note += "," + NoteConstants.Limits.LIMIT_MEDIAN;
                        sample.setNote(note);
                        currentInterval.addTmpSample(sample);
                    }
                }
                break;
        }
        logger.info("Done");
    }

    public void fillAverage() throws Exception {
        switch (gapsAndLimitsType) {
            case GAPS_TYPE:
                for (Gap currentGap : gapList) {
                    for (CleanInterval currentInterval : currentGap.getIntervals()) {
                        Double value = getSpecificValue(currentInterval.getDate());
                        JEVisSample sample = new VirtualSample(currentInterval.getDate(), value);
                        String note = "";
                        note += getNote(currentInterval);
                        note += "," + NoteConstants.Gap.GAP_AVERAGE;
                        sample.setNote(note);
                        currentInterval.addTmpSample(sample);
                    }
                }
                break;
            case LIMITS_TYPE:
                for (LimitBreak currentLimitBreak : limitBreaksList) {
                    for (CleanInterval currentInterval : currentLimitBreak.getIntervals()) {
                        Double value = getSpecificValue(currentInterval.getDate());
                        JEVisSample sample = new VirtualSample(currentInterval.getDate(), value);
                        String note = "";
                        note += getNote(currentInterval);
                        note += "," + NoteConstants.Limits.LIMIT_AVERAGE;
                        sample.setNote(note);
                        currentInterval.addTmpSample(sample);
                    }
                }
                break;
        }
    }

    public void fillInterpolation() throws Exception {
        switch (gapsAndLimitsType) {
            case GAPS_TYPE:
                for (Gap currentGap : gapList) {
                    Double firstValue = currentGap.getFirstValue();
                    Double lastValue = currentGap.getLastValue();
                    int size = currentGap.getIntervals().size(); //if there is a gap of 2, then you have 3 steps
                    if (firstValue != null && lastValue != null) {
                        Double stepSize = (lastValue - firstValue) / size;
                        Double currentValue = firstValue;
                        for (CleanInterval currentInterval : currentGap.getIntervals()) {
                            JEVisSample sample = new VirtualSample(currentInterval.getDate(), currentValue);
                            String note = "";
                            note += getNote(currentInterval);
                            note += "," + NoteConstants.Gap.GAP_INTERPOLATION;
                            sample.setNote(note);
                            currentValue += stepSize;
                            currentInterval.addTmpSample(sample);
                        }
                    }
                }
                break;
            case LIMITS_TYPE:
                for (LimitBreak currentLimitBreak : limitBreaksList) {
                    Double firstValue = currentLimitBreak.getFirstValue();
                    Double lastValue = currentLimitBreak.getLastValue();
                    int size = currentLimitBreak.getIntervals().size(); //if there is a gap of 2, then you have 3 steps
                    if (firstValue != null && lastValue != null) {
                        Double stepSize = (lastValue - firstValue) / size;
                        Double currentValue = firstValue;
                        for (CleanInterval currentInterval : currentLimitBreak.getIntervals()) {
                            JEVisSample sample = new VirtualSample(currentInterval.getDate(), currentValue);
                            String note = "";
                            note += getNote(currentInterval);
                            note += "," + NoteConstants.Limits.LIMIT_INTERPOLATION;
                            sample.setNote(note);
                            currentValue += stepSize;
                            currentInterval.addTmpSample(sample);
                        }
                    }
                }
                break;
        }
    }

    public void fillDefault() throws Exception {
        Double defaultValue = Double.valueOf(c.getDefaultvalue());
        switch (gapsAndLimitsType) {
            case GAPS_TYPE:
                for (Gap currentGap : gapList) {
                    for (CleanInterval currentInterval : currentGap.getIntervals()) {
                        JEVisSample sample = new VirtualSample(currentInterval.getDate(), defaultValue);
                        String note = "";
                        note += getNote(currentInterval);
                        note += "," + NoteConstants.Gap.GAP_DEFAULT;
                        sample.setNote(note);
                        currentInterval.addTmpSample(sample);
                    }
                }
                break;
            case LIMITS_TYPE:
                for (LimitBreak currentLimitBreak : limitBreaksList) {
                    for (CleanInterval currentInterval : currentLimitBreak.getIntervals()) {
                        JEVisSample sample = new VirtualSample(currentInterval.getDate(), defaultValue);
                        String note = "";
                        note += getNote(currentInterval);
                        note += "," + NoteConstants.Limits.LIMIT_DEFAULT;
                        sample.setNote(note);
                        currentInterval.addTmpSample(sample);
                    }
                }
                break;
        }
    }

    public void fillMinimum() throws Exception {
        switch (gapsAndLimitsType) {
            case GAPS_TYPE:
                for (Gap currentGap : gapList) {
                    for (CleanInterval currentInterval : currentGap.getIntervals()) {
                        Double value = getSpecificValue(currentInterval.getDate());
                        JEVisSample sample = new VirtualSample(currentInterval.getDate(), value);
                        String note = "";
                        note += getNote(currentInterval);
                        note += "," + NoteConstants.Gap.GAP_MIN;
                        sample.setNote(note);
                        currentInterval.addTmpSample(sample);
                    }
                }
                break;
            case LIMITS_TYPE:
                for (LimitBreak currentLimitBreak : limitBreaksList) {
                    for (CleanInterval currentInterval : currentLimitBreak.getIntervals()) {
                        Double value = getSpecificValue(currentInterval.getDate());
                        JEVisSample sample = new VirtualSample(currentInterval.getDate(), value);
                        String note = "";
                        note += getNote(currentInterval);
                        note += "," + NoteConstants.Limits.LIMIT_MIN;
                        sample.setNote(note);
                        currentInterval.addTmpSample(sample);
                    }
                }
                break;
        }
    }

    public void fillStatic() throws Exception {
        switch (gapsAndLimitsType) {
            case GAPS_TYPE:
                for (Gap currentGap : gapList) {
                    Double firstValue = currentGap.getFirstValue();
                    for (CleanInterval currentInterval : currentGap.getIntervals()) {
                        JEVisSample sample = new VirtualSample(currentInterval.getDate(), firstValue);
                        String note = "";
                        note += getNote(currentInterval);
                        note += "," + NoteConstants.Gap.GAP_STATIC;
                        sample.setNote(note);
                        currentInterval.addTmpSample(sample);
                    }
                }
                break;
            case LIMITS_TYPE:
                for (LimitBreak currentLimitBreak : limitBreaksList) {
                    Double firstValue = currentLimitBreak.getFirstValue();
                    for (CleanInterval currentInterval : currentLimitBreak.getIntervals()) {
                        JEVisSample sample = new VirtualSample(currentInterval.getDate(), firstValue);
                        String note = "";
                        note += getNote(currentInterval);
                        note += "," + NoteConstants.Limits.LIMIT_STATIC;
                        sample.setNote(note);
                        currentInterval.addTmpSample(sample);
                    }
                }
                break;
        }
    }

    private DateTime getFirstDate(DateTime lastDate) {
        final String referencePeriod = c.getReferenceperiod();
        int referencePeriodCount = Integer.parseInt(c.getReferenceperiodcount());
        switch (referencePeriod) {
            case (JEDataProcessorConstants.GapFillingReferencePeriod.DAY):
                return lastDate.minusDays(referencePeriodCount);
            case (JEDataProcessorConstants.GapFillingReferencePeriod.WEEK):
                return lastDate.minusWeeks(referencePeriodCount);
            case (JEDataProcessorConstants.GapFillingReferencePeriod.MONTH):
                return lastDate.minusMonths(referencePeriodCount);
            case (JEDataProcessorConstants.GapFillingReferencePeriod.YEAR):
                return lastDate.minusYears(referencePeriodCount);
            default:
                return lastDate.minusMonths(referencePeriodCount);
        }
    }

    public enum GapsAndLimitsType {
        LIMITS_TYPE, GAPS_TYPE
    }
}
