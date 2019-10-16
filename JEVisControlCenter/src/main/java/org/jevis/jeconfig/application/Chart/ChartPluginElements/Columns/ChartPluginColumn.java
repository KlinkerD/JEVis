package org.jevis.jeconfig.application.Chart.ChartPluginElements.Columns;

import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisObject;
import org.jevis.commons.chart.ChartDataModel;
import org.jevis.commons.utils.AlphanumComparator;
import org.jevis.jeconfig.application.Chart.AnalysisTimeFrame;
import org.jevis.jeconfig.application.Chart.data.GraphDataModel;
import org.jevis.jeconfig.application.jevistree.JEVisTreeRow;
import org.jevis.jeconfig.application.jevistree.plugin.ChartPluginTree;
import org.jevis.jeconfig.tool.I18n;

import java.util.*;

/**
 * @author <gerrit.schutz@envidatec.com>Gerrit Schutz</gerrit.schutz@envidatec.com>
 */

public interface ChartPluginColumn {
    Image imgMarkAll = new Image(ChartPluginTree.class.getResourceAsStream("/icons/" + "jetxee-check-sign-and-cross-sign-3.png"));

    Tooltip tooltipMarkAll = new Tooltip(I18n.getInstance().getString("plugin.graph.dialog.changesettings.tooltip.forall"));

    default ChartDataModel getData(JEVisTreeRow row) {
        Long id = Long.parseLong(row.getID());
        if (getData() != null && getData().getSelectedData() != null && getData().containsId(id)) {
            return getData().get(id);
        } else {
            ChartDataModel newData = new ChartDataModel(getDataSource());
            newData.setObject(row.getJEVisObject());
            try {
                List<JEVisObject> children = row.getJEVisObject().getChildren();
                AlphanumComparator ac = new AlphanumComparator();
                children.sort((o1, o2) -> ac.compare(o1.getName(), o2.getName()));
                List<String> cleanNames = new ArrayList<>();
                for (Map.Entry<Locale, ResourceBundle> entry : I18n.getInstance().getAllBundles().entrySet()) {
                    ResourceBundle resourceBundle = entry.getValue();
                    try {
                        cleanNames.add(resourceBundle.getString("tree.treehelper.cleandata.name"));
                    } catch (Exception e) {
                    }
                }
                for (JEVisObject object : children) {
                    for (String s : cleanNames) {
                        if (object.getName().contains(s)) {
                            newData.setDataProcessor(object);
                            break;
                        }
                    }
                    if (newData.getDataProcessor() != null) {
                        break;
                    }
                }
            } catch (JEVisException e) {
                e.printStackTrace();
            }
            newData.setAttribute(row.getJEVisAttribute());

            if (getData().isglobalAnalysisTimeFrame()) {
                AnalysisTimeFrame analysisTimeFrame = getData().getGlobalAnalysisTimeFrame();
                newData.setSelectedStart(analysisTimeFrame.getStart());
                newData.setSelectedEnd(analysisTimeFrame.getEnd());
            }

            getData().getSelectedData().add(newData);

            return newData;
        }
    }

    void setGraphDataModel(GraphDataModel graphDataModel);

    void buildColumn();

    default void update() {
        buildColumn();
    }

    GraphDataModel getData();

    JEVisDataSource getDataSource();

}
