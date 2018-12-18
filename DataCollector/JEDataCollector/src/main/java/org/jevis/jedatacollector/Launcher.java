/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.jedatacollector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisClass;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisObject;
import org.jevis.commons.cli.AbstractCliApp;
import org.jevis.commons.database.SampleHandler;
import org.jevis.commons.driver.DataCollectorTypes;
import org.jevis.commons.driver.DataSource;
import org.jevis.commons.driver.DataSourceFactory;
import org.jevis.commons.driver.DriverHelper;
import org.jevis.commons.task.LogTaskManager;
import org.jevis.commons.task.Task;
import org.jevis.commons.task.TaskPrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author broder
 * @author ai
 */
public class Launcher extends AbstractCliApp {

    public static final String APP_INFO = "JEDataCollector";
    private final String APP_SERVICE_CLASS_NAME = "JEDataCollector";
    public static String KEY = "process-id";
    private static final Logger logger = LogManager.getLogger(Launcher.class);
    private final Command commands = new Command();


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        logger.info("-------Start JEDataCollector-------");
        Launcher app = new Launcher(args, APP_INFO);
        app.execute();
    }

    public Launcher(String[] args, String appname) {
        super(args, appname);
    }

    /**
     * Run all datasources in Threads. The maximum number of threads is defined
     * in the JEVis System. There is only one thread per data source.
     * <p>
     *
     * @param dataSources
     */
    private void excecuteDataSources(List<JEVisObject> dataSources) {

        initializeThreadPool(APP_SERVICE_CLASS_NAME);

        logger.info("Number of Requests: " + dataSources.size());

        try {
            forkJoinPool.submit(() -> dataSources.parallelStream().forEach(object -> {
                if (!runningJobs.containsKey(object.getID().toString())) {

                    runningJobs.put(object.getID().toString(), "true");
                    LogTaskManager.getInstance().buildNewTask(object.getID(), object.getName());
                    LogTaskManager.getInstance().getTask(object.getID()).setStatus(Task.Status.STARTED);

                    try {
                        logger.info("----------------Execute DataSource " + object.getName() + "-----------------");
                        DataSource dataSource = DataSourceFactory.getDataSource(object);

                        dataSource.initialize(object);
                        dataSource.run();
                    } catch (Exception e) {
                        if (logger.isDebugEnabled() || logger.isTraceEnabled()) {
                            logger.error("[{}] Error in process: \n {} \n ", object.getID(), org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
                        } else {
                            logger.error("[{}] Error in process: \n {} message: {}", object.getID(), LogTaskManager.getInstance().getShortErrorMessage(e), e.getMessage());
                        }
                        LogTaskManager.getInstance().getTask(object.getID()).setExeption(e);
                        LogTaskManager.getInstance().getTask(object.getID()).setStatus(Task.Status.FAILED);
                    }
                    LogTaskManager.getInstance().getTask(object.getID()).setStatus(Task.Status.FINISHED);
                    runningJobs.remove(object.getID().toString());

                } else {
                    logger.error("Still processing DataSource " + object.getName() + ":" + object.getID());
                }
            })).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Thread Pool was interrupted or execution was stopped: " + e);
        } finally {
            if (forkJoinPool != null) {
                forkJoinPool.shutdown();
                System.gc();
            }
        }
        logger.info("---------------------finish------------------------");

    }


    @Override
    protected void addCommands() {
        comm.addObject(commands);
    }

    @Override
    protected void handleAdditionalCommands() {
        DriverHelper.loadDriver(ds, commands.driverFolder);
    }

    @Override
    protected void runSingle(Long id) {
        logger.info("Start Single Mode");

        try {
            logger.info("Try adding Single Mode for ID " + id);
            JEVisObject dataSourceObject = ds.getObject(id);
            List<JEVisObject> jeVisObjectList = new ArrayList<>();
            jeVisObjectList.add(dataSourceObject);
            excecuteDataSources(jeVisObjectList);
        } catch (Exception ex) {
            logger.error(ex);
        }
    }


    @Override
    protected void runServiceHelp() {

        try {
            ds.preload();
            getCycleTimeFromService(APP_SERVICE_CLASS_NAME);
        } catch (JEVisException e) {
            logger.error(e);
        }

        if (checkServiceStatus(APP_SERVICE_CLASS_NAME)) {
            logger.info("Service is enabled.");
            List<JEVisObject> dataSources = getEnabledDataSources(ds);
            excecuteDataSources(dataSources);
        } else {
            logger.info("Service is disabled.");
        }
        try {
            TaskPrinter.printJobStatus(LogTaskManager.getInstance());
            logger.info("Entering sleep mode for " + cycleTime + " ms.");
            Thread.sleep(cycleTime);

            runServiceHelp();
        } catch (InterruptedException e) {
            logger.error("Interrupted sleep: ", e);
        }

    }

    @Override
    protected void runComplete() {
        logger.info("Start Compete Mode");
        List<JEVisObject> dataSources = new ArrayList<JEVisObject>();
        dataSources = getEnabledDataSources(ds);
        excecuteDataSources(dataSources);
    }


    private List<JEVisObject> getEnabledDataSources(JEVisDataSource client) {
        List<JEVisObject> enabledDataSources = new ArrayList<JEVisObject>();
        try {
            SampleHandler sampleHandler = new SampleHandler();
            JEVisClass dataSourceClass = client.getJEVisClass(DataCollectorTypes.DataSource.NAME);
            List<JEVisObject> allDataSources = client.getObjects(dataSourceClass, true);
            for (JEVisObject dataSource : allDataSources) {
                try {
                    Boolean enabled = sampleHandler.getLastSampleAsBoolean(dataSource, DataCollectorTypes.DataSource.ENABLE, false);
                    if (enabled && DataSourceFactory.containDataSource(dataSource)) {
                        enabledDataSources.add(dataSource);
                    }
                } catch (Exception ex) {
                    logger.error("DataSource failed while checking enabled status:", ex);
                }
            }
        } catch (JEVisException ex) {
            logger.error(ex);
        }
        return enabledDataSources;
    }


}
