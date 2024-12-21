/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.common.util.JacksonUtil;

/**
 * Custom node for processing alarms based on device-specific thresholds.
 */
@RuleNode(
        type = ComponentType.ACTION,
        name = "Custom Alarm",
        relationTypes = {"Success", "Failure"},
        configClazz = TbCustomAlarmNodeConfiguration.class,
        nodeDescription = "Triggers alarms based on custom device-specific thresholds.",
        nodeDetails = "Processes messages from devices and triggers alarms when thresholds are exceeded. Configurable thresholds and severities.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCustomAlarmConfig",
        icon = "notifications_active"
)
public class TbCustomAlarmNode extends TbAbstractAlarmNode<TbCustomAlarmNodeConfiguration> {

    @Override
    protected TbCustomAlarmNodeConfiguration loadAlarmNodeConfig(TbNodeConfiguration configuration) {
        return JacksonUtil.convertValue(configuration, TbCustomAlarmNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<TbAlarmResult> processAlarm(TbContext ctx, TbMsg msg) {
        TbMsgMetaData metaData = msg.getMetaData();
        String deviceType = metaData.getValue("deviceType");
        AlarmSeverity severity = determineSeverity(deviceType, metaData);

        if (severity != null) {
            // Create an Alarm object
            Alarm alarm = new Alarm();
            alarm.setSeverity(severity);
            alarm.setOriginator(msg.getOriginator());

            // Return a new TbAlarmResult for a triggered alarm
            return Futures.immediateFuture(new TbAlarmResult(true, false, false, alarm));
        }

        // Return a default TbAlarmResult (no alarm created)
        return Futures.immediateFuture(new TbAlarmResult(false, false, false, null));
    }

    private AlarmSeverity determineSeverity(String deviceType, TbMsgMetaData metaData) {
        if ("temperatureSensor".equals(deviceType)) {
            String temperature = metaData.getValue("temperature");
            if (temperature != null && Double.parseDouble(temperature) > 50.0) {
                return AlarmSeverity.CRITICAL;
            }
        }
        return null;
    }
}