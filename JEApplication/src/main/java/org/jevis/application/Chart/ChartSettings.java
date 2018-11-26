package org.jevis.application.Chart;

import static org.jevis.application.Chart.ChartType.AREA;

public class ChartSettings {

    private Long id;
    private String name;
    private ChartType chartType;
    private Double height;

    public ChartSettings(String name) {
        this.name = name;
        this.chartType = AREA;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public ChartType getChartType() {
        return chartType;
    }

    public void setChartType(ChartType chartType) {
        this.chartType = chartType;
    }
}
