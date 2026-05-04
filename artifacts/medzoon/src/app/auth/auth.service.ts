import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { inject } from '@angular/core';
import { ROLE_HOMES, User, UserRole } from './auth.types';
import { BackendApiService } from '../core/api/backend-api.service';
import { firstValueFrom } from 'rxjs';

interface DemoAccount extends User {
  password: string;
}

const STORAGE_KEY = 'medzoon.session.v1';
const TOKEN_KEY = 'medzoon.auth.token';
@Injectable({ providedIn: 'root' })
export class AuthService {
  private router = inject(Router);
  private api = inject(BackendApiService);
  private _user = signal<User | null>(this.loadFromStorage());

  user = this._user.asReadonly();
  isAuthenticated = computed(() => this._user() !== null);
  role = computed<UserRole | null>(() => this._user()?.role ?? null);

  async signIn(email: string, password: string): Promise<{ ok: true; user: User; requires2fa?: boolean } | { ok: false; error: string }> {
    try {
      const res = await firstValueFrom(this.api.login({ email, password }));
      if (res.requires2fa) {
        return { ok: true, user: null as any, requires2fa: true };
      }
      const user: User = {
        id: email,
        email,
        firstName: res.prenom ?? '',
        lastName: res.nom ?? '',
        role: res.role === 'ADMIN' ? 'admin' : res.role === 'COORDINATRICE' ? 'coordinatrice' : 'medecin',
        title: res.role === 'ADMIN' ? 'Administrateur Plateforme' : res.role === 'COORDINATRICE' ? 'Coordinatrice Santé' : 'Médecin du Travail',
        organization: 'Medzoon',
        avatar: '',
        token: res.accessToken,
        assignedMedecinId: res.assignedMedecinId || undefined
      };
      this.setUser(user);
      localStorage.setItem(TOKEN_KEY, res.accessToken);
      return { ok: true, user };
    } catch (err: any) {
      const msg = err?.error?.message || 'Email ou mot de passe invalide.';
      return { ok: false, error: msg };
    }
  }

  async verify2fa(email: string, code: string): Promise<{ ok: true; user: User } | { ok: false; error: string }> {
    try {
      const res = await firstValueFrom(this.api.verify2fa(email, code));
      const user: User = {
        id: email,
        email,
        firstName: res.prenom ?? '',
        lastName: res.nom ?? '',
        role: res.role === 'ADMIN' ? 'admin' : res.role === 'COORDINATRICE' ? 'coordinatrice' : 'medecin',
        title: res.role === 'ADMIN' ? 'Administrateur Plateforme' : res.role === 'COORDINATRICE' ? 'Coordinatrice Santé' : 'Médecin du Travail',
        organization: 'Medzoon',
        avatar: '',
        token: res.accessToken,
        assignedMedecinId: res.assignedMedecinId || undefined
      };
      this.setUser(user);
      localStorage.setItem(TOKEN_KEY, res.accessToken);
      return { ok: true, user };
    } catch (err: any) {
      const msg = err?.error?.message || 'Code de vérification invalide.';
      return { ok: false, error: msg };
    }
  }

  signOut(): void {
    this._user.set(null);
    try { localStorage.removeItem(STORAGE_KEY); } catch { /* ignore */ }
    try { localStorage.removeItem(TOKEN_KEY); } catch { /* ignore */ }
    this.router.navigateByUrl('/');
  }

  async updateProfile(profile: { firstName: string; lastName: string; phone: string }): Promise<void> {
    const current = this._user();
    if (!current) return;

    try {
      // 1. Sync with backend
      await firstValueFrom(this.api.updateUserProfile({
        prenom: profile.firstName,
        nom: profile.lastName,
        telephone: profile.phone
      }));

      // 2. Update local state
      const updated: User = { ...current, ...profile };
      this._user.set(updated);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    } catch (err) {
      console.error('Failed to update profile on backend', err);
      throw err;
    }
  }

  private setUser(user: User) {
    this._user.set(user);
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(user)); } catch { /* ignore */ }
  }

  homeForRole(role: UserRole): string {
    return ROLE_HOMES[role];
  }

  private loadFromStorage(): User | null {
    if (typeof localStorage === 'undefined') return null;
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as User) : null;
    } catch {
      return null;
    }
  }
}
