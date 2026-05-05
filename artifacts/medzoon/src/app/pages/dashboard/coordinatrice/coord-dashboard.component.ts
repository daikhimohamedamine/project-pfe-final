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
  reminders = signal<any[]>([]);
  deptStats = signal<{ label: string; pct: number }[]>([]);

  kpis = signal<any[]>([
    { label: 'Visites cette semaine', value: '0', delta: 'Planifié',    up: true,  icon: 'calendar' as const },
    { label: 'Total Employés',        value: '0', delta: 'Enregistré',  up: true,  icon: 'users'    as const },
    { label: 'Rappels envoyés',       value: '0', delta: "Aujourd'hui", up: true,  icon: 'bell'     as const },
    { label: 'Taux de conformité',    value: '0%', delta: 'Stable',     up: true,  icon: 'shield'   as const },
  ]);

  constructor() {
    this.load();
  }

  async load() {
    try {
      const now = new Date();
      const todayStart = new Date(now); todayStart.setHours(0,0,0,0);
      const todayEnd   = new Date(now); todayEnd.setHours(23,59,59,999);
      const weekStart  = new Date(todayStart);
      weekStart.setDate(todayStart.getDate() - ((todayStart.getDay() + 6) % 7)); // Monday
      const weekEnd = new Date(weekStart); weekEnd.setDate(weekStart.getDate() + 6);

      const [empRes, userRes, apptsRes, weekApptsRes, remindersRes] = await Promise.all([
        firstValueFrom(this.api.employees()),
        firstValueFrom(this.api.adminUsers()),
        firstValueFrom(this.api.appointments(todayStart.toISOString(), todayEnd.toISOString())),
        firstValueFrom(this.api.appointments(weekStart.toISOString(), weekEnd.toISOString())),
        firstValueFrom(this.api.reminders(todayStart.toISOString().slice(0,10), weekEnd.toISOString().slice(0,10)))
      ]);

      const empRows   = Array.isArray(empRes?.content) ? empRes.content : (Array.isArray(empRes) ? empRes : []);
      const docRows   = (Array.isArray(userRes) ? userRes : []).filter((u: any) => u.role === 'MEDECIN');
      const todayAppts  = Array.isArray(apptsRes)      ? apptsRes      : (Array.isArray(apptsRes?.content)      ? apptsRes.content      : []);
      const weekAppts   = Array.isArray(weekApptsRes)  ? weekApptsRes  : (Array.isArray(weekApptsRes?.content)  ? weekApptsRes.content  : []);
      const remindList  = Array.isArray(remindersRes)  ? remindersRes  : (Array.isArray(remindersRes?.content)  ? remindersRes.content  : []);

      // --- KPIs ---
      const sentToday = remindList.filter((r: any) => r.envoye || r.sent).length;
      // Conformity: employees who had a visit in the last year
      const oneYearAgo = new Date(); oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
      const seenIds = new Set(weekAppts.concat(todayAppts).map((a: any) => a.employeeId));
      const conformPct = empRows.length > 0 ? Math.round((seenIds.size / empRows.length) * 100) : 0;

      this.kpis.set([
        { label: 'Visites cette semaine', value: String(weekAppts.length),  delta: 'Planifié',    up: true,  icon: 'calendar' as const },
        { label: 'Total Employés',        value: String(empRows.length),    delta: 'Enregistré',  up: true,  icon: 'users'    as const },
        { label: 'Rappels envoyés',       value: String(sentToday),         delta: "Aujourd'hui", up: sentToday > 0, icon: 'bell' as const },
        { label: 'Taux de conformité',    value: `${conformPct}%`,          delta: 'Stable',      up: conformPct > 80, icon: 'shield' as const },
      ]);

      // --- Today's Schedule ---
      this.scheduleList.set(todayAppts.map((a: any) => {
        const emp = empRows.find((e: any) => e.id == a.employeeId);
        const doc = docRows.find((d: any) => d.id == a.medecinId);
        return {
          time:     new Date(a.dateDebut).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          employee: emp ? `${emp.prenom} ${emp.nom}` : `ID: ${a.employeeId}`,
          type:     a.typeVisite ?? '-',
          doctor:   doc ? `Dr. ${doc.prenom} ${doc.nom}` : (a.medecinId ? `Dr. #${a.medecinId}` : 'Non assigné'),
          status:   a.statut === 'PLANIFIE' ? 'Pending' : (a.statut === 'EFFECTUE' ? 'Confirmed' : 'Annulé')
        };
      }));

      // --- Real Reminders ---
      const realReminders = remindList.slice(0, 5).map((r: any) => {
        const emp = empRows.find((e: any) => e.id == r.employeeId);
        const dueDate = r.dueDate ? new Date(r.dueDate) : null;
        const diffDays = dueDate ? Math.ceil((dueDate.getTime() - Date.now()) / 86400000) : null;
        let when = 'Date inconnue';
        let level = 'ok';
        if (diffDays !== null) {
          if (diffDays < 0)        { when = `retard ${Math.abs(diffDays)}j`; level = 'danger'; }
          else if (diffDays === 0) { when = "aujourd'hui";                   level = 'warn';   }
          else if (diffDays === 1) { when = 'demain';                        level = 'warn';   }
          else                     { when = `dans ${diffDays} jours`;        level = diffDays < 7 ? 'warn' : 'ok'; }
        }
        return {
          who:   emp ? `${emp.prenom} ${emp.nom}` : (r.employeeEmail ?? `Employé #${r.employeeId}`),
          what:  r.message ?? r.type ?? 'Rappel médical',
          when,
          level
        };
      });
      this.reminders.set(realReminders.length > 0 ? realReminders : []);

      // --- Department Stats (computed from employees) ---
      const deptMap: Record<string, { total: number; seen: number }> = {};
      empRows.forEach((e: any) => {
        const dept = e.departement || e.department || 'Autre';
        if (!deptMap[dept]) deptMap[dept] = { total: 0, seen: 0 };
        deptMap[dept].total++;
        if (seenIds.has(e.id)) deptMap[dept].seen++;
      });
      const deptArr = Object.entries(deptMap)
        .map(([label, d]) => ({ label, pct: Math.round((d.seen / d.total) * 100) }))
        .sort((a, b) => b.pct - a.pct)
        .slice(0, 4);
      this.deptStats.set(deptArr.length > 0 ? deptArr : [
        { label: 'Aucune donnée disponible', pct: 0 }
      ]);

    } catch (err) {
      console.error('Dashboard load failed', err);
    }
  }

  openReminders() { this.router.navigateByUrl('/dashboard/coordinatrice/reminders'); }
  openSchedule()  { this.router.navigateByUrl('/dashboard/coordinatrice/schedule'); }
}
