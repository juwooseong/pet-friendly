export type Species = 'DOG' | 'CAT';
export type SizeCategory = 'SMALL' | 'MEDIUM' | 'LARGE';

export interface Pet {
  id: string;
  userId: string;
  name: string;
  species: Species;
  breed: string;
  weightKg: number;
  sizeCategory: SizeCategory;
  ageYears: number;
  isVaccinated: boolean;
  photoUrl?: string;
  notes?: string;
  createdAt?: string;
}

export interface User {
  id: string;
  email: string;
  nickname: string;
  avatarUrl?: string;
  pets: Pet[];
  activePetId?: string;
}
