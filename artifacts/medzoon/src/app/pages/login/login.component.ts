import { ChangeDetectionStrategy, Component, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { IconComponent } from '../../shared/icon.component';
import { ROLE_LABELS } from '../../auth/auth.types';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = signal('');
  password = signal('');
  verificationCode = signal('');
  show2fa = signal(false);
  error = signal<string | null>(null);
  loading = signal(false);
  ROLE_LABELS = ROLE_LABELS;

  async submit(event: Event) {
    event.preventDefault();
    this.error.set(null);
    this.loading.set(true);
    const result = await this.auth.signIn(this.email(), this.password());
    this.loading.set(false);
    if (!result.ok) {
      this.error.set(result.error);
      return;
    }
    
    if (result.requires2fa) {
      this.show2fa.set(true);
      return;
    }

    this.router.navigateByUrl(this.auth.homeForRole(result.user.role));
  }

  async submit2fa(event: Event) {
    event.preventDefault();
    this.error.set(null);
    this.loading.set(true);
    const result = await this.auth.verify2fa(this.email(), this.verificationCode());
    this.loading.set(false);
    if (!result.ok) {
      this.error.set(result.error);
      return;
    }
    this.router.navigateByUrl(this.auth.homeForRole(result.user.role));
  }
}
