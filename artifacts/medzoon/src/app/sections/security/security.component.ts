import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IconComponent, IconName } from '../../shared/icon.component';
import { RevealDirective } from '../../shared/reveal.directive';

interface RoleCard { icon: IconName; title: string; body: string; color: string; }

@Component({
  selector: 'app-security',
  standalone: true,
  imports: [IconComponent, RevealDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './security.component.html',
  styleUrl: './security.component.scss',
})
export class SecurityComponent {
  roleCards: RoleCard[] = [
    {
      icon: 'stethoscope',
      title: 'Médecin du Travail',
      color: '#1e3a5f',
      body: 'Accès complet aux dossiers médicaux, consultations, examens et prescriptions de tous les employés.'
    },
    {
      icon: 'shield',
      title: 'Coordinatrice Santé',
      color: '#06b6d4',
      body: 'Gestion des dossiers employés, planification des visites, rappels périodiques et suivi administratif.'
    },
    {
      icon: 'lock',
      title: 'Administrateur',
      color: '#f97316',
      body: 'Gestion des utilisateurs, journaux d\'audit, paramètres de sécurité et conformité RGPD.'
    }
  ];
}
