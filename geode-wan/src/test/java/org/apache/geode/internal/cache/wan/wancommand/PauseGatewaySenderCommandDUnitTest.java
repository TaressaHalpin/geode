/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.cache.wan.wancommand;

import static org.apache.geode.test.dunit.Assert.assertEquals;
import static org.apache.geode.test.dunit.Assert.assertFalse;
import static org.apache.geode.test.dunit.Assert.assertTrue;
import static org.apache.geode.test.dunit.Assert.fail;
import static org.apache.geode.test.dunit.LogWriterUtils.getLogWriter;
import static org.apache.geode.test.dunit.Wait.pause;

import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.management.cli.Result;
import org.apache.geode.management.internal.cli.i18n.CliStrings;
import org.apache.geode.management.internal.cli.result.CommandResult;
import org.apache.geode.management.internal.cli.result.TabularResultData;
import org.apache.geode.test.junit.categories.DistributedTest;

@Category(DistributedTest.class)
public class PauseGatewaySenderCommandDUnitTest extends WANCommandTestBase {
  @Test
  public void testPauseGatewaySender_ErrorConditions() {
    Integer dsIdPort = vm1.invoke(() -> createFirstLocatorWithDSId(1));
    propsSetUp(dsIdPort);
    vm2.invoke(() -> createFirstRemoteLocator(2, dsIdPort));

    vm3.invoke(() -> createCache(dsIdPort));
    vm3.invoke(() -> createSender("ln", 2, false, 100, 400, false, false, null, true));

    final DistributedMember vm1Member = vm3.invoke(this::getMember);
    String command = CliStrings.PAUSE_GATEWAYSENDER + " --" + CliStrings.PAUSE_GATEWAYSENDER__ID
        + "=ln --" + CliStrings.MEMBER + "=" + vm1Member.getId() + " --" + CliStrings.GROUP
        + "=SenderGroup1";
    CommandResult cmdResult = executeCommand(command);

    if (cmdResult != null) {
      String strCmdResult = commandResultToString(cmdResult);
      getLogWriter().info("testPauseGatewaySender stringResult : " + strCmdResult + ">>>>");
      assertEquals(Result.Status.ERROR, cmdResult.getStatus());
      assertTrue(strCmdResult.contains(CliStrings.PROVIDE_EITHER_MEMBER_OR_GROUP_MESSAGE));
    } else {
      fail("testPauseGatewaySender failed as did not get CommandResult");
    }
  }

  /**
   * test to validate that the start gateway sender starts the gateway sender on a member
   */
  @Test
  public void testPauseGatewaySender_onMember() {
    Integer dsIdPort = vm1.invoke(() -> createFirstLocatorWithDSId(1));
    propsSetUp(dsIdPort);
    vm2.invoke(() -> createFirstRemoteLocator(2, dsIdPort));

    vm3.invoke(() -> createCache(dsIdPort));
    vm3.invoke(() -> createSender("ln", 2, false, 100, 400, false, false, null, true));
    vm3.invoke(() -> startSender("ln"));
    vm3.invoke(() -> verifySenderState("ln", true, false));

    final DistributedMember vm1Member = vm3.invoke(this::getMember);
    pause(10000);
    String command = CliStrings.PAUSE_GATEWAYSENDER + " --" + CliStrings.PAUSE_GATEWAYSENDER__ID
        + "=ln --" + CliStrings.MEMBER + "=" + vm1Member.getId();
    CommandResult cmdResult = executeCommand(command);

    if (cmdResult != null) {
      String strCmdResult = commandResultToString(cmdResult);
      getLogWriter().info("testPauseGatewaySender stringResult : " + strCmdResult + ">>>>");
      assertEquals(Result.Status.OK, cmdResult.getStatus());
      assertTrue(strCmdResult.contains("is paused on member"));
    } else {
      fail("testPauseGatewaySender failed as did not get CommandResult");
    }
    vm3.invoke(() -> verifySenderState("ln", true, true));
  }

