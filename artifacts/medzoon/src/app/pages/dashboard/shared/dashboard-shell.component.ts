import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../auth/auth.service';
import { ROLE_LABELS, UserRole } from '../../../auth/auth.types';
import { IconComponent, IconName } from '../../../shared/icon.component';
import { AiAssistantComponent } from '../doctor/ai-assistant.component';
import { FeedbackService } from '../../../core/ui/feedback.service';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { firstValueFrom } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DashboardStateService } from '../../../core/ui/dashboard-state.service';

interface NavItem {
  label: string;
  route: string;
  icon: IconName;
}

@Component({
  selector: 'app-dashboard-shell',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink, RouterLinkActive, IconComponent, AiAssistantComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard-shell.component.html',
  styleUrl: './dashboard-shell.component.scss',
})
export class DashboardShellComponent {
  auth = inject(AuthService);
  router = inject(Router);
  feedback = inject(FeedbackService);
  private api = inject(BackendApiService);
  state = inject(DashboardStateService);
  ROLE_LABELS = ROLE_LABELS;
  user = this.auth.user;
  menuOpen = signal(false);
  sidebarOpen = signal(false);
  searchQuery = signal('');
  searchResults = signal<any[]>([]);
  searchOpen = signal(false);
  private _searchTimer: any = null;

  saving = signal(false);
  notificationCount = signal(0);
  private audio = new Audio('https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3');

  navItems = computed<NavItem[]>(() => {
    const role = this.user()?.role;
    if (!role) return [];
    return NAV_BY_ROLE[role] || [];
  });

  constructor() {
    this.refreshNotifications();
    setInterval(() => this.refreshNotifications(), 30000); // Check every 30s
  }

  async refreshNotifications() {
    try {
      const notes = await firstValueFrom(this.api.notifications());
      const count = Array.isArray(notes) ? notes.filter((n: any) => n.statut === 'PENDING' || !n.read).length : 0;
      if (count > this.notificationCount()) {
        this.playNotificationSound();
      }
      this.notificationCount.set(count);
    } catch { /* ignore */ }
  }

  playNotificationSound() {
    this.audio.play().catch(e => console.log('Sound playback blocked', e));
  }

  toggleMenu() { this.menuOpen.update((v) => !v); }
  closeMenu()  { this.menuOpen.set(false); }
  toggleSidebar() { this.sidebarOpen.update((v) => !v); }
  closeSidebar()  { this.sidebarOpen.set(false); }

  signOut() {
    this.closeMenu();
    this.auth.signOut();
  }

  openNotifications() {
    const role = this.user()?.role;
    if (role === 'admin') this.router.navigateByUrl('/dashboard/admin/audit');
    if (role === 'coordinatrice') this.router.navigateByUrl('/dashboard/coordinatrice/reminders');
    if (role === 'medecin') this.router.navigateByUrl('/dashboard/doctor/vaccines');
  }

  closeFeedback() {
    this.feedback.clear();
  }

  getRoleLabel(role: any): string {
    return (ROLE_LABELS as any)[role] || role;
  }

  // ---- Global Search ----
  onSearchInput(value: string) {
    this.searchQuery.set(value);
    if (this._searchTimer) clearTimeout(this._searchTimer);
    if (!value.trim()) { this.searchResults.set([]); this.searchOpen.set(false); return; }
    this._searchTimer = setTimeout(() => this.runSearch(value.trim()), 300);
  }

