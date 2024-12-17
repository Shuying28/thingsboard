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
