package org.jevis.jecalc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.jecalc.workflow.ProcessManager;
import org.jevis.jecalc.workflow.ProcessManagerFactory;

import java.util.List;

public class ServiceMode {
    private static final Logger logger = LogManager.getLogger(Launcher.class);
    private Integer cycleTime = 900000;

    public ServiceMode(Integer cycleTime) {
        this.cycleTime = cycleTime;
    }

    public ServiceMode() {
    }

    public void run() {
        Thread service = new Thread(() -> runServiceHelp());
        Runtime.getRuntime().addShutdownHook(
                new JEDataProcessorShutdownHookThread(service)
        );

        try {

            service.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            logger.info("Press CTRL^C to exit..");
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void runServiceHelp() {

        this.runProcesses();
        try {
            logger.info("Entering Sleep mode for " + cycleTime + "ms.");
            Thread.sleep(cycleTime);
            runServiceHelp();
        } catch (InterruptedException e) {
            logger.error("Interrupted sleep: ", e);
        }
    }

    private void runProcesses() {
        List<ProcessManager> processes = ProcessManagerFactory.getProcessManagerList();

        logger.info("{} cleaning jobs found", processes.size());
        for (ProcessManager currentProcess : processes) {
            try {
                currentProcess.start();
            } catch (Exception ex) {
                logger.debug(ex);
            }
        }
        logger.info("Cleaning finished.");
    }
}
