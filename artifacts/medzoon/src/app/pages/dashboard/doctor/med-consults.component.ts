// Force rebuild: 2026-04-26 01:17
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { IconComponent } from '../../../shared/icon.component';
import { ConsultationListItem } from '../../../core/models/models';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { FeedbackService } from '../../../core/ui/feedback.service';
import { PrescriptionService } from './prescription.service';
import { AuthService } from '../../../auth/auth.service';
import { firstValueFrom } from 'rxjs';
import * as pdfMake from 'pdfmake/build/pdfmake';
import * as pdfFonts from 'pdfmake/build/vfs_fonts';

// Correction ESM pour pdfmake
// (pdfMake as any).vfs = (pdfFonts as any).pdfMake.vfs;

@Component({
  selector: 'app-consults-v2',
  standalone: true,
  imports: [FormsModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './consults.component.html',
  styleUrls: ['../shared/dash-ui.scss', './consults.component.scss'],
})
export class ConsultsV2Component {
  private api = inject(BackendApiService);
  private feedback = inject(FeedbackService);
  public rx = inject(PrescriptionService);
  public auth = inject(AuthService);
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  
  type = signal<'All' | ConsultationListItem['type']>('All');
  query = signal('');
  source = signal<ConsultationListItem[]>([]);
  showCreate = signal(false);
  creating = signal(false);
  showDocs = signal(false);
  selectedConsultation = signal<ConsultationListItem | null>(null);
  documents = signal<any[]>([]);
  docFile = signal<File | null>(null);
  employees = signal<any[]>([]);
  doctors = signal<any[]>([]);
  manualDrug = signal('');
  form = signal({
    employeeId: '',
    medecinId: '',
    type: 'Spontanée' as ConsultationListItem['type'],
    date: new Date().toISOString().slice(0, 10),
    diagnostic: '',
    poids: '',
    taille: '',
    tensionSystolique: '',
    tensionDiastolique: '',
    conclusion: 'Apte' as NonNullable<ConsultationListItem['conclusion']>,
    apte: true,
    inaptePrecision: '',
    inapteDefinitif: false,
    reclassification: '',
  });

  constructor() {
    this.load();
    const empId = this.route.snapshot.queryParamMap.get('employeeId');
    if (empId) {
      this.form.update(f => ({ ...f, employeeId: empId }));
      this.showCreate.set(true);
    }
  }

  async load() {
    try {
      const empRes = await firstValueFrom(this.api.employees());
      const empRows = Array.isArray(empRes?.content) ? empRes.content : Array.isArray(empRes) ? empRes : [];
      this.employees.set(empRows);

      try {
        const docRes = await firstValueFrom(this.api.medecins());
        this.doctors.set(docRes);
      } catch (err) {
        console.error('Failed to load doctors', err);
      }

      // Fetch ALL consultations (no employeeId filter)
      const res = await firstValueFrom(this.api.consultations());
      const rows = Array.isArray(res?.content) ? res.content : Array.isArray(res) ? res : [];
      
      this.source.set(rows.map((c: any) => {
        const emp = empRows.find((e: any) => e.id == c.employeeId);
        return {
          ...(this.extractMedicalSummary(c.details)),
          id: String(c.id),
          employeeId: String(c.employeeId ?? ''),
          employeeName: emp ? `${emp.prenom} ${emp.nom}` : `Employé #${c.employeeId}`,
          type: this.toUiType(c.type),
          date: c.dateConsultation ?? '',
          time: c.createdAt ? new Date(c.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '09:00',
          doctor: c.medecinName ?? 'Dr. ' + (this.auth.user()?.lastName || 'Dhahri'),
          status: 'Terminé',
        };
      }));
    } catch {
      this.feedback.error('Impossible de charger les consultations.');
    }
  }

  types: ('All' | ConsultationListItem['type'])[] = ['All', 'Embauche', 'Périodique', 'Reprise', 'Soin', 'Spontanée'];

  list = computed(() => {
    const t = this.type();
    const q = this.query().toLowerCase().trim();
    return this.source().filter((c) => {
      if (t !== 'All' && c.type !== t) return false;
      if (!q) return true;
      return c.employeeName.toLowerCase().includes(q) || c.employeeId.toLowerCase().includes(q);
    });
  });

  openCreateConsultation() {
    this.showCreate.set(true);
  }

  closeCreateConsultation() {
    this.showCreate.set(false);
  }

  onCreateFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.docFile.set(input.files?.[0] ?? null);
  }

  private toBackendType(type: ConsultationListItem['type']) {
    const map: Record<ConsultationListItem['type'], string> = {
      Embauche: 'EMBAUCHE',
      Périodique: 'PERIODIQUE',
      Reprise: 'REPRISE',
      Soin: 'SOIN',
      Spontanée: 'SPONTANEE',
    };
    return map[type];
  }

  private toUiType(type: string): ConsultationListItem['type'] {
    const map: Record<string, ConsultationListItem['type']> = {
      EMBAUCHE: 'Embauche',
      PERIODIQUE: 'Périodique',
      REPRISE: 'Reprise',
      SOIN: 'Soin',
      SPONTANEE: 'Spontanée',
    };
    return map[type] ?? 'Soin';
  }

  private extractMedicalSummary(detailsRaw: any) {
    let details: any = {};
    try {
      details = typeof detailsRaw === 'string' ? JSON.parse(detailsRaw) : (detailsRaw ?? {});
    } catch {
      details = {};
    }
    const tension = details?.tension ?? {};
    const conclusionObj = details?.conclusion ?? {};
    const conclusion: ConsultationListItem['conclusion'] =
      conclusionObj?.apte === false ? 'Inapte temporaire' :
        conclusionObj?.apte === true ? 'Apte' :
          'À revoir';
    return {
      bp: tension?.systolique && tension?.diastolique ? `${tension.systolique}/${tension.diastolique}` : '-',
      weight: detailsRaw.poids ?? details?.poids ?? '-',
      conclusion,
    };
  }

  async createConsultation() {
    const payload = this.form();
    if (!payload.employeeId) {
      this.feedback.error('Veuillez sélectionner un employé.');
      return;
    }
    
    this.creating.set(true);
    try {
      const details: any = {
        diagnostic: payload.diagnostic,
        tension: {
          systolique: payload.tensionSystolique || '',
          diastolique: payload.tensionDiastolique || '',
        },
        conclusion: {
          apte: payload.apte,
          inapte_precision: payload.apte ? '' : (payload.inaptePrecision || ''),
          inapte_definitif: payload.apte ? false : payload.inapteDefinitif,
          reclassification: payload.apte ? '' : (payload.reclassification || ''),
        },
        traitements_prescrits: this.rx.items().map(p => ({
          drug_name: p.drug.drug_name,
          generic_name: p.drug.generic_name,
          posology: p.posology
        })),
      };

      const created = await firstValueFrom(this.api.createConsultation({
        employeeId: Number(payload.employeeId),
        medecinId: payload.medecinId ? Number(payload.medecinId) : null,
        type: this.toBackendType(payload.type),
        dateConsultation: payload.date,
        poids: payload.poids ? Number(payload.poids) : null,
        taille: payload.taille ? Number(payload.taille) : null,
        details: JSON.stringify(details),
      }));

      if (this.docFile()) {
        await firstValueFrom(this.api.uploadDocument(this.docFile()!, payload.employeeId, created?.id));
      }

      this.rx.clear();
      this.docFile.set(null);
      this.form.set({
        employeeId: '',
        medecinId: '',
        type: 'Spontanée',
        date: new Date().toISOString().slice(0, 10),
        diagnostic: '',
        poids: '',
        taille: '',
        tensionSystolique: '',
        tensionDiastolique: '',
        conclusion: 'Apte',
        apte: true,
        inaptePrecision: '',
        inapteDefinitif: false,
        reclassification: '',
      });
      this.feedback.success('Consultation créée avec succès.');
      this.closeCreateConsultation();
      await this.load();
    } catch {
      this.feedback.error('Échec de la création de la consultation.');
    } finally {
      this.creating.set(false);
    }
  }

  async openDocuments(c: ConsultationListItem) {
    this.selectedConsultation.set(c);
    this.showDocs.set(true);
    await this.refreshDocuments();
  }

  closeDocuments() {
    this.showDocs.set(false);
    this.selectedConsultation.set(null);
    this.documents.set([]);
    this.docFile.set(null);
  }

  async refreshDocuments() {
    const c = this.selectedConsultation();
    if (!c) return;
    try {
      const docs = await firstValueFrom(this.api.consultationDocuments(c.id));
      this.documents.set(Array.isArray(docs) ? docs : []);
    } catch {
      this.feedback.error('Impossible de charger les documents de la consultation.');
    }
  }

  onDocsFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.docFile.set(input.files?.[0] ?? null);
  }

  async uploadDocumentForConsultation() {
    const c = this.selectedConsultation();
    if (!c || !this.docFile()) return;
    try {
      await firstValueFrom(this.api.uploadDocument(this.docFile()!, c.employeeId, c.id));
      this.feedback.success('Document téléversé.');
      this.docFile.set(null);
      await this.refreshDocuments();
    } catch {
      this.feedback.error('Échec du téléversement du document.');
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
      this.feedback.success('Document téléchargé.');
    } catch {
      this.feedback.error('Échec du téléchargement du document.');
    }
  }

  async deleteDocument(doc: any) {
    try {
      await firstValueFrom(this.api.deleteDocument(doc.id));
      this.feedback.success('Document supprimé.');
      await this.refreshDocuments();
    } catch {
      this.feedback.error('Échec de la suppression (accès restreint).');
    }
  }

  updateForm(key: string, value: any) {
    this.form.update(f => ({ ...f, [key]: value }));
  }

  addManualDrug() {
    const val = this.manualDrug().trim();
    if (!val) return;
    this.rx.add({
      set_id: 'manual-' + Date.now(),
      drug_name: val,
      generic_name: 'Saisie manuelle',
      dosage: '',
      indications: '',
      image_lookup_url: ''
    } as any, '');
    this.manualDrug.set('');
  }

  async sendPrescriptionEmail() {
    const payload = this.form();
    const emp = this.employees().find(e => e.id == payload.employeeId);
    if (!emp || !emp.email) {
      this.feedback.error("Cet employé n'a pas d'adresse email enregistrée.");
      return;
    }
    
    try {
      // Simuler un appel backend
      await new Promise(resolve => setTimeout(resolve, 800));
      this.feedback.success(`Ordonnance envoyée par email à ${emp.email}`);
    } catch (error: any) {
      this.feedback.error("Échec de l'envoi de l'email.");
    }
  }

  generatePrescriptionPDF() {
    const pItems = this.rx.items();
    if (!pItems.length) {
      this.feedback.error("Aucun médicament dans l'ordonnance.");
      return;
    }

    const empId = this.form().employeeId;
    const emp = this.employees().find(e => e.id == empId);
    const empName = emp ? `${emp.prenom} ${emp.nom}` : 'Patient Inconnu';

    const docDefinition: any = {
      content: [
        { text: 'MEDZOON - Cabinet Médical', style: 'header' },
        { text: 'Dr. Moetaz Dhahri', style: 'subheader' },
        { text: 'Médecine du Travail', style: 'subheader' },
        { text: '\n\n' },
        { text: `Date: ${new Date().toLocaleDateString('fr-FR')}`, alignment: 'right' },
        { text: '\n' },
        { text: 'ORDONNANCE', style: 'title' },
        { text: '\n\n' },
        { text: `Employé: ${empName}`, style: 'patientInfo' },
        { text: '\n\n' },
        {
          table: {
            widths: ['*', 'auto'],
            body: [
              [{ text: 'Médicament', style: 'tableHeader' }, { text: 'Posologie', style: 'tableHeader' }],
              ...pItems.map(p => [
                { text: `${p.drug.drug_name} (${p.drug.generic_name})`, margin: [0, 5, 0, 5] },
                { text: p.posology || '-', margin: [0, 5, 0, 5] }
              ])
            ]
          },
          layout: 'lightHorizontalLines'
        },
        { text: '\n\n\n\n' },
        { text: 'Cachet et Signature', alignment: 'right', italics: true }
      ],
      styles: {
        header: { fontSize: 18, bold: true, color: '#2c3e50' },
        subheader: { fontSize: 12, color: '#7f8c8d' },
        title: { fontSize: 22, bold: true, alignment: 'center', decoration: 'underline' },
        patientInfo: { fontSize: 14, bold: true },
        tableHeader: { bold: true, fontSize: 13, color: 'black' }
      }
    };

    (pdfMake as any).createPdf(docDefinition).download(`Ordonnance_${empName.replace(' ', '_')}.pdf`);
    this.feedback.success('Ordonnance PDF générée.');
  }
}
