package org.jevis.jeconfig.application.control;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class YearBox extends ComboBox<Integer> {

    private MonthBox monthBox;
    private DayBox dayBox;

    public YearBox() {
        super();

        List<Integer> list = new ArrayList<>();

        for (int i = 2001; i < 2050; i++) {
            list.add(i);
        }

        ObservableList<Integer> integers = FXCollections.observableArrayList(list);
        setItems(integers);

        Integer year = DateTime.now().getYear();
        getSelectionModel().select(year);
    }

    public void setRelations(MonthBox monthBox, DayBox dayBox) {
        this.monthBox = monthBox;
        this.dayBox = dayBox;

        getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                int selectedIndex = monthBox.getSelectionModel().getSelectedIndex();
                if (selectedIndex > 0) {
                    monthBox.getSelectionModel().select(0);
                } else {
                    monthBox.getSelectionModel().select(1);
                }
                monthBox.getSelectionModel().select(newValue);
            }
        });
    }
}