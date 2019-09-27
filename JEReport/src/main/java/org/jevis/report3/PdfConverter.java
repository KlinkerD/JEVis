/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.report3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jodconverter.JodConverter;
import org.jodconverter.office.LocalOfficeManager;
import org.jodconverter.office.OfficeException;
import org.jodconverter.office.OfficeUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;


/**
 * @author broder
 */
public class PdfConverter {
    private static final Logger logger = LogManager.getLogger(PdfConverter.class);

    private File xlsFile;
    private File pdfFile;

    public PdfConverter(String filePrefix, byte[] fileContent) {
        try {
            Path xlsFilePath = Files.createTempFile(filePrefix, ".xlsx");
            xlsFile = xlsFilePath.toFile();
            Files.write(xlsFilePath, fileContent);
            xlsFile.deleteOnExit();

            Path pdfFilePath = Files.createTempFile(filePrefix, ".pdf");
            pdfFile = pdfFilePath.toFile();
            pdfFile.deleteOnExit();
        } catch (IOException ex) {
            logger.error(ex);
        }
    }

    public File runPdfConverter() {

        final LocalOfficeManager officeManager = LocalOfficeManager.install();
        try {
            officeManager.start();
            JodConverter
                    .convert(xlsFile)
                    .to(pdfFile)
                    .execute();
        } catch (OfficeException e) {
            logger.error(e);
        } finally {
            OfficeUtils.stopQuietly(officeManager);
        }
        return pdfFile;
    }

    public static void main(String[] args) throws IOException {
        Locale aDefault = Locale.getDefault();
        Charset defaultCharset = Charset.defaultCharset();
        String displayName = defaultCharset.displayName();
        String displayLanguage = aDefault.getDisplayLanguage();
        Path fileLocation = Paths.get("");
        byte[] data = Files.readAllBytes(fileLocation);
        PdfConverter converter = new PdfConverter("test", data);
        converter.runPdfConverter();
    }
}
