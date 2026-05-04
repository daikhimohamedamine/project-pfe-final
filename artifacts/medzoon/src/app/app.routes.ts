import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './pages/login/login.component';
import { DashboardShellComponent } from './pages/dashboard/shared/dashboard-shell.component';
import { AdminDashboardComponent } from './pages/dashboard/admin/admin-dashboard.component';
import { AdminUsersComponent } from './pages/dashboard/admin/users.component';
import { AdminAuditComponent } from './pages/dashboard/admin/audit.component';
import { AdminSettingsComponent } from './pages/dashboard/admin/settings.component';
import { CoordDashboardComponent } from './pages/dashboard/coordinatrice/coord-dashboard.component';
import { CoordEmployeesComponent } from './pages/dashboard/coordinatrice/employees.component';
import { CoordScheduleComponent } from './pages/dashboard/coordinatrice/schedule.component';
import { CoordRemindersComponent } from './pages/dashboard/coordinatrice/reminders.component';
import { DoctorDashboardComponent } from './pages/dashboard/doctor/doctor-dashboard.component';
import { PatientsComponent } from './pages/dashboard/doctor/patients.component';
import { ConsultsV2Component as MedConsultsComponent } from './pages/dashboard/doctor/med-consults.component';
import { VaccinesComponent } from './pages/dashboard/doctor/vaccines.component';
import { DrugsComponent } from './pages/dashboard/doctor/drugs.component';
import { MedExamsComponent } from './pages/dashboard/doctor/med-exams.component';
import { DossierMedicalComponent } from './pages/dashboard/doctor/dossier';
import { ProfilePageComponent } from './pages/dashboard/shared/profile-page.component';
import { SecurityPageComponent } from './pages/dashboard/shared/security-page.component';
import { authGuard, dashboardRedirectGuard, roleGuard } from './auth/auth.guards';

export const routes: Routes = [
  { path: '', component: HomeComponent, pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    component: DashboardShellComponent,
    children: [
      { path: '', canActivate: [dashboardRedirectGuard], children: [] },

      { path: 'admin',          canActivate: [roleGuard('admin')], component: AdminDashboardComponent },
      { path: 'admin/users',    canActivate: [roleGuard('admin')], component: AdminUsersComponent },
      { path: 'admin/dossiers', canActivate: [roleGuard('admin')], component: DossierMedicalComponent },
      { path: 'admin/dossiers/:id', canActivate: [roleGuard('admin')], component: DossierMedicalComponent },
      { path: 'admin/schedule', canActivate: [roleGuard('admin')], component: CoordScheduleComponent },
      { path: 'admin/consults', canActivate: [roleGuard('admin')], component: MedConsultsComponent },
      { path: 'admin/audit',    canActivate: [roleGuard('admin')], component: AdminAuditComponent },
      { path: 'admin/settings', canActivate: [roleGuard('admin')], component: AdminSettingsComponent },
      { path: 'profile',        canActivate: [roleGuard('admin', 'coordinatrice', 'medecin')], component: ProfilePageComponent },
      { path: 'security',       canActivate: [roleGuard('admin', 'coordinatrice', 'medecin')], component: SecurityPageComponent },

      { path: 'coordinatrice',           canActivate: [roleGuard('coordinatrice')], component: CoordDashboardComponent },
      { path: 'coordinatrice/employees', canActivate: [roleGuard('coordinatrice')], component: CoordEmployeesComponent },
      { path: 'coordinatrice/schedule',  canActivate: [roleGuard('coordinatrice')], component: CoordScheduleComponent },
      { path: 'coordinatrice/reminders', canActivate: [roleGuard('coordinatrice')], component: CoordRemindersComponent },

      { path: 'doctor',          canActivate: [roleGuard('medecin')], component: DoctorDashboardComponent },
      { path: 'doctor/patients', canActivate: [roleGuard('medecin')], component: PatientsComponent },
      { path: 'doctor/consults', canActivate: [roleGuard('medecin')], component: MedConsultsComponent },
      { path: 'doctor/exams',    canActivate: [roleGuard('medecin')], component: MedExamsComponent },
      { path: 'doctor/vaccines', canActivate: [roleGuard('medecin')], component: VaccinesComponent },
      { path: 'doctor/drugs',    canActivate: [roleGuard('medecin')], component: DrugsComponent },
      { path: 'doctor/dossiers', canActivate: [roleGuard('medecin')], component: DossierMedicalComponent },
      { path: 'doctor/dossiers/:id', canActivate: [roleGuard('medecin')], component: DossierMedicalComponent },
    ],
  },
  { path: '**', redirectTo: '' },
];
