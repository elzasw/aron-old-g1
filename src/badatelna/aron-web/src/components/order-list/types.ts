export interface ArchivalieInfo {
  cisloNad: number;
  dilciList?: number;
  ukladaciJednotka?: string;
  aipId?: string;
  archivniIdentifikator?: string;
  jineOznacei?: string;
  popis?: string;
  formNazev?: string;
  uplnyPopis?: string;
  url?: string;
  datace?: string;
}

export interface InstitutionInfo {
  name: string;
  number: number;
}

export interface OrderItemInfo {
  archivalie: ArchivalieInfo;
  institution: InstitutionInfo;
}

export enum ZpusobZpristupneni {
  NAHLIZENI = "NAHLIZENI",
  REPLIKA = "REPLIKA",
  OVERENI_SHODY = "OVERENI_SHODY",
}

export interface OrderItemInfoEx extends OrderItemInfo {
  apuId: string;
  type: ZpusobZpristupneni;
}
