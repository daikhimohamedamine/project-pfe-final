import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../../../shared/icon.component';
import { AuditEvent } from '../../../core/models/models';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-admin-audit',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './audit.component.html',
  styleUrls: ['../shared/dash-ui.scss', './audit.component.scss'],
})
export class AdminAuditComponent {
  private api = inject(BackendApiService);
  level = signal<'All' | 'ok' | 'warn' | 'danger'>('All');
  query = signal('');
  source = signal<AuditEvent[]>([]);
  selectedUserActivity = signal<any | null>(null);
  showUserDetail = signal(false);

  constructor() {
    this.load();
  }

  async load() {
    try {
      const rows = await firstValueFrom(this.api.adminAuditLogs());
      if (Array.isArray(rows) && rows.length) {
        this.source.set(rows.map((e: any) => ({
          id: e.id,
          time: e.timestamp ? new Date(e.timestamp).toLocaleString('fr-FR') : '',
          actor: e.actorName || (e.userId ? `Utilisateur #${e.userId}` : 'Système'),
          actorRole: e.userRole?.toLowerCase() || 'system',
          action: this.translateAction(e.action),
          target: e.targetName || `${e.entityType ?? ''} #${e.entityId ?? '-'}`,
          medecinContext: e.medecinContextName,
          ip: e.ipAddress ?? '-',
          level: (e.action?.includes('DELETE') || e.action?.includes('DISABLE')) ? 'danger' : (e.action?.includes('UPDATE') ? 'warn' : 'ok'),
          userId: e.userId
        })));
      }
    } catch {}
  }

  private translateAction(a: string): string {
    const map: Record<string, string> = {
      'VIEW_EMPLOYEE': 'Consultation dossier',
      'CREATE_EMPLOYEE': 'Création employé',
      'UPDATE_EMPLOYEE': 'Modification employé',
      'DELETE_EMPLOYEE': 'Suppression employé',
      'CREATE_CONSULTATION': 'Nouvelle consultation',
      'UPDATE_CONSULTATION': 'Modif. consultation',
      'CREATE_USER': 'Création utilisateur',
      'UPDATE_USER': 'Modif. utilisateur',
      'DISABLE_USER': 'Désactivation compte',
      'CREATE_REMINDER': 'Programmation rappel',
      'SEND_REMINDER': 'Envoi manuel rappel',
      'DELETE_REMINDER': 'Suppression rappel'
    };
    return map[a] || a;
  }

  async viewActivity(userId: number | undefined) {
    if (!userId) return;
    try {
      const activity = await firstValueFrom(this.api.adminUserActivity(userId));
      this.selectedUserActivity.set(activity);
      this.showUserDetail.set(true);
    } catch {
      // Feedback omitted for brevity or use inject(FeedbackService)
    }
  }

  closeDetail() {
    this.showUserDetail.set(false);
    this.selectedUserActivity.set(null);
  }

  exportCsv() {
    const rows = this.list();
    const csv = ['Date,Auteur,Action,Cible,IP,Niveau', ...rows.map((r) => [r.time, r.actor, r.action, r.target, r.ip, r.level].join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'audit-logs.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  list = computed(() => {
    const l = this.level();
    const q = this.query().toLowerCase().trim();
    return this.source().filter((e) => {
      if (l !== 'All' && e.level !== l) return false;
      if (!q) return true;
      return e.actor.toLowerCase().includes(q) || e.action.toLowerCase().includes(q) || e.target.toLowerCase().includes(q);
    });
  });
}
