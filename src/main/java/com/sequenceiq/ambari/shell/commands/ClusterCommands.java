/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sequenceiq.ambari.shell.commands;

import static com.sequenceiq.ambari.shell.support.TableRenderer.renderMultiValueMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.shell.model.AmbariContext;
import com.sequenceiq.ambari.shell.model.FocusType;

/**
 * Cluster related commands used in the shell.
 *
 * @see com.sequenceiq.ambari.client.AmbariClient
 */
@Component
public class ClusterCommands implements CommandMarker {

  private AmbariClient client;
  private AmbariContext context;
  private Map<String, List<String>> hostGroups;

  @Autowired
  public ClusterCommands(AmbariClient client, AmbariContext context) {
    this.client = client;
    this.context = context;
  }

  /**
   * Checks whether the cluster build command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("cluster build")
  public boolean isFocusBlueprintCommandAvailable() {
    return !context.isConnectedToCluster();
  }

  /**
   * Sets the focus on cluster building. Takes a blueprint id, if it does not exists it wont focus.
   * After focus the users are able to assign hosts to host groups.
   *
   * @param id id of the blueprint
   * @return prints the blueprint as formatted table if exists, otherwise error message
   */
  @CliCommand(value = "cluster build", help = "Starts to build a cluster")
  public String focusBlueprint(
    @CliOption(key = "blueprint", mandatory = true, help = "Id of the blueprint, use 'blueprints' command to see the list") String id) {
    String message = "Not a valid blueprint id";
    if (client.doesBlueprintExists(id)) {
      context.setFocus(id, FocusType.CLUSTER_BUILD);
      message = renderMultiValueMap(client.getBlueprintMap(id), "HOSTGROUP", "COMPONENT");
      createNewHostGroups();
    }
    return message;
  }

  /**
   * Checks whether the cluster assign command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("cluster assign")
  public boolean isAssignCommandAvailable() {
    return context.isFocusOnClusterBuild();
  }

  /**
   * Assign hosts to host groups provided in the blueprint.
   *
   * @param host  host to assign
   * @param group which host group to
   * @return status message
   */
  @CliCommand(value = "cluster assign", help = "Assign host to host group")
  public String assign(
    @CliOption(key = "host", mandatory = true, help = "Fully qualified host name") String host,
    @CliOption(key = "hostGroup", mandatory = true, help = "Host group which to assign the host") String group) {
    return addHostToGroup(host, group) ?
      String.format("%s has been added to %s", host, group) : String.format("%s is not a valid host group", group);
  }

  /**
   * Checks whether the cluster preview command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("cluster preview")
  public boolean isAssignShowCommandAvailable() {
    return context.isFocusOnClusterBuild();
  }

  /**
   * Shows the currently assigned hosts.
   *
   * @return formatted host - host group table
   */
  @CliCommand(value = "cluster preview", help = "Shows the currently assigned hosts")
  public String showAssignments() {
    return renderMultiValueMap(hostGroups, "HOSTGROUP", "HOST");
  }

  /**
   * Checks whether the cluster create command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("cluster create")
  public boolean isCreateClusterCommandAvailable() {
    return context.isFocusOnClusterBuild();
  }

  /**
   * Creates a new cluster based on the provided host - host group associations and the selected blueprint.
   * If the cluster creation fails, deletes the cluster.
   *
   * @return status message
   */
  @CliCommand(value = "cluster create", help = "Create a cluster based on current blueprint and assigned hosts")
  public String createCluster() {
    String blueprint = context.getFocusValue();
    boolean success = client.createCluster(blueprint, blueprint, hostGroups);
    if (success) {
      context.connectCluster();
      context.resetFocus();
    } else {
      deleteCluster(blueprint);
      createNewHostGroups();
    }
    return success ? "Successfully created cluster" : "Failed to create cluster";
  }

  /**
   * Checks whether the cluster delete command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("cluster delete")
  public boolean isDeleteClusterCommandAvailable() {
    return context.isConnectedToCluster();
  }

  /**
   * Deletes the cluster.
   *
   * @return status message
   */
  @CliCommand(value = "cluster delete", help = "Delete the cluster")
  public String deleteCluster() {
    return deleteCluster(context.getCluster()) ? "Successfully deleted the cluster" : "Could not delete the cluster";
  }

  private boolean deleteCluster(String id) {
    return client.deleteCluster(id);
  }

  private void createNewHostGroups() {
    Map<String, List<String>> groups = new HashMap<String, List<String>>();
    for (String hostGroup : client.getHostGroups(context.getFocusValue())) {
      groups.put(hostGroup, new ArrayList<String>());
    }
    this.hostGroups = groups;
  }

  private boolean addHostToGroup(String host, String group) {
    boolean result = true;
    List<String> hosts = hostGroups.get(group);
    if (hosts == null) {
      result = false;
    } else {
      hosts.add(host);
    }
    return result;
  }
}