  async runSearch(q: string) {
    try {
      const [empRes, apptRes] = await Promise.all([
        firstValueFrom(this.api.employees()),
        firstValueFrom(this.api.appointments(
          new Date(Date.now() - 30*86400000).toISOString(),
          new Date(Date.now() + 30*86400000).toISOString()
        ))
      ]);
      const emps  = Array.isArray(empRes)        ? empRes        : (empRes?.content   || []);
      const appts = Array.isArray(apptRes)       ? apptRes       : (apptRes?.content  || []);
      const ql = q.toLowerCase();

      const empHits = emps
        .filter((e: any) =>
          `${e.prenom} ${e.nom}`.toLowerCase().includes(ql) ||
          (e.dossierNumber ?? '').toLowerCase().includes(ql) ||
          (e.poste ?? '').toLowerCase().includes(ql)
        )
        .slice(0, 5)
        .map((e: any) => ({
          type: 'employee',
          label: `${e.prenom} ${e.nom}`,
          sub: `${e.poste ?? ''} · ${e.dossierNumber ?? ''}`,
          icon: 'users',
          id: e.id
        }));

      const apptHits = appts
        .filter((a: any) => {
          const emp = emps.find((e: any) => e.id === a.employeeId);
          return emp && `${emp.prenom} ${emp.nom}`.toLowerCase().includes(ql);
        })
        .slice(0, 3)
        .map((a: any) => {
          const emp = emps.find((e: any) => e.id === a.employeeId);
          return {
            type: 'appointment',
            label: emp ? `${emp.prenom} ${emp.nom}` : `RDV #${a.id}`,
            sub: `Rendez-vous · ${new Date(a.dateDebut).toLocaleDateString('fr-FR')}`,
            icon: 'calendar',
            id: a.employeeId
          };
        });

      this.searchResults.set([...empHits, ...apptHits]);
      this.searchOpen.set(this.searchResults().length > 0);
    } catch { this.searchResults.set([]); }
  }

  navigateToResult(r: any) {
    this.searchQuery.set('');
    this.searchResults.set([]);
    this.searchOpen.set(false);
    const role = this.user()?.role;
    if (r.type === 'employee' || r.type === 'appointment') {
      if (role === 'medecin') this.router.navigateByUrl(`/dashboard/doctor/dossiers/${r.id}`);
      else if (role === 'coordinatrice') this.router.navigateByUrl(`/dashboard/coordinatrice/employees`);
      else this.router.navigateByUrl(`/dashboard/admin/dossiers`);
    }
  }

  clearSearch() { this.searchQuery.set(''); this.searchResults.set([]); this.searchOpen.set(false); }

  // Profile
  openProfile() {
    this.closeMenu();
    this.router.navigate(['/dashboard/profile']);
  }

  // Security
  openSecurity() {
    this.closeMenu();
    this.router.navigate(['/dashboard/security']);
  }
}

const NAV_BY_ROLE: Record<UserRole, NavItem[]> = {
  admin: [
    { label: "Vue d'ensemble", route: '/dashboard/admin',          icon: 'chart' },
    { label: 'Utilisateurs',    route: '/dashboard/admin/users',    icon: 'users' },
    { label: 'Dossiers Médicaux', route: '/dashboard/admin/dossiers', icon: 'folder' },
    { label: 'Rendez-vous',     route: '/dashboard/admin/schedule', icon: 'calendar' },
    { label: 'Consultations',   route: '/dashboard/admin/consults', icon: 'document' },
    { label: 'Audit',           route: '/dashboard/admin/audit',    icon: 'document' },
    { label: 'Paramètres',      route: '/dashboard/admin/settings', icon: 'sparkles' },
  ],
  coordinatrice: [
    { label: "Vue d'ensemble", route: '/dashboard/coordinatrice',           icon: 'chart' },
    { label: 'Employés',        route: '/dashboard/coordinatrice/employees', icon: 'users' },
    { label: 'Rendez-vous',     route: '/dashboard/coordinatrice/schedule',  icon: 'calendar' },
    { label: 'Rappels',         route: '/dashboard/coordinatrice/reminders', icon: 'bell' },
  ],
  medecin: [
    { label: "Tableau de bord", route: '/dashboard/doctor',           icon: 'stethoscope' },
    { label: 'Employés',        route: '/dashboard/doctor/patients',  icon: 'users' },
    { label: 'Dossiers Médicaux', route: '/dashboard/doctor/dossiers', icon: 'folder' },
    { label: 'Consultations',   route: '/dashboard/doctor/consults',  icon: 'document' },
    { label: 'Examens Médicaux', route: '/dashboard/doctor/exams',     icon: 'sparkles' },
    { label: 'Vaccinations',    route: '/dashboard/doctor/vaccines',  icon: 'syringe' },
    { label: 'Bibliothèque',    route: '/dashboard/doctor/drugs',     icon: 'pill' },
  ],
};
