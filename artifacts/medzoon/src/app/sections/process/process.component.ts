import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IconComponent, IconName } from '../../shared/icon.component';
import { RevealDirective } from '../../shared/reveal.directive';

interface Step { num: string; title: string; description: string; icon: IconName; }

@Component({
  selector: 'app-process',
  standalone: true,
  imports: [IconComponent, RevealDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './process.component.html',
  styleUrl: './process.component.scss',
})
export class ProcessComponent {
  steps: Step[] = [
    { num: '01', title: 'Intégration',       icon: 'users',        description: 'Ajoutez un employé avec ses informations complètes et antécédents en un seul formulaire.' },
    { num: '02', title: 'Examen',           icon: 'stethoscope',  description: 'Saisissez les visites d\'embauche, périodiques ou de reprise avec des modèles structurés.' },
    { num: '03', title: 'Suivi Vaccinal',   icon: 'syringe',      description: 'Planifiez et suivez les vaccinations avec des alertes automatiques de retard.' },
    { num: '04', title: 'Conformité',       icon: 'shield',       description: 'Générez des rapports prêts pour l\'audit pour le RGPD et vos ressources humaines internes.' },
  ];
}
