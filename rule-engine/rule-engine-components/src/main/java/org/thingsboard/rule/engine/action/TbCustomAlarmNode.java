package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.common.util.JacksonUtil;

/**
 * Custom node for processing alarms based on device-specific thresholds.
 */
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
