import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CustomAlarmWidgetService {
  constructor(private http: HttpClient) {}

  saveCustomAlarmConfig(nodeId: string, config: any): Observable<any> {
    // Replace with actual ThingsBoard API endpoint for updating node configuration
    return this.http.post(`/api/custom-alarm-node/${nodeId}/config`, config);
  }
}
