import { ChangeDetectionStrategy, Component, computed, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { FeedbackService } from '../../../core/ui/feedback.service';
import { IconComponent } from '../../../shared/icon.component';
import { AuthService } from '../../../auth/auth.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-dossier-medical',
  standalone: true,
  imports: [CommonModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dossier.html',
  styleUrls: ['../shared/dash-ui.scss', './dossier.scss'],
})
export class DossierMedicalComponent {
  private api = inject(BackendApiService);
  private feedback = inject(FeedbackService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  public auth = inject(AuthService);

  employees = signal<any[]>([]);
  searchQuery = signal('');
  loading = signal(false);
  activeEmployeeId = signal<string | null>(null);
  
  // Detail signals
  employee = signal<any>(null);
  consultations = signal<any[]>([]);
  vaccinations = signal<any[]>([]);
  documents = signal<any[]>([]);
  activeTab = signal<'info' | 'consults' | 'vaccines' | 'docs'>('info');

  filteredEmployees = computed(() => {
    const query = this.searchQuery().toLowerCase();
    return this.employees().filter(e => 
      e.nom.toLowerCase().includes(query) || 
      e.prenom.toLowerCase().includes(query) || 
      e.matricule?.toLowerCase().includes(query) ||
      e.dossierNumber?.toLowerCase().includes(query)
    );
  });

  constructor() {
    this.loadEmployees();
    
    // Watch for route param changes
    effect(() => {
      const id = this.route.snapshot.paramMap.get('id');
      this.activeEmployeeId.set(id);
      if (id) this.loadDossier(id);
    });
  }

  async loadEmployees() {
    try {
      this.loading.set(true);
      const res = await firstValueFrom(this.api.employees());
      this.employees.set(Array.isArray(res?.content) ? res.content : (Array.isArray(res) ? res : []));
    } catch (e) {
      this.feedback.error('Impossible de charger les employés.');
    } finally {
      this.loading.set(false);
    }
  }

  async loadDossier(id: string) {
    try {
      this.loading.set(true);
      const [emp, consults, vaccines, docs] = await Promise.all([
        firstValueFrom(this.api.employeeById(id)),
        firstValueFrom(this.api.consultations(id)),
        firstValueFrom(this.api.vaccinations(id)),
        firstValueFrom(this.api.documents(id))
      ]);
      
      this.employee.set(emp);
      this.consultations.set(Array.isArray(consults?.content) ? consults.content : (Array.isArray(consults) ? consults : []));
      this.vaccinations.set(Array.isArray(vaccines?.content) ? vaccines.content : (Array.isArray(vaccines) ? vaccines : []));
      this.documents.set(Array.isArray(docs?.content) ? docs.content : (Array.isArray(docs) ? docs : []));
    } catch (e) {
      this.feedback.error('Erreur lors du chargement du dossier.');
    } finally {
      this.loading.set(false);
    }
  }

  onSearch(e: Event) {
    this.searchQuery.set((e.target as HTMLInputElement).value);
  }

  viewDossier(id: string) {
    const role = this.auth.role();
    const base = role === 'admin' ? '/dashboard/admin/dossiers' : '/dashboard/doctor/dossiers';
    this.router.navigate([base, id]);
  }

  goBack() {
    const role = this.auth.role();
    const base = role === 'admin' ? '/dashboard/admin/dossiers' : '/dashboard/doctor/dossiers';
    this.router.navigate([base]);
  }

  setTab(tab: any) {
    this.activeTab.set(tab);
  }

  openNewConsultation() {
    const role = this.auth.role();
    const base = role === 'admin' ? '/dashboard/admin/consultations' : '/dashboard/doctor/consults';
    this.router.navigate([base], { queryParams: { employeeId: this.activeEmployeeId() } });
  }

  async downloadDoc(doc: any) {
    try {
      const blob = await firstValueFrom(this.api.downloadDocument(doc.id));
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = doc.fileName || 'document';
      a.click();
    } catch (e) {
      this.feedback.error('Erreur lors du téléchargement.');
    }
  }

  async onFileUpload(event: any) {
    const file = event.target.files[0];
    const id = this.activeEmployeeId();
    if (file && id) {
      try {
        await firstValueFrom(this.api.uploadDocument(file, id));
        this.feedback.success('Document ajouté.');
        const docs = await firstValueFrom(this.api.documents(id));
        this.documents.set(Array.isArray(docs?.content) ? docs.content : (Array.isArray(docs) ? docs : []));
      } catch (e) {
        this.feedback.error('Échec de l\'envoi.');
      }
    }
  }

  parseDetails(details: string) {
    try {
      return JSON.parse(details);
    } catch {
      return { diagnostic: details };
    }
  }
}
