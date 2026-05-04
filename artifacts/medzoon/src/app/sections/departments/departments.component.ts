import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IconComponent, IconName } from '../../shared/icon.component';
import { RevealDirective } from '../../shared/reveal.directive';

interface Dept {
  id: string;
  name: string;
  icon: IconName;
  description: string;
}

@Component({
  selector: 'app-departments',
  standalone: true,
  imports: [IconComponent, RevealDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './departments.component.html',
  styleUrl: './departments.component.scss',
})
export class DepartmentsComponent {
  departments: Dept[] = [
    {
      id: '01',
      name: 'Consultations Médicales',
      icon: 'stethoscope',
      description: 'Enregistrement et suivi de toutes les visites médicales : embauche, périodiques, soins et reprises.'
    },
    {
      id: '02',
      name: 'Vaccinations',
      icon: 'shield',
      description: 'Gestion du calendrier vaccinal des employés avec rappels automatiques et suivi de couverture.'
    },
    {
      id: '03',
      name: 'Dossiers Médicaux',
      icon: 'folder',
      description: 'Centralisation sécurisée de l\'historique médical complet de chaque employé COFICAB.'
    },
    {
      id: '04',
      name: 'Alertes & Rappels',
      icon: 'bell',
      description: 'Notifications automatiques pour les visites périodiques et les rappels de vaccination.'
    },
  ];
}
