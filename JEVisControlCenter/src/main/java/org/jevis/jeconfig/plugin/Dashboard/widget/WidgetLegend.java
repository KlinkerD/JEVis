package org.jevis.jeconfig.plugin.Dashboard.widget;

import com.sun.javafx.charts.Legend;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.lang.reflect.Field;

public class WidgetLegend extends Legend {

    public Legend.LegendItem buildLegendItem(String name, Color color, Color fontcolor) {

        Rectangle r = new Rectangle();
        r.setX(0);
        r.setY(0);
        r.setWidth(12);
        r.setHeight(12);
        r.setArcWidth(20);
        r.setArcHeight(20);
        r.setStroke(color);
        r.setFill(color);

        /**
         * TODO: replace this hack with an own implementation of an legend
         */
        Legend.LegendItem item = new Legend.LegendItem(name, r);
        try {
            Field privateStringField = Legend.LegendItem.class.
                    getDeclaredField("label");
            privateStringField.setAccessible(true);
            Label label = (Label) privateStringField.get(item);
            label.setTextFill(fontcolor);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return item;
    }
}
