import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IconComponent } from '../../shared/icon.component';
import { RevealDirective } from '../../shared/reveal.directive';

interface Doctor {
  name: string;
  role: string;
  photo: string;
}

@Component({
  selector: 'app-team',
  standalone: true,
  imports: [IconComponent, RevealDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './team.component.html',
  styleUrl: './team.component.scss',
})
export class TeamComponent {
  doctors: Doctor[] = [
    { name: 'Dr. Helena Marsh',  role: 'Médecine du Travail',
      photo: 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&w=600&q=80' },
    { name: 'Dr. Idris Okafor',  role: 'Chef Cardiologie',
      photo: 'https://images.unsplash.com/photo-1612531385446-f7e6d131e1d0?auto=format&fit=crop&w=600&q=80' },
    { name: 'Dr. Aria Nakamura', role: 'Coordinatrice Médicale',
      photo: 'https://images.unsplash.com/photo-1551601651-2a8555f1a136?auto=format&fit=crop&w=600&q=80' },
    { name: 'Dr. Samuel Reyes',  role: 'Neurologie & Réflexes',
      photo: 'https://images.unsplash.com/photo-1622253692010-333f2da6031d?auto=format&fit=crop&w=600&q=80' },
  ];
}
