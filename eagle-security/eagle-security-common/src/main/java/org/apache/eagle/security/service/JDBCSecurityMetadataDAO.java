/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  * <p/>
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  * <p/>
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.eagle.security.service;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Since 8/8/16.
 */
public class JDBCSecurityMetadataDAO implements ISecurityMetadataDAO  {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCSecurityMetadataDAO.class);

    private Config config;
    /**
     * composite primary key: site and hbase_resource
     */
    private final String QUERY_ALL_STATEMENT = "SELECT site, hbase_resource, sensitivity_type FROM hbase_sensitivity_entity";
    private final String INSERT_STATEMENT = "INSERT INTO hbase_sensitivity_entity (site, hbase_resource, sensitivity_type) VALUES (?, ?, ?)";

    // get connection url from config
    public JDBCSecurityMetadataDAO(Config config){
        this.config = config;
    }

    @Override
    public Collection<HBaseSensitivityEntity> listHBaseSensitivies() {
        Connection connection = null;
        PreparedStatement statement = null;
        Collection<HBaseSensitivityEntity> ret = new ArrayList<>();
        ResultSet rs = null;
        try {
            connection = getJdbcConnection();
            statement = connection.prepareStatement(QUERY_ALL_STATEMENT);
            rs = statement.executeQuery();
            while (rs.next()) {
                HBaseSensitivityEntity entity = new HBaseSensitivityEntity();
                entity.setSite(rs.getString(1));
                entity.setHbaseResource(rs.getString(2));
                entity.setSensitivityType(rs.getString(3));
                ret.add(entity);
            }
        }catch(Exception e) {
            LOG.error("error in querying hbase_sensitivity_entity table", e);
        }finally{
            try{
                if(rs != null)
                    rs.close();
                if(statement != null)
                    statement.close();
                if(connection != null)
                    connection.close();
            }catch(Exception ex){
                LOG.error("error in closing database resources", ex);
            }
        }
        return ret;
    }

    @Override
    public OpResult addHBaseSensitivity(Collection<HBaseSensitivityEntity> h) {
        Connection connection = null;
        PreparedStatement statement = null;
        try{
            connection = getJdbcConnection();
            statement = connection.prepareStatement(INSERT_STATEMENT);
            connection.setAutoCommit(false);
            for(HBaseSensitivityEntity entity : h){
                statement.setString(1, entity.getSite());
                statement.setString(2, entity.getHbaseResource());
                statement.setString(3, entity.getSensitivityType());
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        }catch(Exception ex){
            LOG.error("error in querying hbase_sensitivity_entity table", ex);
        }finally {
            try {
                if (statement != null)
                    statement.close();
                if(connection != null)
                    connection.close();
            }catch(Exception ex){
                LOG.error("error in closing database resources", ex);
            }
        }
        return null;
    }

    private Connection getJdbcConnection() throws Exception {
        Connection connection = null;
        String conn = config.getString("connection");
        try {
            connection = DriverManager.getConnection(conn, "root", "");
        } catch (Exception e) {
            LOG.error("error get connection for {}", conn, e);
            throw e;
        }
        return connection;
    }
}