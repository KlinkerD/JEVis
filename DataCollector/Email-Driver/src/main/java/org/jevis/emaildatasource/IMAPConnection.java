/**
 * Copyright (C) 2013 - 2016 Envidatec GmbH <info@envidatec.com>
 * <p>
 * This file is part of JEAPI.
 * <p>
 * JEAPI is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation in version 3.
 * <p>
 * JEAPI is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * JEAPI. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * JEAPI is part of the OpenJEVis project, further project information are
 * published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.emaildatasource;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPSSLStore;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.apache.logging.log4j.LogManager;


/**
 * The IMAPConnection class
 *
 * @author Artur Iablokov
 */
public class IMAPConnection implements EMailConnection {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(IMAPConnection.class);

    private Store _store;
    private IMAPFolder _folder;
    private IMAPSSLStore _sslStore;
    private String _foldName;

    @Override
    public void setConnection(Session session, EMailServerParameters param) {

        if (param.isSsl()) {
            _store = new IMAPSSLStore(session, null);
        } else {
            _store = new IMAPStore(session, null);
        }
        _foldName = param.getFolderName();
        try {
            _store.connect(param.getHost(), param.getUserEMail(), param.getPassword());
        } catch (MessagingException ex) {
            logger.error("EMail Connection setting failed. Wrong login data or properties.", ex);
        }

    }

    @Override
    public IMAPFolder getFolder() {
        try {
            if (!_store.isConnected()) {
                logger.error("Connected not possible");
            }
            _folder = (IMAPFolder) _store.getFolder(_foldName);
            if (_folder == null) {
                Folder[] f = _store.getDefaultFolder().list();

                logger.info("A list of available folders: ");
                for (Folder fd : f) {
                    logger.info(">> {}", fd.getName());
                }

            }

        } catch (MessagingException ex) {
            logger.error("Unable to open the inbox folder", ex);
        }
        return _folder;
    }

    @Override
    public void terminate() {
        try {
            _folder.close(false);
        } catch (MessagingException ex) {
            logger.error("Email-Folder terminate failed", ex);
        }
        try {
            _store.close();
        } catch (MessagingException ex) {
            logger.error("Email-Store terminate failed", ex);
        }
    }
}
