import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { IconComponent } from '../../../shared/icon.component';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { FeedbackService } from '../../../core/ui/feedback.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-vaccines',
  standalone: true,
  imports: [IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './vaccines.component.html',
  styleUrls: ['../shared/dash-ui.scss', './vaccines.component.scss'],
})
export class VaccinesComponent {
  private api = inject(BackendApiService);
  private feedback = inject(FeedbackService);
  vaccineList = signal([
    { name: 'Tétanos (Tdap)',  coverage: 92, total: 245, due: 18 },
    { name: 'Hépatite B',       coverage: 88, total: 235, due: 24 },
    { name: 'DTP',              coverage: 95, total: 252, due: 11 },
    { name: 'Tuberculose (BCG)',coverage: 78, total: 207, due: 38 },
    { name: 'Vaccin de confinement', coverage: 100, total: 10, due: 0 },
    { name: 'Grippe saisonnière', coverage: 64, total: 170, due: 95 },
  ]);

  vaccineTypes = signal([
    'Tétanos (Tdap)',
    'Hépatite B',
    'DTP',
    'Tuberculose (BCG)',
    'Diphtérie',
    'Grippe saisonnière',
    'Vaccin de confinement',
    'COVID-19 Booster',
    'Fièvre jaune'
  ]);

  alerts = signal([
    { id: 'EMP-1188', name: 'Marc Lefèvre',     vaccine: 'Tdap booster',     due: 'Dans 3 jours',   level: 'warn',
      avatar: 'https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=120&q=80' },
    { id: 'EMP-0907', name: 'Camille Beaulieu', vaccine: 'Hep B (3ème dose)', due: 'En retard 5j',  level: 'danger',
      avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=120&q=80' },
    { id: 'EMP-0654', name: 'Hugo Martin',      vaccine: 'MMR catch-up',      due: 'Dans 12 jours', level: 'ok',
      avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=120&q=80' },
    { id: 'EMP-0233', name: 'Elena Rossi',      vaccine: 'Grippe saisonnière',due: 'Demain',        level: 'ok',
      avatar: 'https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&w=120&q=80' },
    { id: 'EMP-0322', name: 'Tomas Reyes',      vaccine: 'Tétanos rappel',    due: 'En retard 12j', level: 'danger',
      avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=120&q=80' },
  ]);
  showRegister = signal(false);
  employees = signal<any[]>([]);
  registerForm = signal({
    employeeId: '',
    vaccineType: 'Tétanos (Tdap)',
    dateEcheance: new Date(Date.now() + 30 * 24 * 3600 * 1000).toISOString().slice(0, 10),
  });

  constructor() {
    this.load();
  }

  async load() {
    try {
      // Charger les employés pour le dropdown
      const empRes = await firstValueFrom(this.api.employees());
      const empRows = Array.isArray(empRes?.content) ? empRes.content : Array.isArray(empRes) ? empRes : [];
      this.employees.set(empRows);

      const from = new Date();
      const to = new Date();
      to.setDate(to.getDate() + 30);
      const reminders = await firstValueFrom(this.api.reminders(from.toISOString().slice(0, 10), to.toISOString().slice(0, 10)));
      if (Array.isArray(reminders) && reminders.length) {
        this.alerts.set(reminders.map((r: any) => ({
          id: `EMP-${r.employeeId}`,
          name: r.employeeName || `Employee #${r.employeeId}`,
          vaccine: r.type === 'PERIODIQUE' ? 'Rappel périodique' : 'Rappel manuel',
          due: r.dateEcheance ? new Date(r.dateEcheance).toLocaleDateString('fr-FR') : '-',
          level: r.envoye ? 'ok' : 'warn',
          avatar: 'https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=120&q=80',
        })));
      } else {
        this.alerts.set([]);
      }
    } catch {
      this.feedback.error('Unable to load vaccine alerts.');
    }
  }

  exportPdf() {
    const content = JSON.stringify(this.vaccineList(), null, 2);
    const blob = new Blob([content], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'vaccines-report.json';
    a.click();
    URL.revokeObjectURL(url);
  }

  async registerVaccine() {
    const f = this.registerForm();
    if (!f.employeeId) {
      this.feedback.error('Employee ID is required.');
      return;
    }
    try {
      await firstValueFrom(this.api.createManualReminder({
        employeeId: Number(f.employeeId),
        type: 'VACCIN: ' + f.vaccineType,
        dateEcheance: f.dateEcheance,
        message: 'Rappel pour votre vaccination : ' + f.vaccineType,
        sendEmailNow: true
      }));
      await this.load();
      this.showRegister.set(false);
      this.feedback.success('Vaccine reminder registered.');
    } catch {
      this.feedback.error('Failed to register vaccine reminder.');
    }
  }

  openRegister() { this.showRegister.set(true); }
  closeRegister() { this.showRegister.set(false); }

  updateEmployeeId(employeeId: string) {
    this.registerForm.update(f => ({ ...f, employeeId }));
  }
  updateDateEcheance(dateEcheance: string) {
    this.registerForm.update(f => ({ ...f, dateEcheance }));
  }
  updateVaccineType(vaccineType: string) {
    this.registerForm.update(f => ({ ...f, vaccineType }));
  }
}
