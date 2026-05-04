import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { IconComponent } from '../../../shared/icon.component';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { FeedbackService } from '../../../core/ui/feedback.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './users.component.html',
  styleUrls: ['../shared/dash-ui.scss', './users.component.scss'],
})
export class AdminUsersComponent {
  private api = inject(BackendApiService);
  private feedback = inject(FeedbackService);
  users = signal([
    { name: 'Léa Bernard',     email: 'lea.bernard@medzoon.health',    role: 'coord',  status: 'Active', mfa: true,  last: '2 min',
      avatar: 'https://images.unsplash.com/photo-1551601651-2a8555f1a136?auto=format&fit=crop&w=120&q=80' },
    { name: 'Camille Dubois',  email: 'camille.dubois@medzoon.health', role: 'doctor', status: 'Active', mfa: true,  last: '14 min',
      avatar: 'https://images.unsplash.com/photo-1594824476967-48c8b964273f?auto=format&fit=crop&w=120&q=80' },
    { name: 'Idris Okafor',    email: 'idris.okafor@medzoon.health',   role: 'doctor', status: 'Active', mfa: true,  last: '1 h',
      avatar: 'https://images.unsplash.com/photo-1612531385446-f7e6d131e1d0?auto=format&fit=crop&w=120&q=80' },
    { name: 'Aria Nakamura',   email: 'aria.nakamura@medzoon.health',  role: 'coord',  status: 'Pending', mfa: false, last: '—',
      avatar: 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&w=120&q=80' },
    { name: 'Margaux Laurent', email: 'admin@medzoon.health',          role: 'admin',  status: 'Active', mfa: true,  last: '3 j',
      avatar: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=120&q=80' },
    { name: 'Marc Lefèvre',    email: 'marc.lefevre@medzoon.health',   role: 'admin',  status: 'Suspended', mfa: false, last: '12 j',
      avatar: 'https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=120&q=80' },
  ]);

  showInvite = signal(false);
  invite = signal({ firstName: '', lastName: '', email: '', password: '', role: 'COORDINATRICE', assignedMedecinId: null as number | null });
  doctors = signal<any[]>([]);

  constructor() {
    this.loadUsers();
  }

  async loadUsers() {
    try {
      const rows = await firstValueFrom(this.api.adminUsers());
      if (Array.isArray(rows) && rows.length) {
        this.users.set(rows.map((u: any) => ({
          id: u.id,
          name: `${u.prenom ?? ''} ${u.nom ?? ''}`.trim(),
          email: u.email,
          role: u.role === 'ADMIN' ? 'Administrateur' : u.role === 'COORDINATRICE' ? 'Coordinatrice' : 'Médecin',
          status: u.enabled ? 'Actif' : 'Suspendu',
          mfa: true,
          last: 'En ligne',
          avatar: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=120&q=80',
          assignedMedecinId: u.assignedMedecinId
        })));

        // Extract doctors for the dropdown
        this.doctors.set(rows.filter((u: any) => u.role === 'MEDECIN').map((u: any) => ({
          id: u.id,
          name: `Dr. ${u.prenom} ${u.nom}`
        })));
      }
    } catch {
      this.feedback.error('Unable to load users.');
    }
  }

  open()  { this.showInvite.set(true); }
  close() { this.showInvite.set(false); }

  async inviteUser() {
    const v = this.invite();
    if (!v.email || !v.firstName || !v.lastName) return;
    try {
      await firstValueFrom(this.api.createAdminUser({
        email: v.email,
        passwordHash: v.password || 'password123',
        nom: v.lastName,
        prenom: v.firstName,
        role: v.role,
        enabled: true,
        assignedMedecinId: v.role === 'COORDINATRICE' ? v.assignedMedecinId : null
      }));
      await this.loadUsers();
      this.close();
      this.feedback.success('Utilisateur invité avec succès.');
    } catch (err: any) {
      console.error('Invite Error:', err);
      let msg = "Échec de l'invitation.";
      
      if (err.error && typeof err.error === 'object' && err.error.message) {
        msg = err.error.message;
      } else if (err.error && typeof err.error === 'string') {
        msg = err.error;
      } else if (err.statusText) {
        msg = err.statusText;
      }

      this.feedback.error(msg, "Action impossible");
    }
  }

  async deleteUser(user: any) {
    if (!user?.id) return;
    
    const result = await (window as any).Swal.fire({
      title: 'Êtes-vous sûr ?',
      text: "L'utilisateur sera supprimé définitivement.",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#3B82F6',
      cancelButtonColor: '#EF4444',
      confirmButtonText: 'Oui, supprimer',
      cancelButtonText: 'Annuler'
    });

    if (result.isConfirmed) {
      try {
        await firstValueFrom(this.api.disableAdminUser(user.id));
        await this.loadUsers();
        this.feedback.success('Utilisateur supprimé avec succès.');
      } catch {
        this.feedback.error('Échec de la suppression.');
      }
    }
  }

  updateInvite(key: string, value: any) {
    this.invite.update(i => ({ ...i, [key]: value }));
  }
}
