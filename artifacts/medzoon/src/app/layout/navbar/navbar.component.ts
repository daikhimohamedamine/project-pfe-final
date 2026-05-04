import { ChangeDetectionStrategy, Component, HostListener, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { IconComponent } from '../../shared/icon.component';

interface NavItem { label: string; href: string; }

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [NgClass, RouterLink, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent {
  scrolled = signal(false);
  mobileOpen = signal(false);

  links: NavItem[] = [
    { label: 'Accueil', href: '#home' },
    { label: 'Services', href: '#departments' },
    { label: 'Rôles', href: '#security' },
    { label: 'Nous Trouver', href: '#location' },
    { label: 'Contact', href: '#footer' },
  ];

  @HostListener('window:scroll')
  onScroll() {
    this.scrolled.set(window.scrollY > 24);
  }

  toggleMobile() {
    this.mobileOpen.update((v) => !v);
  }

  closeMobile() {
    this.mobileOpen.set(false);
  }
}
