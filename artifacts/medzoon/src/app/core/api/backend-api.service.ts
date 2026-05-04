import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from './api.config';
import { Observable, of as rxjsOf } from 'rxjs';

export interface Appointment {
  id: number;
  dateDebut: string;
  employeeId: number;
  typeVisite?: string;
  statut: string;
}

export interface Employee {
  id: number;
  prenom: string;
  nom: string;
}

export interface Reminder {
  id: number;
  message: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class BackendApiService {
  private http = inject(HttpClient);

  login(payload: { email: string; password: string }) {
    return this.http.post<{ accessToken: string; refreshToken: string; role: string; nom: string; prenom: string; requires2fa: boolean; assignedMedecinId?: number; avatarUrl?: string }>(
      `${API_BASE_URL}/auth/login`,
      payload,
    );
  }

  verify2fa(email: string, code: string) {
    return this.http.post<{ accessToken: string; refreshToken: string; role: string; nom: string; prenom: string; assignedMedecinId?: number; avatarUrl?: string }>(
      `${API_BASE_URL}/auth/verify-2fa`,
      { email, code }
    );
  }

  employees(): Observable<PaginatedResponse<Employee>> {
    return this.http.get<PaginatedResponse<Employee>>(`${API_BASE_URL}/employees`);
  }

  createEmployee(payload: any) {
    return this.http.post<any>(`${API_BASE_URL}/employees`, payload);
  }

  archiveEmployee(id: string | number) {
    return this.http.patch<any>(`${API_BASE_URL}/employees/${id}/archive`, {});
  }

  employeeById(id: string | number) {
    return this.http.get<any>(`${API_BASE_URL}/employees/${id}`);
  }

  updateEmployee(id: string | number, payload: any) {
    return this.http.put<any>(`${API_BASE_URL}/employees/${id}`, payload);
  }

  deleteEmployee(id: string | number) {
    return this.http.delete<any>(`${API_BASE_URL}/employees/${id}`);
  }

  drugs(query = '') {
    return this.http.get<any>(`${API_BASE_URL}/drugs`, { params: { query } });
  }

  consultations(employeeId?: string | number) {
    const params: any = {};
    if (employeeId != null) params.employeeId = employeeId;
    return this.http.get<any>(`${API_BASE_URL}/consultations`, { params });
  }

  createConsultation(payload: any) {
    return this.http.post<any>(`${API_BASE_URL}/consultations`, payload);
  }

  consultationDocuments(consultationId: string | number) {
    return this.http.get<any>(`${API_BASE_URL}/documents`, { params: { consultationId } });
  }

  uploadDocument(file: File, employeeId: string | number, consultationId?: string | number) {
    const form = new FormData();
    form.append('file', file);
    form.append('employeeId', String(employeeId));
    if (consultationId != null) form.append('consultationId', String(consultationId));
    return this.http.post<any>(`${API_BASE_URL}/documents/upload`, form);
  }

  downloadDocument(id: string | number) {
    return this.http.get(`${API_BASE_URL}/documents/${id}/download`, { responseType: 'blob' });
  }

  vaccinations(employeeId: string | number) {
    return this.http.get<any>(`${API_BASE_URL}/reminders`, { params: { employeeId: String(employeeId) } });
  }

  documents(employeeId: string | number) {
    return this.http.get<any>(`${API_BASE_URL}/documents`, { params: { employeeId } });
  }

  deleteDocument(id: string | number) {
    return this.http.delete<void>(`${API_BASE_URL}/documents/${id}`);
  }

  appointments(from?: string, to?: string): Observable<PaginatedResponse<Appointment>> {
    const params: any = {};
    if (from) params.from = from;
    if (to) params.to = to;
    return this.http.get<PaginatedResponse<Appointment>>(`${API_BASE_URL}/appointments`, { params });
  }

  createAppointment(payload: any) {
    return this.http.post<any>(`${API_BASE_URL}/appointments`, payload);
  }

  cancelAppointment(id: string | number) {
    return this.http.delete<any>(`${API_BASE_URL}/appointments/${id}`);
  }

  updateAppointment(id: string | number, payload: any) {
    return this.http.put<any>(`${API_BASE_URL}/appointments/${id}`, payload);
  }

  reminders(from?: string, to?: string): Observable<PaginatedResponse<Reminder>> {
    const params: any = {};
    if (from) params.from = from;
    if (to) params.to = to;
    return this.http.get<PaginatedResponse<Reminder>>(`${API_BASE_URL}/reminders`, { params });
  }

  createReminder(payload: any) {
    return this.http.post<any>(`${API_BASE_URL}/reminders`, payload);
  }

  deleteReminder(id: string | number) {
    return this.http.delete<any>(`${API_BASE_URL}/reminders/${id}`);
  }

  createManualReminder(payload: any) {
    return this.http.post<any>(`${API_BASE_URL}/reminders/manual`, payload);
  }

  sendReminder(id: string | number) {
    return this.http.put<any>(`${API_BASE_URL}/reminders/${id}/send`, {});
  }

  notifications() {
    return this.http.get<any>(`${API_BASE_URL}/notifications`);
  }

  adminUsers() {
    return this.http.get<any>(`${API_BASE_URL}/admin/users`);
  }

  adminStats() {
    return this.http.get<any>(`${API_BASE_URL}/admin/stats`);
  }

  createAdminUser(payload: any) {
    return this.http.post<any>(`${API_BASE_URL}/admin/users`, payload);
  }

  updateAdminUser(id: string | number, payload: any) {
    return this.http.put<any>(`${API_BASE_URL}/admin/users/${id}`, payload);
  }

  disableAdminUser(id: string | number) {
    return this.http.delete<any>(`${API_BASE_URL}/admin/users/${id}`);
  }

  adminAuditLogs() {
    return this.http.get<any>(`${API_BASE_URL}/admin/audit-logs`);
  }

  importDrugsCsv(file: File, clearBefore = false) {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<any>(`${API_BASE_URL}/admin/drugs/import`, form, {
      params: { clearBefore },
    });
  }

  generateReport(payload: any) {
    return this.http.post<any>(`${API_BASE_URL}/reports/generate`, payload);
  }

  downloadReport(id: string) {
    return this.http.get(`${API_BASE_URL}/reports/${id}/download`, { responseType: 'blob' });
  }

  aiRecommend(payload: { employeeId: number; symptoms: string }) {
    return this.http.post<any>(`${API_BASE_URL}/ai/recommend`, payload);
  }

  aiChat(message: string, history: any[]) {
    return this.http.post<any>(`${API_BASE_URL}/ai/chat`, { message, history });
  }

  updateUserProfile(payload: any) {
    return this.http.put<any>(`${API_BASE_URL}/users/me`, payload);
  }

  changePassword(payload: any) {
    return this.http.put<any>(`${API_BASE_URL}/auth/change-password`, payload);
  }

  // Doctor Library
  doctorLibraryDocs(categorie?: string) {
    const params: any = {};
    if (categorie) params.categorie = categorie;
    return this.http.get<any[]>(`${API_BASE_URL}/doctor-library`, { params });
  }

  uploadDoctorLibraryDoc(file: File, categorie: string, description?: string) {
    const form = new FormData();
    form.append('file', file);
    form.append('categorie', categorie);
    if (description) form.append('description', description);
    return this.http.post<any>(`${API_BASE_URL}/doctor-library/upload`, form);
  }

  downloadDoctorLibraryDoc(id: string | number) {
    return this.http.get(`${API_BASE_URL}/doctor-library/download/${id}`, { responseType: 'blob' });
  }

  deleteDoctorLibraryDoc(id: string | number) {
    return this.http.delete<void>(`${API_BASE_URL}/doctor-library/${id}`);
  }

  uploadAvatar(file: File) {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<{ url: string }>(`${API_BASE_URL}/doctor-library/avatar`, form);
  }

  // Admin User Activity
  adminUserActivity(id: string | number) {
    return this.http.get<any>(`${API_BASE_URL}/admin/users/${id}/activity`);
  }

  medecins(): Observable<any[]> {
    return this.http.get<any[]>(`${API_BASE_URL}/users/medecins`);
  }
}
