import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../auth/auth.service';
import { IconComponent } from '../../../shared/icon.component';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['../shared/dash-ui.scss', './admin-dashboard.component.scss'],
})
export class AdminDashboardComponent {
  user = inject(AuthService).user;
  private router = inject(Router);
  private api = inject(BackendApiService);
  usersList = signal<any[]>([]);
  auditList = signal<any[]>([]);

  kpis = signal([
    { label: 'Utilisateurs Actifs', value: '...', delta: 'Chargement', up: true, icon: 'users' as const },
    { label: 'Dossiers Médicaux',   value: '...', delta: 'Chargement', up: true, icon: 'document' as const },
    { label: 'Logs Audit',          value: '...', delta: 'Chargement', up: false, icon: 'shield' as const },
    { label: 'Stockage Utilisé',    value: '...', delta: 'Chargement', up: false, icon: 'chart' as const },
  ]);

  rolesDist = signal([
    { label: 'Médecins',       pct: 0 },
    { label: 'Coordinatrices', pct: 0 },
    { label: 'Administrateurs',pct: 0 },
  ]);

  constructor() {
    this.load();
  }

  async load() {
    try {
      const [users, logs, stats] = await Promise.all([
        firstValueFrom(this.api.adminUsers()),
        firstValueFrom(this.api.adminAuditLogs()),
        firstValueFrom(this.api.adminStats())
      ]);
      this.usersList.set(users.map((u: any) => ({
        name: `${u.prenom} ${u.nom}`,
        email: u.email,
        role: u.role?.toLowerCase(),
        status: u.enabled ? 'Active' : 'Disabled',
        last: '-',
        avatar: ''
      })));
      this.auditList.set(logs.slice(0, 5).map((l: any) => ({
        actor: `User #${l.userId}`,
        action: l.action,
        target: `${l.entityType}#${l.entityId}`,
        time: l.timestamp,
        level: 'ok'
      })));

      this.kpis.set([
        { label: 'Utilisateurs Actifs', value: stats.activeUsers + '', delta: `sur ${stats.totalUsers}`, up: true, icon: 'users' as const },
        { label: 'Dossiers Médicaux',   value: stats.recordsStored + '', delta: 'Actifs', up: true, icon: 'document' as const },
        { label: 'Logs Audit',          value: stats.openAuditItems + '', delta: 'Récent', up: false, icon: 'shield' as const },
        { label: 'Stockage Utilisé',    value: stats.storageUsed, delta: 'Capacité', up: false, icon: 'chart' as const },
      ]);

      this.rolesDist.set([
        { label: 'Médecins',       pct: stats.roleDistDoctors },
        { label: 'Coordinatrices', pct: stats.roleDistCoords },
        { label: 'Administrateurs',pct: stats.roleDistAdmins },
      ]);
    } catch {}
  }

  goUsers() { this.router.navigateByUrl('/dashboard/admin/users'); }
  goAudit() { this.router.navigateByUrl('/dashboard/admin/audit'); }
  exportReport() { this.router.navigateByUrl('/dashboard/admin/audit'); }
}
