/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.ftpdatasource;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.*;
import org.jevis.commons.DatabaseHelper;
import org.jevis.commons.driver.*;
import org.jevis.commons.utils.JEVisDates;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bf
 */
public class FTPDataSource implements DataSource {

    private static final Logger logger = LogManager.getLogger(FTPDataSource.class);
    private Long _id;
    private String _name;
    private String _serverURL;
    private Integer _port;
    private Integer _connectionTimeout;
    private Integer _readTimeout;
    private String _userName;
    private String _password;
    private Boolean _ssl = false;
    private DateTimeZone _timezone;
    private Boolean _enabled;
    protected FTPClient _fc;
    private Parser _parser;
    private Importer _importer;
    private List<JEVisObject> _channels;
    private List<Result> _result;

    private JEVisObject _dataSource;

    @Override
    public void parse(List<InputStream> input) {
        _parser.parse(input, _timezone);
        _result = _parser.getResult();
    }

    @Override
    public void run() {

        for (JEVisObject channel : _channels) {

            try {
                _result = new ArrayList<Result>();
                JEVisClass parserJevisClass = channel.getDataSource().getJEVisClass(DataCollectorTypes.Parser.NAME);
                JEVisObject parser = channel.getChildren(parserJevisClass, true).get(0);

                _parser = ParserFactory.getParser(parser);
                _parser.initialize(parser);

                List<InputStream> input = this.sendSampleRequest(channel);

                if (!input.isEmpty()) {
                    this.parse(input);
                }

                if (!_result.isEmpty()) {

//                    this.importResult();
//
//                    DataSourceHelper.setLastReadout(channel, _importer.getLatestDatapoint());
                    JEVisImporterAdapter.importResults(_result, _importer, channel);
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }

    @Override
    public void importResult() {
//        _importer.importResult(_result);

    }

    @Override
    public void initialize(JEVisObject ftpObject) {
        _dataSource = ftpObject;
        initializeAttributes(ftpObject);
        initializeChannelObjects(ftpObject);

        _importer = ImporterFactory.getImporter(_dataSource);
        if (_importer != null) {
            _importer.initialize(_dataSource);
        }

    }

    @Override
    public List<InputStream> sendSampleRequest(JEVisObject channel) {
        List<InputStream> answerList = new ArrayList<InputStream>();
        try {
            if (_ssl) {
                logger.info("ftps connection");
                _fc = new FTPSClient();
            } else {
                _fc = new FTPClient();
            }
//            _fc.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));

            if (_connectionTimeout != 0) {
                _fc.setConnectTimeout(_connectionTimeout.intValue() * 1000);
            }
            if (_readTimeout != 0) {
                _fc.setDataTimeout(_readTimeout.intValue() * 1000);
            }

            _fc.connect(_serverURL, _port);
//            _fc.setFileType(FTP.BINARY_FILE_TYPE, FTP.BINARY_FILE_TYPE);

            if (_fc.login(_userName, _password) == false) {
                logger.error("No Login possible");
//                throw new FetchingException(_id, FetchingExceptionType.CONNECTION_ERROR);
            }

//            _fc.setFileType(FTP.BINARY_FILE_TYPE);
//            _fc.setFileTransferMode(FTP.COMPRESSED_TRANSFER_MODE);
            _fc.setBufferSize(1024000);

            _fc.setUseEPSVwithIPv4(false);
            _fc.enterLocalPassiveMode();

            InputStream answer = null;
            JEVisClass channelClass = channel.getJEVisClass();
            JEVisType pathType = channelClass.getType(DataCollectorTypes.Channel.FTPChannel.PATH);
            String filePath = DatabaseHelper.getObjectAsString(channel, pathType);
            JEVisType readoutType = channelClass.getType(DataCollectorTypes.Channel.FTPChannel.LAST_READOUT);
            DateTime lastReadout = DatabaseHelper.getObjectAsDate(channel, readoutType, JEVisDates.DEFAULT_DATE_FORMAT);
//            String filePath = dp.getFilePath();
            logger.info("SendSampleRequest2");
            List<String> fileNames = DataSourceHelper.getFTPMatchedFileNames(_fc, lastReadout, filePath);
//        String currentFilePath = Paths.get(filePath).getParent().toString();
            logger.info("Nr of Matched Files " + fileNames.size());
            for (String fileName : fileNames) {
                logger.info("FileInputName: " + fileName);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                String query = Paths.get(fileName);
                logger.info("FTPQuery " + fileName);
                boolean retrieveFile = _fc.retrieveFile(fileName, out);
                logger.info("Request status: " + retrieveFile);
                InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
                answer = new BufferedInputStream(inputStream);
//                InputHandler inputConverter = InputHandlerFactory.getInputConverter(answer);
//                inputConverter.setFilePath(fileName);
                answerList.add(answer);

            }
        } catch (JEVisException ex) {
            logger.error(ex);
        } catch (IOException ex) {
            logger.error(ex);
        } catch (Exception ex) {
            logger.error(ex);
        }

        if (answerList.isEmpty()) {
            logger.error("Cant get any data from the device");
        }

        return answerList;
    }

    private void initializeAttributes(JEVisObject ftpObject) {
        try {
            JEVisClass ftpType = ftpObject.getDataSource().getJEVisClass(DataCollectorTypes.DataSource.DataServer.FTP.NAME);
            JEVisType sslType = ftpType.getType(DataCollectorTypes.DataSource.DataServer.FTP.SSL);
            JEVisType serverType = ftpType.getType(DataCollectorTypes.DataSource.DataServer.FTP.HOST);
            JEVisType portType = ftpType.getType(DataCollectorTypes.DataSource.DataServer.FTP.PORT);
            JEVisType connectionTimeoutType = ftpType.getType(DataCollectorTypes.DataSource.DataServer.FTP.CONNECTION_TIMEOUT);
            JEVisType readTimeoutType = ftpType.getType(DataCollectorTypes.DataSource.DataServer.FTP.READ_TIMEOUT);
            JEVisType userType = ftpType.getType(DataCollectorTypes.DataSource.DataServer.FTP.USER);
            JEVisType passwordType = ftpType.getType(DataCollectorTypes.DataSource.DataServer.FTP.PASSWORD);
            JEVisType timezoneType = ftpType.getType(DataCollectorTypes.DataSource.DataServer.FTP.TIMEZONE);
            JEVisType enableType = ftpType.getType(DataCollectorTypes.DataSource.DataServer.ENABLE);

            _name = ftpObject.getName();
            _id = ftpObject.getID();
            _ssl = DatabaseHelper.getObjectAsBoolean(ftpObject, sslType);
            _serverURL = DatabaseHelper.getObjectAsString(ftpObject, serverType);
            _port = DatabaseHelper.getObjectAsInteger(ftpObject, portType);
            if (_port == null) {
                _port = 21;
            }
            _connectionTimeout = DatabaseHelper.getObjectAsInteger(ftpObject, connectionTimeoutType);
            _readTimeout = DatabaseHelper.getObjectAsInteger(ftpObject, readTimeoutType);

            JEVisAttribute userAttr = ftpObject.getAttribute(userType);
            if (!userAttr.hasSample()) {
                _userName = "";
            } else {
                _userName = DatabaseHelper.getObjectAsString(ftpObject, userType);
            }

            JEVisAttribute passAttr = ftpObject.getAttribute(passwordType);
            if (!passAttr.hasSample()) {
                _password = "";
            } else {
                _password = DatabaseHelper.getObjectAsString(ftpObject, passwordType);
            }
            String timezoneString = DatabaseHelper.getObjectAsString(ftpObject, timezoneType);
            _timezone = DateTimeZone.forID(timezoneString);
            _enabled = DatabaseHelper.getObjectAsBoolean(ftpObject, enableType);

        } catch (JEVisException ex) {
            logger.error(ex);
        }
    }

    private void initializeChannelObjects(JEVisObject ftpObject) {
        try {
            JEVisClass channelDirClass = ftpObject.getDataSource().getJEVisClass(DataCollectorTypes.ChannelDirectory.FTPChannelDirectory.NAME);
            JEVisObject channelDir = ftpObject.getChildren(channelDirClass, false).get(0);
            JEVisClass channelClass = ftpObject.getDataSource().getJEVisClass(DataCollectorTypes.Channel.FTPChannel.NAME);
            _channels = channelDir.getChildren(channelClass, false);
        } catch (Exception ex) {
            logger.error(ex);
        }
    }
}
