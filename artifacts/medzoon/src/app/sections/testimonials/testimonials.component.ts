import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { IconComponent } from '../../shared/icon.component';
import { RevealDirective } from '../../shared/reveal.directive';

interface Testimonial {
  quote: string;
  name: string;
  role: string;
  photo: string;
}

@Component({
  selector: 'app-testimonials',
  standalone: true,
  imports: [IconComponent, RevealDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './testimonials.component.html',
  styleUrl: './testimonials.component.scss',
})
export class TestimonialsComponent {
  active = signal(0);
  items: Testimonial[] = [
    {
      quote: 'MediCab a transformé nos classeurs de dossiers papier en un tableau de bord vivant. Notre taux de complétion des visites annuelles est passé de 62% à 94% dès les six premiers mois.',
      name:  'Dr. Camille Dubois',
      role:  'Médecin du Travail · GroupeIndus SA',
      photo: 'https://images.unsplash.com/photo-1594824476967-48c8b964273f?auto=format&fit=crop&w=300&q=80',
    },
    {
      quote: 'Le moteur de rappels justifie à lui seul l\'utilisation. Nous ne manquons plus jamais un rappel de vaccin ou une visite de reprise — tout est automatique.',
      name:  'Léa Bernard',
      role:  'Coordinatrice Santé · MetroLogistics',
      photo: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=300&q=80',
    },
    {
      quote: 'La préparation aux audits prenait auparavant une semaine entière. Avec MediCab, nous générons tout le pack de conformité en un seul clic.',
      name:  'Marc Lefèvre',
      role:  'Directeur RH · Atelier Nord',
      photo: 'https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=300&q=80',
    },
  ];

  go(i: number) { this.active.set(i); }
}
