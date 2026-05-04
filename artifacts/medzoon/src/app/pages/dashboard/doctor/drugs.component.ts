import { ChangeDetectionStrategy, Component, computed, inject, signal, Pipe, PipeTransform } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { IconComponent } from '../../../shared/icon.component';
import { Drug } from '../../../core/models/models';
import { PrescriptionService } from './prescription.service';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { FeedbackService } from '../../../core/ui/feedback.service';
import { firstValueFrom } from 'rxjs';

@Pipe({ name: 'safeUrl', standalone: true })
export class SafeUrlPipe implements PipeTransform {
  private sanitizer = inject(DomSanitizer);
  transform(url: string): SafeResourceUrl {
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
}

@Component({
  selector: 'app-drugs',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent, SafeUrlPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './drugs.component.html',
  styleUrls: ['../shared/dash-ui.scss', './drugs.component.scss'],
})
export class DrugsComponent {
  rx = inject(PrescriptionService);
  private api = inject(BackendApiService);
  private feedback = inject(FeedbackService);
  private sanitizer = inject(DomSanitizer);

  activeTab = signal<'DRUGS' | 'RESOURCES'>('RESOURCES');
  query = signal('');
  category = signal<'All' | Drug['category']>('All');
  
  // Doctor Profile
  doctorName = signal('Amine Daikhi');
  doctorPhoto = signal('https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?auto=format&fit=crop&w=150&q=80');

  // Resources State
  resources = signal<any[]>([]);
  resourceCategory = signal<'All' | 'CERTIFICAT' | 'EXPERIENCE' | 'LIVRE' | 'JOURNAL' | 'DATASET'>('All');
  showUploadResource = signal(false);
  viewingResource = signal<any | null>(null);
  uploadFile = signal<File | null>(null);
  uploadData = signal({
    categorie: 'LIVRE' as any,
    description: ''
  });

  // Drugs State
  source = signal<Drug[]>([]);

  constructor() {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        if (user.prenom) this.doctorName.set(user.prenom + ' ' + user.nom);
        if (user.avatar) {
          const baseUrl = 'http://localhost:8081';
          const fullUrl = user.avatar.startsWith('http') ? user.avatar : baseUrl + user.avatar;
          this.doctorPhoto.set(fullUrl);
        }
      } catch (e) {}
    }
    this.load();
  }

  async load() {
    await this.loadDrugs();
    await this.loadResources();
  }

  async loadDrugs() {
    try {
      const res = await firstValueFrom(this.api.drugs());
      const rows = Array.isArray(res?.content) ? res.content : Array.isArray(res) ? res : [];
      this.source.set(rows.map((d: any) => ({
        set_id: d.setId ?? String(d.id),
        drug_name: d.drugName ?? '',
        generic_name: d.genericName ?? '',
        category: 'Respiratory',
        dosage: d.dosage ?? '',
        indications: d.indications ?? '',
        sicknesses: Array.isArray(d.sicknesses) ? d.sicknesses : [],
      } as Drug)));
    } catch {}
  }

  async loadResources() {
    try {
      const cat = this.resourceCategory() === 'All' ? undefined : this.resourceCategory();
      const res = await firstValueFrom(this.api.doctorLibraryDocs(cat));
      this.resources.set(res);
    } catch {
      this.feedback.error('Impossible de charger vos ressources.');
    }
  }

  setTab(tab: 'DRUGS' | 'RESOURCES') {
    this.activeTab.set(tab);
    this.load();
  }

  filteredDrugs = computed(() => {
    const q = this.query().toLowerCase().trim();
    const c = this.category();
    return this.source().filter((d) => {
      if (c !== 'All' && d.category !== c) return false;
      if (!q) return true;
      return d.drug_name.toLowerCase().includes(q) || d.generic_name.toLowerCase().includes(q);
    });
  });

  filteredResources = computed(() => {
    const q = this.query().toLowerCase().trim();
    return this.resources().filter(r => !q || (r.titre && r.titre.toLowerCase().includes(q)) || (r.fileName && r.fileName.toLowerCase().includes(q)) || r.description?.toLowerCase().includes(q));
  });

  onFileSelected(ev: Event) {
    const input = ev.target as HTMLInputElement;
    this.uploadFile.set(input.files?.[0] ?? null);
  }

  async uploadResource() {
    const file = this.uploadFile();
    if (!file) return;
    try {
      await firstValueFrom(this.api.uploadDoctorLibraryDoc(file, this.uploadData().categorie, this.uploadData().description));
      this.feedback.success('Ressource ajoutée avec succès.');
      this.showUploadResource.set(false);
      this.uploadFile.set(null);
      this.loadResources();
    } catch {
      this.feedback.error('Échec de l\'ajout de la ressource.');
    }
  }

  getResourceUrl(r: any): string {
    if (!r) return '';
    const baseUrl = 'http://localhost:8081';
    return `${baseUrl}/api/v1/doctor-library/download/${r.id}`;
  }

  viewFullResource(r: any) {
    this.viewingResource.set(r);
  }

  async downloadResource(r: any) {
    try {
      const blob = await firstValueFrom(this.api.downloadDoctorLibraryDoc(r.id));
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = r.titre || r.fileName;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      this.feedback.error('Échec du téléchargement.');
    }
  }

  async deleteResource(r: any) {
    if (!confirm('Voulez-vous supprimer ce document ?')) return;
    try {
      await firstValueFrom(this.api.deleteDoctorLibraryDoc(r.id));
      this.feedback.success('Document supprimé.');
      this.loadResources();
    } catch {
      this.feedback.error('Échec de la suppression.');
    }
  }

  async onPhotoSelected(ev: Event) {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    try {
      const res = await firstValueFrom(this.api.uploadAvatar(file));
      const baseUrl = 'http://localhost:8081';
      const fullUrl = res.url.startsWith('http') ? res.url : baseUrl + res.url;
      this.doctorPhoto.set(fullUrl);
      
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      user.avatar = fullUrl;
      localStorage.setItem('user', JSON.stringify(user));
      
      this.feedback.success('Photo de profil mise à jour.');
    } catch {
      this.feedback.error('Échec de la mise à jour de la photo.');
    }
  }

  categories: ('All' | Drug['category'])[] = ['All','Antibiotic','Analgesic','Respiratory','Cardiovascular','Gastric','Vitamin'];
  resourceCategories = ['All', 'CERTIFICAT', 'EXPERIENCE', 'LIVRE', 'JOURNAL', 'DATASET'];

  addToRx(d: Drug) { this.rx.add(d); }
  removeFromRx(setId: string) { this.rx.remove(setId); }
  setPosology(setId: string, value: string) { this.rx.setPosology(setId, value); }
}
