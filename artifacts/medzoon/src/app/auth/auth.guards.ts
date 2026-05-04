import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { UserRole } from './auth.types';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) return true;
  router.navigateByUrl('/login');
  return false;
};

export const roleGuard = (...allowed: UserRole[]): CanActivateFn => {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const user = auth.user();
    if (!user) {
      router.navigateByUrl('/login');
      return false;
    }
    if (!allowed.includes(user.role)) {
      router.navigateByUrl(auth.homeForRole(user.role));
      return false;
    }
    return true;
  };
};

export const dashboardRedirectGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.user();
  if (!user) {
    router.navigateByUrl('/login');
    return false;
  }
  return router.parseUrl(auth.homeForRole(user.role));
};
