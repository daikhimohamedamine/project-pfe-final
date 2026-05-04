import { Injectable, signal } from '@angular/core';
import { Drug } from '../../../core/models/models';

export interface PrescribedDrug {
  drug: Drug;
  posology: string;
}

@Injectable({ providedIn: 'root' })
export class PrescriptionService {
  private _items = signal<PrescribedDrug[]>([]);
  items = this._items.asReadonly();

  add(drug: Drug, posology = '') {
    if (this._items().some((p) => p.drug.set_id === drug.set_id)) return;
    this._items.update((arr) => [...arr, { drug, posology }]);
  }
  setPosology(setId: string, posology: string) {
    this._items.update((arr) => arr.map((p) => p.drug.set_id === setId ? { ...p, posology } : p));
  }
  remove(setId: string) {
    this._items.update((arr) => arr.filter((p) => p.drug.set_id !== setId));
  }
  treatmentPayload() {
    return this._items().map((p) => ({
      drug_id: p.drug.set_id,
      drug_name: p.drug.drug_name,
      generic_name: p.drug.generic_name,
      dosage: p.drug.dosage,
      posologie: p.posology || '',
    }));
  }
  clear() { this._items.set([]); }
}