  @Test
  public void testPauseGatewaySender() {
    Integer dsIdPort = vm1.invoke(() -> createFirstLocatorWithDSId(1));
    propsSetUp(dsIdPort);
    vm2.invoke(() -> createFirstRemoteLocator(2, dsIdPort));

    vm3.invoke(() -> createCache(dsIdPort));
    vm3.invoke(() -> createSender("ln", 2, false, 100, 400, false, false, null, true));
    vm4.invoke(() -> createCache(dsIdPort));
    vm4.invoke(() -> createSender("ln", 2, false, 100, 400, false, false, null, true));
    vm5.invoke(() -> createCache(dsIdPort));
    vm5.invoke(() -> createSender("ln", 2, false, 100, 400, false, false, null, true));
    vm3.invoke(() -> startSender("ln"));
    vm4.invoke(() -> startSender("ln"));
    vm5.invoke(() -> startSender("ln"));
    vm3.invoke(() -> verifySenderState("ln", true, false));
    vm4.invoke(() -> verifySenderState("ln", true, false));
    vm5.invoke(() -> verifySenderState("ln", true, false));

    pause(10000);
    String command =
        CliStrings.PAUSE_GATEWAYSENDER + " --" + CliStrings.PAUSE_GATEWAYSENDER__ID + "=ln";
    CommandResult cmdResult = executeCommand(command);

    if (cmdResult != null) {
      String strCmdResult = commandResultToString(cmdResult);
      getLogWriter().info("testPauseGatewaySender stringResult : " + strCmdResult + ">>>>");
      assertEquals(Result.Status.OK, cmdResult.getStatus());
      TabularResultData resultData = (TabularResultData) cmdResult.getResultData();
      List<String> status = resultData.retrieveAllValues("Result");
      assertEquals(5, status.size());
      assertTrue(status.contains("Error"));
      assertTrue(status.contains("OK"));
    } else {
      fail("testPauseGatewaySender failed as did not get CommandResult");
    }
    vm3.invoke(() -> verifySenderState("ln", true, true));
    vm4.invoke(() -> verifySenderState("ln", true, true));
    vm5.invoke(() -> verifySenderState("ln", true, true));
  }

  /**
   * test to validate that the start gateway sender starts the gateway sender on a group of members
   */
  @Test
  public void testPauseGatewaySender_Group() {
    Integer dsIdPort = vm1.invoke(() -> createFirstLocatorWithDSId(1));
    propsSetUp(dsIdPort);
    vm2.invoke(() -> createFirstRemoteLocator(2, dsIdPort));

    vm3.invoke(() -> createCacheWithGroups(dsIdPort, "SenderGroup1"));
    vm3.invoke(() -> createSender("ln", 2, false, 100, 400, false, false, null, true));
    vm4.invoke(() -> createCacheWithGroups(dsIdPort, "SenderGroup1"));
    vm4.invoke(() -> createSender("ln", 2, false, 100, 400, false, false, null, true));
    vm5.invoke(() -> createCacheWithGroups(dsIdPort, "SenderGroup1"));
    vm5.invoke(() -> createSender("ln", 2, false, 100, 400, false, false, null, true));
    vm3.invoke(() -> startSender("ln"));
    vm4.invoke(() -> startSender("ln"));
    vm5.invoke(() -> startSender("ln"));
    vm3.invoke(() -> verifySenderState("ln", true, false));
    vm4.invoke(() -> verifySenderState("ln", true, false));
    vm5.invoke(() -> verifySenderState("ln", true, false));

    pause(10000);
    String command = CliStrings.PAUSE_GATEWAYSENDER + " --" + CliStrings.PAUSE_GATEWAYSENDER__ID
        + "=ln --" + CliStrings.GROUP + "=SenderGroup1";
    CommandResult cmdResult = executeCommand(command);

    if (cmdResult != null) {
      String strCmdResult = commandResultToString(cmdResult);
      getLogWriter().info("testPauseGatewaySender_Group stringResult : " + strCmdResult + ">>>>");
      assertEquals(Result.Status.OK, cmdResult.getStatus());
      TabularResultData resultData = (TabularResultData) cmdResult.getResultData();
      List<String> status = resultData.retrieveAllValues("Result");
      assertEquals(3, status.size());
      assertFalse(status.contains("Error"));
      assertTrue(status.contains("OK"));
    } else {
      fail("testPauseGatewaySender failed as did not get CommandResult");
    }
    vm3.invoke(() -> verifySenderState("ln", true, true));
    vm4.invoke(() -> verifySenderState("ln", true, true));
    vm5.invoke(() -> verifySenderState("ln", true, true));
  }

