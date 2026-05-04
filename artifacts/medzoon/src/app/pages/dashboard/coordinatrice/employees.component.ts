import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../../../shared/icon.component';
import { EmployeeListItem, EmployeeStatus } from '../../../core/models/models';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { FeedbackService } from '../../../core/ui/feedback.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-coord-employees',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './employees.component.html',
  styleUrls: ['../shared/dash-ui.scss', './employees.component.scss'],
})
export class CoordEmployeesComponent {
  private api = inject(BackendApiService);
  private feedback = inject(FeedbackService);
  
  query = signal('');
  status = signal<'All' | 'Active' | 'Archived'>('All');
  source = signal<EmployeeListItem[]>([]);
  loading = signal(false);

  // Modal states
  showCreate = signal(false);
  showView = signal(false);
  showEdit = signal(false);
  showDelete = signal(false);
  
  selectedEmployee = signal<any>(null);

  form = signal({
    nom: '',
    prenom: '',
    dateNaissance: '',
    lieuNaissance: '',
    situationFamiliale: 'Célibataire',
    nombreEnfants: 0,
    adresse: '',
    codePostal: '',
    departement: 'Production',
    posteTravail: '',
    dateEmbauche: '',
    dossierNumber: '',
    matriculeCaisse: '',
    telephone: '',
    email: '',
    gouvernorat: 'Tunis',
    antecedentsChirurgicaux: '',
    antecedentsMedicaux: '',
    antecedentsGynecologiques: '',
    antecedentsHereditaires: '',
    tabac: false,
    alcool: false,
    automedication: false
  });

  constructor() {
    this.load();
  }

  async load() {
    try {
      this.loading.set(true);
      const res = await firstValueFrom(this.api.employees());
      const rows = Array.isArray(res?.content) ? res.content : Array.isArray(res) ? res : [];
      this.source.set(rows.map((e: any) => ({
        id: e.dossierNumber ?? String(e.id),
        numericId: e.id,
        firstName: e.prenom ?? '',
        lastName: e.nom ?? '',
        position: e.posteTravail ?? '',
        department: e.departement ?? '',
        birthDate: e.dateNaissance ?? '',
        phone: e.telephone ?? '',
        email: e.email ?? '',
        hireDate: e.dateEmbauche ?? '-',
        lastVisit: '-',
        avatar: 'https://ui-avatars.com/api/?name=' + (e.prenom + '+' + e.nom) + '&background=random',
        status: (e.statut === 'ARCHIVE' ? 'Archived' : 'Active') as EmployeeStatus,
      })));
    } catch {
      this.feedback.error('Impossible de charger les employés.');
    } finally {
      this.loading.set(false);
    }
  }

  list = computed(() => {
    const q = this.query().toLowerCase().trim();
    const s = this.status();
    return this.source().filter((e) => {
      if (s !== 'All' && e.status !== s) return false;
      if (!q) return true;
      return e.firstName.toLowerCase().includes(q) || 
             e.lastName.toLowerCase().includes(q) ||
             e.id.toLowerCase().includes(q) || 
             (e.position ?? '').toLowerCase().includes(q);
    });
  });

  // Modal actions
  openCreate() {
    this.resetForm();
    this.showCreate.set(true);
  }

  async openView(e: any) {
    try {
      const data = await firstValueFrom(this.api.employeeById(e.numericId));
      this.selectedEmployee.set(data);
      this.showView.set(true);
    } catch {
      this.feedback.error('Erreur lors du chargement des détails.');
    }
  }

