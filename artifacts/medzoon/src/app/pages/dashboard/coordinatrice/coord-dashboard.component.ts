import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../auth/auth.service';
import { IconComponent } from '../../../shared/icon.component';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-coord-dashboard',
  standalone: true,
  imports: [IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './coord-dashboard.component.html',
  styleUrls: ['../shared/dash-ui.scss', './coord-dashboard.component.scss'],
})
export class CoordDashboardComponent {
  user = inject(AuthService).user;
  private router = inject(Router);
  private api = inject(BackendApiService);
  
  scheduleList = signal<any[]>([]);
  kpis = signal<any[]>([
    { label: 'Visites cette semaine', value: '0',  delta: '0',  up: true,  icon: 'calendar' as const },
    { label: 'Total Employés',  value: '0',  delta: '0',  up: true, icon: 'users' as const },
    { label: 'Rappels envoyés',   value: '0',  delta: '0',  up: true,  icon: 'bell' as const },
    { label: 'Taux de conformité',  value: '95%', delta: 'Stable', up: true,  icon: 'shield' as const },
  ]);

  constructor() {
    this.load();
  }

  async load() {
    try {
      const now = new Date();
      const todayStart = new Date(now);
      todayStart.setHours(0,0,0,0);
      const todayEnd = new Date(todayStart);
      todayEnd.setHours(23,59,59,999);
      
      const weekStart = new Date(todayStart);
      weekStart.setDate(todayStart.getDate() - todayStart.getDay() + 1); // Monday
      const weekEnd = new Date(weekStart);
      weekEnd.setDate(weekStart.getDate() + 6); // Sunday

      // Parallel data fetching
      const [empRes, userRes, apptsRes, weekApptsRes, remindersRes] = await Promise.all([
        firstValueFrom(this.api.employees()),
        firstValueFrom(this.api.adminUsers()),
        firstValueFrom(this.api.appointments(todayStart.toISOString(), todayEnd.toISOString())),
        firstValueFrom(this.api.appointments(weekStart.toISOString(), weekEnd.toISOString())),
        firstValueFrom(this.api.reminders(todayStart.toISOString().slice(0,10), todayEnd.toISOString().slice(0,10)))
      ]);

      const empRows = Array.isArray(empRes?.content) ? empRes.content : (Array.isArray(empRes) ? empRes : []);
      const docRows = (Array.isArray(userRes) ? userRes : (Array.isArray(userRes?.content) ? userRes.content : [])).filter((u: any) => u.role === 'MEDECIN');
      const todayAppts = Array.isArray(apptsRes) ? apptsRes : (Array.isArray(apptsRes?.content) ? apptsRes.content : []);
      const weekAppts = Array.isArray(weekApptsRes) ? weekApptsRes : (Array.isArray(weekApptsRes?.content) ? weekApptsRes.content : []);
      const todayReminders = Array.isArray(remindersRes) ? remindersRes : (Array.isArray(remindersRes?.content) ? remindersRes.content : []);

      // 1. Update KPIs
      this.kpis.set([
        { label: 'Visites cette semaine', value: String(weekAppts.length), delta: 'Planifié', up: true, icon: 'calendar' as const },
        { label: 'Total Employés', value: String(empRows.length), delta: 'Enregistré', up: true, icon: 'users' as const },
        { label: 'Rappels envoyés', value: String(todayReminders.filter((r: any) => r.envoye).length), delta: 'Aujourd\'hui', up: true, icon: 'bell' as const },
        { label: 'Taux de conformité', value: '98%', delta: '+2%', up: true, icon: 'shield' as const },
      ]);

      // 2. Resolve Schedule Names
      this.scheduleList.set(todayAppts.map((a: any) => {
        const emp = empRows.find((e: any) => e.id == a.employeeId);
        const doc = docRows.find((d: any) => d.id == a.medecinId);
        return {
          time: new Date(a.dateDebut).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          employee: emp ? `${emp.prenom} ${emp.nom}` : `ID: ${a.employeeId}`,
          type: a.typeVisite ?? '-',
          doctor: doc ? `Dr. ${doc.prenom} ${doc.nom}` : (a.medecinId ? `Dr. #${a.medecinId}` : 'Non assigné'),
          status: a.statut === 'PLANIFIE' ? 'Pending' : (a.statut === 'EFFECTUE' ? 'Confirmed' : 'Cancelled')
        };
      }));

    } catch (err) {
      console.error('Dashboard load failed', err);
    }
  }

  reminders = [
    { who: 'Marc Lefèvre',   what: 'Rappel Tétanos dû',          when: 'dans 3 jours',  level: 'warn' },
    { who: 'Camille Beaulieu', what: 'Visite annuelle en retard',    when: 'retard 5j', level: 'danger' },
    { who: 'Hugo Martin',    what: 'Audiométrie recommandée',    when: 'dans 12 jours', level: 'ok' },
    { who: 'Elena Rossi',    what: 'Spirométrie planifiée',      when: 'demain',   level: 'ok' },
  ];

  openReminders() { this.router.navigateByUrl('/dashboard/coordinatrice/reminders'); }
  openSchedule() { this.router.navigateByUrl('/dashboard/coordinatrice/schedule'); }
}
