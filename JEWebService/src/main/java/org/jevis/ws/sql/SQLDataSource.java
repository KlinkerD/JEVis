/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jevis.ws.sql;

import org.apache.commons.net.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisClass;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisObject;
import org.jevis.api.JEVisRelationship;
import org.jevis.commons.ws.json.*;
import org.jevis.rest.Config;
import org.jevis.rest.ErrorBuilder;
import org.jevis.ws.sql.tables.*;
import org.joda.time.DateTime;

import javax.security.sasl.AuthenticationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * @author fs
 */
public class SQLDataSource {

    private static final Logger logger = LogManager.getLogger(org.jevis.ws.sql.SQLDataSource.class);
    private Connection dbConn;
    private JEVisUserNew user;

    private LoginTable lTable;
    private ObjectTable oTable;
    private AttributeTable aTable;
    private SampleTable sTable;
    private RelationshipTable rTable;

    private List<JsonRelationship> allRelationships = new LinkedList<>();
    //    private List<JsonClassRelationship> allClassRelationships = new LinkedList<>();
//    private List<JsonJEVisClass> allClasses = new LinkedList<>();
    private List<JsonObject> allObjects = new LinkedList<>();
    private Map<String, List<JsonType>> allTypes = new HashMap<>();
    private UserRightManagerForWS um;
    private Profiler pf = new Profiler();
    private List<String> sqlLog = new ArrayList<>();

    public enum PRELOAD {
        ALL_REL, ALL_CLASSES, ALL_OBJECT
    }

    public SQLDataSource(HttpHeaders httpHeaders, Request request, UriInfo url) throws AuthenticationException, JEVisException {

        try {
            pf.setSQLList(sqlLog);
            pf.addEvent(String.format("Methode: %s URL: %s ", request.getMethod(), url.getRequestUri()), "");
            ConnectionFactory.getInstance().registerMySQLDriver(Config.getDBHost(), Config.getDBPort(), Config.getSchema(), Config.getDBUser(), Config.getDBPW());

            dbConn = ConnectionFactory.getInstance().getConnection();

            if (dbConn.isValid(2000)) {
                pf.addEvent("DS", "Connection established");
                lTable = new LoginTable(this);
                jevisLogin(httpHeaders);
                pf.addEvent("DS", "JEVis user login done");
                oTable = new ObjectTable(this);
                aTable = new AttributeTable(this);
                sTable = new SampleTable(this);
                rTable = new RelationshipTable(this);
                um = new UserRightManagerForWS(this);
                pf.addEvent("DS", "URM done loading");

            }
        } catch (SQLException se) {
            throw new JEVisException("Database connection errror", 5438, se);
        }

    }

    public Profiler getProfiler() {
        return pf;
    }

    public UserRightManagerForWS getUserManager() {
        return um;
    }

    public void preload(PRELOAD preload) throws JEVisException {
        logger.debug("prelaod {}", preload.toString());
        try {
            switch (preload) {
                case ALL_REL:
                    allRelationships = getRelationshipTable().getAll();
                    getProfiler().addEvent("DS", "done reloading Relationships");
                    break;
                case ALL_CLASSES:
                    Config.getClassCache();
//                    allTypes = getTypeTable().//todo
                    getProfiler().addEvent("DS", "done reloading Classes-/Relationships");
                    break;
                case ALL_OBJECT:
                    allObjects = getObjectTable().getAllObjects();
                    getProfiler().addEvent("DS", "done reloading Objects");
                    break;
            }
        } catch (Exception sx) {
            logger.error("Error while reloading", sx);
        }
    }

    public RelationshipTable getRelationshipTable() {
        return rTable;
    }

    public ObjectTable getObjectTable() {
        return oTable;
    }

    public SampleTable getSampleTable() {
        return sTable;
    }


    public AttributeTable getAttributeTable() {
        return aTable;
    }

    public void addQuery(String id, String sql) {
        sqlLog.add(id + ": " + sql);
    }

    public Connection getConnection() {
        return dbConn;
    }

    public List<JsonObject> filterObjectByClass(List<JsonObject> objects, String jclass) throws JEVisException {
        List<JsonObject> filterd = new ArrayList<>();
        List<String> heir = new ArrayList<>();
        heir.add(jclass);
        JEVisClassHelper.findHeir(jclass, heir);
        for (JsonObject obj : objects) {
            if (heir.contains(obj.getJevisClass())) {
                filterd.add(obj);
            }
        }
        return filterd;
    }

