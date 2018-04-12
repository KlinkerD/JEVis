/**
 * Copyright (C) 2016 Envidatec GmbH <info@envidatec.com>
 *
 * This file is part of JEAPI-WS.
 *
 * JEAPI-WS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation in version 3.
 *
 * JEAPI-WS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * JEAPI-WS. If not, see <http://www.gnu.org/licenses/>.
 *
 * JEAPI-WS is part of the OpenJEVis project, further project information are
 * published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.jeapi.ws;

import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javax.imageio.ImageIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisClass;
import org.jevis.api.JEVisClassRelationship;
import org.jevis.api.JEVisConstants;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisEvent;
import org.jevis.api.JEVisEventListener;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisType;
import org.jevis.commons.ws.json.JsonClassRelationship;
import org.jevis.commons.ws.json.JsonJEVisClass;

/**
 *
 * @author fs
 */
public class JEVisClassWS implements JEVisClass {

    private Logger logger = LogManager.getLogger(JEVisClassWS.class);
//    private String name = "";
    private JEVisDataSourceWS ds = null;
//    private String description = "";
//    private boolean isUnique = false;
    private BufferedImage image = null;
    private List<JEVisType> types = null;
    private List<JEVisClassRelationship> relations = null;
    private JsonJEVisClass json;

    public JEVisClassWS(JEVisDataSourceWS ds, JsonJEVisClass json) {

//        name = json.getName();
        this.ds = ds;
//        description = json.getDescription();
//        isUnique = json.getUnique();
//        image = SwingFXUtils.fromFXImage(getImage("1472562626_unknown.png", 60, 60).getImage(), null);
        this.json = json;
    }

    /**
     * TMP solution
     *
     * TODO: remove, does not belog here
     */
    public static ImageView getImage(String icon, double height, double width) {
        ImageView image = new ImageView(getImage(icon));
        image.fitHeightProperty().set(height);
        image.fitWidthProperty().set(width);
        return image;
    }

    /**
     * TMP solution
     *
     * TODO: remove, does not belog here
     */
    public static Image getImage(String icon) {
        try {
            return new Image(JEVisClassWS.class.getResourceAsStream("/" + icon));
        } catch (Exception ex) {
            System.out.println("Could not load icon: " + "/icons/   " + icon);
            return new Image(JEVisClassWS.class.getResourceAsStream("/icons/1393355905_image-missing.png"));
        }
    }

    @Override
    public List<JEVisClass> getValidChildren() throws JEVisException {
        List<JEVisClass> vaildParents = new LinkedList<>();
        for (JEVisClassRelationship rel : getRelationships()) {
            try {
                if (rel.isType(JEVisConstants.ClassRelationship.OK_PARENT)
                        && rel.getEnd().equals(this)) {
                    if (!vaildParents.contains(rel.getOtherClass(this))) {
                        vaildParents.add(rel.getOtherClass(this));
                        //This class can also be createt under classes which are hiers?!
                        vaildParents.addAll(rel.getOtherClass(this).getHeirs());
                    }

                }
            } catch (Exception ex) {
                logger.error("An JEClassRelationship had an error for '{}': {}", getName(), ex);
            }
        }

        Collections.sort(vaildParents);

        return vaildParents;
    }

    @Override
    public boolean deleteType(String type) throws JEVisException {
        try {
            logger.trace("Delete: {}", type);

            String resource = REQUEST.API_PATH_V1
                    + REQUEST.CLASSES.PATH
                    + getName() + "/"
                    + REQUEST.CLASSES.TYPES.PATH
                    + type;

            HttpURLConnection conn = ds.getHTTPConnection().getDeleteConnection(resource);
            logger.trace("Connection.ResonseCode: {}", conn.getResponseCode());
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {

                //TODO: maybe remove from the list of cached relationships but for now we dont have such a list
                //alternativ whould be to fire an update event but this whould make some gui trouble in this version
                for (JEVisType ty : types) {
                    if (ty.getName().equals(type)) {
                        types.remove(ty);
                        break;
                    }
                }

                return true;
            }

            return false;

        } catch (Exception ex) {
            logger.catching(ex);
            return false;
        }
    }

    @Override
    public String getName() throws JEVisException {
        return this.json.getName();
    }

    @Override
    public void setName(String name) throws JEVisException {
        this.json.setName(name);
    }

    @Override
    public BufferedImage getIcon() throws JEVisException {
        if (image != null) {
            return image;
        } else {
            image = ds.getClassIcon(json.getName());
            return image;
        }
    }

    @Override
    public void setIcon(BufferedImage icon) throws JEVisException {
        this.image = icon;
        ;//TODO
    }

    @Override
    public void setIcon(File icon) throws JEVisException {
        try {
            this.image = ImageIO.read(icon);
//            System.out.println("set icon from file: " + _icon.getWidth());
        } catch (IOException ex) {
            logger.catching(ex);
        }
    }

    @Override
    public String getDescription() throws JEVisException {
        return json.getDescription();
    }

    @Override
    public void setDescription(String description) throws JEVisException {
        json.setDescription(description);
    }

    @Override
    public List<JEVisType> getTypes() throws JEVisException {
        //TODO maybe we should not cache the type list but then again the
        //function will be called quit often
        if (types == null) {
            types = ds.getTypes(this);
        }
//
        return types;
    }

    @Override
    public JEVisType getType(String typename) throws JEVisException {

        for (JEVisType type : getTypes()) {
            if (type.getName().equals(typename)) {
                return type;
            }
        }

        return null;

    }

    @Override
    public JEVisType buildType(String name) throws JEVisException {
        JEVisType newType = new JEVisTypeWS(ds, name, this);
        return newType;

    }

