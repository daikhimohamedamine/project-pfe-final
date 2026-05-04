import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../../../shared/icon.component';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { FeedbackService } from '../../../core/ui/feedback.service';
import { firstValueFrom } from 'rxjs';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-coord-reminders',
  standalone: true,
  imports: [CommonModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './reminders.component.html',
  styleUrls: ['../shared/dash-ui.scss', './reminders.component.scss'],
})
export class CoordRemindersComponent {
  private api = inject(BackendApiService);
  private feedback = inject(FeedbackService);
  private http = inject(HttpClient);
  upcoming = signal<any[]>([]);
  history = signal<any[]>([]);
  employees = signal<any[]>([]);
  empSearch = signal('');
  
  showCreate = signal(false);
  createForm = signal({
    employeeId: null as number | null,
    employeeEmail: '',
    type: 'Visite périodique annuelle',
    message: '',
    dueDate: new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString().slice(0, 10),
    time: '10:00',
    sendEmailNow: true
  });

  selectedEmployee = computed(() => {
    const id = this.createForm().employeeId;
    return this.employees().find(e => e.id === id) || null;
  });

  filteredEmployees = computed(() => {
    const q = this.empSearch().toLowerCase().trim();
    if (!q) return [];
    return this.employees().filter(e => 
      `${e.prenom} ${e.nom}`.toLowerCase().includes(q) || 
      (e.dossierNumber || '').toLowerCase().includes(q)
    ).slice(0, 8);
  });

  constructor() {
    this.load();
  }

  async load() {
    try {
      const now = new Date();
      const from = new Date(now.getFullYear(), now.getMonth() - 1, 1).toISOString().slice(0, 10);
      const to = new Date(now.getFullYear(), now.getMonth() + 3, 0).toISOString().slice(0, 10);

      const [rems, emps, notes] = await Promise.all([
        firstValueFrom(this.api.reminders(from, to)),
        firstValueFrom(this.api.employees()),
        firstValueFrom(this.api.notifications())
      ]);

      const empContent = Array.isArray(emps) ? emps : (emps?.content || []);
      this.employees.set(empContent);
      
      const rows = Array.isArray(rems) ? rems : (Array.isArray(rems?.content) ? rems.content : []);
      this.upcoming.set(rows.map((r: any) => {
        const emp = empContent.find((e: any) => e.id === r.employeeId);
        return {
          id: r.id,
          employeeName: emp ? `${emp.prenom} ${emp.nom}` : `Employé #${r.employeeId}`,
          employeeEmail: r.employeeEmail || emp?.email || null,
          type: r.type,
          dueDate: r.dueDate || r.dateEcheance,
          sent: r.envoye || r.sent,
          status: this.getReminderStatus(r)
        };
      }));

      if (Array.isArray(notes)) {
        this.history.set(notes.map((n: any) => ({
          date: n.dateEnvoi,
          who: n.destinataireEmail,
          subject: n.sujet,
          channel: 'Email',
          status: n.statut === 'SUCCESS' ? 'Envoyé' : 'Échec',
        })));
      }
    } catch (err) {
      console.error(err);
      this.feedback.error('Impossible de charger les rappels.');
    }
  }

  private getReminderStatus(r: any) {
    if (r.envoye || r.sent) return 'Envoyé';
    const due = new Date(r.dueDate || r.dateEcheance);
    const now = new Date();
    now.setHours(0,0,0,0);
    return due < now ? 'En retard' : 'En attente';
  }

  async sendReminder(id: number, email: string) {
    if (!email) {
      this.feedback.error('Aucune adresse email pour cet employé.');
      return;
    }
    
    try {
      // Appel au backend pour enregistrer l'envoi et générer une notification
      await firstValueFrom(this.api.sendReminder(id));
      this.feedback.success(`Rappel envoyé avec succès à ${email}`);
      this.load();
    } catch (error: any) {
      console.error('Erreur backend:', error);
      this.feedback.error('Échec de l\'envoi du rappel au serveur.');
    }
  }

  async deleteReminder(id: number) {
    if (!confirm('Êtes-vous sûr de vouloir supprimer ce rappel ?')) return;
    try {
      await firstValueFrom(this.api.deleteReminder(id));
      this.feedback.success('Rappel supprimé.');
      this.load();
    } catch {
      this.feedback.error('Échec de la suppression.');
    }
  }

  async createManual() {
    const f = this.createForm();
    
    if (!f.employeeId) {
      this.feedback.error('Veuillez sélectionner un employé.');
      return;
    }

    try {
      const timeVal = f.time ? ` à ${f.time}` : '';
      const dateStr = f.dueDate ? new Date(f.dueDate).toLocaleDateString('fr-FR') : 'prochainement';
      const defaultMessage = `Bonjour, ceci est un rappel pour votre ${f.type.toLowerCase()} prévu le ${dateStr}${timeVal}.\n\nMerci de vous présenter à l'heure.\n\n${f.message ? 'Note du médecin : ' + f.message : ''}`;

      const payload = {
        employeeId: f.employeeId,
        employeeEmail: f.employeeEmail || null,
        type: f.type,
        message: defaultMessage,
        dueDate: f.dueDate,
        sendEmailNow: f.sendEmailNow && !!f.employeeEmail
      };

      await firstValueFrom(this.api.createReminder(payload));
      
      if (payload.sendEmailNow) {
        this.feedback.success(`✅ Rappel créé et email envoyé à ${f.employeeEmail}`);
      } else {
        this.feedback.success('✅ Rappel créé (sans envoi email)');
      }
      
      this.closeCreateManual();
      this.load();
    } catch {
      this.feedback.error('Échec de la création du rappel.');
    }
  }

  selectEmployee(emp: any) {
    this.updateCreateForm('employeeId', emp.id);
    this.updateCreateForm('employeeEmail', emp.email || '');
    this.empSearch.set(`${emp.prenom} ${emp.nom}`);
  }

  openCreateManual() { 
    this.resetCreateForm();
    this.showCreate.set(true); 
  }
  closeCreateManual() { this.showCreate.set(false); }

  resetCreateForm() {
    this.empSearch.set('');
    this.createForm.set({
      employeeId: null,
      employeeEmail: '',
      type: 'Visite périodique annuelle',
      message: '',
      dueDate: new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString().slice(0, 10),
      time: '10:00',
      sendEmailNow: true
    });
  }

  updateCreateForm(key: string, value: any) {
    this.createForm.update(f => ({ ...f, [key]: value }));
  }
}
