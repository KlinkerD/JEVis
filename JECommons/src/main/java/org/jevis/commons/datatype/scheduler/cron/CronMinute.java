//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.jevis.commons.datatype.scheduler.cron;

import java.security.InvalidParameterException;

public class CronMinute extends CronTimeUnit {
    private static final int LIMIT = 59;
    private String min;

    public CronMinute() {
    }

    public String getValue() {
        return this.min;
    }

    protected void setValue(String str) throws InvalidParameterException {
        String tmp = str.replaceAll(" ", "");
        if (tmp.equalsIgnoreCase("")) {
            tmp = "*";
            this.isAlias = true;
            this.denominator = 0;
        } else if (tmp.equals("*")) {
            this.isAlias = true;
            this.denominator = 0;
        } else if (tmp.contains("/")) {
            String[] splStr = tmp.split("/");
            if (splStr.length == 2 && this.isNumeratorValid(splStr[0]) && this.isDenominatorValid(splStr[1], 59)) {
                this.isAlias = true;
                this.denominator = Integer.parseInt(splStr[1]);
            }
        } else if (!this.isValueValid(tmp, 59)) {
            throw new InvalidParameterException("time parameter " + tmp + " is wrong");
        }

        this.min = tmp;
    }
}
