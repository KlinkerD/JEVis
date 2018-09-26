/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.report3.data.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisFile;
import org.jevis.api.JEVisObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author broder
 */
public class ReportServiceProperty {
    private static final Logger logger = LogManager.getLogger(ReportServiceProperty.class);
    private JEVisFile notificationFile;
    private Long mailID;

    public void initialize(JEVisObject jevisobject) {
        try {
            JEVisAttribute notificationAttribute = jevisobject.getAttribute(JEReportService.NOTIFICATION_FILE);
            notificationFile = notificationAttribute.getLatestSample().getValueAsFile();
            mailID = jevisobject.getAttribute(JEReportService.REPORT_MAIL_ID).getLatestSample().getValueAsLong();
        } catch (JEVisException ex) {
            logger.error(ex);
        }
    }

    public File getNotificationFile() {
        File tmpJar = null;
        try {
            tmpJar = File.createTempFile("notification", ".jar");
            tmpJar.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tmpJar);
            fos.write(notificationFile.getBytes());
        } catch (IOException ex) {
            logger.error(ex);
        }
        return tmpJar;
    }

    public Long getMailID() {
        return mailID;
    }
}
