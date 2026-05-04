import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { NgSwitch, NgSwitchCase } from '@angular/common';

export type IconName =
  | 'shield'
  | 'lock'
  | 'bell'
  | 'heart'
  | 'stethoscope'
  | 'syringe'
  | 'brain'
  | 'tooth'
  | 'bone'
  | 'cardio'
  | 'eye'
  | 'pill'
  | 'document'
  | 'users'
  | 'check'
  | 'arrow-right'
  | 'phone'
  | 'mail'
  | 'pin'
  | 'calendar'
  | 'clock'
  | 'play'
  | 'menu'
  | 'close'
  | 'plus'
  | 'star'
  | 'chart'
  | 'sparkles'
  | 'facebook'
  | 'twitter'
  | 'instagram'
  | 'linkedin'
  | 'globe'
  | 'map'
  | 'folder'
  | 'quote'
  | 'briefcase'
  | 'edit'
  | 'camera';

@Component({
  selector: 'app-icon',
  standalone: true,
  imports: [NgSwitch, NgSwitchCase],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      [attr.width]="size"
      [attr.height]="size"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.8"
      stroke-linecap="round"
      stroke-linejoin="round"
      [attr.aria-hidden]="true"
    >
      <ng-container [ngSwitch]="name">
        <g *ngSwitchCase="'shield'"><path d="M12 2 4 5v7c0 5 3.5 9 8 10 4.5-1 8-5 8-10V5l-8-3Z"/><path d="m9 12 2 2 4-4"/></g>
        <g *ngSwitchCase="'lock'"><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 1 1 8 0v4"/></g>
        <g *ngSwitchCase="'bell'"><path d="M6 8a6 6 0 1 1 12 0c0 7 3 7 3 9H3c0-2 3-2 3-9Z"/><path d="M10 21a2 2 0 0 0 4 0"/></g>
        <g *ngSwitchCase="'heart'"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78Z"/></g>
        <g *ngSwitchCase="'stethoscope'"><path d="M4 3h2v6a4 4 0 0 0 8 0V3h2"/><path d="M14 11v3a4 4 0 0 0 4 4 3 3 0 0 0 0-6"/><circle cx="18" cy="9" r="2"/></g>
        <g *ngSwitchCase="'syringe'"><path d="m18 2 4 4"/><path d="m15 5 4 4"/><path d="M11.5 8.5 8 12"/><path d="M2 22l4-4"/><path d="M14 6 6 14l4 4 8-8z"/></g>
        <g *ngSwitchCase="'brain'"><path d="M12 5a3 3 0 0 0-3-3 3 3 0 0 0-3 3 3 3 0 0 0-3 3 3 3 0 0 0 1 2.2A3 3 0 0 0 4 13a3 3 0 0 0 2 2.8A3 3 0 0 0 9 19a3 3 0 0 0 3-2"/><path d="M12 5a3 3 0 0 1 3-3 3 3 0 0 1 3 3 3 3 0 0 1 3 3 3 3 0 0 1-1 2.2A3 3 0 0 1 20 13a3 3 0 0 1-2 2.8A3 3 0 0 1 15 19a3 3 0 0 1-3-2V5Z"/></g>
        <g *ngSwitchCase="'tooth'"><path d="M12 2C8 2 6 4 6 7c0 3 1 4 1 8s1 7 2 7 2-2 2-5c0-1 1-1 1 0 0 3 1 5 2 5s2-3 2-7 1-5 1-8c0-3-2-5-5-5Z"/></g>
        <g *ngSwitchCase="'bone'"><path d="M17 10c.7-.7 1-1.6 1-2.5a3.5 3.5 0 0 0-7 0V8c0 .9-.5 1.7-1.3 2.1L4.4 13.4A3 3 0 0 0 7 18.6l3.3-1.6c.4-.2.9-.2 1.3 0l3.3 1.6a3 3 0 0 0 2.6-5.2l-1-.6c-.4-.2-.6-.6-.6-1L16 10Z"/></g>
        <g *ngSwitchCase="'cardio'"><path d="M3 12h3l2-6 4 12 2-6h7"/></g>
        <g *ngSwitchCase="'eye'"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></g>
        <g *ngSwitchCase="'pill'"><rect x="2" y="9" width="20" height="6" rx="3" transform="rotate(-45 12 12)"/><path d="m9 9 6 6"/></g>
        <g *ngSwitchCase="'document'"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M9 13h6"/><path d="M9 17h4"/></g>
        <g *ngSwitchCase="'users'"><circle cx="9" cy="8" r="3"/><path d="M3 21v-1a6 6 0 0 1 12 0v1"/><circle cx="17" cy="9" r="2.5"/><path d="M14 21v-1a4 4 0 0 1 7-2.7"/></g>
        <g *ngSwitchCase="'check'"><path d="M5 12l5 5 9-11"/></g>
        <g *ngSwitchCase="'arrow-right'"><path d="M5 12h14"/><path d="m13 6 6 6-6 6"/></g>
        <g *ngSwitchCase="'phone'"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.13.96.37 1.9.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.91.33 1.85.57 2.81.7A2 2 0 0 1 22 16.92Z"/></g>
        <g *ngSwitchCase="'mail'"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/></g>
        <g *ngSwitchCase="'pin'"><path d="M12 22s8-7 8-13a8 8 0 1 0-16 0c0 6 8 13 8 13Z"/><circle cx="12" cy="9" r="3"/></g>
        <g *ngSwitchCase="'calendar'"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18"/><path d="M8 3v4"/><path d="M16 3v4"/></g>
        <g *ngSwitchCase="'clock'"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></g>
        <g *ngSwitchCase="'play'"><path d="M7 5v14l12-7-12-7Z"/></g>
        <g *ngSwitchCase="'menu'"><path d="M4 6h16"/><path d="M4 12h16"/><path d="M4 18h16"/></g>
        <g *ngSwitchCase="'close'"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></g>
        <g *ngSwitchCase="'plus'"><path d="M12 5v14"/><path d="M5 12h14"/></g>
        <g *ngSwitchCase="'star'"><path d="m12 3 2.6 5.6 6.4.6-4.8 4.4 1.4 6.4L12 17l-5.6 3 1.4-6.4L3 9.2l6.4-.6L12 3Z" fill="currentColor"/></g>
        <g *ngSwitchCase="'chart'"><path d="M3 3v18h18"/><path d="m7 15 4-4 3 3 5-6"/></g>
        <g *ngSwitchCase="'sparkles'"><path d="M12 3v4"/><path d="M12 17v4"/><path d="M3 12h4"/><path d="M17 12h4"/><path d="m6 6 2 2"/><path d="m16 16 2 2"/><path d="m6 18 2-2"/><path d="m16 8 2-2"/></g>
        <g *ngSwitchCase="'facebook'"><path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z"/></g>
        <g *ngSwitchCase="'twitter'"><path d="M22 5.8a8.5 8.5 0 0 1-2.4.7 4.2 4.2 0 0 0 1.8-2.3 8.4 8.4 0 0 1-2.6 1A4.2 4.2 0 0 0 11 8.6a12 12 0 0 1-8.6-4.4 4.2 4.2 0 0 0 1.3 5.6 4.2 4.2 0 0 1-1.9-.5v.1a4.2 4.2 0 0 0 3.4 4.1 4.2 4.2 0 0 1-1.9.1 4.2 4.2 0 0 0 4 2.9A8.5 8.5 0 0 1 2 18.3a12 12 0 0 0 6.5 1.9c7.8 0 12-6.5 12-12.1v-.6A8.5 8.5 0 0 0 22 5.8Z"/></g>
        <g *ngSwitchCase="'instagram'"><rect x="3" y="3" width="18" height="18" rx="5"/><circle cx="12" cy="12" r="4"/><circle cx="17.5" cy="6.5" r="1" fill="currentColor"/></g>
        <g *ngSwitchCase="'linkedin'"><rect x="3" y="3" width="18" height="18" rx="3"/><path d="M8 10v8"/><circle cx="8" cy="7" r="1"/><path d="M12 18v-5a3 3 0 0 1 6 0v5"/></g>
        <g *ngSwitchCase="'globe'"><circle cx="12" cy="12" r="10"/><path d="M2 12h20"/><path d="M12 2a15.3 15.3 0 0 1 0 20 15.3 15.3 0 0 1 0-20"/></g>
        <g *ngSwitchCase="'map'"><path d="M3 6l6-3 6 3 6-3v15l-6 3-6-3-6 3V6z"/><path d="M9 3v15"/><path d="M15 6v15"/></g>
        <g *ngSwitchCase="'folder'"><path d="M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"/></g>
        <g *ngSwitchCase="'quote'"><path d="M7 7h4v4H8c0 2 1 3 3 3v3c-3 0-6-2-6-6V7zm9 0h4v4h-3c0 2 1 3 3 3v3c-3 0-6-2-6-6V7z" fill="currentColor" stroke="none"/></g>
        <g *ngSwitchCase="'edit'"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></g>
        <g *ngSwitchCase="'camera'"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></g>
      </ng-container>
    </svg>
  `,
})
export class IconComponent {
  @Input({ required: true }) name!: IconName;
  @Input() size: number = 22;
}
