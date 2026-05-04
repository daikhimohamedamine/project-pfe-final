import { ChangeDetectionStrategy, Component } from '@angular/core';
import { HeroComponent } from '../../sections/hero/hero.component';
import { DepartmentsComponent } from '../../sections/departments/departments.component';
import { SolutionsComponent } from '../../sections/solutions/solutions.component';
import { StatsComponent } from '../../sections/stats/stats.component';
import { ProcessComponent } from '../../sections/process/process.component';
import { TeamComponent } from '../../sections/team/team.component';
import { SecurityComponent } from '../../sections/security/security.component';
import { MapComponent } from '../../sections/map/map.component';
import { TestimonialsComponent } from '../../sections/testimonials/testimonials.component';
import { CtaComponent } from '../../sections/cta/cta.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    HeroComponent,
    DepartmentsComponent,
    SolutionsComponent,
    StatsComponent,
    ProcessComponent,
    TeamComponent,
    SecurityComponent,
    MapComponent,
    TestimonialsComponent,
    CtaComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-hero/>
    <app-departments/>
    <app-solutions/>
    <app-stats/>
    <app-process/>
    <app-team/>
    <app-security/>
    <app-testimonials/>
    <app-cta/>
    <app-map/>
  `,
})
export class HomeComponent {}
