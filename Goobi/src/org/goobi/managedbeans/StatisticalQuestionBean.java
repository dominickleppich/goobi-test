package org.goobi.managedbeans;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.log4j.Logger;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.statistics.enums.CalculationUnit;
import org.goobi.production.flow.statistics.enums.TimeUnit;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.IStatisticPlugin;

@ManagedBean(name = "StatisticalQuestionBean")
@SessionScoped
public class StatisticalQuestionBean {

    private static final Logger logger = Logger.getLogger(StatisticalQuestionBean.class);

    private List<String> possiblePluginNames;

    private String currentPluginName = "";

    private String data = "";

    private IStatisticPlugin currentPlugin;

    private boolean calculateFinished = false;

    public StatisticalQuestionBean() {
        possiblePluginNames = PluginLoader.getListOfPlugins(PluginType.Statistics);
        Collections.sort(possiblePluginNames);
    }

    public List<String> getPossiblePluginNames() {
        return possiblePluginNames;
    }

    public String setStatisticalQuestion(String pluginName) {
        currentPluginName = pluginName;

        currentPlugin = (IStatisticPlugin) PluginLoader.getPluginByTitle(PluginType.Statistics, currentPluginName);
        currentPlugin.setFilter("");
        return "statistics";
    }

    public String getCurrentPluginName() {
        return currentPluginName;
    }

    public IStatisticPlugin getCurrentPlugin() {
        return currentPlugin;
    }

    public void setCurrentPlugin(IStatisticPlugin currentPlugin) {
        this.currentPlugin = currentPlugin;
    }

    /**
     * Get all {@link TimeUnit} from enum
     * 
     * @return all timeUnits
     *************************************************************************************/
    public List<TimeUnit> getAllTimeUnits() {
        return Arrays.asList(TimeUnit.values());
    }

    public void calculate() {
        //        currentPlugin.setFilter("");       
        calculateFinished = true;
    }

    /**
     * Get all {@link CalculationUnit} from enum
     * 
     * @return all calculationUnit
     *************************************************************************************/
    public List<CalculationUnit> getAllCalculationUnits() {
        return Arrays.asList(CalculationUnit.values());
    }

    public boolean isCalculateFinished() {
        return calculateFinished;
    }

    public void setCalculateFinished(boolean calculateFinished) {
        this.calculateFinished = calculateFinished;
    }

    public String getData() {
        if (calculateFinished) {
            data = currentPlugin.getData();
        }
        calculateFinished = false;
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
