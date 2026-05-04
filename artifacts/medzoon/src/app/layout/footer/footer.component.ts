import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss',
})
export class FooterComponent {
  year = new Date().getFullYear();

  about = [
    { label: 'About Us', href: '#solutions' },
    { label: 'Our Doctors', href: '#team' },
    { label: 'Careers', href: '#contact' },
    { label: 'Press Kit', href: '#blog' },
  ];

  services = [
    { label: 'Medical Records', href: '#departments' },
    { label: 'Periodic Visits', href: '#process' },
    { label: 'Vaccination Tracking', href: '#departments' },
    { label: 'Compliance & Audit', href: '#security' },
  ];

  resources = [
    { label: 'Help Center', href: '#contact' },
    { label: 'Documentation', href: '#contact' },
    { label: 'API Reference', href: '#contact' },
    { label: 'Status Page', href: '#contact' },
  ];
}
