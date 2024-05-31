import { ApuPartItemDataType, ApuType, ApuPartViewType } from '../enums';
import { Entity, LinkedEntity } from './basic';
import { Dao } from './dao';

export interface ApuEntity extends LinkedEntity {
  name: string;
  description: string;
  published: boolean;
  source: ApuSource;
  type: ApuType;
  childCnt: number;
  depth: number;
  order: number;
  pos: number;
  parent?: ApuEntity;
  parts?: ApuPart[];
  attachments?: ApuAttachment[];
  digitalObjects?: Dao[];
}

export interface ApuEntitySimplified extends Entity {
  name: string | null;
  description?: string | null;
  order: number;
}

export interface ApuPart extends Entity {
  value: string;
  type: string;
  items: ApuPartItem[];
}

export interface ApuPartItem extends Entity {
  value: string;
  visible: boolean;
  href: string;
  type: string;
}

export interface ApuAttachmentFileFile extends Entity {
  name: string;
  contentType: string;
  size: number;
  permanent: boolean;
}

export interface ApuAttachmentFile extends Entity {
  order: number;
  type: string;
  metadata: MetadataItem[];
  file: ApuAttachmentFileFile;
}

export interface ApuAttachment extends Entity {
  name: string;
  order: number;
  file: ApuAttachmentFile;
}

export interface ApuSource extends Entity {
  published: string;
  data: string;
}

export interface ApuLocale {
  lang: string;
  text: string;
}

export interface ApuPartType extends Entity {
  code: string;
  name: string;
  viewType: ApuPartViewType;
  lang: ApuLocale[];
}

export interface ApuPartItemType extends Entity {
  code: string;
  name: string;
  type: ApuPartItemDataType;
  lang: ApuLocale[];
  viewOrder: number;
  caseInsensitive: boolean;
}

export interface ApuTree extends Entity {
  name: string;
  type: ApuType;
  children: ApuTree[];
}

export interface MetadataItem extends Entity {
  value: string;
  type: string;
}
