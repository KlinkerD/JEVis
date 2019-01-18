package org.jevis.commons.dataprocessing;

public enum ManipulationMode {

    TOTAL, AVERAGE, MIN, MAX, MEDIAN, RUNNING_MEAN, CENTRIC_RUNNING_MEAN, SORTED_MIN, SORTED_MAX, NONE;

    public static ManipulationMode get(String modeName) {
        String[] modeArray = modeName.split("_");
        String mode = NONE.name();
        if (modeArray.length == 2) {
            mode = modeArray[1];
        }
        return valueOf(mode);
    }

    public static ManipulationMode parseManipulation(String manipulation) {
        switch (manipulation) {
            case ("None"):
                return NONE;
            case ("Total"):
                return TOTAL;
            case ("Average"):
                return AVERAGE;
            case ("Min"):
                return MIN;
            case ("Max"):
                return MAX;
            case ("Median"):
                return MEDIAN;
            case ("Running Mean"):
                return RUNNING_MEAN;
            case ("Centric Running Mean"):
                return CENTRIC_RUNNING_MEAN;
            case ("Sorted Min"):
                return SORTED_MIN;
            case ("Sorted Max"):
                return SORTED_MAX;
            default:
                return NONE;
        }
    }
}
