import type { SizeCategory } from './user';

export type PlaceCategory = 'CAFE' | 'HOTEL' | 'PARK' | 'HOSPITAL' | 'SALON';

export interface PetPolicy {
  maxWeightLimitKg: number | null;
  allowedSizes: SizeCategory[];
  isIndoorAllowed: boolean;
  isOutdoorAllowed: boolean;
  hasOffLeashZone: boolean;
  policyChecklist: string[];
}

export interface Place {
  id: string;
  publicDataId?: string;
  name: string;
  category: PlaceCategory;
  categoryName: string;
  address: string;
  latitude: number;
  longitude: number;
  phone: string;
  operatingHours: string;
  imageUrl: string;
  description: string;
  rating: number;
  reviewCount: number;
  facilities: string[];
  petPolicy: PetPolicy;
}

export type MatchStatus = 'PASS' | 'WARN' | 'DENY' | 'INFO';

export interface MatchResult {
  status: MatchStatus;
  message: string;
}