  /**
   * Test to validate the scenario gateway sender is started when one or more sender members belongs
   * to multiple groups
   */
  @Test
  public void testPauseGatewaySender_MultipleGroup() {
    Integer dsIdPort = vm1.invoke(() -> createFirstLocatorWithDSId(1));
    propsSetUp(dsIdPort);
    vm2.invoke(() -> createFirstRemoteLocator(2, dsIdPort));

    vm3.invoke(() -> createCacheWithGroups(dsIdPort, "SenderGroup1"));
    vm3.invoke(() -> createSender("ln", 2, false, 100, 400, false, false, null, true));
    vm4.invoke(() -> createCacheWithGroups(dsIdPort, "SenderGroup1"));
    vm4.invoke(() -> createSender("ln", 2, false, 100, 400, false, false, null, true));
    vm5.invoke(() -> createCacheWithGroups(dsIdPort, "SenderGroup1, SenderGroup2"));
    vm5.invoke(() -> createSender("ln", 2, false, 100, 400, false, false, null, true));
    vm6.invoke(() -> createCacheWithGroups(dsIdPort, "SenderGroup2"));
    vm6.invoke(() -> createSender("ln", 2, false, 100, 400, false, false, null, true));
    vm7.invoke(() -> createCacheWithGroups(dsIdPort, "SenderGroup3"));
    vm7.invoke(() -> createSender("ln", 2, false, 100, 400, false, false, null, true));
    vm3.invoke(() -> startSender("ln"));
    vm4.invoke(() -> startSender("ln"));
    vm5.invoke(() -> startSender("ln"));
    vm6.invoke(() -> startSender("ln"));
    vm7.invoke(() -> startSender("ln"));
    vm3.invoke(() -> verifySenderState("ln", true, false));
    vm4.invoke(() -> verifySenderState("ln", true, false));
    vm5.invoke(() -> verifySenderState("ln", true, false));
    vm6.invoke(() -> verifySenderState("ln", true, false));
    vm7.invoke(() -> verifySenderState("ln", true, false));

    pause(10000);
    String command = CliStrings.PAUSE_GATEWAYSENDER + " --" + CliStrings.PAUSE_GATEWAYSENDER__ID
        + "=ln --" + CliStrings.GROUP + "=SenderGroup1,SenderGroup2";
    CommandResult cmdResult = executeCommand(command);

    if (cmdResult != null) {
      String strCmdResult = commandResultToString(cmdResult);
      getLogWriter().info("testPauseGatewaySender_Group stringResult : " + strCmdResult + ">>>>");
      assertEquals(Result.Status.OK, cmdResult.getStatus());
      TabularResultData resultData = (TabularResultData) cmdResult.getResultData();
      List<String> status = resultData.retrieveAllValues("Result");
      assertEquals(4, status.size());
      assertFalse(status.contains("Error"));
      assertTrue(status.contains("OK"));
    } else {
      fail("testPauseGatewaySender failed as did not get CommandResult");
    }
    vm3.invoke(() -> verifySenderState("ln", true, true));
    vm4.invoke(() -> verifySenderState("ln", true, true));
    vm5.invoke(() -> verifySenderState("ln", true, true));
    vm6.invoke(() -> verifySenderState("ln", true, true));
    vm7.invoke(() -> verifySenderState("ln", true, false));
  }
}