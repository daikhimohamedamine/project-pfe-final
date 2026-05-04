import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterModule } from '@angular/router';
import { AuthService } from '../../../auth/auth.service';
import { FeedbackService } from '../../../core/ui/feedback.service';
import { IconComponent } from '../../../shared/icon.component';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './profile-page.component.html',
  styleUrls: ['../shared/dash-ui.scss', './profile-page.component.scss'],
})
export class ProfilePageComponent {
  private auth = inject(AuthService);
  private feedback = inject(FeedbackService);
  private api = inject(BackendApiService);

  user = this.auth.user;
  saving = signal(false);

  form = signal({
    firstName: this.user()?.firstName ?? '',
    lastName: this.user()?.lastName ?? '',
    phone: this.user()?.phone ?? '',
  });

  async save() {
    if (!this.user()) return;
    this.saving.set(true);
    try {
      await firstValueFrom(this.api.updateUserProfile({
        prenom: this.form().firstName,
        nom: this.form().lastName,
        telephone: this.form().phone
      }));
      this.feedback.success('Profil mis à jour avec succès.');
      // Update local auth state if possible or reload
    } catch (err) {
      this.feedback.error('Erreur lors de la sauvegarde du profil.');
    } finally {
      this.saving.set(false);
    }
  }

  updateField(key: any, value: string) {
    this.form.update((f: any) => ({ ...f, [key]: value }));
  }
}
