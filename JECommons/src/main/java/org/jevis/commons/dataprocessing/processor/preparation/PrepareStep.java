/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.commons.dataprocessing.processor.preparation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisSample;
import org.jevis.commons.dataprocessing.CleanDataObject;
import org.jevis.commons.dataprocessing.processor.workflow.*;
import org.jevis.commons.datetime.PeriodComparator;
import org.jevis.commons.datetime.PeriodHelper;
import org.jevis.commons.datetime.WorkDays;
import org.jevis.commons.task.LogTaskManager;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Creates empty interval classes from start date to end date
 *
 * @author gschutz
 */
public class PrepareStep implements ProcessStep {

    private static final Logger logger = LogManager.getLogger(PrepareStep.class);
    private final ProcessManager processManager;
    private ResourceManager resourceManager;

    public PrepareStep(ProcessManager processManager) {
        this.processManager = processManager;
    }

    @Override

    public void run(ResourceManager resourceManager) throws Exception {
        this.resourceManager = resourceManager;
        CleanDataObject cleanDataObject = resourceManager.getCleanDataObject();

        //get the raw samples for the cleaning
        logger.info("[{}] Request raw samples", resourceManager.getID());
        List<JEVisSample> rawSamplesDown = cleanDataObject.getRawSamplesDown();
//        List<JEVisSample> rawSamplesUp = cleanDataObject.getRawSamplesUp();
        logger.info("[{}] raw samples found for cleaning: {}", resourceManager.getID(), rawSamplesDown.size());
        LogTaskManager.getInstance().getTask(resourceManager.getID()).addStep("Raw S.", rawSamplesDown.size() + "");

//        if (rawSamplesDown.isEmpty() || rawSamplesUp.isEmpty()) {
        if (rawSamplesDown.isEmpty()) {
            throw new RuntimeException(String.format("[%s] No new raw data. Stopping this job", resourceManager.getID()));
        }

        resourceManager.setRawSamplesDown(rawSamplesDown);

        Map<DateTime, JEVisSample> notesMap = cleanDataObject.getNotesMap();
        resourceManager.setNotesMap(notesMap);

        Map<DateTime, JEVisSample> userDataMap = cleanDataObject.getUserDataMap();
        resourceManager.setUserDataMap(userDataMap);

        List<PeriodRule> periodCleanData = cleanDataObject.getCleanDataPeriodAlignment();

        if (periodCleanData.isEmpty() && cleanDataObject.getIsPeriodAligned()) {
            throw new RuntimeException("No Input Sample Rate given for Object Clean Data and Attribute Value");
        } else if (cleanDataObject.getIsPeriodAligned()) {
            List<CleanInterval> cleanIntervals = getIntervals(cleanDataObject, periodCleanData);
            resourceManager.setIntervals(cleanIntervals);
        } else {
            List<CleanInterval> cleanIntervals = getIntervalsFromRawSamples(cleanDataObject, rawSamplesDown);
            resourceManager.setIntervals(cleanIntervals);
        }

        if (resourceManager.getIntervals().isEmpty()) {
            throw new RuntimeException(String.format("[%s] No new intervals. Stopping this job", resourceManager.getID()));
        }
    }

