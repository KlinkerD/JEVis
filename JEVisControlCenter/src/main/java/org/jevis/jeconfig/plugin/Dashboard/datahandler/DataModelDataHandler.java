package org.jevis.jeconfig.plugin.Dashboard.datahandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.property.*;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisObject;
import org.jevis.api.JEVisSample;
import org.jevis.commons.chart.ChartDataModel;
import org.jevis.commons.dataprocessing.AggregationPeriod;
import org.jevis.commons.dataprocessing.ManipulationMode;
import org.jevis.jeconfig.application.jevistree.plugin.SimpleTargetPlugin;
import org.jevis.jeconfig.plugin.Dashboard.config.DataModelNode;
import org.jevis.jeconfig.plugin.Dashboard.config.DataPointNode;
import org.jevis.jeconfig.plugin.Dashboard.timeframe.LastPeriod;
import org.jevis.jeconfig.plugin.Dashboard.timeframe.TimeFrameFactory;
import org.jevis.jeconfig.plugin.Dashboard.timeframe.TimeFrames;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataModelDataHandler {

    public final static String TYPE = "SimpleDataHandler";
    private static final Logger logger = LogManager.getLogger(DataModelDataHandler.class);
    private final JEVisDataSource jeVisDataSource;
    public ObjectProperty<DateTime> lastUpdate = new SimpleObjectProperty<>();
    private Map<String, JEVisAttribute> attributeMap = new HashMap<>();
    private BooleanProperty enableMultiSelect = new SimpleBooleanProperty(false);
    private StringProperty unitProperty = new SimpleStringProperty("");
    private SimpleTargetPlugin simpleTargetPlugin = new SimpleTargetPlugin();
    private List<ChartDataModel> chartDataModels = new ArrayList<>();
    private ObjectProperty<Interval> durationProperty = new SimpleObjectProperty<>();
    private DataModelNode dataModelNode = new DataModelNode();
    private boolean autoAggregation = false;
    private boolean forcedInterval = false;
    private TimeFrames timeFrames;
    private List<TimeFrameFactory> timeFrameFactories = new ArrayList<>();
    private String forcedPeriod;
    private ObjectMapper mapper = new ObjectMapper();

    public DataModelDataHandler(JEVisDataSource jeVisDataSource, JsonNode configNode) {
        this.jeVisDataSource = jeVisDataSource;

//        if (configNode != null) {
//            try {
//                System.out.println("DMDH: " + new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(configNode));
//
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }

        try {
            if (configNode != null) {


                DataModelNode dataModelNode = this.mapper.treeToValue(configNode, DataModelNode.class);
                this.dataModelNode = dataModelNode;

//                JsonNode widgets = jsonNode.get(WIDGET_NODE);

//                if (configNode.isArray()) {
//                    System.out.println("-> data is array");
//                    for (final JsonNode objNode : configNode) {
//
//                        WidgetPojo newConfig = new WidgetPojo(objNode);
//                        dashboardPojo.getWidgetList().add(newConfig);
//                        logger.debug("Add Widget: {}", newConfig);
//                    }
//                }


            }

        } catch (Exception ex) {
            logger.error(ex);
        }

        if (!this.dataModelNode.getForcedInterval().isEmpty()) {
            this.forcedInterval = true;
            try {
                this.forcedPeriod = this.dataModelNode.getForcedInterval();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }


        setData(this.dataModelNode.getData());
        this.timeFrames = new TimeFrames(jeVisDataSource);
        this.timeFrames.setWorkdays(this.chartDataModels.stream().findFirst().map(ChartDataModel::getObject).orElse(null));
        this.timeFrameFactories.addAll(this.timeFrames.getAll());


    }

    public void setData(List<DataPointNode> data) {
        this.dataModelNode.setData(data);
        this.chartDataModels.clear();
        this.attributeMap.clear();

        this.dataModelNode.getData().forEach(dataPointNode -> {
            try {
//                logger.error("Add node: " + dataPointNode.toString());
//                logger.debug("Add attribute: {}:{}", dataPointNode.getObjectID(), dataPointNode.getAttribute());
                JEVisObject jevisobject = this.jeVisDataSource.getObject(dataPointNode.getObjectID());
                JEVisObject cleanObject = null;
                if (dataPointNode.getCleanObjectID() != null && dataPointNode.getCleanObjectID() > 0) {
                    cleanObject = this.jeVisDataSource.getObject(dataPointNode.getCleanObjectID());
                }


                if (jevisobject != null) {
                    JEVisAttribute jeVisAttribute = jevisobject.getAttribute(dataPointNode.getAttribute());
                    if (jeVisAttribute != null) {
                        ChartDataModel chartDataModel = new ChartDataModel(jevisobject.getDataSource());
                        List<Integer> list = new ArrayList<>();
                        list.add(0);

                        /** add fake start date so the model does not ty to load the last 7 days **/
                        chartDataModel.setSelectedStart(new DateTime(1000, 1, 1, 1, 1, 1));
                        chartDataModel.setSelectedEnd(new DateTime(1000, 1, 1, 1, 1, 2));

                        chartDataModel.setAbsolute(dataPointNode.isAbsolute());
                        chartDataModel.setSelectedCharts(list);
                        chartDataModel.setObject(jeVisAttribute.getObject());
                        chartDataModel.setAttribute(jeVisAttribute);
                        if (cleanObject != null) {
                            chartDataModel.setDataProcessor(cleanObject);
                            chartDataModel.setAttribute(cleanObject.getAttribute(dataPointNode.getAttribute()));
                        }


                        chartDataModel.setManipulationMode(dataPointNode.getManipulationMode());
                        chartDataModel.setAggregationPeriod(dataPointNode.getAggregationPeriod());
                        chartDataModel.setEnPI(dataPointNode.isEnpi());


                        if (dataPointNode.getColor() != null) {
                            chartDataModel.setColor(dataPointNode.getColor());
                        } else {
                            chartDataModel.setColor(Color.LIGHTBLUE);
                        }


                        this.chartDataModels.add(chartDataModel);


                        this.attributeMap.put(generateValueKey(jeVisAttribute), jeVisAttribute);

                        if (dataPointNode.isEnpi()) {
                            chartDataModel.setEnPI(dataPointNode.isEnpi());
                            if (dataPointNode.getCalculationID() != null && !dataPointNode.getCalculationID().equals("0")) {
                                chartDataModel.setCalculationObject(dataPointNode.getCalculationID().toString());
                            }

                        }

                    } else {
                        logger.error("Attribute does not exist: {}", dataPointNode.getAttribute());
                    }


                } else {
                    logger.error("Object not found: {}", dataPointNode.getObjectID());
                }
            } catch (Exception ex) {
//                logger.error("Error '{}' in line {}: ", ex.getMessage(), ex.getStackTrace()[0].getLineNumber(), ex.getStackTrace()[0]);
                ex.printStackTrace();
            }

        });

    }

    /**
     * Set if the date in the interval will use the auto aggregation
     * [if -> then]
     * Day -> Display Interval
     * Week -> Hourly
     * Month -> Daily
     * Year -> Weekly
     *
     * @param enable
     */
    public void setAutoAggregation(boolean enable) {
        this.autoAggregation = enable;
    }

    public static String generateValueKey(JEVisAttribute attribute) {
        return attribute.getObjectID() + ":" + attribute.getName();
    }


    public JEVisDataSource getJeVisDataSource() {
        return this.jeVisDataSource;
    }

    public void setInterval(Interval interval) {


        if (this.forcedInterval) {

            boolean foundFactory = false;

            for (TimeFrameFactory timeFrameFactory : this.timeFrameFactories) {
                if (timeFrameFactory.getID().equals(this.forcedPeriod)) {
                    interval = timeFrameFactory.getInterval(interval.getEnd());
                    foundFactory = true;
                }
            }

            if (!foundFactory) {
                try {
                    LastPeriod lastPeriod = new LastPeriod(Period.parse(this.forcedPeriod));
                    interval = lastPeriod.getInterval(interval.getEnd());

                } catch (Exception ex) {
                    logger.error(ex);
                }
            }


        }
        this.durationProperty.setValue(interval);


        for (ChartDataModel chartDataModel : getDataModel()) {

            AggregationPeriod aggregationPeriod = AggregationPeriod.NONE;
            ManipulationMode manipulationMode = ManipulationMode.NONE;
            if (this.autoAggregation) {

                /** less then an week take original **/
                if (interval.toDuration().getStandardDays() < 6) {
                    aggregationPeriod = AggregationPeriod.NONE;
                }
                /** less then an month take hour **/
                else if (interval.toDuration().getStandardDays() < 32) {
                    aggregationPeriod = AggregationPeriod.HOURLY;
                    manipulationMode = ManipulationMode.NONE;
                }
                /** less than year take day **/
                else if (interval.toDuration().getStandardDays() < 364) {
                    aggregationPeriod = AggregationPeriod.DAILY;
                    manipulationMode = ManipulationMode.NONE;
                }
                /** more than an year take week **/
                else {
                    aggregationPeriod = AggregationPeriod.WEEKLY;
                    manipulationMode = ManipulationMode.NONE;
                }
                chartDataModel.setAggregationPeriod(aggregationPeriod);
                chartDataModel.setManipulationMode(manipulationMode);
            }

        }
    }

    public JsonNode toJsonNode() {
        ArrayNode dataArrayNode = JsonNodeFactory.instance.arrayNode();

        this.dataModelNode.getData().forEach(dataPointNode -> {
            try {
                logger.debug("Add attribute: {}:{}", dataPointNode.getObjectID(), dataPointNode.getAttribute());

                ObjectNode dataNode = JsonNodeFactory.instance.objectNode();
                dataNode.put("objectID", dataPointNode.getObjectID());
                dataNode.put("attribute", dataPointNode.getAttribute());
                dataNode.put("cleanObjectID", dataPointNode.getCleanObjectID());
                dataNode.put("calculationID", dataPointNode.getCalculationID());
                dataNode.put("aggregationPeriod", dataPointNode.getAggregationPeriod().toString());
                dataNode.put("manipulationMode", dataPointNode.getManipulationMode().toString());

                dataNode.put("color", dataPointNode.getColor().toString());
                dataArrayNode.add(dataNode);
            } catch (Exception ex) {
                logger.error(ex);
            }
        });
        ObjectNode dataHandlerNode = JsonNodeFactory.instance.objectNode();
        dataHandlerNode.set("data", dataArrayNode);
        dataHandlerNode.set("type", JsonNodeFactory.instance.textNode(TYPE));

        return dataHandlerNode;

    }

    public Map<String, JEVisAttribute> getAttributeMap() {
        return this.attributeMap;
    }

    public List<ChartDataModel> getDataModel() {
        return this.chartDataModels;
    }

    public DataModelNode getDateNode() {
        return this.dataModelNode;
    }

    public void update() {
        logger.debug("Update Samples: {}", this.durationProperty.getValue());
//        logger.error("AttributeMap: {}", attributeMap.size());

        this.chartDataModels.forEach(chartDataModel -> {

            chartDataModel.setSelectedStart(this.durationProperty.getValue().getStart());
            chartDataModel.setSelectedEnd(this.durationProperty.getValue().getEnd());

        });

        this.lastUpdate.setValue(new DateTime());
    }

    public void setMultiSelect(boolean enable) {
        this.enableMultiSelect.set(enable);
    }


    public StringProperty getUnitProperty() {
        return this.unitProperty;
    }


    public static Double getTotal(List<JEVisSample> samples) {
        Double total = 0d;
        for (JEVisSample jeVisSample : samples) {
            try {
                total += jeVisSample.getValueAsDouble();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return total;

    }
}
