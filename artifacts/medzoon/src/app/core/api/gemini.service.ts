import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from './api.config';

@Injectable({
  providedIn: 'root'
})
export class GeminiService {
  // On utilise le backend comme proxy pour sécuriser la clé API et profiter des fonctions avancées
  private readonly apiUrl = `${API_BASE_URL}/ai/chat`;

  constructor(private http: HttpClient) {}

  async sendMessage(message: string, history: any[] = []): Promise<string> {
    try {
      // Préparation de l'historique pour le format attendu par le backend
      const formattedHistory = history.map(h => ({
        role: h.role,
        content: h.content
      }));

      // Appel au backend
      const response = await firstValueFrom(
        this.http.post<{ response: string }>(this.apiUrl, { 
          message: message,
          history: formattedHistory
        })
      );

      return response.response;
    } catch (error: any) {
      console.error("Erreur lors de l'appel à l'assistant via le backend:", error);
      
      if (error.status === 429) {
        return "Désolé, l'assistant a atteint sa limite de messages pour le moment. Veuillez réessayer dans quelques minutes.";
      }
      
      return "Désolé, je rencontre une difficulté technique. Pouvez-vous répéter votre question ?";
    }
  }
}
