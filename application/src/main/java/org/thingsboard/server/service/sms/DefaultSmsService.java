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
package org.thingsboard.server.service.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.sms.SmsSender;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sms.config.SmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultSmsService implements SmsService {
	
	public static final int TIMEOUT = 30;
    private final SmsSenderFactory smsSenderFactory;
    private final AdminSettingsService adminSettingsService;
    private final TbApiUsageStateService apiUsageStateService;
    private final TbApiUsageReportClient apiUsageClient;
    private final SmsExecutorService smsExecutor;
    private SmsSender smsSender;
    ScheduledExecutorService timeoutScheduler;

    // Initialize the SMS configuration after the service is created
    @PostConstruct
    private void init() {
        updateSmsConfiguration();
    }

    // Cleanup resources when the service is destroyed
    @PreDestroy
    private void destroy() {
        if (this.smsSender != null) {
            this.smsSender.destroy();
        }
        if (timeoutScheduler != null) {
            timeoutScheduler.shutdownNow();
        }
    }

    // Updates the SMS configuration by retrieving settings and creating a new SMS sender
    @Override
    public void updateSmsConfiguration() {
        AdminSettings settings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "sms");
        if (settings != null) {
            try {
                JsonNode jsonConfig = settings.getJsonValue();
                SmsProviderConfiguration configuration = JacksonUtil.convertValue(jsonConfig, SmsProviderConfiguration.class);
                SmsSender newSmsSender = this.smsSenderFactory.createSmsSender(configuration);
                if (this.smsSender != null) {
                    this.smsSender.destroy();
                }
                this.smsSender = newSmsSender;
            } catch (Exception e) {
                log.error("Failed to create SMS sender", e);
            }
        }
    }

    protected int sendSms(String numberTo, String message) throws ThingsboardException {
        if (this.smsSender == null) {
            throw new ThingsboardException("Unable to send SMS: no SMS provider configured!", ThingsboardErrorCode.GENERAL);
        }
        return this.sendSms(this.smsSender, numberTo, message);
    }

    //Sends SMS messages asynchronously to multiple recipients  
    @Override
    public ListenableFuture<Void> sendSms(SmsSender smsSender, String[] numbersToList, String message) {
        ListenableFuture<Void> future = smsExecutor.executeAsync(() -> {
            for (String numberTo : numbersToList) {
                this.smsSender.sendSms(numberTo, message);
            }
            return null;
        });
        return Futures.withTimeout(future, TIMEOUT, TimeUnit.SECONDS, timeoutScheduler);
    }

    @Override
    public ListenableFuture<Void> sendSms(TenantId tenantId, CustomerId customerId, String[] numbersTo, String message) {
        ListenableFuture<Void> future = smsExecutor.executeAsync(() -> {
            doSendSms(tenantId, customerId, numbersTo, message);
            return null;
        });
        return Futures.withTimeout(future, TIMEOUT, TimeUnit.SECONDS, timeoutScheduler);
    }

    // Handles the sending of SMS in a transaction
    public void doSendSms(TenantId tenantId, CustomerId customerId, String[] numbersTo, String message) {
    	if (apiUsageStateService.getApiUsageState(tenantId).isSmsSendEnabled()) {
            int smsCount = 0;
            try {
                for (String numberTo : numbersTo) {
                    smsCount += this.sendSms(numberTo, message);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (smsCount > 0) {
                    apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.SMS_EXEC_COUNT, smsCount);
                }
            }
        } else {
            throw new RuntimeException("SMS sending is disabled due to API limits!");
        }
    }

    @Override
    public ListenableFuture<Void> sendTestSms(TestSmsRequest testSmsRequest) {
        ListenableFuture<Void> future = smsExecutor.executeAsync(() -> {
            try (SmsSender testSmsSender = this.smsSenderFactory.createSmsSender(testSmsRequest.getProviderConfiguration())){
                this.sendSms(testSmsSender, testSmsRequest.getNumberTo(), testSmsRequest.getMessage());
            } catch (Exception e) {
                throw handleException(e);
            }
            return null;
        });
        return Futures.withTimeout(future, TIMEOUT, TimeUnit.SECONDS, timeoutScheduler);
    }

    @Override
    public boolean isConfigured(TenantId tenantId) {
        return smsSender != null;
    }

    // Sends an SMS message to a single recipient
    private int sendSms(SmsSender smsSender, String numberTo, String message) throws ThingsboardException {
        try {
            int sentSms = smsSender.sendSms(numberTo, message);
            log.trace("Successfully sent sms to number: {}", numberTo);
            return sentSms;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private ThingsboardException handleException(Exception exception) {
        String message;
        if (exception instanceof NestedRuntimeException) {
            message = ((NestedRuntimeException) exception).getMostSpecificCause().getMessage();
        } else {
            message = exception.getMessage();
        }
        log.warn("Unable to send SMS: {}", message);
        return new ThingsboardException(String.format("Unable to send SMS: %s", message),
                ThingsboardErrorCode.GENERAL);
    }
}