    private void jevisLogin(HttpHeaders httpHeaders) throws AuthenticationException, JEVisException {
        if (httpHeaders.getRequestHeader("authorization") == null || httpHeaders.getRequestHeader("authorization").isEmpty()) {
            throw new AuthenticationException("Authorization header is missing");
        }
//        try {
        String auth = httpHeaders.getRequestHeader("authorization").get(0);
        if (auth != null && !auth.isEmpty()) {

            auth = auth.replaceFirst("[Bb]asic ", "");
            byte[] decoded = Base64.decodeBase64(auth);

            try {
                String decodeS = (new String(decoded, "UTF-8"));
                String[] dauth = decodeS.split(":");
                if (dauth.length == 2) {

                    String username = dauth[0];
                    String password = dauth[1];
                    try {
                        user = lTable.loginUser(username, password);
                        getProfiler().setUser(username);

                    } catch (JEVisException ex) {
                        ex.printStackTrace();
                        throw new AuthenticationException("Username/Password is not correct.");
                    }
                } else {
                    throw ErrorBuilder.ErrorBuilder(Response.Status.BAD_REQUEST.getStatusCode(), 2002, "The HTML authorization header is not correct formate");
                }
            } catch (UnsupportedEncodingException uee) {
                throw ErrorBuilder.ErrorBuilder(Response.Status.BAD_REQUEST.getStatusCode(), 2003, "The HTML authorization header is not in Base64");
            } catch (NullPointerException nex) {
                throw new AuthenticationException("Username/Password is not correct.");
            }
        } else {
            throw new AuthenticationException("Authorization header is missing");
        }
//        } 
//        catch (Exception ex) {
//            ex.printStackTrace();
//            throw new AuthenticationException("Authorization header incorrect", ex);
//        }
    }

    public JEVisClass buildClass(String name) throws JEVisException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public JEVisObject buildLink(String name, JEVisObject parent, JEVisObject linkedObject) throws JEVisException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public JEVisRelationship buildRelationship(Long fromObject, Long toObject, int type) throws JEVisException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public List<JsonObject> getRootObjects() throws JEVisException {
        return getUserManager().getRoots();
    }

    public List<JsonObject> getObjects(String jevisClass, boolean addheirs) throws JEVisException {
        List<JsonObject> list = new LinkedList();
        List<String> allHeir = new ArrayList<>();
        allHeir.add(jevisClass);
        if (addheirs) {
            JEVisClassHelper.findHeir(jevisClass, allHeir);
        }

        for (JsonObject ob : getObjects()) {
            if (allHeir.contains(ob.getJevisClass())) {
                list.add(ob);
            }
        }
        return list;

    }

    public JsonObject getObject(Long id) throws JEVisException {
        logger.debug("getObject: {}", id);
        if (!allObjects.isEmpty()) {
            for (JsonObject ob : allObjects) {
                if (ob.getId() == id) {
                    logger.debug("getObject- cache");
                    return ob;
                }
            }
        }
        logger.debug("getObject- NON cache");
        JsonObject ob = oTable.getObject(id);
        if (ob != null) {
            allObjects.add(ob);
        }
        return ob;

    }

    public void addRelationhsipsToObjects(List<JsonObject> objs, List<JsonRelationship> rels) {
        getProfiler().addEvent("DS", "addRelationhsipsToObjects");
        for (JsonObject ob : objs) {
            for (JsonRelationship rel : rels) {
                if (rel.getFrom() == ob.getId() || rel.getTo() == ob.getId()) {
                    if (ob.getRelationships() == null) {
                        ob.setRelationships(new ArrayList<JsonRelationship>());
                    }
                    ob.getRelationships().add(rel);
                }
            }
        }
        getProfiler().addEvent("DS", "done");
    }

    public JsonObject updateObject(long id, String newname, boolean ispublic) throws JEVisException {
        logger.debug("updateObject");
        return getObjectTable().updateObject(id, newname, ispublic);
    }

    public List<JsonObject> getObjects() throws JEVisException {
        logger.debug("getObjectS");
        if (!allObjects.isEmpty()) {
            logger.debug("Cache");
            return allObjects;
        }

        allObjects = oTable.getAllObjects();
        logger.debug("NONE-Cache");
        return allObjects;
    }


