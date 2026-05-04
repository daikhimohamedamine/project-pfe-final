import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../../../shared/icon.component';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { FeedbackService } from '../../../core/ui/feedback.service';
import { firstValueFrom } from 'rxjs';
import { DashboardStateService } from '../../../core/ui/dashboard-state.service';
import { AuthService } from '../../../auth/auth.service';

@Component({
  selector: 'app-coord-schedule',
  standalone: true,
  imports: [CommonModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './schedule.component.html',
  styleUrls: ['../shared/dash-ui.scss', './schedule.component.scss'],
})
export class CoordScheduleComponent {
  private api = inject(BackendApiService);
  private feedback = inject(FeedbackService);
  private state = inject(DashboardStateService);
  public auth = inject(AuthService);
  week = signal<any[]>([]);
  monthView = signal(false);
  showCreate = signal(false);
  employees = signal<any[]>([]);
  doctors = signal<any[]>([]);
  createForm = signal({
    employeeId: '',
    medecinId: String(this.auth.user()?.assignedMedecinId || ''),
    typeVisite: 'PERIODIQUE',
    dateDebut: new Date().toISOString().slice(0, 16),
    notes: '',
  });

  tomorrowAppointments = signal<any[]>([]);
  showBulkConfirm = signal(false);
  sendingBulk = signal(false);

  showEdit = signal(false);
  editId = signal<number | null>(null);
  editForm = signal({
    employeeId: '',
    medecinId: '',
    typeVisite: 'PERIODIQUE',
    dateDebut: '',
    dateFin: '',
    notes: '',
    statut: 'PLANIFIE'
  });

  constructor() {
    this.load();
  }

  async load() {
    try {
      // 1. Load Employees
      try {
        const empRes = await firstValueFrom(this.api.employees());
        const empRows = Array.isArray(empRes?.content) ? empRes.content : (Array.isArray(empRes) ? empRes : []);
        this.employees.set(empRows);
      } catch (err) {
        console.error('Failed to load employees', err);
      }

      // 2. Load Doctors
      try {
        const docRes = await firstValueFrom(this.api.medecins());
        this.doctors.set(docRes);
      } catch (err) {
        console.error('Failed to load doctors', err);
      }

      // 3. Load appointments
      const now = new Date();
      const from = new Date(now);
      from.setHours(0,0,0,0);
      // Reculer au lundi de cette semaine
      from.setDate(from.getDate() - (from.getDay() === 0 ? 6 : from.getDay() - 1));
      
      const to = new Date(from);
      to.setDate(to.getDate() + (this.monthView() ? 30 : 6)); // Afficher un mois ou une semaine

      const fromStr = from.toISOString();
      const toStr = to.toISOString();
      
      let rows: any[] = [];
      try {
        const res = await firstValueFrom(this.api.appointments(fromStr, toStr));
        rows = Array.isArray(res) ? res : (Array.isArray(res?.content) ? res.content : []);
      } catch (err: any) {
        // Problem 1: Handle 404 or empty response gracefully
        if (err.status !== 404) console.error('Appointment error', err);
        rows = [];
      }

      if (rows.length) {
        const grouped: any[] = [];
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        const tomorrowStr = tomorrow.toISOString().slice(0, 10);
        const tAppts: any[] = [];

        const empRows = this.employees();
        const docRows = this.doctors();

        for (const row of rows) {
          const date = new Date(row.dateDebut).toISOString().slice(0, 10);
          
          if (date === tomorrowStr) {
            const emp = empRows.find((e: any) => e.id == row.employeeId);
            tAppts.push({ ...row, employee: emp });
          }

          let day = grouped.find((g) => g.date === date);
          if (!day) {
            day = { day: new Date(date).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' }), date, visits: [] };
            grouped.push(day);
          }
          const emp = empRows.find((e: any) => e.id == row.employeeId);
          const doc = docRows.find((u: any) => u.id == row.medecinId);
          
          day.visits.push({
            id: row.id,
            time: new Date(row.dateDebut).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            name: emp ? `${emp.prenom} ${emp.nom}` : `Employé #${row.employeeId}`,
            type: row.typeVisite ?? 'Visite',
            doctor: doc ? `Dr. ${doc.prenom} ${doc.nom}` : (row.medecinId ? `Médecin #${row.medecinId}` : 'Non assigné'),
            notes: row.notes || '',
            status: row.statut === 'ANNULE' ? 'Done' : 'Now',
            raw: row
          });
        }
        // Sort by date
        grouped.sort((a, b) => a.date.localeCompare(b.date));
        this.week.set(grouped);
        this.tomorrowAppointments.set(tAppts);
        this.state.tomorrowAppointmentsCount.set(tAppts.length);
      } else {
        this.week.set([]);
        this.tomorrowAppointments.set([]);
        this.state.tomorrowAppointmentsCount.set(0);
      }
    } catch (err) {
      // General error (not just appointments)
      console.error('CoordScheduleComponent.load failed', err);
    }
  }

  toggleView() {
    this.monthView.update((v) => !v);
    this.load(); // Reload data with new date range
  }

  openCreateVisit() { 
    this.createForm.set({
      employeeId: '',
      medecinId: String(this.auth.user()?.assignedMedecinId || ''),
      typeVisite: 'PERIODIQUE',
      dateDebut: new Date().toISOString().slice(0, 16),
      notes: '',
    });
    this.showCreate.set(true); 
  }
  closeCreateVisit() { 
    this.showCreate.set(false);
    this.createForm.set({
      employeeId: '',
      medecinId: String(this.auth.user()?.assignedMedecinId || ''),
      typeVisite: 'PERIODIQUE',
      dateDebut: new Date().toISOString().slice(0, 16),
      notes: '',
    });
  }

  async planVisit() {
    const f = this.createForm();
    if (!f.employeeId || !f.dateDebut) {
      this.feedback.error('Le patient et la date sont obligatoires.');
      return;
    }
    try {
      await firstValueFrom(this.api.createAppointment({
        employeeId: Number(f.employeeId),
        medecinId: f.medecinId ? Number(f.medecinId) : null,
        dateDebut: f.dateDebut,
        typeVisite: f.typeVisite,
        notes: f.notes,
        statut: 'PLANIFIE',
      }));
      this.feedback.success('Rendez-vous créé avec succès');
      await this.load();
      this.closeCreateVisit();
    } catch {
      this.feedback.error('Erreur lors de la création du rendez-vous.');
    }
  }

  updateCreateForm(key: string, value: any) {
    this.createForm.update(f => ({ ...f, [key]: value }));
  }

  openEdit(v: any) {
    const raw = v.raw;
    this.editId.set(v.id);
    this.editForm.set({
      employeeId: String(raw.employeeId),
      medecinId: raw.medecinId ? String(raw.medecinId) : '',
      typeVisite: raw.typeVisite || 'PERIODIQUE',
      dateDebut: new Date(raw.dateDebut).toISOString().slice(0, 16),
      dateFin: new Date(raw.dateFin).toISOString().slice(0, 16),
      notes: raw.notes || '',
      statut: raw.statut || 'PLANIFIE'
    });
    this.showEdit.set(true);
  }

  updateEditForm(key: string, value: any) {
    this.editForm.update(f => ({ ...f, [key]: value }));
  }

  async updateVisit() {
    const id = this.editId();
    if (!id) return;
    const f = this.editForm();
    try {
      await firstValueFrom(this.api.updateAppointment(id, {
        employeeId: Number(f.employeeId),
        medecinId: f.medecinId ? Number(f.medecinId) : null,
        dateDebut: f.dateDebut,
        typeVisite: f.typeVisite,
        notes: f.notes,
        statut: f.statut
      }));
      this.feedback.success('Rendez-vous mis à jour');
      await this.load();
      this.showEdit.set(false);
    } catch {
      this.feedback.error('Erreur lors de la mise à jour');
    }
  }

  async deleteVisit() {
    const id = this.editId();
    if (!id) return;
    try {
      await firstValueFrom(this.api.cancelAppointment(id));
      this.feedback.success('Rendez-vous supprimé avec succès');
      await this.load();
      this.showEdit.set(false);
    } catch {
      this.feedback.error('Erreur lors de la suppression du rendez-vous');
    }
  }

  async sendBulkReminders() {
    const appts = this.tomorrowAppointments();
    const toSend = appts.filter(a => a.employee?.email);
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowDate = tomorrow.toISOString().slice(0, 10);

    this.sendingBulk.set(true);
    let sentCount = 0;
    let failCount = 0;

    for (const a of toSend) {
      try {
        const time = new Date(a.dateDebut).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        const date = new Date(a.dateDebut).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
        
        await firstValueFrom(this.api.createReminder({
          employeeId: a.employeeId,
          employeeEmail: a.employee.email,
          type: 'Rendez-vous médical',
          message: `Bonjour ${a.employee.prenom}, nous vous rappelons que vous avez un rendez-vous médical demain ${date} à ${time} au Service Médical COFICAB. Merci de vous présenter à l'heure. Cordialement, Service Médical COFICAB.`,
          dueDate: tomorrowDate,
          sendEmailNow: true
        }));
        sentCount++;
      } catch (err) {
        console.error('Bulk send fail', err);
        failCount++;
      }
    }

    this.sendingBulk.set(false);
    this.showBulkConfirm.set(false);
    
    if (sentCount > 0 || failCount > 0) {
      const ignored = appts.length - toSend.length;
      this.feedback.success(`✅ ${sentCount} email(s) envoyé(s), ${ignored} ignoré(s) (pas d'email)`);
    } else {
      this.feedback.info('Aucun rappel à envoyer (manque d\'email).');
    }
  }
}
