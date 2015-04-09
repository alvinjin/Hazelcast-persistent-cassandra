/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.paytmlabs.hazelcast.mapstore.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.paytmlabs.hazelcast.Constants;
import com.paytmlabs.hazelcast.mapstore.EntryEntity;
import com.paytmlabs.hazelcast.mapstore.HazelcastDao;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraDao implements HazelcastDao<EntryEntity> {

  static final Logger log = LoggerFactory.getLogger(CassandraDao.class);

  private Cluster cluster;

  public void initialize(String node) {
    cluster = Cluster.builder().addContactPoint(node).build();
    final Metadata metadata = cluster.getMetadata();
    log.info("Connected to cluster: {}", metadata.getClusterName());
    metadata.getAllHosts().stream().
        forEach((host) -> {
          log.info("Datacenter: {}; Host: {}; Rack: {}",
              host.getDatacenter(), host.getAddress(), host.getRack());
    });
  }

  @Override
  public void persist(final EntryEntity value) {
    connect().execute(//
        "INSERT INTO " + Constants.CASSANDRA_KEYSPACE_TABLE_NAME//
            + " (id, data) "//
            + "VALUES ('" + value.getId() + "','" + value.getMessage() //
            + "');");
  }

  @Override
  public void remove(String key) {
    connect().execute(//
        "DELETE FROM  " + Constants.CASSANDRA_KEYSPACE_TABLE_NAME//
            + " WHERE id = '" + key + "';");
  }

  @Override
  public EntryEntity find(String key) {
    ResultSet res = connect().execute(//
        "SELECT * FROM  " + Constants.CASSANDRA_KEYSPACE_TABLE_NAME//
            + " WHERE id = '" + key + "';");
    if (res.getAvailableWithoutFetching() > 0) {
      final Row r = res.one();
      return new EntryEntity(r.getString(0), r.getString(1));
    }
    return null;
  }

  @Override
  public List<EntryEntity> findAll() {
    final ResultSet rowList = connect().execute(
    "SELECT * FROM " + Constants.CASSANDRA_KEYSPACE_TABLE_NAME + ";");
    if(rowList.getAvailableWithoutFetching()>0) {
      final List<EntryEntity> result = new  ArrayList<>();
      rowList.all().stream().
          forEach((row) -> {
            result.add(new EntryEntity(row.getString(0), row.getString(1)));
      });
      return result;
    }
    return Collections.EMPTY_LIST;
  }

  public Session connect() {
    return cluster.connect();
  }

  public void close() {
    cluster.close();
  }

  @Override
  public List<EntryEntity> findAll(Collection<String> ids) {
    //Transform collection into a string
    final String filters = String.join(",", ids.stream().
        map((id) -> "'" + id + "'").collect(Collectors.toList()));
    
    
    String query = //
        "SELECT * FROM  " + Constants.CASSANDRA_KEYSPACE_TABLE_NAME//
            + " WHERE id IN (" + filters + ");";
    log.info("Query: {}",query);
    ResultSet rowList = connect().execute(query);
    if(rowList.getAvailableWithoutFetching()>0) {
      final List<EntryEntity> result = new  ArrayList<>();
      rowList.all().stream().
          forEach((row) -> {
            result.add(new EntryEntity(row.getString(0), row.getString(1)));
      });
      return result;
    }
    return Collections.EMPTY_LIST;
  }
}
