package org.jevis.jeconfig.plugin.Dashboard.datahandler;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jevis.api.JEVisDataSource;
import org.jevis.jeconfig.plugin.Dashboard.wizzard.Page;
import org.joda.time.Interval;

public abstract class SampleHandler {

    private final JEVisDataSource jeVisDataSource;
    public ObjectProperty<Interval> durationProperty = new SimpleObjectProperty<>();
    public BooleanProperty configurationDone = new SimpleBooleanProperty(false);

    public SampleHandler(JEVisDataSource jeVisDataSource) {
        this.jeVisDataSource = jeVisDataSource;
    }

    public JEVisDataSource getDataSource() {
        return jeVisDataSource;
    }

//    public void setDataSource(JEVisDataSource dataSource) {
//        this.jeVisDataSource = dataSource;
//    }

    public void setPeriod(Interval period) {
        durationProperty.setValue(period);
    }

    public abstract void setUserSelectionDone();

    public abstract Page getPage();

    public abstract void update();

}