    private List<CleanInterval> getIntervals(CleanDataObject cleanDataObject, List<PeriodRule> periodCleanData) throws JEVisException {
        List<CleanInterval> cleanIntervals = new ArrayList<>();

        if (cleanDataObject.getMaxEndDate() == null) {
            logger.info("[{}] No Raw data, nothing to to", cleanDataObject.getCleanObject().getID());
            return cleanIntervals;
        }

        List<PeriodRule> periodRawData = cleanDataObject.getRawDataPeriodAlignment();
        List<DifferentialRule> differentialRules = cleanDataObject.getDifferentialRules();
        DateTime currentDate = cleanDataObject.getFirstDate();
        DateTimeFormatter datePattern = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        WorkDays wd = new WorkDays(cleanDataObject.getCleanObject());
        LocalTime dtStart = wd.getWorkdayStart();
        LocalTime dtEnd = wd.getWorkdayEnd();
        DateTime maxEndDate = cleanDataObject.getMaxEndDate();

        int indexLastRawSample = cleanDataObject.getRawSamplesDown().size() - 1;
        Period lastPeriod = CleanDataObject.getPeriodForDate(cleanDataObject.getCleanDataPeriodAlignment(), cleanDataObject.getRawSamplesDown().get(indexLastRawSample).getTimestamp());
        if (dtEnd.isBefore(dtStart) && IntStream.of(lastPeriod.getYears(), lastPeriod.getMonths(), lastPeriod.getWeeks()).anyMatch(i -> i > 0)) {
            currentDate = currentDate.minus(lastPeriod);
            maxEndDate = maxEndDate.minus(lastPeriod);
        }

        logger.info("[{}] getIntervals: currentDate: {}  MaxEndDate: {} ", cleanDataObject.getCleanObject().getID(), currentDate, maxEndDate);

        if (currentDate == null || maxEndDate == null || !currentDate.isBefore(maxEndDate)) {
            logger.warn("Nothing to do with only one interval");
            return cleanIntervals;
        } else {
            logger.info("[{}] Calc interval between start date {} and end date {}", cleanDataObject.getCleanObject().getID(), datePattern.print(currentDate), datePattern.print(maxEndDate));

            PeriodComparator periodComparator = new PeriodComparator();
            DateTime lastDate = null;

            Boolean firstIsDifferential = CleanDataObject.isDifferentialForDate(differentialRules, currentDate);
            Period firstRawPeriod = CleanDataObject.getPeriodForDate(periodRawData, currentDate);
            Period firstCleanPeriod = CleanDataObject.getPeriodForDate(periodCleanData, currentDate);
            int maxProcessingSize = cleanDataObject.getMaxProcessingSize();
            boolean isFinished = true;

            //add half a period to maxEndDate
            if (firstCleanPeriod.getYears() > 0) {
                currentDate = currentDate.minusYears(1).withMonthOfYear(1).withDayOfMonth(1);
                maxEndDate = maxEndDate.plusMonths(6);
            }
            if (firstCleanPeriod.getMonths() > 0) {
                currentDate = currentDate.minusMonths(1).withDayOfMonth(1);

                if (dtEnd.isBefore(dtStart)) {
                    maxEndDate = maxEndDate.plusMonths(1);
                }
            }
            if (firstCleanPeriod.getWeeks() > 0) {
                currentDate = currentDate.minusWeeks(1).withDayOfWeek(1);
                maxEndDate = maxEndDate.plusDays(3).plusHours(12);
            }
            if (firstCleanPeriod.getDays() > 0) {
                currentDate = currentDate.minusDays(1);
                maxEndDate = maxEndDate.plusHours(12);
            }
            if (firstCleanPeriod.getHours() > 0) {
                currentDate = currentDate.minusHours(1);
                maxEndDate = maxEndDate.plusMinutes(30);
            }

            while (currentDate.isBefore(maxEndDate) && !periodCleanData.isEmpty() && !currentDate.equals(lastDate)) {
                DateTime startDateTime = null;
                DateTime endDateTime = null;
                Period rawPeriod = CleanDataObject.getPeriodForDate(periodRawData, currentDate);
                Period cleanPeriod = CleanDataObject.getPeriodForDate(periodCleanData, currentDate);
                Boolean isDifferential = CleanDataObject.isDifferentialForDate(differentialRules, currentDate);
                boolean greaterThenDays = false;

                startDateTime = new DateTime(currentDate.getYear(), currentDate.getMonthOfYear(), currentDate.getDayOfMonth(),
                        currentDate.getHourOfDay(), currentDate.getMinuteOfHour(), currentDate.getSecondOfMinute());
                endDateTime = new DateTime(currentDate.getYear(), currentDate.getMonthOfYear(), currentDate.getDayOfMonth(),
                        currentDate.getHourOfDay(), currentDate.getMinuteOfHour(), currentDate.getSecondOfMinute());

                if (cleanPeriod.getYears() > 0) {
                    startDateTime = startDateTime.minusYears(cleanPeriod.getYears()).withMonthOfYear(1).withDayOfMonth(1);
                    endDateTime = startDateTime.plusYears(cleanPeriod.getYears()).withMonthOfYear(1).withDayOfMonth(1).minusDays(1);
                    greaterThenDays = true;
                }
                if (cleanPeriod.getMonths() > 0) {
                    startDateTime = startDateTime.minusMonths(cleanPeriod.getMonths()).withDayOfMonth(1);
                    endDateTime = startDateTime.plusMonths(cleanPeriod.getMonths()).withDayOfMonth(1).minusDays(1);
                    greaterThenDays = true;
                }
                if (cleanPeriod.getWeeks() > 0) {
                    startDateTime = startDateTime.minusWeeks(cleanPeriod.getWeeks()).withDayOfWeek(1);
                    endDateTime = startDateTime.plusWeeks(cleanPeriod.getWeeks()).withDayOfWeek(1).minusDays(1);
                    greaterThenDays = true;
                }
                if (cleanPeriod.getDays() > 0) {
                    startDateTime = startDateTime.minusDays(cleanPeriod.getDays()).withTime(0, 0, 0, 0);
                    endDateTime = startDateTime.plusDays(cleanPeriod.getDays()).minusSeconds(1);
                    greaterThenDays = true;
                }
                if (cleanPeriod.getHours() > 0) {
                    startDateTime = startDateTime.minusHours(cleanPeriod.getHours());
                }
                if (cleanPeriod.getMinutes() > 0) {
                    startDateTime = startDateTime.minusMinutes(cleanPeriod.getMinutes());
                }
                if (cleanPeriod.getSeconds() > 0) {
                    startDateTime = startDateTime.minusSeconds(cleanPeriod.getSeconds());
                }

                if (greaterThenDays) {
                    startDateTime = new DateTime(startDateTime.getYear(), startDateTime.getMonthOfYear(), startDateTime.getDayOfMonth(),
                            dtStart.getHour(), dtStart.getMinute(), dtStart.getSecond());
                    endDateTime = new DateTime(endDateTime.getYear(), endDateTime.getMonthOfYear(), endDateTime.getDayOfMonth(),
                            dtEnd.getHour(), dtEnd.getMinute(), dtEnd.getSecond());

                    if (dtEnd.isBefore(dtStart)) {
                        startDateTime = startDateTime.minusDays(1);
                    }
                }

                CleanInterval currentInterval;
                if (!greaterThenDays) {
                    Interval interval = null;
                    if (rawPeriod.equals(Period.minutes(15)) && cleanPeriod.equals(Period.minutes(5))) {
                        interval = new Interval(startDateTime.minusMinutes(5).plusSeconds(1), endDateTime.plusMinutes(5));
                    } else {
                        interval = new Interval(startDateTime.plusSeconds(1), endDateTime);
                    }

                    currentInterval = new CleanInterval(interval, endDateTime);
                    currentInterval.getResult().setTimeStamp(endDateTime);
                } else if (!isDifferential) {
                    Interval interval = new Interval(startDateTime.plusSeconds(1), endDateTime.plusSeconds(1));
                    currentInterval = new CleanInterval(interval, startDateTime);
                    currentInterval.getResult().setTimeStamp(startDateTime);
                } else {
                    Interval interval = new Interval(startDateTime.plusSeconds(1), endDateTime.plusSeconds(1));
                    currentInterval = new CleanInterval(interval, endDateTime.plusSeconds(1));
                    currentInterval.getResult().setTimeStamp(endDateTime.plusSeconds(1));
                }

                currentInterval.setInputPeriod(rawPeriod);
                currentInterval.setOutputPeriod(cleanPeriod);
                currentInterval.setDifferential(isDifferential);
                currentInterval.setCompare(periodComparator.compare(cleanPeriod, rawPeriod));
                cleanIntervals.add(currentInterval);

                lastDate = currentDate;
                currentDate = PeriodHelper.addPeriodToDate(currentDate, cleanPeriod);

                if (cleanIntervals.size() >= maxProcessingSize) {
                    isFinished = false;
                    break;
                }
            }

            DateTime startDate = cleanIntervals.get(0).getDate();
            DateTime endDate = cleanIntervals.get(cleanIntervals.size() - 1).getDate();

            logger.info("[{}] {} intervals calculated between {} and {}",
                    cleanDataObject.getCleanObject().getID(), cleanIntervals.size(),
                    startDate, endDate);

            processManager.setFinished(isFinished);
        }

        return cleanIntervals;
    }

