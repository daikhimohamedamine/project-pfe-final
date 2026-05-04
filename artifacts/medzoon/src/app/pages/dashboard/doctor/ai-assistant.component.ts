import { ChangeDetectionStrategy, Component, signal, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../../../shared/icon.component';
import { Drug } from '../../../core/models/models';
import { PrescriptionService } from './prescription.service';
import { BackendApiService } from '../../../core/api/backend-api.service';
import { GeminiService } from '../../../core/api/gemini.service';
import { firstValueFrom } from 'rxjs';

interface Message {
  role: 'user' | 'assistant';
  text?: string;
  suggestions?: { drug: Drug; reason: string; warning?: string }[];
  isDisclaimer?: boolean;
}

const DISCLAIMER_TEXT = "⚠️ Avertissement : Cet assistant IA est un outil d'aide à la décision réservé aux professionnels. Il ne remplace pas l'expertise du praticien et ne pose aucun diagnostic médical définitif.";

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [FormsModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ai-assistant.component.html',
  styleUrl: './ai-assistant.component.scss',
})
export class AiAssistantComponent {
  private rx = inject(PrescriptionService);
  private api = inject(BackendApiService);
  private gemini = inject(GeminiService);
  private drugs = signal<Drug[]>([]);

  open = signal(false);
  input = signal('');
  thinking = signal(false);
  messages = signal<Message[]>([
    {
      role: 'assistant',
      text: "Bonjour ! Je suis votre assistant MediCab. Je peux vous aider à naviguer dans l'application ou suggérer des traitements basés sur les symptômes décrits.",
    },
    {
      role: 'assistant',
      text: DISCLAIMER_TEXT,
      isDisclaimer: true
    }
  ]);

  badgeCount = computed(() => this.rx.items().length);

  toggle() { this.open.update((v) => !v); }
  constructor() {
    this.loadDrugs();
  }

  private async loadDrugs() {
    try {
      const res = await firstValueFrom(this.api.drugs());
      const rows = Array.isArray(res?.content) ? res.content : Array.isArray(res) ? res : [];
      this.drugs.set(rows.map((d: any) => ({
        set_id: d.setId ?? String(d.id),
        drug_name: d.drugName ?? '',
        generic_name: d.genericName ?? '',
        category: 'Respiratory',
        dosage: d.dosage ?? '',
        indications: d.indications ?? '',
        sicknesses: Array.isArray(d.sicknesses) ? d.sicknesses : [],
        image_lookup_url: d.imageLookupUrl ?? '',
      } as Drug)));
    } catch {}
  }

  send() {
    const text = this.input().trim();
    if (!text || this.thinking()) return;
    this.input.set('');
    this.messages.update((m) => [...m, { role: 'user', text }]);
    this.thinking.set(true);

    this.callAi(text);
  }

  private async callAi(text: string) {
    try {
      // Build history for the LLM
      const history = this.messages()
        .filter(m => !m.isDisclaimer && m.text !== text)
        .map(m => ({ 
          role: m.role, 
          content: m.text ?? "Recommandations de médicaments" 
        }));

      const responseText = await this.gemini.sendMessage(text, history);
      
      this.messages.update((m) => [...m, { role: 'assistant', text: responseText }]);
    } catch (err) {
      console.error('AI Error', err);
      this.messages.update((m) => [...m, { role: 'assistant', text: "Désolé, je rencontre une difficulté technique. Veuillez réessayer." }]);
    } finally {
      this.thinking.set(false);
    }
  }

  prescribe(drug: Drug) {
    this.rx.add(drug);
  }
}
