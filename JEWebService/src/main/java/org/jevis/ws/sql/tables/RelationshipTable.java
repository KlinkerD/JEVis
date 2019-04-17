/**
 * Copyright (C) 2013 - 2014 Envidatec GmbH <info@envidatec.com>
 * <p>
 * This file is part of JEAPI-SQL.
 * <p>
 * JEAPI-SQL is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation in version 3.
 * <p>
 * JEAPI-SQL is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * JEAPI-SQL. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * JEAPI-SQL is part of the OpenJEVis project, further project information are
 * published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.ws.sql.tables;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jevis.api.JEVisException;
import org.jevis.commons.ws.json.JsonRelationship;
import org.jevis.ws.sql.SQLDataSource;
import org.jevis.ws.sql.SQLtoJsonFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Florian Simon <florian.simon@envidatec.com>
 */
public class RelationshipTable {

    public final static String TABLE = "relationship";
    public final static String COLUMN_START = "startobject";
    public final static String COLUMN_END = "endobject";
    public final static String COLUMN_TYPE = "relationtype";
    private static final Logger logger = LogManager.getLogger(RelationshipTable.class);
    private SQLDataSource _connection;

    public RelationshipTable(SQLDataSource ds) {
        _connection = ds;
    }

    public List<JsonRelationship> selectByType(int type) {
        String sql = String.format("select * from %s where %s=?", TABLE, COLUMN_TYPE);


        List<JsonRelationship> relations = new LinkedList<>();
        try (PreparedStatement ps = _connection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, type);

            logger.trace("SQL: {}", ps);
            _connection.addQuery("Relationship.byTypeg(int)", ps.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    relations.add(SQLtoJsonFactory.buildRelationship(rs));
                } catch (Exception ex) {
                    logger.error(ex);
                }
            }
        } catch (SQLException ex) {
            logger.error(ex);
        }

        return relations;
    }

    /**
     * @param id jevisObject id
     * @return
     */
    public List<JsonRelationship> selectForObject(long id) {
        String sql = String.format("select * from %s where %s=?  or %s=?", TABLE, COLUMN_START, COLUMN_END);

        List<JsonRelationship> relations = new LinkedList<>();

        try (PreparedStatement ps = _connection.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, id);

            logger.trace("SQL: {}", ps);
            _connection.addQuery("Relationship.selectForObject(long)", ps.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    relations.add(SQLtoJsonFactory.buildRelationship(rs));
                } catch (Exception ex) {
                    logger.error(ex);
                }
            }
        } catch (SQLException ex) {
            logger.error(ex);
        }

        return relations;
    }

    /**
     * @param start
     * @param end
     * @param type
     * @return
     * @throws JEVisException
     */
    public JsonRelationship insert(long start, long end, int type) throws JEVisException {

        String sql = String.format("insert into %s (%s,%s,%s) values (?,?,?)", TABLE, COLUMN_START, COLUMN_END, COLUMN_TYPE);

        try (PreparedStatement ps = _connection.getConnection().prepareStatement(sql)) {
            ps.setLong(1, start);
            ps.setLong(2, end);
            ps.setInt(3, type);
            logger.trace("SQL: {}", ps);
            _connection.addQuery("Relationship.insert()", ps.toString());
            int count = ps.executeUpdate();
            if (count == 1) {
                JsonRelationship json = new JsonRelationship();
                json.setFrom(start);
                json.setTo(end);
                json.setType(type);

                return json;
            } else {
                throw new JEVisException("Could not create the relationship", 1964823);
            }
        } catch (SQLException ex) {
            logger.error(ex);
            throw new JEVisException("Could not create the relationship", 1964824, ex);
        }
    }

    public boolean delete(JsonRelationship rel) {
        return delete(rel.getFrom(), rel.getTo(), rel.getType());
    }


    public boolean delete(long start, long end, int type) {

        String sql = String.format("delete from %s where %s=? and %s=? and %s=?", TABLE, COLUMN_START, COLUMN_END, COLUMN_TYPE);


        try (PreparedStatement ps = _connection.getConnection().prepareStatement(sql)) {
            ps.setLong(1, start);
            ps.setLong(2, end);
            ps.setInt(3, type);
            logger.trace("SQL: {}", ps);
            _connection.addQuery("Relationship.delete()", ps.toString());
            int count = ps.executeUpdate();
            if (count == 1) {
                return true;
            } else {
                return true;//delete is allways true
            }
        } catch (SQLException ex) {
            logger.error(ex);
            return false;
        }
    }


    public boolean deleteAll(List<Long> ids) {
        String in = " IN(";
        for (int i = 0; i < ids.size(); i++) {
            in += ids.get(i);
            if (i != ids.size() - 1) {
                in += ",";
            }
        }
        in += ")";

        String sql = String.format("delete from %s where %s%s or %s%s", TABLE, COLUMN_START, in, COLUMN_END, in);


        try (PreparedStatement ps = _connection.getConnection().prepareStatement(sql)) {
            logger.trace("SQL: {}", ps);

            _connection.addQuery("Relationship.deleteAll()", ps.toString());
            int count = ps.executeUpdate();

            return count == 1;


        } catch (SQLException ex) {
            logger.error(ex);
            return false;
        }

    }

    public List<JsonRelationship> getAllForObject(long object) {
        List<JsonRelationship> relations = new LinkedList<>();


        String sql = String.format("select * from %s where %s=? or %s=?", TABLE, COLUMN_END, COLUMN_START);

        try (PreparedStatement ps = _connection.getConnection().prepareStatement(sql)) {
            ps.setLong(1, object);
            ps.setLong(2, object);
            logger.trace("SQL: {}", ps);
            _connection.addQuery("Relationship.getAllForObject()", ps.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    relations.add(SQLtoJsonFactory.buildRelationship(rs));
                } catch (Exception ex) {
                    logger.error(ex);
                }
            }

        } catch (SQLException ex) {
            logger.error(ex);
        }

        return relations;
    }


    public List<JsonRelationship> getAll() {
        List<JsonRelationship> relations = new LinkedList<>();
        String sql = String.format("select * from %s", TABLE);

        try (PreparedStatement ps = _connection.getConnection().prepareStatement(sql)) {
            logger.trace("SQL: {}", ps);
            _connection.addQuery("Relationship.getAll()", ps.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    relations.add(SQLtoJsonFactory.buildRelationship(rs));
                } catch (Exception ex) {
                    logger.error(ex);
                }
            }
        } catch (SQLException ex) {
            logger.error(ex);
        }

        return relations;
    }

    public List<JsonRelationship> getAll(List<Integer> types) {

        List<JsonRelationship> relations = new LinkedList<>();

        String in = " IN(";
        boolean first = true;
        for (int i : types) {
            if (!first) {
                in += ",";
            }
            in += i;
            if (first) {
                first = false;
            }
        }

        in += ")";

        String sql = String.format("select * from %s where %s%s", TABLE, COLUMN_TYPE, in);

        try (PreparedStatement ps = _connection.getConnection().prepareStatement(sql)) {
            int pos = 0;
            for (int type : types) {
                ps.setInt(++pos, type);
            }
            logger.trace("SQL: {}", ps);
            _connection.addQuery("Relationship.getAll(List<int>)", ps.toString());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    relations.add(SQLtoJsonFactory.buildRelationship(rs));
                } catch (Exception ex) {
                    logger.error(ex);
                }
            }
        } catch (SQLException ex) {
            logger.error(ex);
        }


        return relations;
    }
}
