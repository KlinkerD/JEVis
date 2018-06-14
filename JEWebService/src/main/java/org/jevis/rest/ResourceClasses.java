/**
 * Copyright (C) 2013 - 2016 Envidatec GmbH <info@envidatec.com>
 * <p>
 * This file is part of JEWebService.
 * <p>
 * JEWebService is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation in version 3.
 * <p>
 * JEWebService is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * JEWebService. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * JEWebService is part of the OpenJEVis project, further project information
 * are published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.apache.logging.log4j.LogManager;
import org.jevis.api.JEVisException;
import org.jevis.commons.ws.json.JsonJEVisClass;
import org.jevis.ws.sql.JEVisClassHelper;
import org.jevis.ws.sql.SQLDataSource;

import javax.imageio.ImageIO;
import javax.security.sasl.AuthenticationException;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handel all the JEVIsObjects related requests
 *
 * @author Florian Simon <florian.simon@envidatec.com>
 */
@Path("/JEWebService/v1/classes")
public class ResourceClasses {

    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(ResourceClasses.class);

    /**
     * Returns an List of JEVisClasses as Json
     *
     * @param httpHeaders
     * @return
     * @throws JEVisException
     */
    @GET
    @Logged
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(
            @Context HttpHeaders httpHeaders,
            @Context Request request,
            @Context UriInfo url) throws JEVisException {
        System.out.println("get all classes");
        SQLDataSource ds = null;
        try {
            ds = new SQLDataSource(httpHeaders, request, url);


            return getClassResponse(null);
        } catch (JEVisException jex) {
            logger.error(jex);
            return Response.serverError().build();
        } catch (AuthenticationException ex) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(ex.getMessage()).build();
        } finally {
            Config.CloseDS(ds);
        }

    }

    /**
     * Returns the requested JEVisClass
     *
     * @param httpHeaders
     * @param name
     * @return
     */
    @GET
    @Logged
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{name}")
    public Response getJEVisClass(
            @Context HttpHeaders httpHeaders,
            @Context Request request,
            @Context UriInfo url,
            @PathParam("name") String name) {

        SQLDataSource ds = null;
        try {
            ds = new SQLDataSource(httpHeaders, request, url);

            return getClassResponse(name);

        } catch (JEVisException jex) {
            logger.catching(jex);
            return Response.serverError().build();
        } catch (AuthenticationException ex) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(ex.getMessage()).build();
        } finally {
            Config.CloseDS(ds);
        }

    }

    @DELETE
    @Logged
    @Path("/{name}")
    public Response deleteJEVisClass(
            @Context HttpHeaders httpHeaders,
            @Context Request request,
            @Context UriInfo url,
            @PathParam("name") String name) {

        SQLDataSource ds = null;
        try {
            ds = new SQLDataSource(httpHeaders, request, url);


            JsonJEVisClass jclass = Config.getClassCache().get(name);
            if (jclass != null || ds.getUserManager().isSysAdmin()) {
                //TODO: delete orphaned relationships on other classes
//                for (Map.Entry<String, JsonJEVisClass> jc : Config.getClassCache().entrySet()) {
//                    //delete.relationship with delete class
//                }

                for (File file : Config.getClassDir().listFiles()) {
                    if (file.getName().equalsIgnoreCase(name) && file.canWrite()) {
                        file.delete();
                        //TODO: Also delete Icon
                        return Response.ok().build();
                    }
                }

                return Response.status(Response.Status.NOT_FOUND).build();

            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

        } catch (JEVisException jex) {
            logger.catching(jex);
            return Response.serverError().build();
        } catch (AuthenticationException ex) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(ex.getMessage()).build();
        } finally {
            Config.CloseDS(ds);
        }

    }

    @POST
    @Logged
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{name}")
    public Response postJEVisClass(
            @Context HttpHeaders httpHeaders,
            @Context Request request,
            @Context UriInfo url,
            @PathParam("name") String name,
            String input) {

        SQLDataSource ds = null;
        try {
            System.out.println("PostClass: " + input);
            ds = new SQLDataSource(httpHeaders, request, url);

            if (ds.getUserManager().isSysAdmin()) {
                JsonJEVisClass json = (new Gson()).fromJson(input, JsonJEVisClass.class);//parse it again to be save and to make it pretty
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                PrintWriter writer = new PrintWriter(Config.getClassDir().getAbsoluteFile() + "/" + name + ".json", "UTF-8");
                writer.println(gson.toJson(json));
                writer.close();


                Config.getClassCache().clear();
                return Response.ok(ds.getJEVisClass(name)).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

        } catch (AuthenticationException ex) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(ex.getMessage()).build();
        } catch (Exception jex) {
            logger.catching(jex);
            return Response.serverError().build();
        } finally {
            Config.CloseDS(ds);
        }

    }

    /**
     * Returns the Icon of the requested JEVisClass
     *
     * @param httpHeaders
     * @param name
     * @return
     */
    @GET
    @Logged
    @Path("/{name}/icon")
    @Produces({"image/png", "image/jpg", "image/gif"})
    public Response getClassIcon(
            @Context HttpHeaders httpHeaders,
            @Context Request request,
            @Context UriInfo url,
            @PathParam("name") String name
    ) {
        SQLDataSource ds = null;
        try {
            ds = new SQLDataSource(httpHeaders, request, url);

            FileFilter ff = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.getName().endsWith(".png")) {
                        return true;
                    } else if (pathname.getName().endsWith(".jpg")) {
                        return true;
                    } else if (pathname.getName().endsWith(".gif")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            };

            for (File icon : Config.getClassDir().listFiles(ff)) {
                int lastDot = icon.getName().lastIndexOf(".");
                if (name.equalsIgnoreCase(icon.getName().substring(0, lastDot))) {
                    return Response.ok(ImageIO.read(icon)).build();
                }
            }
            File placeholder = new File(Config.getClassDir().getAbsolutePath() + "MissingPlaceholder.png");
            return Response.ok(ImageIO.read(placeholder)).build();


        } catch (JEVisException jex) {
            Logger.getLogger(ResourceClasses.class.getName()).log(Level.SEVERE, null, jex);
            return Response.serverError().build();
        } catch (AuthenticationException ex) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(ex.getMessage()).build();
        } catch (IOException ioex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ioex.getMessage()).build();
        } finally {
            Config.CloseDS(ds);
        }

    }


    public Response getClassResponse(String classname) throws JEVisException {
        if (classname == null || classname.isEmpty()) {
            return Response.ok(Config.getClassCache().values()).build();
        } else {
            if (Config.getClassCache().containsKey(classname)) {
                return Response.ok(Config.getClassCache().get(classname)).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        }
    }

    public Map<String, JsonJEVisClass> loadJsonClasses() throws JEVisException {
        Gson gson = new GsonBuilder().create();
        Map<String, JsonJEVisClass> classMap = Collections.synchronizedMap(new HashMap<String, JsonJEVisClass>());

        File classDir = Config.getClassDir();

        if (classDir.exists()) {
            FileFilter jsonFilter = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.getName().endsWith(".json")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            };
            for (File jsonFile : classDir.listFiles(jsonFilter)) {
                try {
                    JsonReader reader = new JsonReader(new FileReader(jsonFile));
                    JsonJEVisClass data = gson.fromJson(reader, JsonJEVisClass.class);
                    classMap.put(data.getName(), data);

                } catch (Exception ex) {
                    logger.error("Error while loading Classfile: " + jsonFile.getName(), ex);
                }
            }
        }

        JEVisClassHelper.completeClasses(classMap);

//        Map<String, JsonClassRelationship> clRelationships = new HashMap<>();
//        //Add cross relationships
//        for (Map.Entry<String, JsonJEVisClass> jc : classMap.entrySet()) {
//            try {
//                for (JsonClassRelationship rel : jc.getValue().getRelationships()) {
//                    try {
//                        String relKey = rel.getStart() + ":" + rel.getEnd() + ":" + rel.getType();
//                        clRelationships.put(relKey, rel);
//                    } catch (Exception ex) {
//                        logger.error("Error while listing classes relationships[" + jc.getKey() + "]", ex);
//                    }
//                }
//            } catch (Exception ex) {
//                logger.error("Error while listing classes[" + jc.getKey() + "]", ex);
//            }
//        }
//
//        for (Map.Entry<String, JsonClassRelationship> rel : clRelationships.entrySet()) {
//            try {
//                JsonJEVisClass startClass = classMap.get(rel.getValue().getStart());
//                JsonJEVisClass endClass = classMap.get(rel.getValue().getEnd());
//
//                if (!startClass.getRelationships().contains(rel.getValue())) {
//                    startClass.getRelationships().add(rel.getValue());
//                }
//
//                if (!endClass.getRelationships().contains(rel.getValue())) {
//                    endClass.getRelationships().add(rel.getValue());
//                }
//            } catch (Exception ex) {
//                logger.error("Error while mapping class relationships[" + rel.getKey() + "]", ex);
//            }
//        }

        return classMap;
    }


    /**
     * @param httpHeaders
     * @param name
     * @param imageBytes
     * @return
     */
    @POST
    @Path("/{name}/icon")
    @Consumes({"image/png", "image/jpg", "image/gif"})
    public Response postClassIcon(
            @Context HttpHeaders httpHeaders,
            @Context Request request,
            @Context UriInfo url,
            @PathParam("name") String name, byte[] imageBytes) {

        SQLDataSource ds = null;
        try {
            System.out.println("Post icon for: " + name + " byte: " + imageBytes.length);
            ds = new SQLDataSource(httpHeaders, request, url);
            ds.getProfiler().addEvent("ResourceClasses", "putClassIcon");


            if (!ds.getUserManager().isSysAdmin()) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }


            try {
                File newFile = new File(Config.getClassDir().getAbsoluteFile() + "/" + name + ".png");
                System.out.println("class.icon: " + newFile + "  size: " + newFile.length());
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
                //TODO: maybe resize the image to an default size
                ImageIO.write(img, "png", newFile);

                //clean cache
                File tmpZipFile = new File(FileCache.CLASS_ICON_FILE);
                tmpZipFile.delete();

                return Response.status(Response.Status.OK).build();
            } catch (IOException ioex) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ioex.getMessage()).build();
            }


        } catch (JEVisException jex) {
            logger.error("ClassIcon upload error: {}", jex);
            return Response.serverError().build();
        } catch (AuthenticationException ex) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(ex.getMessage()).build();
        } catch (Exception ex) {
            logger.error("Error while uploadign classicon {}", ex);
            return Response.serverError().entity(ex).build();
        } finally {
            Config.CloseDS(ds);
        }

    }

}
