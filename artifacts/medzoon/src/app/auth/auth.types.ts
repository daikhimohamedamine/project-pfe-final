export type UserRole = 'admin' | 'coordinatrice' | 'medecin';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: UserRole;
  title: string;
  avatar: string;
  organization: string;
  token?: string;
  assignedMedecinId?: number;
}

export const ROLE_LABELS: Record<UserRole, string> = {
  admin: 'Administrateur',
  coordinatrice: 'Coordinatrice Santé',
  medecin: 'Médecin du Travail',
};

export const ROLE_HOMES: Record<UserRole, string> = {
  admin: '/dashboard/admin',
  coordinatrice: '/dashboard/coordinatrice',
  medecin: '/dashboard/doctor',
};
