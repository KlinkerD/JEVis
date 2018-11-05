/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.application.Chart.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.*;
import org.jevis.application.Chart.AnalysisTimeFrame;
import org.jevis.application.Chart.ChartDataModel;
import org.jevis.application.Chart.ChartSettings;
import org.jevis.application.Chart.ChartType;
import org.jevis.application.application.AppLocale;
import org.jevis.application.application.SaveResourceBundle;
import org.jevis.application.jevistree.AlphanumComparator;
import org.jevis.commons.dataprocessing.AggregationPeriod;
import org.jevis.commons.json.JsonAnalysisDataRow;
import org.jevis.commons.json.JsonChartDataModel;
import org.jevis.commons.json.JsonChartSettings;
import org.jevis.commons.unit.JEVisUnitImp;
import org.jevis.commons.ws.json.JsonUnit;
import org.joda.time.DateTime;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author broder
 */
public class GraphDataModel extends Observable {

    private static SaveResourceBundle rb = new SaveResourceBundle(AppLocale.BUNDLE_ID, AppLocale.getInstance().getLocale());
    private final Logger logger = LogManager.getLogger(GraphDataModel.class);
    private Set<ChartDataModel> selectedData = new HashSet<>();
    private Set<ChartSettings> charts = new HashSet<>();
    private Boolean hideShowIcons = true;
    private ObservableList<String> selectedDataNames = FXCollections.observableArrayList(new ArrayList<>());
    private AnalysisTimeFrame analysisTimeFrame = new AnalysisTimeFrame();
    private JEVisDataSource ds;
    private List<JEVisObject> listAnalyses = new ArrayList<>();
    private ObservableList<String> observableListAnalyses = FXCollections.observableArrayList();
    private JsonChartDataModel listAnalysisModel;
    private List<JsonChartSettings> listChartsSettings;
    private LocalTime workdayStart = LocalTime.of(0, 0, 0, 0);
    private LocalTime workdayEnd = LocalTime.of(23, 59, 59, 999999999);
    private JEVisObject currentAnalysis;
    private String nameCurrentAnalysis;

    public GraphDataModel(JEVisDataSource ds) {
        this.ds = ds;
    }


    public Set<ChartDataModel> getSelectedData() {
        return selectedData;
    }

    public void setSelectedData(Set<ChartDataModel> selectedData) {
        this.selectedData = selectedData;
        selectedDataNames.clear();


        if (getSelectedData() != null) {
            for (ChartDataModel mdl : getSelectedData()) {

                if (mdl.getSelected()) {
                    boolean found = false;
                    for (String chartName : mdl.getSelectedcharts()) {
                        for (ChartSettings set : getCharts()) {
                            if (chartName.equals(set.getName())) {
                                if (!selectedDataNames.contains(set.getName())) {

                                    selectedDataNames.add(set.getName());
                                    found = true;
                                }
                            }
                        }
                        if (!found) {
                            if (!selectedDataNames.contains(chartName)) {
                                getCharts().add(new ChartSettings(chartName));
                                selectedDataNames.add(chartName);
                            }
                        }
                    }
                }
            }
        }

        AlphanumComparator ac = new AlphanumComparator();
        selectedDataNames.sort(ac);

        setChanged();
        notifyObservers();
    }

    public Set<ChartSettings> getCharts() {
        return charts;
    }

    public void setCharts(Set<ChartSettings> charts) {
        this.charts = charts;
    }

    public Boolean getHideShowIcons() {
        return hideShowIcons;
    }

    public void setHideShowIcons(Boolean hideShowIcons) {
        this.hideShowIcons = hideShowIcons;

        setChanged();
        notifyObservers();
    }

    public ObservableList<String> getChartsList() {
        return selectedDataNames;
    }

    public boolean containsId(Long id) {
        if (!getSelectedData().isEmpty()) {
            AtomicBoolean found = new AtomicBoolean(false);
            getSelectedData().forEach(chartDataModel -> {
                if (chartDataModel.getObject().getID().equals(id)) {
                    found.set(true);
                }
            });
            return found.get();
        } else return false;
    }

