import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DashboardStateService {
  tomorrowAppointmentsCount = signal(0);
}
