package org.jevis.jeconfig.application.Chart.Charts;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.jevis.commons.chart.ChartDataModel;
import org.jevis.jeconfig.application.Chart.ChartElements.TableEntry;
import org.jevis.jeconfig.application.Chart.Zoom.ChartPanManager;
import org.jevis.jeconfig.application.Chart.Zoom.JFXChartUtil;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

public class BubbleChart implements Chart {
    private javafx.scene.chart.BubbleChart<Number, Number> bubbleChart;
    private List<Color> hexColors = new ArrayList<>();

    public BubbleChart(List<ChartDataModel> chartDataModels, Boolean showRawData, Boolean showSum, Boolean hideShowIcons, Integer chartId, String chartName) {

    }

    @Override
    public String getChartName() {
        return null;
    }

    @Override
    public void setTitle(String s) {

    }

    @Override
    public Integer getChartId() {
        return null;
    }

    @Override
    public void updateTable(MouseEvent mouseEvent, DateTime valueForDisplay) {

    }

    @Override
    public DateTime getStartDateTime() {
        return null;
    }

    @Override
    public DateTime getEndDateTime() {
        return null;
    }

    @Override
    public void updateChart() {

    }

    @Override
    public void setDataModels(List<ChartDataModel> chartDataModels) {

    }

    @Override
    public void setHideShowIcons(Boolean hideShowIcons) {

    }

    @Override
    public void setChartSettings(ChartSettingsFunction function) {
        //TODO: implement me, see PieChart
    }

    @Override
    public ChartPanManager getPanner() {
        return null;
    }

    @Override
    public JFXChartUtil getJfxChartUtil() {
        return null;
    }

    @Override
    public void setRegion(Region region) {

    }

    @Override
    public void showNote(MouseEvent mouseEvent) {

    }

    @Override
    public void applyColors() {
        for (int i = 0; i < hexColors.size(); i++) {
            Color currentColor = hexColors.get(i);
            String preIdent = ".default-color" + i;
            Node node = bubbleChart.lookup(preIdent + ".chart-series-area-fill");
            Node nodew = bubbleChart.lookup(preIdent + ".chart-series-area-line");
        }
    }

    @Override
    public DateTime getValueForDisplay() {
        return null;
    }

    @Override
    public DateTime getNearest() {
        return null;
    }

    @Override
    public void setValueForDisplay(DateTime valueForDisplay) {

    }

    @Override
    public javafx.scene.chart.Chart getChart() {
        return null;
    }

    @Override
    public Region getRegion() {
        return null;
    }

    @Override
    public void initializeZoom() {

    }

    @Override
    public ObservableList<TableEntry> getTableData() {
        return null;
    }

    @Override
    public Period getPeriod() {
        return null;
    }
}