  async openEdit(e: any) {
    try {
      const data = await firstValueFrom(this.api.employeeById(e.numericId));
      this.selectedEmployee.set(data);
      this.form.set({
        nom: data.nom || '',
        prenom: data.prenom || '',
        dateNaissance: data.dateNaissance || '',
        lieuNaissance: data.lieuNaissance || '',
        situationFamiliale: data.situationFamiliale || 'Célibataire',
        nombreEnfants: data.nombreEnfants || 0,
        adresse: data.adresse || '',
        codePostal: data.codePostal || '',
        departement: data.departement || 'Production',
        posteTravail: data.posteTravail || '',
        dateEmbauche: data.dateEmbauche || '',
        dossierNumber: data.dossierNumber || '',
        matriculeCaisse: data.matriculeCaisse || '',
        telephone: data.telephone || '',
        email: data.email || '',
        gouvernorat: data.gouvernorat || 'Tunis',
        antecedentsChirurgicaux: data.antecedentsChirurgicaux || '',
        antecedentsMedicaux: data.antecedentsMedicaux || '',
        antecedentsGynecologiques: data.antecedentsGynecologiques || '',
        antecedentsHereditaires: data.antecedentsHereditaires || '',
        tabac: data.tabac || false,
        alcool: data.alcool || false,
        automedication: data.automedication || false
      });
      this.showEdit.set(true);
    } catch {
      this.feedback.error('Erreur lors du chargement de l\'employé.');
    }
  }

  openDelete(e: any) {
    this.selectedEmployee.set(e);
    this.showDelete.set(true);
  }

  closeModals() {
    this.showCreate.set(false);
    this.showView.set(false);
    this.showEdit.set(false);
    this.showDelete.set(false);
  }

  // API calls
  async createEmployee() {
    if (!this.validateForm()) return;
    try {
      await firstValueFrom(this.api.createEmployee(this.normalizeFormPayload()));
      this.feedback.success('Employé créé avec succès');
      this.closeModals();
      this.load();
    } catch {
      this.feedback.error('Échec de la création.');
    }
  }

  async saveEmployee() {
    if (!this.validateForm()) return;
    const id = this.selectedEmployee().id;
    try {
      await firstValueFrom(this.api.updateEmployee(id, this.normalizeFormPayload()));
      this.feedback.success('Employé mis à jour');
      this.closeModals();
      this.load();
    } catch {
      this.feedback.error('Échec de la mise à jour.');
    }
  }

  async deleteEmployee() {
    const id = this.selectedEmployee().numericId;
    try {
      await firstValueFrom(this.api.deleteEmployee(id));
      this.feedback.success('Employé supprimé');
      this.closeModals();
      this.load();
    } catch {
      this.feedback.error('Échec de la suppression.');
    }
  }

  private validateForm() {
    const f = this.form();
    if (!f.nom || !f.prenom || !f.dateNaissance || !f.posteTravail || !f.dateEmbauche || !f.dossierNumber) {
      this.feedback.error('Veuillez remplir tous les champs obligatoires (*)');
      return false;
    }
    return true;
  }

  private normalizeFormPayload() {
    const f = this.form();
    return {
      ...f,
      nombreEnfants: Number(f.nombreEnfants ?? 0),
      dossierNumber: String(f.dossierNumber ?? '').trim(),
      matriculeCaisse: String(f.matriculeCaisse ?? '').trim(),
      telephone: String(f.telephone ?? '').trim(),
      email: String(f.email ?? '').trim(),
      codePostal: String(f.codePostal ?? '').trim(),
    };
  }

  private resetForm() {
    this.form.set({
      nom: '', prenom: '', dateNaissance: '', lieuNaissance: '',
      situationFamiliale: 'Célibataire', nombreEnfants: 0,
      adresse: '', codePostal: '', departement: 'Production',
      posteTravail: '', dateEmbauche: '',
      dossierNumber: '', matriculeCaisse: '',
      telephone: '', email: '', gouvernorat: 'Tunis',
      antecedentsChirurgicaux: '', antecedentsMedicaux: '',
      antecedentsGynecologiques: '', antecedentsHereditaires: '',
      tabac: false, alcool: false, automedication: false
    });
  }

  updateField(key: string, value: any) {
    this.form.update(f => ({ ...f, [key]: value }));
  }

  exportCsv() {
    const rows = this.list();
    const csv = ['Matricule,Prénom,Nom,Poste,Département,Statut', ...rows.map((e: any) =>
      [e.id, e.firstName, e.lastName, e.position, e.department ?? '', e.status].join(','),
    )].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'employes.csv';
    a.click();
    URL.revokeObjectURL(url);
  }
}
