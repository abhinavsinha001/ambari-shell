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

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.support.util.FileUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.shell.AmbariShell;

@Component
public class HelloWorldCommands implements CommandMarker {

  @CliAvailabilityIndicator({"hw simple"})
  public boolean isCommandAvailable() {
    return true;
  }

  @CliCommand(value = "hello", help = "Prints a simple hello world message")
  public String simple() {
    return FileUtils.readBanner(AmbariShell.class, "banner.txt");
  }
}