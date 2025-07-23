import { DaoBundleType } from '../enums';
import { LinkedEntity, Entity } from './basic';

export interface Dao extends LinkedEntity {
  // name: string;
  files: DaoFile[];
  licenseCode?: string;
}

export interface DaoFile extends LinkedEntity {
  order: number;
  metadata: MetadataItem[];
  type: DaoBundleType;
  file: DaoFileFileType;
  name?: string;
  referencedFile?: string | null;
  selected?: boolean;
}

interface DaoFileFileType {
  id: string;
  name: string;
}

interface MetadataItem extends Entity {
  value: string;
  type: string;
}
