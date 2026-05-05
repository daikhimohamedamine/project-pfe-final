import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../auth/auth.service';
import { IconComponent } from '../../../shared/icon.component';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-doctor-dashboard',
  standalone: true,
  imports: [IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './doctor-dashboard.component.html',
  styleUrls: ['../shared/dash-ui.scss', './doctor-dashboard.component.scss'],
})
export class DoctorDashboardComponent {
  Math = Math; // expose to template
  user = inject(AuthService).user;
  private router = inject(Router);
  private api = inject(BackendApiService);
  agendaList = signal<any[]>([]);
  alerts = signal<any[]>([]);

  kpis = signal([
    { label: "Consultations aujourd'hui", value: '0', delta: 'Chargement', up: true,  icon: 'stethoscope' as const },
    { label: 'Employés suivis',           value: '0', delta: 'Chargement', up: true,  icon: 'users'       as const },
    { label: 'Consultations ce mois',     value: '0', delta: 'Ce mois-ci', up: true,  icon: 'document'    as const },
    { label: 'Alertes vaccins',           value: '0', delta: 'Urgents',    up: false, icon: 'syringe'     as const },
  ]);

  constructor() {
    this.load();
  }

  async load() {
    try {
      const today = new Date();
      const todayStr  = today.toISOString().split('T')[0];
      const fromStr   = todayStr + 'T00:00:00';
      const toStr     = todayStr + 'T23:59:59';
      const monthFrom = new Date(today.getFullYear(), today.getMonth(), 1).toISOString();
      const monthTo   = today.toISOString();

      const [rows, emps, monthRows] = await Promise.all([
        firstValueFrom(this.api.appointments(fromStr, toStr)),
        firstValueFrom(this.api.employees()),
        firstValueFrom(this.api.appointments(monthFrom, monthTo)),
      ]);

      const rowItems   = Array.isArray(rows)       ? rows       : (rows?.content       || []);
      const empItems   = Array.isArray(emps)       ? emps       : (emps?.content       || []);
      const monthItems = Array.isArray(monthRows)  ? monthRows  : (monthRows?.content  || []);

      // --- Agenda today ---
      this.agendaList.set(rowItems.map((a: any) => {
        const emp = empItems.find((e: any) => e.id === a.employeeId);
        return {
          time:       new Date(a.dateDebut).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          patient:    emp ? `${emp.prenom} ${emp.nom}` : `Employé #${a.employeeId}`,
          type:       a.typeVisite ?? '-',
          status:     a.statut === 'EFFECTUE' ? 'completed' : 'pending',
          id:         String(a.id),
          employeeId: a.employeeId,
          avatar:     `https://api.dicebear.com/7.x/initials/svg?seed=${emp ? emp.prenom + '+' + emp.nom : a.employeeId}&backgroundColor=3B82F6&textColor=ffffff`
        };
      }));

      // --- Vaccine Alerts (from employee overdue next-visit data) ---
      const now = Date.now();
      const vaccAlerts = empItems
        .filter((e: any) => e.prochainRappel || e.nextVaccineDate)
        .map((e: any) => {
          const due = new Date(e.prochainRappel ?? e.nextVaccineDate);
          const diffDays = Math.ceil((due.getTime() - now) / 86400000);
          let dueLabel = `Dans ${diffDays}j`;
          let level    = 'ok';
          if (diffDays < 0)        { dueLabel = `Retard de ${Math.abs(diffDays)}j`; level = 'danger'; }
          else if (diffDays <= 7)  { dueLabel = `Dans ${diffDays} jours`;          level = 'warn';   }
          return { patient: `${e.prenom} ${e.nom}`, vaccine: e.vaccin ?? 'Rappel vaccinal', due: dueLabel, level, diff: diffDays };
        })
        .filter((a: any) => a.diff <= 30)
        .sort((a: any, b: any) => a.diff - b.diff)
        .slice(0, 4);
      this.alerts.set(vaccAlerts);

      // --- KPIs ---
      this.kpis.set([
        { label: "Consultations aujourd'hui", value: String(rowItems.length),   delta: rowItems.length > 0 ? "À voir" : 'Aucun prévu', up: rowItems.length > 0, icon: 'stethoscope' as const },
        { label: 'Employés suivis',           value: String(empItems.length),   delta: 'Dossiers actifs',   up: true,                   icon: 'users'       as const },
        { label: 'Consultations ce mois',     value: String(monthItems.length), delta: 'Ce mois-ci',        up: monthItems.length > 0,  icon: 'document'    as const },
        { label: 'Alertes vaccins',           value: String(vaccAlerts.filter((a:any) => a.level !== 'ok').length), delta: 'Urgents', up: false, icon: 'syringe' as const },
      ]);

    } catch (e) {
      console.error('Error loading dashboard data', e);
    }
  }

  openConsults() { this.router.navigateByUrl('/dashboard/doctor/consults'); }
  openPatients() { this.router.navigateByUrl('/dashboard/doctor/patients'); }
  openVaccines() { this.router.navigateByUrl('/dashboard/doctor/vaccines'); }
}