    private void removeLastIntervalsWithoutSamples(CleanDataObject cleanDataObject, List<CleanInterval> cleanIntervals) throws JEVisException {
        List<JEVisSample> samples = cleanDataObject.getRawSamplesDown();
        List<CleanInterval> intervalsToRemove = new ArrayList<>();
        int lastSample = samples.size() - 1;

        for (int i = cleanIntervals.size() - 1; i > -1; i--) {
            CleanInterval cleanInterval = cleanIntervals.get(i);
            DateTime start = cleanInterval.getInterval().getStart();
            DateTime end = cleanInterval.getInterval().getEnd();
            boolean hasSamples = false;

            while (lastSample > -1) {
                JEVisSample sample = samples.get(lastSample);
                if (sample.getTimestamp().equals(end) || (sample.getTimestamp().isAfter(start) && sample.getTimestamp().isBefore(end))) {
                    hasSamples = true;
                    break;
                }

                if (lastSample > 0) {
                    lastSample--;
                } else break;
            }

            if (!hasSamples) {
                intervalsToRemove.add(cleanInterval);
            } else {
                break;
            }
        }

        cleanIntervals.removeAll(intervalsToRemove);
    }

    private List<CleanInterval> getIntervalsFromRawSamples(CleanDataObject cleanDataObject, List<JEVisSample> rawSamples) throws Exception {
        List<CleanInterval> cleanIntervals = new ArrayList<>();

        for (JEVisSample curSample : rawSamples) {

            DateTime timestamp = curSample.getTimestamp().plusSeconds(cleanDataObject.getPeriodOffset());
            Period rawPeriod = CleanDataObject.getPeriodForDate(cleanDataObject.getRawDataPeriodAlignment(), timestamp);
            Period cleanPeriod = CleanDataObject.getPeriodForDate(cleanDataObject.getCleanDataPeriodAlignment(), timestamp);

            DateTime start = timestamp.minusMillis(1);
            DateTime end = timestamp;

            Period periodForDate = CleanDataObject.getPeriodForDate(cleanDataObject.getRawDataPeriodAlignment(), timestamp);

            if (cleanDataObject.getIsPeriodAligned() && periodForDate.equals(Period.months(1))) {
                timestamp = timestamp.minusMonths(1).withDayOfMonth(1).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
                start = timestamp.plusMillis(1);
                end = timestamp.plusMonths(1).withDayOfMonth(1).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
            }

            Boolean isDifferential = CleanDataObject.isDifferentialForDate(cleanDataObject.getDifferentialRules(), timestamp);

            Interval interval = new Interval(start, end);
            CleanInterval cleanInterval = new CleanInterval(interval, timestamp);
            cleanInterval.getResult().setTimeStamp(timestamp);
            cleanInterval.setInputPeriod(rawPeriod);
            cleanInterval.setOutputPeriod(cleanPeriod);
            cleanInterval.setDifferential(isDifferential);
            cleanIntervals.add(cleanInterval);
        }

        logger.info("[{}] {} intervals calculated", cleanDataObject.getCleanObject().getID(), cleanIntervals.size());
        processManager.setFinished(true);

        return cleanIntervals;
    }
}
