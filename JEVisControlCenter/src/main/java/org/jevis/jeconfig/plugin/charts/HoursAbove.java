package org.jevis.jeconfig.plugin.charts;

import org.jevis.api.JEVisException;
import org.jevis.api.JEVisSample;
import org.jevis.commons.dataprocessing.CleanDataObject;
import org.jevis.commons.dataprocessing.processor.workflow.PeriodRule;
import org.jevis.commons.i18n.I18n;
import org.jevis.jeconfig.application.Chart.data.ChartDataRow;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import java.util.List;

public class HoursAbove {

    private final ChartDataRow chartDataRow;
    private final List<PeriodRule> periodRules;
    private String below = "";
    private String above = "";

    public HoursAbove(ChartDataRow chartDataRow, Double limit) {
        this.chartDataRow = chartDataRow;
        this.periodRules = CleanDataObject.getPeriodAlignmentForObject(chartDataRow.getAttribute().getObject());

        Period abovePeriod = Period.ZERO;
        Period belowPeriod = Period.ZERO;

        try {
            for (JEVisSample sample : chartDataRow.getSamples()) {
                if (sample.getValueAsDouble() < limit) {
                    belowPeriod = belowPeriod.plus(getPeriodForDate(sample.getTimestamp()));
                } else {
                    abovePeriod = abovePeriod.plus(getPeriodForDate(sample.getTimestamp()));
                }
            }
        } catch (JEVisException e) {
            e.printStackTrace();
        }

        below = PeriodFormat.wordBased().withLocale(I18n.getInstance().getLocale()).print(belowPeriod.normalizedStandard())
                + " (" + PeriodFormat.wordBased().withLocale(I18n.getInstance().getLocale()).print(belowPeriod.toStandardHours()) + ")";
        above = PeriodFormat.wordBased().withLocale(I18n.getInstance().getLocale()).print(abovePeriod.normalizedStandard())
                + " (" + PeriodFormat.wordBased().withLocale(I18n.getInstance().getLocale()).print(abovePeriod.toStandardHours()) + ")";
    }

    private Period getPeriodForDate(DateTime dateTime) {
        for (PeriodRule periodRule : periodRules) {
            PeriodRule nextRule = null;
            if (periodRules.size() > periodRules.indexOf(periodRule) + 1) {
                nextRule = periodRules.get(periodRules.indexOf(periodRule) + 1);
            }

            DateTime ts = periodRule.getStartOfPeriod();
            if (dateTime.equals(ts) || dateTime.isAfter(ts) && (nextRule == null || dateTime.isBefore(nextRule.getStartOfPeriod()))) {
                return periodRule.getPeriod();
            }
        }
        return Period.ZERO;
    }

    public String getBelow() {
        return below;
    }

    public String getAbove() {
        return above;
    }

    public ChartDataRow getChartDataRow() {
        return chartDataRow;
    }
}
