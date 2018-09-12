package org.jevis.jeconfig.plugin.graph;

import org.jevis.commons.database.ObjectHandler;
import org.jevis.jeconfig.plugin.graph.data.CustomPeriodObject;
import org.jevis.jeconfig.tool.I18n;
import org.joda.time.DateTime;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class DateHelper {
    private LocalTime startTime = LocalTime.of(0, 0, 0, 0);
    private LocalTime endTime = LocalTime.of(23, 59, 59, 999);
    private DateTime startDate;
    private DateTime endDate;
    private LocalDate checkDate;
    private LocalTime checkTime;
    private TransformType type;
    private DateTime now;
    private InputType inputType;
    private Boolean userSet = true;
    private CustomPeriodObject customPeriodObject;

    public DateHelper(TransformType type) {
        this.type = type;
        now = DateTime.now();
    }

    public DateHelper() {
        now = DateTime.now();
    }

    private DateTime nowStartWithTime() {
        if (startTime.isAfter(endTime)) {
            DateTime now = DateTime.now();
            return new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(),
                    startTime.getHour(), startTime.getMinute(), startTime.getSecond()).minusDays(1);
        } else {
            return new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(),
                    startTime.getHour(), startTime.getMinute(), startTime.getSecond());
        }
    }

    private DateTime nowEndWithTime() {
        DateTime now = DateTime.now();
        return new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(),
                endTime.getHour(), endTime.getMinute(), endTime.getSecond());
    }

    public DateHelper(InputType inputType, LocalDate localDate) {
        this.inputType = inputType;
        checkDate = localDate;
    }

    public DateHelper(InputType inputType, LocalTime localTime) {
        this.inputType = inputType;
        checkTime = localTime;
    }

    public DateHelper(CustomPeriodObject cpo, TransformType type) {
        this.customPeriodObject = cpo;
        this.type = type;
    }

    public DateTime getStartDate() {

        switch (type) {
            case CUSTOM:
                break;
            case TODAY:
                if (startTime.isAfter(endTime)) now = now.minusDays(1);
                startDate = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), startTime.getHour(), startTime.getMinute(), startTime.getSecond());
                break;
            case LAST7DAYS:
                if (startTime.isAfter(endTime)) now = now.minusDays(1);
                startDate = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), startTime.getHour(), startTime.getMinute(), startTime.getSecond()).minusDays(7);
                break;
            case LAST30DAYS:
                if (startTime.isAfter(endTime)) now = now.minusDays(1);
                startDate = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), startTime.getHour(), startTime.getMinute(), startTime.getSecond()).minusDays(30);
                break;
            case LASTDAY:
                if (startTime.isAfter(endTime)) now = now.minusDays(1);
                startDate = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), startTime.getHour(), startTime.getMinute(), startTime.getSecond()).minusDays(1);
                break;
            case LASTWEEK:
                if (startTime.isAfter(endTime)) now = now.minusDays(1);
                now = now.minusDays(now.getDayOfWeek() - 1).minusWeeks(1);
                startDate = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), startTime.getHour(), startTime.getMinute(), startTime.getSecond());
                break;
            case LASTMONTH:
                now = now.minusDays(now.getDayOfMonth() - 1);
                startDate = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), startTime.getHour(), startTime.getMinute(), startTime.getSecond()).minusMonths(1);
                break;
            case CUSTOM_PERIOD:
                if (customPeriodObject.getStartReferencePoint() != null) {
                    Long startYears = 0L;
                    Long startMonths = 0L;
                    Long startWeeks = 0L;
                    Long startDays = 0L;
                    Long startHours = 0L;
                    Long startMinutes = 0L;
                    switch (customPeriodObject.getStartReferencePoint()) {
                        case "NOW":
                            startDate = DateTime.now();
                            break;
                        case "STARTTIMEDAY":
                            break;
                        case "CUSTOM_PERIOD":
                            try {
                                CustomPeriodObject cpo = new CustomPeriodObject(customPeriodObject.getStartReferenceObject(),
                                        new ObjectHandler(customPeriodObject.getObject().getDataSource()));
                                DateHelper dh = new DateHelper(cpo, TransformType.CUSTOM_PERIOD);
                                dh.setStartTime(startTime);
                                dh.setEndTime(endTime);
                                if (cpo.getStartReferencePoint().contains("DAY")) {

                                    Long startInterval = customPeriodObject.getStartInterval();
                                    DateTime newDT = getDateTimeForDayPeriod(dh, startInterval);

                                    startDate = new DateTime(newDT.getYear(), newDT.getMonthOfYear(), newDT.getDayOfMonth(), newDT.getHourOfDay(), newDT.getMinuteOfHour(), newDT.getSecondOfMinute());
                                }
                            } catch (Exception e) {
                            }
                            break;
                        case "CURRENT_WEEK":
                            startDate = now.minusDays(now.getDayOfWeek());
                            break;
                        case "CURRENT_MONTH":
                            startDate = now.minusDays(now.getDayOfMonth());
                            break;
                        case "CURRENT_DAY":
                            startDate = DateTime.now();
                            break;
                        default:
                            break;
                    }

                    startYears = customPeriodObject.getStartYears();
                    startMonths = customPeriodObject.getStartMonths();
                    startWeeks = customPeriodObject.getStartWeeks();
                    startDays = customPeriodObject.getStartDays();
                    startHours = customPeriodObject.getStartHours();
                    startMinutes = customPeriodObject.getStartMinutes();

                    if (startYears < 0)
                        startDate = startDate.minusYears((int) Math.abs(startYears));
                    else if (startYears > 0)
                        startDate = startDate.plusYears((int) Math.abs(startYears));

                    if (startMonths < 0)
                        startDate = startDate.minusMonths((int) Math.abs(startMonths));
                    else if (startMonths > 0)
                        startDate = startDate.plusMonths((int) Math.abs(startMonths));

                    if (startWeeks < 0)
                        startDate = startDate.minusWeeks((int) Math.abs(startWeeks));
                    else if (startWeeks > 0)
                        startDate = startDate.plusWeeks((int) Math.abs(startWeeks));

                    if (startDays < 0)
                        startDate = startDate.minusDays((int) Math.abs(startDays));
                    else if (startDays > 0)
                        startDate = startDate.plusDays((int) Math.abs(startDays));

                    if (startHours < 0)
                        startDate = startDate.minusHours((int) Math.abs(startHours));
                    else if (startHours > 0)
                        startDate = startDate.plusHours((int) Math.abs(startHours));

                    if (startMinutes < 0)
                        startDate = startDate.minusMinutes((int) Math.abs(startMinutes));
                    else if (startMinutes > 0)
                        startDate = startDate.plusMinutes((int) Math.abs(startMinutes));
                }
                break;
            default:
                break;
        }
        return startDate;
    }

    public void setStartDate(DateTime startDate) {
        this.startDate = startDate;
    }

    private DateTime getDateTimeForDayPeriod(DateHelper dh, Long interval) {
        DateTime returnTimeStamp = null;

        DateTime start = nowStartWithTime();
        DateTime end = nowEndWithTime();

        Long d = dh.getEndDate().getMillis() - dh.getStartDate().getMillis();

        if (d > 0) {
            List<DateTime> listTimeStamps = new ArrayList<>();
            listTimeStamps.add(start);
            DateTime currentDateTime = start.plus(d);
            while (currentDateTime.isBefore(end)) {
                listTimeStamps.add(currentDateTime);
                currentDateTime = currentDateTime.plus(d);
            }

            if (interval < 0) {
                Integer index = (int) Math.abs(interval);
                for (int i = listTimeStamps.size() - 1; i >= 0; i--) {
                    if (listTimeStamps.get(i).isBefore(DateTime.now())) return listTimeStamps.get(i - index);
                }
            } else if (interval > 0) {
                Integer index = (int) Math.abs(interval);
                for (int i = listTimeStamps.size() - 1; i >= 0; i--) {
                    if (listTimeStamps.get(i).isBefore(DateTime.now())) return listTimeStamps.get(i + index);
                }
            } else {
                for (int i = listTimeStamps.size() - 1; i >= 0; i--) {
                    if (listTimeStamps.get(i).isBefore(DateTime.now())) return listTimeStamps.get(i);
                }
            }
        }

        return returnTimeStamp;
    }

    public void setType(TransformType type) {
        this.type = type;
    }

    public DateTime getEndDate() {
        //if (startTime.isAfter(endTime)) now = now.minusDays(1);

        switch (type) {
            case CUSTOM:
                break;
            case TODAY:
                endDate = now;
                break;
            case LAST7DAYS:
                endDate = now;
                break;
            case LAST30DAYS:
                endDate = now;
                break;
            case LASTDAY:
                endDate = now.minusDays(1);
                break;
            case LASTWEEK:
                now = now.minusDays(now.getDayOfWeek() - 1).minusWeeks(1);
                endDate = new DateTime(now.getYear(), now.getDayOfMonth(), now.getDayOfMonth(), endTime.getHour(), endTime.getMinute(), endTime.getSecond()).plusDays(6);
                break;
            case LASTMONTH:
                now = now.minusDays(now.getDayOfMonth() - 1);
                endDate = new DateTime(now.getYear(), now.getDayOfMonth(), now.getDayOfMonth(), endTime.getHour(), endTime.getMinute(), endTime.getSecond()).minusDays(1);
                break;
            case CUSTOM_PERIOD:
                if (customPeriodObject.getEndReferencePoint() != null) {
                    Long endYears = 0L;
                    Long endMonths = 0L;
                    Long endWeeks = 0L;
                    Long endDays = 0L;
                    Long endHours = 0L;
                    Long endMinutes = 0L;
                    switch (customPeriodObject.getEndReferencePoint()) {
                        case "NOW":
                            endDate = DateTime.now();
                            break;
                        case "STARTTIMEDAY":
                            break;
                        case "CUSTOM_PERIOD":
                            try {
                                CustomPeriodObject cpo = new CustomPeriodObject(customPeriodObject.getEndReferenceObject(),
                                        new ObjectHandler(customPeriodObject.getObject().getDataSource()));
                                DateHelper dh = new DateHelper(cpo, TransformType.CUSTOM_PERIOD);
                                dh.setStartTime(startTime);
                                dh.setEndTime(endTime);
                                if (cpo.getEndReferencePoint().contains("DAY")) {

                                    Long endInterval = customPeriodObject.getEndInterval();
                                    DateTime newDT = getDateTimeForDayPeriod(dh, endInterval);

                                    endDate = new DateTime(newDT.getYear(), newDT.getMonthOfYear(), newDT.getDayOfMonth(), newDT.getHourOfDay(), newDT.getMinuteOfHour(), newDT.getSecondOfMinute());
                                }
                            } catch (Exception e) {
                            }
                            break;
                        case "CURRENT_WEEK":
                            endDate = now.minusDays(now.getDayOfWeek());
                            break;
                        case "CURRENT_MONTH":
                            endDate = now.minusDays(now.getDayOfMonth());
                            break;
                        case "CURRENT_DAY":
                            endDate = DateTime.now();
                            break;
                        default:
                            break;
                    }
                    endYears = customPeriodObject.getEndYears();
                    endMonths = customPeriodObject.getEndMonths();
                    endWeeks = customPeriodObject.getEndWeeks();
                    endDays = customPeriodObject.getEndDays();
                    endHours = customPeriodObject.getEndHours();
                    endMinutes = customPeriodObject.getEndMinutes();

                    if (endYears < 0)
                        endDate = endDate.minusYears((int) Math.abs(endYears));
                    else if (endYears > 0)
                        endDate = endDate.plusYears((int) Math.abs(endYears));

                    if (endMonths < 0)
                        endDate = endDate.minusMonths((int) Math.abs(endMonths));
                    else if (endMonths > 0)
                        endDate = endDate.plusMonths((int) Math.abs(endMonths));

                    if (endWeeks < 0)
                        endDate = endDate.minusWeeks((int) Math.abs(endWeeks));
                    else if (endWeeks > 0)
                        endDate = endDate.plusWeeks((int) Math.abs(endWeeks));

                    if (endDays < 0)
                        endDate = endDate.minusDays((int) Math.abs(endDays));
                    else if (endDays > 0)
                        endDate = endDate.plusDays((int) Math.abs(endDays));

                    if (endHours < 0)
                        endDate = endDate.minusHours((int) Math.abs(endHours));
                    else if (endHours > 0)
                        endDate = endDate.plusHours((int) Math.abs(endHours));

                    if (endMinutes < 0)
                        endDate = endDate.minusMinutes((int) Math.abs(endMinutes));
                    else if (endMinutes > 0)
                        endDate = endDate.plusMinutes((int) Math.abs(endMinutes));
                }
                break;
            default:
                break;
        }
        return endDate;
    }

    public void setEndDate(DateTime endDate) {
        this.endDate = endDate;
    }

    public LocalTime getStartTime() {
        DateTime start = getStartDate();
        return LocalTime.of(start.getHourOfDay(), start.getMinuteOfHour(), start.getSecondOfMinute());
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public enum TransformType {CUSTOM, TODAY, LAST7DAYS, LAST30DAYS, LASTDAY, LASTWEEK, LASTMONTH, CUSTOM_PERIOD}

    public enum InputType {STARTDATE, ENDDATE, STARTTIME, ENDTIME}

    public enum CustomReferencePoint {

        NOW(I18n.getInstance().getString("graph.datehelper.referencepoint.now")), CUSTOM_PERIOD(I18n.getInstance().getString("graph.datehelper.referencepoint.customperiod")),
        WEEKDAY(I18n.getInstance().getString("graph.datehelper.referencepoint.weekday")), MONTH(I18n.getInstance().getString("graph.datehelper.referencepoint.month")),
        STARTTIMEDAY(I18n.getInstance().getString("graph.datehelper.referencepoint.starttimeday")), EDNTIMEDAY(I18n.getInstance().getString("graph.datehelper.referencepoint.endtimeday"));

        private final String referencePointName;

        CustomReferencePoint(String referencePointName) {
            this.referencePointName = referencePointName;
        }

        public String getReferencePointName() {
            return referencePointName;
        }
    }

    public enum Weekday {

        MONDAY(I18n.getInstance().getString("graph.datehelper.weekday.monday")), TUESDAY(I18n.getInstance().getString("graph.datehelper.weekday.tuesday")),
        WEDNESDAY(I18n.getInstance().getString("graph.datehelper.weekday.wednesday")), THURSDAY(I18n.getInstance().getString("graph.datehelper.weekday.thursday")),
        FRIDAY(I18n.getInstance().getString("graph.datehelper.weekday.friday")), SATURDAY(I18n.getInstance().getString("graph.datehelper.weekday.saturday")),
        SUNDAY(I18n.getInstance().getString("graph.datehelper.weekday.sunday"));

        private final String weekdayName;

        Weekday(String weekdayName) {
            this.weekdayName = weekdayName;
        }

        public String getWeekdayName() {
            return weekdayName;
        }
    }

    public enum Month {

        JANUARY(I18n.getInstance().getString("graph.datehelper.months.january")), FEBRUARY(I18n.getInstance().getString("graph.datehelper.months.february")),
        MARCH(I18n.getInstance().getString("graph.datehelper.months.march")), APRIL(I18n.getInstance().getString("graph.datehelper.months.april")),
        MAY(I18n.getInstance().getString("graph.datehelper.months.may")), JUNE(I18n.getInstance().getString("graph.datehelper.months.june")),
        JULY(I18n.getInstance().getString("graph.datehelper.months.july")), AUGUST(I18n.getInstance().getString("graph.datehelper.months.august")),
        SEPTEMBER(I18n.getInstance().getString("graph.datehelper.months.september")), OCTOBER(I18n.getInstance().getString("graph.datehelper.months.october")),
        NOVEMBER(I18n.getInstance().getString("graph.datehelper.months.november")), DECEMBER(I18n.getInstance().getString("graph.datehelper.months.december"));

        private final String monthName;

        Month(String monthName) {
            this.monthName = monthName;
        }

        public String getMonthName() {
            return monthName;
        }
    }

    public LocalTime getEndTime() {
        DateTime end = getEndDate();
        return LocalTime.of(end.getHourOfDay(), end.getMinuteOfHour(), end.getSecondOfMinute());
    }

    public Boolean isCustom() {
        switch (inputType) {
            case STARTDATE:
                for (TransformType tt : TransformType.values()) {
                    DateHelper dh = new DateHelper(tt);
                    dh.setStartTime(startTime);
                    dh.setEndTime(endTime);
                    dh.setStartDate(getStartDate());
                    dh.setEndDate(getEndDate());
                    dh.setCheckDate(checkDate);
                    if (customPeriodObject != null) dh.setCustomPeriodObject(customPeriodObject);
                    if (dh.getCheckDate().equals(dh.getStartDate())) {
                        userSet = false;
                        break;
                    }
                }
                break;
            case ENDDATE:
                for (TransformType tt : TransformType.values()) {
                    DateHelper dh = new DateHelper(tt);
                    dh.setStartTime(startTime);
                    dh.setEndTime(endTime);
                    dh.setStartDate(getStartDate());
                    dh.setEndDate(getEndDate());
                    dh.setCheckDate(checkDate);
                    if (customPeriodObject != null) dh.setCustomPeriodObject(customPeriodObject);
                    if (dh.getCheckDate().equals(dh.getEndDate())) {
                        userSet = false;
                        break;
                    }
                }
                break;
            case STARTTIME:
                if (checkTime.equals(getStartTime())) {
                    userSet = false;
                    break;
                }
                break;
            case ENDTIME:
                if (checkTime.equals(getEndTime())) {
                    userSet = false;
                    break;
                }
                break;
            default:
                break;
        }

        return userSet;
    }

    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public void setCustomPeriodObject(CustomPeriodObject customPeriodObject) {
        this.customPeriodObject = customPeriodObject;
    }

    public void setCheckTime(LocalTime checkTime) {
        this.checkTime = checkTime;
    }

    public LocalDate getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(LocalDate checkDate) {
        this.checkDate = checkDate;
    }

    public LocalDate getStartAsLocalDate() {
        DateTime start = getStartDate();
        return LocalDate.of(start.getYear(), start.getMonthOfYear(), start.getDayOfMonth());
    }

    public LocalDate getEndAsLocalDate() {
        DateTime end = getEndDate();
        return LocalDate.of(end.getYear(), end.getMonthOfYear(), end.getDayOfMonth());
    }
}
