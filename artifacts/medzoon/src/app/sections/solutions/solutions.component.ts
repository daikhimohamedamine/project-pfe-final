import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IconComponent } from '../../shared/icon.component';
import { RevealDirective } from '../../shared/reveal.directive';

@Component({
  selector: 'app-solutions',
  standalone: true,
  imports: [IconComponent, RevealDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './solutions.component.html',
  styleUrl: './solutions.component.scss',
})
export class SolutionsComponent {
  bullets = [
    'Centralisation des infos personnelles et antécédents — recherche instantanée.',
    'Saisie de tous types de visites : embauche, périodique, soin, reprise.',
    'Rappels annuels automatisés avec notifications par email et application.',
    'Accès granulaire par rôle pour médecins du travail et coordinatrices.',
  ];

  hours = [
    { day: 'Lun — Ven', time: '08h00 – 17h00' },
    { day: 'Sam',       time: 'Fermé'  },
    { day: 'Dim',       time: 'Fermé'  },
  ];
}