    public ChartDataModel get(Long id) {
        AtomicReference<ChartDataModel> out = new AtomicReference<>();
        getSelectedData().forEach(chartDataModel -> {
            if (chartDataModel.getObject().getID().equals(id)) {
                out.set(chartDataModel);
            }
        });
        return out.get();
    }

    public AnalysisTimeFrame getAnalysisTimeFrame() {
        return analysisTimeFrame;
    }

    public void setAnalysisTimeFrame(AnalysisTimeFrame analysisTimeFrame) {
        this.analysisTimeFrame = analysisTimeFrame;

        DateHelper dateHelper = new DateHelper();
        if (getWorkdayStart() != null) dateHelper.setStartTime(getWorkdayStart());
        if (getWorkdayEnd() != null) dateHelper.setEndTime(getWorkdayEnd());

        switch (analysisTimeFrame.getTimeFrame()) {
            //Custom
            case custom:
                break;
            //today
            case today:
                dateHelper.setType(DateHelper.TransformType.TODAY);
                updateStartEndToDataModel(dateHelper);
                break;
            //last 7 days
            case last7Days:
                dateHelper.setType(DateHelper.TransformType.LAST7DAYS);
                updateStartEndToDataModel(dateHelper);
                break;
            //last 30 days
            case last30Days:
                dateHelper.setType(DateHelper.TransformType.LAST30DAYS);
                updateStartEndToDataModel(dateHelper);
                break;
            //yesterday
            case yesterday:
                dateHelper.setType(DateHelper.TransformType.YESTERDAY);
                updateStartEndToDataModel(dateHelper);
                break;
            //last Week days
            case lastWeek:
                dateHelper.setType(DateHelper.TransformType.LASTWEEK);
                updateStartEndToDataModel(dateHelper);
                break;
            case lastMonth:
                //last Month
                dateHelper.setType(DateHelper.TransformType.LASTMONTH);
                updateStartEndToDataModel(dateHelper);
                break;
            case customStartEnd:
                break;
            default:
                break;
        }
    }

    private void updateStartEndToDataModel(DateHelper dh) {
        getSelectedData().forEach(chartDataModel -> {
            if (chartDataModel.getSelected()) {
                chartDataModel.setSelectedStart(dh.getStartDate());
                chartDataModel.setSelectedStart(dh.getEndDate());
            }
        });
    }

