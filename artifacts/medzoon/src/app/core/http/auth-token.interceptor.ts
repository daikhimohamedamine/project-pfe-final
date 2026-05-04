import { HttpInterceptorFn } from '@angular/common/http';

const TOKEN_KEY = 'medzoon.auth.token';

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (!token) return next(req);
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
