import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../../../shared/icon.component';
import { Router } from '@angular/router';
import { EmployeeListItem, EmployeeStatus } from '../../../core/models/models';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-patients',
  standalone: true,
  imports: [FormsModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './patients.component.html',
  styleUrls: ['../shared/dash-ui.scss', './patients.component.scss'],
})
export class PatientsComponent {
  private api = inject(BackendApiService);
  private router = inject(Router);
  query = signal('');
  dept = signal<'All' | string>('All');
  source = signal<EmployeeListItem[]>([]);

  constructor() {
    this.load();
  }

  async load() {
    try {
      const res = await firstValueFrom(this.api.employees());
      const rows = Array.isArray(res?.content) ? res.content : Array.isArray(res) ? res : [];
      if (rows.length) {
        this.source.set(rows.map((e: any) => ({
          id: String(e.id),
          matricule: e.dossierNumber ?? '-',
          firstName: e.prenom ?? '',
          lastName: e.nom ?? '',
          position: e.posteTravail ?? '',
          department: e.departement ?? '',
          birthDate: e.dateNaissance ?? '',
          phone: e.telephone ?? '-',
          gouvernorat: e.gouvernorat ?? '-',
          lastVisit: '-',
          vaccines: 0,
          status: (e.statut === 'ARCHIVE' ? 'Archived' : 'Active') as EmployeeStatus,
          avatar: 'https://api.dicebear.com/7.x/initials/svg?seed=' + e.prenom + ' ' + e.nom + '&backgroundColor=3B82F6&textColor=ffffff',
        })));
      }
    } catch {}
  }

  exportList() {
    const csv = ['id,firstName,lastName,position,department,status', ...this.patients().map((p) =>
      [p.id, p.firstName, p.lastName, p.position, p.department, p.status].join(',')),
    ].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'patients.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  departments = computed(() => ['All', ...Array.from(new Set(this.source().map((e) => e.department)))]);

  patients = computed(() => {
    const q = this.query().toLowerCase().trim();
    const d = this.dept();
    return this.source().filter((e) => {
      if (d !== 'All' && e.department !== d) return false;
      if (!q) return true;
      return (
        e.firstName.toLowerCase().includes(q) ||
        e.lastName.toLowerCase().includes(q) ||
        (e.matricule || '').toLowerCase().includes(q) ||
        (e.position ?? '').toLowerCase().includes(q)
      );
    });
  });

  openPatient(id: string) {
    // Navigate to the dossier. The route is /dashboard/doctor/dossiers/:id
    this.router.navigate(['/dashboard/doctor/dossiers', id]);
  }
}
