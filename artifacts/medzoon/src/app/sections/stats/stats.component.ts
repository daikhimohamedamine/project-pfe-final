import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CounterDirective } from '../../shared/counter.directive';
import { RevealDirective } from '../../shared/reveal.directive';

interface Stat { value: number; suffix: string; label: string; }

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [CounterDirective, RevealDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './stats.component.html',
  styleUrl: './stats.component.scss',
})
export class StatsComponent {
  stats: Stat[] = [
    { value: 2400,  suffix: '+',  label: 'Employés suivis'            },
    { value: 12000, suffix: '+',  label: 'Consultations enregistrées' },
    { value: 98,    suffix: '%',  label: 'Couverture vaccinale'       },
    { value: 3,     suffix: '',   label: 'Profils d\'accès sécurisés' },
  ];
}
