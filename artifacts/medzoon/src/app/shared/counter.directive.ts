import {
  AfterViewInit,
  Directive,
  ElementRef,
  Input,
  OnDestroy,
  inject,
} from '@angular/core';

@Directive({
  selector: '[appCounter]',
  standalone: true,
})
export class CounterDirective implements AfterViewInit, OnDestroy {
  @Input({ required: true }) appCounter!: number;
  @Input() suffix: string = '';
  @Input() duration: number = 1800;

  private observer?: IntersectionObserver;
  private started = false;
  private el = inject(ElementRef<HTMLElement>);

  ngAfterViewInit(): void {
    const node = this.el.nativeElement;
    node.textContent = '0' + this.suffix;

    if (typeof IntersectionObserver === 'undefined') {
      this.run();
      return;
    }

    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting && !this.started) {
            this.started = true;
            this.run();
            this.observer?.disconnect();
          }
        }
      },
      { threshold: 0.4 },
    );
    this.observer.observe(node);
  }

  private run() {
    const node = this.el.nativeElement;
    const target = this.appCounter;
    const start = performance.now();
    const animate = (now: number) => {
      const elapsed = now - start;
      const t = Math.min(1, elapsed / this.duration);
      const eased = 1 - Math.pow(1 - t, 3);
      const value = Math.round(target * eased);
      node.textContent = value.toLocaleString() + this.suffix;
      if (t < 1) requestAnimationFrame(animate);
    };
    requestAnimationFrame(animate);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
