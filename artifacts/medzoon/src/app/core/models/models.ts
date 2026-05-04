export type EmployeeStatus = 'Active' | 'Archived';

export interface EmployeeListItem {
  id: string;
  firstName: string;
  lastName: string;
  birthDate?: string;
  position?: string;
  department?: string;
  hireDate?: string;
  lastVisit?: string;
  status: EmployeeStatus;
  avatar?: string;
  vaccines?: number;
  consultations?: number;
  phone?: string;
  email?: string;
  gouvernorat?: string;
  matricule?: string;
}

export type ConsultationType = 'Embauche' | 'Périodique' | 'Reprise' | 'Soin' | 'Spontanée';
export type ConsultationConclusion = 'Apte' | 'Apte avec restrictions' | 'Inapte temporaire' | 'À revoir';

export interface ConsultationListItem {
  id: string;
  employeeId: string;
  employeeName: string;
  doctor: string;
  date: string;
  time?: string;
  type: ConsultationType;
  conclusion?: ConsultationConclusion;
  weight?: number | string;
  bp?: string;
  status?: string;
}

export type DrugCategory = 'Antibiotic' | 'Analgesic' | 'Respiratory' | 'Cardiovascular' | 'Gastric' | 'Vitamin';

export interface Drug {
  set_id: string;
  drug_name: string;
  generic_name: string;
  dosage: string;
  indications: string;
  sicknesses: string[];
  image_lookup_url: string;
  category: DrugCategory;
}

export type AuditLevel = 'ok' | 'warn' | 'danger';
export interface AuditEvent {
  id: string;
  actor: string;
  actorRole: 'admin' | 'coordinatrice' | 'medecin' | 'system';
  action: string;
  target: string;
  ip: string;
  time: string;
  level: AuditLevel;
  medecinContext?: string;
  userId?: number;
}