    public void getListAnalysis() {
        try {
            ds.reloadAttributes();
            if (currentAnalysis == null) {
                updateListAnalyses();
                if (!observableListAnalyses.isEmpty())
                    setJEVisObjectForCurrentAnalysis(observableListAnalyses.get(0));
            }
            if (currentAnalysis != null) {
                if (Objects.nonNull(currentAnalysis.getAttribute("Data Model"))) {
                    if (currentAnalysis.getAttribute("Data Model").hasSample()) {
                        String str = currentAnalysis.getAttribute("Data Model").getLatestSample().getValueAsString();
                        try {
                            if (str.startsWith("[")) {
                                listAnalysisModel = new JsonChartDataModel();
                                List<JsonAnalysisDataRow> listOld = new Gson().fromJson(str, new TypeToken<List<JsonAnalysisDataRow>>() {
                                }.getType());
                                listAnalysisModel.setListDataRows(listOld);
                            } else {
                                try {
                                    listAnalysisModel = new Gson().fromJson(str, new TypeToken<JsonChartDataModel>() {
                                    }.getType());
                                } catch (Exception e) {
                                    logger.error(e);
                                    listAnalysisModel = new JsonChartDataModel();
                                    listAnalysisModel.getListAnalyses().add(new Gson().fromJson(str, JsonAnalysisDataRow.class));
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error: could not read data model", e);
                        }
                    }
                }
                if (Objects.nonNull(currentAnalysis.getAttribute("Charts"))) {
                    if (currentAnalysis.getAttribute("Charts").hasSample()) {
                        String str = currentAnalysis.getAttribute("Charts").getLatestSample().getValueAsString();
                        try {
                            if (str.endsWith("]")) {
                                listChartsSettings = new Gson().fromJson(str, new TypeToken<List<JsonChartSettings>>() {
                                }.getType());

                            } else {
                                listChartsSettings = new ArrayList<>();
                                listChartsSettings.add(new Gson().fromJson(str, JsonChartSettings.class));
                            }
                        } catch (Exception e) {
                            logger.error("Error: could not read chart settings", e);
                        }
                    }
                }
                updateWorkDays();
            }
        } catch (JEVisException e) {
            logger.error("Error: could not get analysis model", e);
        }
    }

    private void updateWorkDays() {
        try {
            JEVisObject site = currentAnalysis.getParents().get(0).getParents().get(0);
            LocalTime start = null;
            LocalTime end = null;
            try {
                JEVisAttribute attStart = site.getAttribute("Workday Beginning");
                JEVisAttribute attEnd = site.getAttribute("Workday End");
                if (attStart.hasSample()) {
                    String startStr = attStart.getLatestSample().getValueAsString();
                    DateTime dtStart = DateTime.parse(startStr);
                    start = LocalTime.of(dtStart.getHourOfDay(), dtStart.getMinuteOfHour(), 0, 0);
                }
                if (attEnd.hasSample()) {
                    String endStr = attEnd.getLatestSample().getValueAsString();
                    DateTime dtEnd = DateTime.parse(endStr);
                    end = LocalTime.of(dtEnd.getHourOfDay(), dtEnd.getMinuteOfHour(), 59, 999999999);
                }
            } catch (Exception e) {
            }

            if (start != null && end != null) {
                workdayStart = start;
                workdayEnd = end;
            }
        } catch (Exception e) {

        }
    }

    public void updateListAnalyses() {
        List<JEVisObject> listAnalysesDirectories = new ArrayList<>();
        try {
            JEVisClass analysesDirectory = ds.getJEVisClass("Analyses Directory");
            listAnalysesDirectories = ds.getObjects(analysesDirectory, false);
        } catch (JEVisException e) {
            logger.error("Error: could not get analyses directories", e);
        }
        if (listAnalysesDirectories.isEmpty()) {
            List<JEVisObject> listBuildings = new ArrayList<>();
            try {
                JEVisClass building = ds.getJEVisClass("Building");
                listBuildings = ds.getObjects(building, false);

                if (!listBuildings.isEmpty()) {
                    JEVisClass analysesDirectory = ds.getJEVisClass("Analyses Directory");
                    JEVisObject analysesDir = listBuildings.get(0).buildObject(rb.getString("plugin.graph.analysesdir.defaultname"), analysesDirectory);
                    analysesDir.commit();
                }
            } catch (JEVisException e) {
                logger.error("Error: could not create new analyses directory", e);
            }

        }
        try {
            listAnalyses = ds.getObjects(ds.getJEVisClass("Analysis"), false);
        } catch (JEVisException e) {
            logger.error("Error: could not get analysis", e);
        }
        observableListAnalyses.clear();
        for (JEVisObject obj : listAnalyses) {
            observableListAnalyses.add(obj.getName());
        }
        Collections.sort(observableListAnalyses, new AlphanumComparator());
    }

    public JsonChartDataModel getListAnalysisModel() {
        return listAnalysisModel;
    }

    public void setJEVisObjectForCurrentAnalysis(String s) {
        JEVisObject currentAnalysis = null;
        for (JEVisObject obj : listAnalyses) {
            if (obj.getName().equals(s)) {
                currentAnalysis = obj;
            }
        }
        this.currentAnalysis = currentAnalysis;
    }

    public ObservableList<String> getObservableListAnalyses() {
        return observableListAnalyses;
    }

    public List<JsonChartSettings> getListChartsSettings() {
        return listChartsSettings;
    }

    public LocalTime getWorkdayStart() {
        if (workdayStart == null)
            updateWorkDays();
        return workdayStart;
    }

    public LocalTime getWorkdayEnd() {
        if (workdayEnd == null)
            updateWorkDays();
        return workdayEnd;
    }

    public List<JEVisObject> getListAnalyses() {
        return listAnalyses;
    }

    public String getNameCurrentAnalysis() {
        if (nameCurrentAnalysis == null)
            this.nameCurrentAnalysis = currentAnalysis.getName();
        return nameCurrentAnalysis;
    }

    public void setNameCurrentAnalysis(String nameCurrentAnalysis) {
        this.nameCurrentAnalysis = nameCurrentAnalysis;
    }

    public JEVisObject getCurrentAnalysis() {
        return currentAnalysis;
    }

    public void setCurrentAnalysis(JEVisObject currentAnalysis) {
        this.currentAnalysis = currentAnalysis;
    }

    public Set<ChartDataModel> getChartDataModels() {
        Map<String, ChartDataModel> data = new HashMap<>();

        AnalysisTimeFrame newATF = new AnalysisTimeFrame();
        try {
            newATF.setTimeFrame(newATF.parseTimeFrameFromString(listAnalysisModel.getAnalysisTimeFrame().getTimeframe()));
            newATF.setId(Long.parseLong(listAnalysisModel.getAnalysisTimeFrame().getId()));
        } catch (Exception e) {
        }
        setAnalysisTimeFrame(newATF);

        for (JsonAnalysisDataRow mdl : getListAnalysisModel().getListAnalyses()) {
            ChartDataModel newData = new ChartDataModel();
            try {
                Long id = Long.parseLong(mdl.getObject());
                Long id_dp = null;
                if (mdl.getDataProcessorObject() != null) id_dp = Long.parseLong(mdl.getDataProcessorObject());
                JEVisObject obj = ds.getObject(id);
                JEVisObject obj_dp = null;
                if (mdl.getDataProcessorObject() != null) obj_dp = ds.getObject(id_dp);
                JEVisUnit unit = new JEVisUnitImp(new Gson().fromJson(mdl.getUnit(), JsonUnit.class));
                DateTime start;
                start = DateTime.parse(mdl.getSelectedStart());
                DateTime end;
                end = DateTime.parse(mdl.getSelectedEnd());
                Boolean selected = Boolean.parseBoolean(mdl.getSelected());
                newData.setObject(obj);

                newData.setSelectedStart(start);
                newData.setSelectedEnd(end);

                newData.setColor(Color.valueOf(mdl.getColor()));
                newData.setTitle(mdl.getName());
                if (mdl.getDataProcessorObject() != null) newData.setDataProcessor(obj_dp);
                newData.getAttribute();
                newData.setAggregationPeriod(AggregationPeriod.parseAggregation(mdl.getAggregation()));
                newData.setSelected(selected);
                newData.setSomethingChanged(true);
                newData.getSamples();
                newData.setSelectedCharts(stringToList(mdl.getSelectedCharts()));
                newData.setUnit(unit);
                data.put(obj.getID().toString(), newData);
            } catch (JEVisException e) {
                logger.error("Error: could not get chart data model", e);
            }
        }
        Set<ChartDataModel> selectedData = new HashSet<>();
        for (Map.Entry<String, ChartDataModel> entrySet : data.entrySet()) {
            ChartDataModel value = entrySet.getValue();
            if (value.getSelected()) {
                selectedData.add(value);
            }
        }
        return selectedData;
    }

    public Set<ChartSettings> getChartSettings() {
        Map<String, ChartSettings> chartSettingsHashMap = new HashMap<>();
        Set<ChartSettings> chartSettings = new HashSet<>();

        if (getListChartsSettings() != null && !getListChartsSettings().isEmpty()) {
            for (JsonChartSettings settings : getListChartsSettings()) {
                ChartSettings newSettings = new ChartSettings("");
                newSettings.setName(settings.getName());
                newSettings.setChartType(ChartType.parseChartType(settings.getChartType()));
                if (settings.getHeight() != null)
                    newSettings.setHeight(Double.parseDouble(settings.getHeight()));
                chartSettingsHashMap.put(newSettings.getName(), newSettings);
            }

            for (Map.Entry<String, ChartSettings> entrySet : chartSettingsHashMap.entrySet()) {
                ChartSettings value = entrySet.getValue();
                chartSettings.add(value);
            }
        }
        return chartSettings;
    }

    private List<String> stringToList(String s) {
        if (Objects.nonNull(s)) {
            List<String> tempList = new ArrayList<>(Arrays.asList(s.split(", ")));
            for (String str : tempList) if (str.contains(", ")) str.replace(", ", "");
            return tempList;
        } else return new ArrayList<>();
    }
}
