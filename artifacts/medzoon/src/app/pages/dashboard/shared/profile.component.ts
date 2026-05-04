import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../auth/auth.service';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { FeedbackService } from '../../../core/ui/feedback.service';
import { IconComponent } from '../../../shared/icon.component';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-header">
      <div class="page-header__title">
        <h1>Mon Profil</h1>
        <p>Gérez vos informations personnelles et votre sécurité</p>
      </div>
    </div>

    <div class="split-2">
      <div class="panel">
        <div class="panel__head">
          <div class="panel__title"><h2>Informations Personnelles</h2></div>
        </div>
        
        <div class="profile-header mb-4">
           <div class="user-avatar user-avatar--lg" style="background: var(--color-primary); color: #fff;">
              {{ user()?.firstName?.charAt(0) }}{{ user()?.lastName?.charAt(0) }}
           </div>
           <div class="profile-header__info">
              <h3>{{ user()?.firstName }} {{ user()?.lastName }}</h3>
              <span class="tag tag--primary">{{ user()?.role }}</span>
           </div>
        </div>

        <div class="emp-form__grid">
          <label class="emp-form__field">
            <span>Prénom</span>
            <input type="text" [(ngModel)]="form().prenom"/>
          </label>
          <label class="emp-form__field">
            <span>Nom</span>
            <input type="text" [(ngModel)]="form().nom"/>
          </label>
          <label class="emp-form__field emp-form__field--full">
            <span>Email (Lecture seule)</span>
            <input type="text" [value]="user()?.email" disabled class="disabled-input"/>
          </label>
          <label class="emp-form__field emp-form__field--full">
            <span>Téléphone</span>
            <input type="text" [(ngModel)]="form().telephone"/>
          </label>
        </div>

        <div class="mt-4 flex justify-end">
          <button class="btn btn-primary btn-sm" [disabled]="saving()" (click)="saveProfile()">
            <app-icon name="sparkles" [size]="14"></app-icon> Sauvegarder les modifications
          </button>
        </div>
      </div>

      <div class="panel">
        <div class="panel__head">
          <div class="panel__title"><h2>Sécurité</h2></div>
        </div>
        <p class="text-sm text-muted mb-4">Changez votre mot de passe pour sécuriser votre compte.</p>

        <div class="emp-form__grid">
          <label class="emp-form__field emp-form__field--full">
            <span>Mot de passe actuel</span>
            <input type="password" [(ngModel)]="securityForm().currentPassword"/>
          </label>
          <label class="emp-form__field emp-form__field--full">
            <span>Nouveau mot de passe</span>
            <input type="password" [(ngModel)]="securityForm().newPassword"/>
          </label>
          <label class="emp-form__field emp-form__field--full">
            <span>Confirmer le mot de passe</span>
            <input type="password" [(ngModel)]="securityForm().confirmPassword"/>
          </label>

          @if (securityError()) {
            <div class="emp-form__field--full alert alert--danger">
              {{ securityError() }}
            </div>
          }
        </div>

        <div class="mt-4 flex justify-end">
          <button class="btn btn-primary btn-sm" [disabled]="saving()" (click)="saveSecurity()">
            <app-icon name="lock" [size]="14"></app-icon> Mettre à jour le mot de passe
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .profile-header {
      display: flex;
      align-items: center;
      gap: 20px;
      padding: 10px 0;
      border-bottom: 1px solid var(--color-border);
    }
    .profile-header__info {
      h3 { margin-bottom: 4px; }
    }
    .disabled-input {
      background: var(--color-bg-soft);
      opacity: 0.7;
      cursor: not-allowed;
    }
    .alert {
      padding: 12px;
      border-radius: 8px;
      font-size: 0.85rem;
      &--danger { background: rgba(220,38,38,0.06); color: var(--color-danger); border: 1px solid rgba(220,38,38,0.1); }
    }
    .mt-4 { margin-top: 1.5rem; }
    .mb-4 { margin-bottom: 1.5rem; }
    .flex { display: flex; }
    .justify-end { justify-content: flex-end; }
    .text-sm { font-size: 0.85rem; }
    .text-muted { color: var(--color-fg-muted); }
  `]
})
export class ProfileComponent {
  auth = inject(AuthService);
  private api = inject(BackendApiService);
  private feedback = inject(FeedbackService);

  user = this.auth.user;
  saving = signal(false);
  securityError = signal('');

  form = signal({
    prenom: this.user()?.firstName || '',
    nom: this.user()?.lastName || '',
    telephone: '' // Assuming phone might be available or fetched
  });

  securityForm = signal({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  async saveProfile() {
    this.saving.set(true);
    try {
      await firstValueFrom(this.api.updateUserProfile(this.form()));
      this.feedback.success('Profil mis à jour avec succès');
    } catch (e) {
      this.feedback.error('Erreur lors de la mise à jour du profil');
    } finally {
      this.saving.set(false);
    }
  }

  async saveSecurity() {
    const f = this.securityForm();
    if (f.newPassword.length < 8) {
      this.securityError.set('Le mot de passe doit contenir au moins 8 caractères.');
      return;
    }
    if (f.newPassword !== f.confirmPassword) {
      this.securityError.set('Les mots de passe ne correspondent pas.');
      return;
    }

    this.saving.set(true);
    this.securityError.set('');
    try {
      await firstValueFrom(this.api.changePassword({
        currentPassword: f.currentPassword,
        newPassword: f.newPassword
      }));
      this.feedback.success('Mot de passe modifié avec succès');
      this.securityForm.set({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (e) {
      this.securityError.set('Mot de passe actuel incorrect');
    } finally {
      this.saving.set(false);
    }
  }
}
