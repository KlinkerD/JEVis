/**
 * Copyright (C) 2014 Envidatec GmbH <info@envidatec.com>
 * <p>
 * This file is part of JEConfig.
 * <p>
 * JEConfig is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation in version 3.
 * <p>
 * JEConfig is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * JEConfig. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * JEConfig is part of the OpenJEVis project, further project information are
 * published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.jeconfig.csv;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.dialog.ProgressDialog;
import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisSample;
import org.jevis.jeconfig.dialog.HiddenConfig;
import org.jevis.jeconfig.dialog.ProgressForm;
import org.jevis.jeconfig.tool.I18n;
import org.joda.time.DateTime;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Florian Simon <florian.simon@envidatec.com>
 */
public class CSVTable extends TableView<CSVLine> {

    private static Logger logger = LogManager.getLogger(CSVTable.class);
    private CSVParser parser;
    private JEVisDataSource ds;
    private List<CSVColumnHeader> header = new ArrayList<>();
    private String customNote = "";

    public CSVTable(JEVisDataSource ds, CSVParser parser) {
        super();
        this.parser = parser;
        this.ds = ds;
        setItems(FXCollections.observableArrayList(parser.getRows()));
        setMaxHeight(1024);
        updateColumns();

    }

    private void updateColumns() {
        getColumns().clear();
        header = new ArrayList<>();

        TableColumn<CSVLine, String> lineColumn = new TableColumn("Nr.");
        lineColumn.setCellFactory(new Callback<TableColumn<CSVLine, String>, TableCell<CSVLine, String>>() {
            @Override
            public TableCell<CSVLine, String> call(TableColumn<CSVLine, String> param) {
                TableCell<CSVLine, String> cell = new TableCell<CSVLine, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText("" + (getTableRow().getIndex() + 1 + +parser.getHeader()));
                    }
                };
                return cell;
            }
        });
        lineColumn.setMaxWidth(30);
        getColumns().add(lineColumn);

        for (int i = 0; i < parser.getColumnCount(); i++) {
            String columnName = "Column " + i;
            TableColumn<CSVLine, String> column = new TableColumn(columnName);
            final CSVColumnHeader header = new CSVColumnHeader(this, i);
            this.header.add(header);
            column.setSortable(false);//layout problem
            column.setPrefWidth(310);
            column.setCellValueFactory(p -> {
                if (p != null) {
                    try {
                        return header.getValueProperty(p.getValue());
                    } catch (NullPointerException ex) {

                    }
                }

                return new SimpleObjectProperty<>("");
            });

            column.setGraphic(header.getGraphic());

            getColumns().add(column);

        }
    }


    public Task<Integer> doImport() {

        Task<Integer> uploadTask = new Task<Integer>() {
            @Override
            public Integer call() throws InterruptedException {
                boolean hadErrors = false;
                updateMessage(I18n.getInstance().getString("csv.progress.message"));
                CSVColumnHeader tsColumn = null;
                CSVColumnHeader dateColumn = null;
                CSVColumnHeader timeColumn = null;
                List<DateTime> combinedList = null;
                for (CSVColumnHeader header : header) {
                    if (header.getMeaning() == CSVColumnHeader.Meaning.DateTime) {
                        tsColumn = header;
                        break;
                    } else if (header.getMeaning() == CSVColumnHeader.Meaning.Date) {
                        dateColumn = header;
                        break;
                    } else if (header.getMeaning() == CSVColumnHeader.Meaning.Time) {
                        timeColumn = header;
                        break;
                    }
                }

                List<CSVLine> toImportList = new ArrayList<>();
                parser.getRows().forEach(csvLine -> {
                    if(!csvLine.isEmpty()){
                        toImportList.add(csvLine);
                    }
                });
                if (dateColumn != null || timeColumn != null) {
                    List<DateTime> listDate = new ArrayList<>();
                    List<DateTime> listTime = new ArrayList<>();
                    for (CSVColumnHeader header : header) {
                        if (header.getMeaning() == CSVColumnHeader.Meaning.Date) {
                            for (CSVLine line : toImportList) {
                                try {
                                    listDate.add(header.getValueAsDate(line.getColumn(header.getColumn())));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (header.getMeaning() == CSVColumnHeader.Meaning.Time) {
                            for (CSVLine line : toImportList) {
                                try {
                                    listTime.add(header.getValueAsDate(line.getColumn(header.getColumn())));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    if (listDate.size() == listTime.size()) {
                        combinedList = new ArrayList<>();
                        for (int i = 0; i < listDate.size(); i++) {
                            DateTime dt = listDate.get(i);
                            DateTime tt = listTime.get(i);
                            combinedList.add(new DateTime(dt.getYear(), dt.getMonthOfYear(), dt.getDayOfMonth(),
                                    tt.getHourOfDay(), tt.getMinuteOfHour(), tt.getSecondOfMinute()).withZoneRetainFields(dt.getZone()));
                        }
                    }
                }

                if (tsColumn == null) {
                    //TODO check for an Date and an Time Column and combine to DateTime
                }

                Integer importedSize=0;
                //find values and import them
                for (CSVColumnHeader header : header) {
                    if (header.getMeaning() == CSVColumnHeader.Meaning.Value || header.getMeaning() == CSVColumnHeader.Meaning.Text) {
                        List<JEVisSample> _newSamples = new ArrayList<>();

                        for (CSVLine line : toImportList) {
                            try {
                                DateTime ts = null;
                                int rowNumber = line.getRowNumber();
                                if (tsColumn != null) {
                                    ts = tsColumn.getValueAsDate(line.getColumn(tsColumn.getColumn()));
                                } else if (combinedList != null) {
                                    ts = combinedList.get(rowNumber);
                                } else {
                                    throw new JEVisException("Found no timestamp", 34253325);
                                }
                                if (header.getMeaning() == CSVColumnHeader.Meaning.Value) {
                                    Double value = header.getValueAsDouble(line.getColumn(header.getColumn()));
                                    JEVisAttribute targetAtt = header.getTarget();
                                    String note = "CSV Import by " + ds.getCurrentUser().getAccountName();
                                    if (!customNote.equals("")) {
                                        note += "; " + customNote;
                                    }
                                    JEVisSample newSample = targetAtt.buildSample(ts, value, note);
                                    _newSamples.add(newSample);
                                }

                            } catch (Exception ex) {
                                logger.error("error while building sample");
                                hadErrors=true;
                                setException(ex);
                            }
                        }
                        try {
                            logger.debug("Import " + _newSamples.size() + " sample(s) into " + header.getTarget().getObject().getID() + "." + header.getTarget().getName());
//                            importedSize+= header.getTarget().addSamples(_newSamples); // not working because of missing API implementation
                            header.getTarget().addSamples(_newSamples);
                            importedSize+= _newSamples.size();
                        } catch (Exception ex) {
                            hadErrors=true;
                            setException(ex);
                            logger.error("Error while importing sample(s) into " + header.getTarget().getObject().getID() + "." + header.getTarget().getName(), ex);
                        }
                    }
                }



                if(hadErrors){
                    failed();
                    return importedSize;
                }else{
                    succeeded();
                    return importedSize;
                }
            }
        };

        return uploadTask;

    }

    public JEVisDataSource getDataSource() {
        return ds;
    }

    public void setScrollTop() {
        final int size = getItems().size();
        if (size > 0) {
            scrollTo(1);
        }
    }


    public void refreshTable() {
//        getItems().clear();
        ObservableList observableList = FXCollections.observableArrayList(parser.parse());
        Platform.runLater(() -> {
//            updateColumns();
            setItems(observableList);
        });

    }


    public void setCSVParser(CSVParser parser) {
        this.parser = parser;
    }

    public CSVParser getParser() {
        return parser;
    }

    public void setCustomNote(String customNote) {
        this.customNote = customNote;
    }
}
