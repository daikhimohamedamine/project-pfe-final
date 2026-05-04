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
  user = inject(AuthService).user;
  private router = inject(Router);
  private api = inject(BackendApiService);
  agendaList = signal<any[]>([]);

  kpis = [
    { label: "Consultations aujourd'hui", value: '...', delta: 'Chargement', up: true, icon: 'stethoscope' as const },
    { label: 'Employés suivis', value: '...', delta: 'Chargement', up: true, icon: 'users' as const },
    { label: 'Rapports à signer', value: '...', delta: 'Chargement', up: false, icon: 'document' as const },
    { label: 'Alertes vaccins', value: '...', delta: 'Chargement', up: false, icon: 'syringe' as const },
    { label: 'Employés', value: '...', delta: 'Chargement', up: true, icon: 'briefcase' as const },
  ];

  constructor() {
    this.load();
  }

  async load() {
    try {
      const today = new Date();
      const fromStr = today.toISOString().split('T')[0] + 'T00:00:00';
      const toStr = today.toISOString().split('T')[0] + 'T23:59:59';

      const [rows, emps] = await Promise.all([
        firstValueFrom(this.api.appointments(fromStr, toStr)),
        firstValueFrom(this.api.employees()),
      ]);

      const rowItems = Array.isArray(rows) ? rows : (rows?.content || []);
      const empItems = Array.isArray(emps) ? emps : (emps?.content || []);

      if (rowItems.length >= 0) {
        this.agendaList.set(rowItems.map((a: any) => {
          const emp = empItems.find((e: any) => e.id === a.employeeId);
          return {
            time: new Date(a.dateDebut).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            patient: emp ? `${emp.prenom} ${emp.nom}` : `Employé #${a.employeeId}`,
            type: a.typeVisite ?? '-',
            status: a.statut === 'EFFECTUE' ? 'completed' : 'pending',
            id: String(a.id),
            employeeId: a.employeeId,
            avatar: emp?.avatar ?? `https://api.dicebear.com/7.x/initials/svg?seed=${emp ? emp.prenom + ' ' + emp.nom : a.employeeId}&backgroundColor=3B82F6&textColor=ffffff`
          };
        }));

        this.kpis[0].value = String(rowItems.length);
        this.kpis[0].delta = rowItems.length > 0 ? "À voir aujourd'hui" : 'Aucun prévu';
      }

      if (empItems.length >= 0) {
        this.kpis[1].value = String(empItems.length);
        this.kpis[1].delta = 'Dossiers actifs';
        this.kpis[4].value = String(empItems.length);
        this.kpis[4].delta = 'Employés actifs';
      }

      this.kpis[2].value = '3';
      this.kpis[2].delta = 'En attente';

      this.kpis[3].value = '2';
      this.kpis[3].delta = 'Urgents';

    } catch (e) {
      console.error('Error loading dashboard data', e);
    }
  }

  alerts = [
    { patient: 'Ahmed Ben Salem', vaccine: 'Rappel Tétanos', due: 'Dans 3 jours', level: 'warn' },
    { patient: 'Leila Mansour', vaccine: 'Hépatite B (3ème dose)', due: 'En retard de 5j', level: 'danger' },
    { patient: 'Sami Jendoubi', vaccine: 'Grippe saisonnière', due: 'Dans 12 jours', level: 'ok' },
  ];

  vitals = [
    { label: 'Temps moyen consult.', pct: 78, value: '14 min' },
    { label: 'Rapports complétés', pct: 92, value: '92%' },
    { label: 'Satisfaction employés', pct: 96, value: '4.8/5' },
  ];

  openConsults() { this.router.navigateByUrl('/dashboard/doctor/consults'); }
  openPatients() { this.router.navigateByUrl('/dashboard/doctor/patients'); }
  openVaccines() { this.router.navigateByUrl('/dashboard/doctor/vaccines'); }
}
