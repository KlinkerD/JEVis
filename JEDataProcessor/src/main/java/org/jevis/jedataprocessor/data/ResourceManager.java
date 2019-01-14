/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.jedataprocessor.data;

import org.jevis.api.JEVisSample;

import java.util.ArrayList;
import java.util.List;

/**
 * @author broder
 */
public class ResourceManager {

    public List<CleanInterval> intervals = new ArrayList<>();
    private CleanDataObject cleanDataObject;
    private List<JEVisSample> rawSamples;

    public List<CleanInterval> getIntervals() {
        return intervals;
    }

    public void setIntervals(List<CleanInterval> intervals) {
        this.intervals = intervals;
    }

    public CleanDataObject getCleanDataObject() {
        return cleanDataObject;
    }

    public void setCleanDataObject(CleanDataObject cleanDataObject) {
        this.cleanDataObject = cleanDataObject;
    }

    public List<JEVisSample> getRawSamples() {
        return rawSamples;
    }

    public void setRawSamples(List<JEVisSample> rawSamples) {
        this.rawSamples = rawSamples;
    }

    public Long getID() {
        if (cleanDataObject == null || cleanDataObject.getObject() == null) {
            return -1L;
        }
        return cleanDataObject.getObject().getID();
    }
}
