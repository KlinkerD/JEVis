package org.jevis.jeconfig.plugin.dashboard;

import javafx.scene.control.TreeTableColumn;
import org.jevis.application.jevistree.JEVisTree;
import org.jevis.application.jevistree.JEVisTreeRow;
import org.jevis.application.jevistree.TreePlugin;

import java.util.List;

public class ColorSelectionPlugin implements TreePlugin {
    @Override
    public void setTree(JEVisTree tree) {
//        FontSelectorDialog
    }

    @Override
    public List<TreeTableColumn<JEVisTreeRow, Long>> getColumns() {
        return null;
    }

    @Override
    public void selectionFinished() {

    }

    @Override
    public String getTitle() {
        return null;
    }
}
