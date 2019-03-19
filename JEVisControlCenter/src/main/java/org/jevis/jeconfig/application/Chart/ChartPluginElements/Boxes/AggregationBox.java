package org.jevis.jeconfig.application.Chart.ChartPluginElements.Boxes;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import org.jevis.commons.chart.ChartDataModel;
import org.jevis.commons.dataprocessing.AggregationPeriod;
import org.jevis.jeconfig.application.Chart.data.GraphDataModel;
import org.jevis.jeconfig.tool.I18n;

public class AggregationBox extends ComboBox<AggregationPeriod> {

    public AggregationBox(GraphDataModel graphDataModel, ChartDataModel data) {

        final String keyPreset = I18n.getInstance().getString("plugin.graph.interval.preset");
        final String keyHourly = I18n.getInstance().getString("plugin.graph.interval.hourly");
        final String keyDaily = I18n.getInstance().getString("plugin.graph.interval.daily");
        final String keyWeekly = I18n.getInstance().getString("plugin.graph.interval.weekly");
        final String keyMonthly = I18n.getInstance().getString("plugin.graph.interval.monthly");
        final String keyQuarterly = I18n.getInstance().getString("plugin.graph.interval.quarterly");
        final String keyYearly = I18n.getInstance().getString("plugin.graph.interval.yearly");

        setItems(FXCollections.observableArrayList(AggregationPeriod.values()));

        Callback<ListView<AggregationPeriod>, ListCell<AggregationPeriod>> cellFactory = new Callback<javafx.scene.control.ListView<AggregationPeriod>, ListCell<AggregationPeriod>>() {
            @Override
            public ListCell<AggregationPeriod> call(javafx.scene.control.ListView<AggregationPeriod> param) {
                return new ListCell<AggregationPeriod>() {
                    @Override
                    protected void updateItem(AggregationPeriod aggregationPeriod, boolean empty) {
                        super.updateItem(aggregationPeriod, empty);
                        if (empty || aggregationPeriod == null) {
                            setText("");
                        } else {
                            String text = "";
                            switch (aggregationPeriod) {
                                case NONE:
                                    text = keyPreset;
                                    break;
                                case HOURLY:
                                    text = keyHourly;
                                    break;
                                case DAILY:
                                    text = keyDaily;
                                    break;
                                case WEEKLY:
                                    text = keyWeekly;
                                    break;
                                case MONTHLY:
                                    text = keyMonthly;
                                    break;
                                case QUARTERLY:
                                    text = keyQuarterly;
                                    break;
                                case YEARLY:
                                    text = keyYearly;
                                    break;
                            }
                            setText(text);
                        }
                    }
                };
            }
        };
        setCellFactory(cellFactory);
        setButtonCell(cellFactory.call(null));

        getSelectionModel().select(graphDataModel.getAggregationPeriod());

    }

}