    public List<JsonRelationship> setRelationships(List<JsonRelationship> rels) throws JEVisException {
        List<JsonRelationship> newRels = new ArrayList<>();
        for (JsonRelationship rel : rels) {
            try {
                newRels.add(setRelationships(rel));
            } catch (Exception ex) {
                //hmmmm
            }
        }
        return newRels;
    }

    public JsonRelationship setRelationships(JsonRelationship rel) throws JEVisException {
        return getRelationshipTable().insert(rel.getFrom(), rel.getTo(), rel.getType());
    }

    public JsonJEVisClass getJEVisClass(String name) throws JEVisException {
        return Config.getClassCache().get(name);
    }


    public JEVisUserNew getCurrentUser() {
        if (user == null) {
            return new JEVisUserNew(this, "Unkown", -1l, false, false);
        }
        return user;
    }

    public List<JsonSample> getSamples(long obj, String attribute, DateTime from, DateTime until, long limit) throws JEVisException {
        return getSampleTable().getSamples(obj, attribute, from, until, limit);
    }

    public int setSamples(long obj, String attribute, int pritype, List<JsonSample> samples) throws JEVisException {
        return getSampleTable().insertSamples(obj, attribute, pritype, samples);
    }

    public JsonSample getLastSample(long obj, String attribute) throws JEVisException {
        return getSampleTable().getLatest(obj, attribute);
    }

    public List<JsonRelationship> getRelationships(long object) throws JEVisException {
        if (!allRelationships.isEmpty()) {
            //TODO
            List<JsonRelationship> list = new LinkedList<>();
            for (JsonRelationship rel : allRelationships) {
                if (rel.getTo() == object || rel.getFrom() == object) {
                    list.add(rel);
                }
            }
            return list;
        }

        return getRelationshipTable().getAllForObject(object);

    }

    public List<JsonRelationship> getRelationships(int type) throws JEVisException {
        List<JsonRelationship> list = new ArrayList<>();

        for (JsonRelationship rel : getRelationships()) {
            if (rel.getType() == type) {
                list.add(rel);
            }
        }

        return list;
    }

    public List<JsonRelationship> getRelationships() throws JEVisException {
        logger.debug("getRelationships");
        if (!allRelationships.isEmpty()) {
            logger.debug("getRelationships - cache");
            return allRelationships;
        }

        allRelationships = rTable.getAll();
        logger.debug("getRelationships - None cache");
        return allRelationships;

    }


    public boolean disconnect() throws JEVisException {
        if (dbConn != null) {
            try {
                dbConn.close();
                return true;
            } catch (SQLException se) {
                throw new JEVisException("Error while closing DB connection", 6492, se);
            }
        }
        return false;
    }

    public JsonAttribute getAttribute(long objectID, String name) throws JEVisException {
        for (JsonAttribute att : getAttributes(objectID)) {
            if (att.getType().equals(name)) {
                return att;
            }
        }
        return null;
    }

    public boolean deleteAllSample(long object, String attribute) throws JEVisException {
        return getSampleTable().deleteAllSamples(object, attribute);
    }

    public boolean deleteSamplesBetween(long object, String attribute, DateTime startDate, DateTime endDate) throws JEVisException {
        return getSampleTable().deleteSamples(object, attribute, startDate, endDate);
    }

    public List<JsonAttribute> getAttributes(long objectID) throws JEVisException {
        JsonObject ob = getObject(objectID);
        List<JsonAttribute> atts = getAttributeTable().getAttributes(objectID);

        return atts;

    }

    public boolean setAttribute(long objectID, JsonAttribute att) throws JEVisException {
        getAttributeTable().updateAttribute(objectID, att);
        return true;
    }


    public JsonObject buildObject(JsonObject obj, long parent) throws JEVisException {
        return getObjectTable().insertObject(obj.getName(), obj.getJevisClass(), parent, obj.getisPublic());
    }

    public boolean deleteObject(JsonObject objectID) throws JEVisException {
        return getObjectTable().deleteObject(objectID);
    }


    public boolean deleteRelationship(Long fromObject, Long toObject, int type) throws JEVisException {
        return getRelationshipTable().delete(fromObject, toObject, type);
    }


}
