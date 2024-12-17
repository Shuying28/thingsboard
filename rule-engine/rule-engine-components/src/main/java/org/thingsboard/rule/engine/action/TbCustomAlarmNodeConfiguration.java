package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
public class TbCustomAlarmNodeConfiguration extends TbAbstractAlarmNodeConfiguration {

    @NoXss
    private String customThreshold; // Example custom property for threshold values

    public TbCustomAlarmNodeConfiguration() {
        this.setScriptLang(ScriptLanguage.JS); // Default script language
        this.setAlarmDetailsBuildJs(ALARM_DETAILS_BUILD_JS_TEMPLATE); // Default JS template
        this.setAlarmDetailsBuildTbel(ALARM_DETAILS_BUILD_TBEL_TEMPLATE); // Default TBEL template
    }
}