    @Override
    public JEVisClass getInheritance() throws JEVisException {
        for (JEVisClassRelationship crel : getRelationships()) {
            if (crel.isType(JEVisConstants.ClassRelationship.INHERIT) && crel.getStart().getName().equals(getName())) {
                return crel.getEnd();
            }
        }
        return null;
    }

    @Override
    public List<JEVisClass> getHeirs() throws JEVisException {
        List<JEVisClass> heirs = new LinkedList<JEVisClass>();
        for (JEVisClassRelationship cr : getRelationships(JEVisConstants.ClassRelationship.INHERIT, JEVisConstants.Direction.BACKWARD)) {
            try {
                heirs.add(cr.getStart());
                heirs.addAll(cr.getStart().getHeirs());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return heirs;
    }

    @Override
    public List<JEVisClass> getValidParents() throws JEVisException {
        List<JEVisClass> vaildParents = new LinkedList<JEVisClass>();

        if (getInheritance() != null) {
            vaildParents.addAll(getInheritance().getValidParents());
        }

        for (JEVisClassRelationship rel : getRelationships()) {
            try {
                if (rel.isType(JEVisConstants.ClassRelationship.OK_PARENT)
                        && rel.getStart().equals(this)) {
                    if (!vaildParents.contains(rel.getOtherClass(this))) {
                        vaildParents.add(rel.getOtherClass(this));
                    }
                    vaildParents.addAll(rel.getOtherClass(this).getHeirs());

                }
            } catch (Exception ex) {

            }
        }

        Collections.sort(vaildParents);

        return vaildParents;
    }

    @Override
    public boolean isAllowedUnder(JEVisClass jevisClass) throws JEVisException {
        List<JEVisClass> vaild = getValidParents();
        for (JEVisClass pClass : vaild) {
            if (pClass.getName().equals(jevisClass.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUnique() throws JEVisException {
        return json.getUnique();
    }

    @Override
    public void setUnique(boolean unique) throws JEVisException {
        json.setUnique(unique);
    }

    @Override
    public boolean delete() throws JEVisException {
        return ds.deleteClass(getName());
    }

    @Override
    public List<JEVisClassRelationship> getRelationships() throws JEVisException {
//        if (relations == null) {//TODO: remove?! we dont want caching in the SQL API
        relations = new ArrayList<>();
        for (JsonClassRelationship crel : json.getRelationships()) {
            relations.add(new JEVisClassRelationshipWS(ds, crel));
        }

        return relations;

//        } else {
//            return relations;
//        }
    }

    @Override
    public List<JEVisClassRelationship> getRelationships(int type) throws JEVisException {
        List<JEVisClassRelationship> tmp = new LinkedList<>();

        for (JEVisClassRelationship cr : getRelationships()) {
            if (cr.isType(type)) {
                tmp.add(cr);
            }
        }

        return tmp;
    }

    @Override
    public List<JEVisClassRelationship> getRelationships(int type, int direction) throws JEVisException {
        List<JEVisClassRelationship> tmp = new LinkedList<JEVisClassRelationship>();

        for (JEVisClassRelationship cr : getRelationships(type)) {
            if (direction == JEVisConstants.Direction.FORWARD && cr.getStart().equals(this)) {
                tmp.add(cr);
            } else if (direction == JEVisConstants.Direction.BACKWARD && cr.getEnd().equals(this)) {
                tmp.add(cr);
            }
        }

        return tmp;
    }

    @Override
    public JEVisClassRelationship buildRelationship(JEVisClass jclass, int type, int direction) throws JEVisException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteRelationship(JEVisClassRelationship rel) throws JEVisException {
        ds.deleteClassRelationship(rel.getStartName(), rel.getEndName(), rel.getType());

    }

    @Override
    public JEVisDataSource getDataSource() throws JEVisException {
        return ds;
    }

    @Override
    public void commit() throws JEVisException {
        try {

            String resource = REQUEST.API_PATH_V1
                    + REQUEST.CLASSES.PATH
                    + getName();

            Gson gson = new Gson();
            StringBuffer response = ds.getHTTPConnection().postRequest(resource, gson.toJson(json));

            JsonJEVisClass newJson = gson.fromJson(response.toString(), JsonJEVisClass.class);
            this.json = newJson;

        } catch (Exception ex) {
            logger.catching(ex);
        }

        try {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(getIcon(), "png", baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            baos.close();

            String resource = REQUEST.API_PATH_V1
                    + REQUEST.CLASSES.PATH
                    + getName() + "/"
                    + REQUEST.CLASSES.ICON.PATH;

            HttpURLConnection connection = ds.getHTTPConnection().getPostIconConnection(resource);

            try (OutputStream os = connection.getOutputStream()) {

                os.write(imageInByte);
                os.flush();
                os.close();
            }
//
            int responseCode = connection.getResponseCode();
            logger.trace("commit icon: " + responseCode);

        } catch (Exception ex) {
            logger.catching(ex);
        }
    }

    @Override
    public void rollBack() throws JEVisException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasChanged() {
        //TODO: class compare
        return false;
    }

    @Override
    public int compareTo(JEVisClass o) {
        try {
            return getName().compareTo(o.getName());
        } catch (JEVisException ex) {
            return 1;
        }
    }

    @Override
    public boolean equals(Object o) {
        try {
            if (o instanceof JEVisClass) {
                JEVisClass obj = (JEVisClass) o;
                if (obj.getName().equals(getName())) {
                    return true;
                }
            }
        } catch (Exception ex) {
            System.out.println("error, cannot compare objects");
            return false;
        }
        return false;
    }

    // TODO : implement listener support
    @Override
    public void addEventListener(JEVisEventListener listener) {

    }

    @Override
    public void removeEventListener(JEVisEventListener listener) {

    }

    @Override
    public void notifyListeners(JEVisEvent event) {

    }

}
