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

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
public class TbCustomAlarmNodeConfiguration extends TbAbstractAlarmNodeConfiguration
        implements NodeConfiguration<TbCustomAlarmNodeConfiguration> {

    @NoXss
    private String customThreshold = "50.0"; // Example custom property for threshold values.

    public TbCustomAlarmNodeConfiguration() {
        this.setScriptLang(ScriptLanguage.JS);
        this.setAlarmDetailsBuildJs(ALARM_DETAILS_BUILD_JS_TEMPLATE);
        this.setAlarmDetailsBuildTbel(ALARM_DETAILS_BUILD_TBEL_TEMPLATE);
    }

    @Override
    public TbCustomAlarmNodeConfiguration defaultConfiguration() {
        TbCustomAlarmNodeConfiguration configuration = new TbCustomAlarmNodeConfiguration();
        configuration.setCustomThreshold("50.0");
        return configuration;
    }
}