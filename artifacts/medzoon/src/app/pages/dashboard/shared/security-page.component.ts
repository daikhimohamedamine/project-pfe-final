import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../../../shared/icon.component';
import { FeedbackService } from '../../../core/ui/feedback.service';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../auth/auth.service';

@Component({
  selector: 'app-security-page',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './security-page.component.html',
  styleUrls: ['../shared/dash-ui.scss', './security-page.component.scss'],
})
export class SecurityPageComponent {
  private api = inject(BackendApiService);
  private feedback = inject(FeedbackService);
  private auth = inject(AuthService);
  
  user = this.auth.user;
  saving = signal(false);
  
  securityForm = signal({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  securityError = signal('');

  async saveSecurity() {
    const f = this.securityForm();
    if (f.newPassword.length < 8) {
      this.securityError.set('Le mot de passe doit contenir au moins 8 caractères.');
      return;
    }
    if (f.newPassword !== f.confirmPassword) {
      this.securityError.set('Les nouveaux mots de passe ne correspondent pas.');
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
    } catch (e: any) {
      this.securityError.set('Mot de passe actuel incorrect');
    } finally {
      this.saving.set(false);
    }
  }

  updateSecurityField(key: string, val: string) {
    this.securityForm.update(f => ({ ...f, [key]: val }));
  }
}
