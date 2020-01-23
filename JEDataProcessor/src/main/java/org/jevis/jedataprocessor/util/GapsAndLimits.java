package org.jevis.jedataprocessor.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisSample;
import org.jevis.commons.constants.GapFillingBoundToSpecific;
import org.jevis.commons.constants.GapFillingReferencePeriod;
import org.jevis.commons.constants.GapFillingType;
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

        GapFillingBoundToSpecific bindToSpecificValue = GapFillingBoundToSpecific.parse(c.getBindtospecific());
        if (Objects.isNull(bindToSpecificValue)) bindToSpecificValue = GapFillingBoundToSpecific.NONE;
        List<JEVisSample> boundListSamples = new ArrayList<>();
        DateTime firstDate;

        firstDate = getFirstDate(lastDate);
        List<JEVisSample> listSamplesNew = new ArrayList<>();
        switch (bindToSpecificValue) {
            case WEEKDAY:
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
            case WEEKOFYEAR:
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
            case MONTHOFYEAR:
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
        final GapFillingType gapFillingType = GapFillingType.parse(c.getType());

        if (Objects.nonNull(listSamples) && !listSamples.isEmpty()) {
            switch (gapFillingType) {
                case MINIMUM:
                    Double minValue = listSamples.get(0).getValueAsDouble();
                    for (JEVisSample sample : listSamples) {
                        if (sample.getValueAsDouble() != null) {
                            minValue = Math.min(minValue, sample.getValueAsDouble());
                        }
                    }
                    return minValue;
                case MAXIMUM:
                    Double maxValue = listSamples.get(0).getValueAsDouble();
                    for (JEVisSample sample : listSamples) {
                        if (sample.getValueAsDouble() != null) {
                            if (sample.getValueAsDouble() != null) {
                                maxValue = Math.max(maxValue, sample.getValueAsDouble());
                            }
                        }
                    }
                    return maxValue;
                case MEDIAN:
                    Double medianValue = 0d;
                    List<Double> sortedArray = new ArrayList<>();
                    for (JEVisSample sample : listSamples) {
                        if (sample.getValueAsDouble() != null) {
                            sortedArray.add(sample.getValueAsDouble());
                        }
                    }
                    Collections.sort(sortedArray);
                    if (!sortedArray.isEmpty()) {
                        if (sortedArray.size() > 2) medianValue = sortedArray.get(sortedArray.size() / 2);
                        else if (sortedArray.size() == 2) medianValue = (sortedArray.get(0) + sortedArray.get(1)) / 2;
                    }

                    return medianValue;
                case AVERAGE:
                    double averageValue = 0d;
                    for (JEVisSample sample : listSamples) {
                        if (sample.getValueAsDouble() != null) {
                            averageValue += sample.getValueAsDouble();
                        }
                    }
                    //logger.info("sum: " + averageValue + " listSize: " + listSamples.size());
                    averageValue = averageValue / listSamples.size();
                    return averageValue;
                case DELETE:
                default:
                    break;
            }
        }
        return 0d;
    }

    public void fillMaximum() throws Exception {
        switch (gapsAndLimitsType) {
            case FORECAST_TYPE:
                int lastIndex = intervals.size() - 1;
                for (CleanInterval currentInterval : intervals) {
                    int index = intervals.indexOf(currentInterval);
                    Double value = getSpecificValue(currentInterval.getDate());
                    JEVisSample sample = new VirtualSample(currentInterval.getDate(), value);
                    String note = "";
                    note += getNote(currentInterval);

                    if (index == 0) {
                        note += "," + NoteConstants.Forecast.FORECAST_1 + NoteConstants.Forecast.FORECAST_MAX;
                    } else if (index == lastIndex) {
                        note += "," + NoteConstants.Forecast.FORECAST_2 + NoteConstants.Forecast.FORECAST_MAX;
                    } else {
                        note += "," + NoteConstants.Forecast.FORECAST + NoteConstants.Forecast.FORECAST_MAX;
                    }

                    sample.setNote(note);
                    currentInterval.addTmpSample(sample);
                }
                break;
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

                        if (currentLimitBreak.getMin() != null && value < currentLimitBreak.getMin()) {
                            value = currentLimitBreak.getMin();
                        }
                        if (currentLimitBreak.getMax() != null && value > currentLimitBreak.getMax()) {
                            value = currentLimitBreak.getMax();
                        }

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
            case FORECAST_TYPE:
                int lastIndex = intervals.size() - 1;
                for (CleanInterval currentInterval : intervals) {
                    int index = intervals.indexOf(currentInterval);
                    Double value = getSpecificValue(currentInterval.getDate());
                    JEVisSample sample = new VirtualSample(currentInterval.getDate(), value);
                    String note = "";
                    note += getNote(currentInterval);

                    if (index == 0) {
                        note += "," + NoteConstants.Forecast.FORECAST_1 + NoteConstants.Forecast.FORECAST_MEDIAN;
                    } else if (index == lastIndex) {
                        note += "," + NoteConstants.Forecast.FORECAST_2 + NoteConstants.Forecast.FORECAST_MEDIAN;
                    } else {
                        note += "," + NoteConstants.Forecast.FORECAST + NoteConstants.Forecast.FORECAST_MEDIAN;
                    }

                    sample.setNote(note);
                    currentInterval.addTmpSample(sample);
                }
                break;
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

                        if (currentLimitBreak.getMin() != null && value < currentLimitBreak.getMin()) {
                            value = currentLimitBreak.getMin();
                        }
                        if (currentLimitBreak.getMax() != null && value > currentLimitBreak.getMax()) {
                            value = currentLimitBreak.getMax();
                        }

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
            case FORECAST_TYPE:
                int lastIndex = intervals.size() - 1;
                for (CleanInterval currentInterval : intervals) {
                    int index = intervals.indexOf(currentInterval);
                    Double value = getSpecificValue(currentInterval.getDate());
                    JEVisSample sample = new VirtualSample(currentInterval.getDate(), value);
                    String note = "";
                    note += getNote(currentInterval);

                    if (index == 0) {
                        note += "," + NoteConstants.Forecast.FORECAST_1 + NoteConstants.Forecast.FORECAST_AVERAGE;
                    } else if (index == lastIndex) {
                        note += "," + NoteConstants.Forecast.FORECAST_2 + NoteConstants.Forecast.FORECAST_AVERAGE;
                    } else {
                        note += "," + NoteConstants.Forecast.FORECAST + NoteConstants.Forecast.FORECAST_AVERAGE;
                    }

                    sample.setNote(note);
                    currentInterval.addTmpSample(sample);
                }
                break;
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

                        if (currentLimitBreak.getMin() != null && value < currentLimitBreak.getMin()) {
                            value = currentLimitBreak.getMin();
                        }
                        if (currentLimitBreak.getMax() != null && value > currentLimitBreak.getMax()) {
                            value = currentLimitBreak.getMax();
                        }

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

    public void fillDelete() {
        switch (gapsAndLimitsType) {
            case GAPS_TYPE:
                List<CleanInterval> tobeRemovedGaps = new ArrayList<>();
                for (CleanInterval cleanInterval : intervals) {
                    for (Gap currentGap : gapList) {
                        for (CleanInterval cleanInterval1 : currentGap.getIntervals()) {
                            if (cleanInterval.getDate().equals(cleanInterval1.getDate())) {
                                tobeRemovedGaps.add(cleanInterval);
                            }
                        }
                    }
                }
                intervals.removeAll(tobeRemovedGaps);
                break;
            case LIMITS_TYPE:
                List<CleanInterval> tobeRemovedLimits = new ArrayList<>();
                for (CleanInterval cleanInterval : intervals) {
                    for (LimitBreak limitBreak : limitBreaksList) {
                        for (CleanInterval cleanInterval1 : limitBreak.getIntervals()) {
                            if (cleanInterval.getDate().equals(cleanInterval1.getDate())) {
                                tobeRemovedLimits.add(cleanInterval);
                            }
                        }
                    }
                }
                intervals.removeAll(tobeRemovedLimits);
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
                            if (currentLimitBreak.getMin() != null && currentValue < currentLimitBreak.getMin()) {
                                currentValue = currentLimitBreak.getMin();
                            }
                            if (currentLimitBreak.getMax() != null && currentValue > currentLimitBreak.getMax()) {
                                currentValue = currentLimitBreak.getMax();
                            }

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
            case FORECAST_TYPE:
                int lastIndex = intervals.size() - 1;
                for (CleanInterval currentInterval : intervals) {
                    int index = intervals.indexOf(currentInterval);
                    Double value = getSpecificValue(currentInterval.getDate());
                    JEVisSample sample = new VirtualSample(currentInterval.getDate(), value);
                    String note = "";
                    note += getNote(currentInterval);

                    if (index == 0) {
                        note += "," + NoteConstants.Forecast.FORECAST_1 + NoteConstants.Forecast.FORECAST_MIN;
                    } else if (index == lastIndex) {
                        note += "," + NoteConstants.Forecast.FORECAST_2 + NoteConstants.Forecast.FORECAST_MIN;
                    } else {
                        note += "," + NoteConstants.Forecast.FORECAST + NoteConstants.Forecast.FORECAST_MIN;
                    }

                    sample.setNote(note);
                    currentInterval.addTmpSample(sample);
                }
                break;
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
        final GapFillingReferencePeriod referencePeriod = GapFillingReferencePeriod.parse(c.getReferenceperiod());
        int referencePeriodCount = Integer.parseInt(c.getReferenceperiodcount());
        switch (referencePeriod) {
            case DAY:
                return lastDate.minusDays(referencePeriodCount);
            case WEEK:
                return lastDate.minusWeeks(referencePeriodCount);
            case MONTH:
                return lastDate.minusMonths(referencePeriodCount);
            case YEAR:
                return lastDate.minusYears(referencePeriodCount);
            case ALL:
                try {
                    return sampleCache.get(0).getTimestamp();
                } catch (JEVisException e) {
                    e.printStackTrace();
                }
            default:
                return lastDate.minusMonths(referencePeriodCount);
        }
    }

    public void clearLists() {
        this.intervals = null;
        this.gapsAndLimitsType = null;
        this.gapList = null;
        this.limitBreaksList = null;
        this.c = null;
        this.sampleCache = null;
    }

    public enum GapsAndLimitsType {
        LIMITS_TYPE, GAPS_TYPE, FORECAST_TYPE
    }
}
