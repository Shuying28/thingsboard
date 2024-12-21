///
/// Copyright © 2016-2024 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component } from '@angular/core';
import { CustomAlarmWidgetService } from './custom-alarm-widget.service';

@Component({
  selector: 'app-custom-alarm-widget',
  templateUrl: './custom-alarm-widget.component.html',
  styleUrls: ['./custom-alarm-widget.component.scss']
})
export class CustomAlarmWidgetComponent {
  // UI state
  deviceType: string = 'temperatureSensor';
  threshold: number | null = null;

  constructor(private alarmService: CustomAlarmWidgetService) {}

  saveSettings() {
    const config = {
      deviceType: this.deviceType,
      customThreshold: this.threshold
    };

    console.log('Saving Custom Alarm Configuration:', config);

    // Replace 'nodeId' with the actual node identifier
    this.alarmService.saveCustomAlarmConfig('nodeId', config).subscribe(
      (response) => console.log('Settings saved successfully', response),
      (error) => console.error('Error saving settings', error)
    );
  }
}
