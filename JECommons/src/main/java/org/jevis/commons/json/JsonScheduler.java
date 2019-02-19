//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.jevis.commons.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "JsonScheduler")
public class JsonScheduler {
    private String timezone;
    private List<JsonSchedulerRule> rules;

    public JsonScheduler() {
    }

    @XmlElement(name = "rules")
    public List<JsonSchedulerRule> getRules() {
        return this.rules;
    }

    public void setRules(List<JsonSchedulerRule> rules) {
        this.rules = rules;
    }

    @XmlElement(name = "timezone")
    public String getTimezone() {
        return this.timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
