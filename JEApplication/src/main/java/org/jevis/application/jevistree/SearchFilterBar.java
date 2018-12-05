package org.jevis.application.jevistree;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.jevis.application.application.AppLocale;
import org.jevis.application.application.SaveResourceBundle;
import org.jevis.application.jevistree.filter.JEVisTreeFilter;

import java.util.List;

public class SearchFilterBar extends HBox {

    private static SaveResourceBundle bundle = new SaveResourceBundle(AppLocale.BUNDLE_ID, AppLocale.getInstance().getLocale());


    private final Finder finder;
    private Label labelFilter = new Label(bundle.getString("searchbar.filter"));
    private Label labelSearch = new Label(bundle.getString("searchbar.search"));
    //    private TextField searchField = new TextField();
    private Spinner<String> searchField = new Spinner<>();
    private ComboBox<JEVisTreeFilter> filterBox;

    public SearchFilterBar(JEVisTree tree, List<JEVisTreeFilter> filter, Finder finder) {
        super(10);
        setPadding(new Insets(8));

        this.finder = finder;

        ObservableList<JEVisTreeFilter> filterList = FXCollections.observableArrayList(filter);
        filterBox = new ComboBox<>(filterList);
        filterBox.setButtonCell(new ListCell<JEVisTreeFilter>() {
            @Override
            protected void updateItem(JEVisTreeFilter item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    setText(item.getName());
                }
            }
        });
        filterBox.setCellFactory(param -> new ListCell<JEVisTreeFilter>() {
            @Override
            protected void updateItem(JEVisTreeFilter item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    setText(item.getName());
                }
            }
        });
        filterBox.getSelectionModel().selectFirst();
        filterBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue) && newValue != null) {
                tree.setFilter(newValue);
            }
        });
        filterBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue) && newValue != null) {
                tree.setFilter(newValue);
            }
        });


        searchField.getStyleClass().add(Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL);
        Background originalBackground = searchField.getEditor().getBackground();
        searchField.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.equals(oldValue)) {
                return;
            }

            if (!newValue.isEmpty() && newValue.length() > 0) {
                if (finder.findMatch(newValue)) {
                    searchField.getEditor().setBackground(originalBackground);
                } else {
                    searchField.getEditor().setBackground(new Background(new BackgroundFill(Color.ORANGERED, new CornerRadii(2), new Insets(2))));
                }
            } else {
                searchField.getEditor().setBackground(originalBackground);
                tree.getHighliterList().clear();
            }
        });
        SpinnerValueFactory<String> spinnerFactory = new SpinnerValueFactory<String>() {
            @Override
            public void decrement(int steps) {
                finder.goPrevious();
            }

            @Override
            public void increment(int steps) {
                finder.goNext();
            }
        };
        searchField.setValueFactory(spinnerFactory);
        searchField.setEditable(true);


        this.setAlignment(Pos.BASELINE_LEFT);
        this.getChildren().addAll(labelFilter, filterBox, labelSearch, searchField);

        Platform.runLater(() -> {
//            filterBox.getSelectionModel().selectLast();
//            System.out.println("select Filter: " + filterBox.getSelectionModel().getSelectedItem().getName());
//            filterBox.getSelectionModel().selectFirst();
            if (!filter.isEmpty()) {
                tree.setFilter(filter.get(0));
            }

        });

    }

    public void hideFilter(boolean hide) {
        labelFilter.setVisible(!hide);
        filterBox.setVisible(!hide);
    }

    public void hideSeachField(boolean hide) {
        labelSearch.setVisible(!hide);
        searchField.setVisible(!hide);
    }


}