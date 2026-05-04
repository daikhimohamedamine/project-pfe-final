import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../../../shared/icon.component';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { FeedbackService } from '../../../core/ui/feedback.service';
import { AuthService } from '../../../auth/auth.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-med-exams',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './med-exams.component.html',
  styleUrls: ['../shared/dash-ui.scss', './consults.component.scss'],
})
export class MedExamsComponent {
  private api = inject(BackendApiService);
  private feedback = inject(FeedbackService);
  public auth = inject(AuthService);
  
  query = signal('');
  source = signal<any[]>([]);
  showCreate = signal(false);
  creating = signal(false);
  showDocs = signal(false);
  selectedExam = signal<any | null>(null);
  documents = signal<any[]>([]);
  docFile = signal<File | null>(null);
  employees = signal<any[]>([]);
  
  examCategories = ['Biologie (Sang/Urine)', 'Audiométrie', 'Spirométrie', 'Électrocardiogramme (ECG)', 'Test visuel', 'Radiologie', 'Autre'];

  form = signal({
    employeeId: '',
    category: 'Biologie (Sang/Urine)',
    date: new Date().toISOString().slice(0, 10),
    results: '',
    observations: '',
  });

  constructor() {
    this.load();
  }

  async load() {
    try {
      const empRes = await firstValueFrom(this.api.employees());
      const empRows = Array.isArray(empRes?.content) ? empRes.content : Array.isArray(empRes) ? empRes : [];
      this.employees.set(empRows);

      // Fetch all consultations and filter by type 'EXAMEN_MEDICAL'
      const res = await firstValueFrom(this.api.consultations());
      const rows = Array.isArray(res?.content) ? res.content : Array.isArray(res) ? res : [];
      
      const examRows = rows.filter((c: any) => c.type === 'EXAMEN_MEDICAL');

      this.source.set(examRows.map((c: any) => {
        const emp = empRows.find((e: any) => e.id == c.employeeId);
        let details: any = {};
        try {
          details = typeof c.details === 'string' ? JSON.parse(c.details) : (c.details || {});
        } catch {}

        return {
          id: String(c.id),
          employeeId: String(c.employeeId ?? ''),
          employeeName: emp ? `${emp.prenom} ${emp.nom}` : `Employé #${c.employeeId}`,
          category: details.category || 'Non spécifié',
          results: details.results || '-',
          date: c.dateConsultation ?? '',
          doctor: c.medecinName ?? 'Dr.',
        };
      }));
    } catch {
      this.feedback.error('Impossible de charger les examens médicaux.');
    }
  }

  list = computed(() => {
    const q = this.query().toLowerCase().trim();
    if (!q) return this.source();
    return this.source().filter((c) => 
      c.employeeName.toLowerCase().includes(q) || 
      c.employeeId.toLowerCase().includes(q) ||
      c.category.toLowerCase().includes(q)
    );
  });

  openCreate() { this.showCreate.set(true); }
  closeCreate() { this.showCreate.set(false); }

  onCreateFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.docFile.set(input.files?.[0] ?? null);
  }

  updateForm(key: string, value: any) {
    this.form.update(f => ({ ...f, [key]: value }));
  }

  async createExam() {
    const payload = this.form();
    if (!payload.employeeId) {
      this.feedback.error('Veuillez sélectionner un employé.');
      return;
    }
    
    this.creating.set(true);
    try {
      const details = {
        category: payload.category,
        results: payload.results,
        observations: payload.observations
      };

      const created = await firstValueFrom(this.api.createConsultation({
        employeeId: Number(payload.employeeId),
        medecinId: this.auth.user()?.id ? Number(this.auth.user()?.id) : null,
        type: 'EXAMEN_MEDICAL',
        dateConsultation: payload.date,
        details: JSON.stringify(details),
      }));

      if (this.docFile() && created?.id) {
        await firstValueFrom(this.api.uploadDocument(this.docFile()!, payload.employeeId, created.id));
      }

      this.docFile.set(null);
      this.form.set({
        employeeId: '',
        category: 'Biologie (Sang/Urine)',
        date: new Date().toISOString().slice(0, 10),
        results: '',
        observations: '',
      });
      this.feedback.success('Examen médical enregistré avec succès.');
      this.closeCreate();
      await this.load();
    } catch {
      this.feedback.error("Échec de l'enregistrement de l'examen.");
    } finally {
      this.creating.set(false);
    }
  }

  // Documents Logic
  async openDocuments(exam: any) {
    this.selectedExam.set(exam);
    this.showDocs.set(true);
    await this.refreshDocuments();
  }

  closeDocuments() {
    this.showDocs.set(false);
    this.selectedExam.set(null);
    this.documents.set([]);
    this.docFile.set(null);
  }

  async refreshDocuments() {
    const e = this.selectedExam();
    if (!e) return;
    try {
      const docs = await firstValueFrom(this.api.consultationDocuments(e.id));
      this.documents.set(Array.isArray(docs) ? docs : []);
    } catch {
      this.feedback.error('Impossible de charger les documents.');
    }
  }

  onDocsFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.docFile.set(input.files?.[0] ?? null);
  }

  async uploadDocumentForExam() {
    const e = this.selectedExam();
    if (!e || !this.docFile()) return;
    try {
      await firstValueFrom(this.api.uploadDocument(this.docFile()!, e.employeeId, e.id));
      this.feedback.success('Document téléversé.');
      this.docFile.set(null);
      await this.refreshDocuments();
    } catch {
      this.feedback.error('Échec du téléversement.');
    }
  }

  async downloadDocument(doc: any) {
    try {
      const blob = await firstValueFrom(this.api.downloadDocument(doc.id));
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = doc.nomFichier ?? `document-${doc.id}`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      this.feedback.error('Échec du téléchargement.');
    }
  }

  async deleteDocument(doc: any) {
    try {
      await firstValueFrom(this.api.deleteDocument(doc.id));
      this.feedback.success('Document supprimé.');
      await this.refreshDocuments();
    } catch {
      this.feedback.error('Échec de la suppression.');
    }
  }
}
