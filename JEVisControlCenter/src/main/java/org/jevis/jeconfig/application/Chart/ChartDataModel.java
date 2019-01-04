package org.jevis.jeconfig.application.Chart;

import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.*;
import org.jevis.commons.dataprocessing.AggregationPeriod;
import org.jevis.commons.dataprocessing.ManipulationMode;
import org.jevis.commons.dataprocessing.SampleGenerator;
import org.jevis.commons.dataprocessing.VirtualSample;
import org.jevis.commons.unit.UnitManager;
import org.jevis.jeconfig.application.Chart.ChartUnits.ChartUnits;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class ChartDataModel {
    private static final Logger logger = LogManager.getLogger(ChartDataModel.class);

    private String _title;
    private DateTime _selectedStart;
    private DateTime _selectedEnd;
    private JEVisObject _object;
    private JEVisAttribute _attribute;
    private Color _color = Color.LIGHTBLUE;
    private AggregationPeriod aggregationPeriod = AggregationPeriod.NONE;
    private ManipulationMode manipulationMode = ManipulationMode.NONE;
    private JEVisObject _dataProcessorObject = null;
    private List<JEVisSample> samples = new ArrayList<>();
    private boolean _somethingChanged = true;
    private JEVisUnit _unit;
    private List<Integer> _selectedCharts = new ArrayList<>();

    public ChartDataModel() {
    }

    public JEVisUnit getUnit() {
        try {
            if (_unit == null) {
                if (getAttribute() != null) {
                    _unit = getAttribute().getDisplayUnit();
                }
            }
        } catch (JEVisException ex) {
            logger.fatal(ex);
        }
        return _unit;
    }

    public void setUnit(JEVisUnit _unit) {
        _somethingChanged = true;
        this._unit = _unit;
    }

    public List<JEVisSample> getSamples() {
        if (_somethingChanged) {
            _somethingChanged = false;
            samples = new ArrayList<>();
            if (getSelectedStart().isBefore(getSelectedEnd())) {
                try {

                    if (_dataProcessorObject != null) {
                        _attribute = _dataProcessorObject.getAttribute("Value");
                    } else {
                        _attribute = _object.getAttribute("Value");
                    }

//                    attribute.getDataSource().reloadAttribute(attribute);

                    SampleGenerator sg = new SampleGenerator(_attribute.getDataSource(), _attribute.getObject(), _attribute, getSelectedStart(),
                            getSelectedEnd(), manipulationMode, aggregationPeriod);

                    samples = sg.generateSamples();
                    samples = sg.getAggregatedSamples(samples);
                    samples = factorizeSamples(samples);

                    /**
                     * Checking for data incongruencies                     *
                     */

                    if (samples.size() > 0 && manipulationMode.equals(ManipulationMode.NONE)) {

                        while (samples.get(0).getTimestamp().isAfter(_selectedStart)) {
                            DateTime newTS = samples.get(0).getTimestamp().minus(getAttribute().getDisplaySampleRate());
                            JEVisSample smp = new VirtualSample(newTS, 0.0);
                            smp.setNote("Empty");
                            samples.add(0, smp);
                        }

                        while (samples.get(samples.size() - 1).getTimestamp().isBefore(_selectedEnd)) {
                            DateTime newTS = samples.get(samples.size() - 1).getTimestamp().plus(getAttribute().getDisplaySampleRate());
                            JEVisSample smp = new VirtualSample(newTS, 0.0);
                            smp.setNote("Empty");
                            samples.add(smp);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                if (getDataProcessor() != null) {
                    logger.error("No interval between timestamps for object {}:{}. The end instant must be greater the start. ",
                            getDataProcessor().getName(), getDataProcessor().getID());
                } else {
                    logger.error("No interval between timestamps for object {}:{}. The end instant must be greater the start. ",
                            getObject().getName(), getObject().getID());
                }
            }
        }

        System.gc();

        return samples;
    }

    public void setSamples(List<JEVisSample> samples) {
        this.samples = samples;
    }

    private List<JEVisSample> factorizeSamples(List<JEVisSample> inputList) throws JEVisException {
        if (_unit != null) {
            String outputUnit = UnitManager.getInstance().format(_unit);
            if (outputUnit.equals("")) outputUnit = _unit.getLabel();

            String inputUnit = UnitManager.getInstance().format(_attribute.getDisplayUnit());
            if (inputUnit.equals("")) inputUnit = _attribute.getDisplayUnit().getLabel();

            ChartUnits cu = new ChartUnits();
            Double finalFactor = cu.scaleValue(inputUnit, outputUnit);

            inputList.forEach(sample -> {
                try {
                    sample.setValue(sample.getValueAsDouble() * finalFactor);
                } catch (Exception e) {
                    try {
                        logger.error("Error in sample: " + sample.getTimestamp() + " : " + sample.getValue()
                                + " of attribute: " + getAttribute().getName()
                                + " of object: " + getObject().getName() + ":" + getObject().getID());
                    } catch (Exception e1) {
                        logger.fatal(e1);
                    }
                }
            });

            return inputList;
        } else return inputList;
    }

    public JEVisObject getDataProcessor() {
        return _dataProcessorObject;
    }

    public void setDataProcessor(JEVisObject _dataProcessor) {
        _somethingChanged = true;
        this._dataProcessorObject = _dataProcessor;
    }

    public AggregationPeriod getAggregationPeriod() {
        return aggregationPeriod;
    }

    public void setAggregationPeriod(AggregationPeriod aggregationPeriod) {
        _somethingChanged = true;
        this.aggregationPeriod = aggregationPeriod;
    }

    public ManipulationMode getManipulationMode() {
        return manipulationMode;
    }

    public void setManipulationMode(ManipulationMode manipulationMode) {
        _somethingChanged = true;
        this.manipulationMode = manipulationMode;
    }

    public String getTitle() {
        return _title;
    }

    public void setTitle(String _title) {
        this._title = _title;
    }

    public DateTime getSelectedStart() {

        if (_selectedStart != null) {
            return _selectedStart;
        } else if (getAttribute() != null) {
            DateTime timeStampFromLastSample = getAttribute().getTimestampFromLastSample();
            if (timeStampFromLastSample != null) {
                timeStampFromLastSample = timeStampFromLastSample.minusDays(7);

                DateTime timeStampFromFirstSample = getAttribute().getTimestampFromFirstSample();
                if (timeStampFromFirstSample != null) {
                    if (timeStampFromFirstSample.isBefore(timeStampFromLastSample))
                        _selectedStart = timeStampFromLastSample;
                } else _selectedStart = timeStampFromFirstSample;

            } else {
                return null;
            }

            return _selectedStart;
        } else {
            return null;
        }
    }

    public void setSelectedStart(DateTime selectedStart) {
        if (_selectedEnd == null || !_selectedEnd.equals(selectedStart)) {
            _somethingChanged = true;
        }
        this._selectedStart = selectedStart;
    }

    public DateTime getSelectedEnd() {
        if (_selectedEnd != null) {
            return _selectedEnd;
        } else if (getAttribute() != null) {
            DateTime timeStampFromLastSample = getAttribute().getTimestampFromLastSample();
            if (timeStampFromLastSample == null) _selectedEnd = DateTime.now();
            else _selectedEnd = timeStampFromLastSample;
            return _selectedEnd;
        } else {
            return null;
        }
    }

    public void setSelectedEnd(DateTime selectedEnd) {
        if (_selectedEnd == null || !_selectedEnd.equals(selectedEnd)) {
            _somethingChanged = true;
        }
        this._selectedEnd = selectedEnd;
    }

    public JEVisObject getObject() {
        return _object;
    }

    public void setObject(JEVisObject _object) {
        this._object = _object;
    }

    public JEVisAttribute getAttribute() {
        if (_attribute == null || _somethingChanged) {
            try {
                JEVisAttribute attribute = null;
                String jevisClassName = getObject().getJEVisClassName();
                if (jevisClassName.equals("Data") || jevisClassName.equals("Clean Data")) {
                    if (_dataProcessorObject == null) attribute = getObject().getAttribute("Value");
                    else attribute = getDataProcessor().getAttribute("Value");

                    _attribute = attribute;
                }
            } catch (Exception ex) {
                logger.fatal(ex);
            }
        }

        return _attribute;
    }

    public void setAttribute(JEVisAttribute _attribute) {
        this._attribute = _attribute;
    }

    public Color getColor() {
        return _color;
    }

    public void setColor(Color _color) {
        this._color = _color;
    }

    public boolean isSelectable() {
        return getAttribute() != null && getAttribute().hasSample();
    }


    public List<Integer> getSelectedcharts() {
        return _selectedCharts;
    }

    public void setSelectedCharts(List<Integer> selectedCharts) {

        _somethingChanged = true;
        this._selectedCharts = selectedCharts;
    }

    public void setSomethingChanged(boolean _somethingChanged) {
        this._somethingChanged = _somethingChanged;
    }

    @Override
    public String toString() {
        return "ChartDataModel{" +

                " _title='" + _title + '\'' +
                ", _selectedStart=" + _selectedStart +
                ", _selectedEnd=" + _selectedEnd +
                ", _object=" + _object +
                ", attribute=" + _attribute +
                ", _color=" + _color +
                ", _somethingChanged=" + _somethingChanged +
                ", _unit=" + _unit +
                ", _selectedCharts=" + _selectedCharts +
                '}';
    }


}