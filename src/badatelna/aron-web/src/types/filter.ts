import { ApuType, FacetType, FacetDisplay } from '../enums';
import { Filter } from './api';
import { ApiFilterOperation } from '@eas/common-web';
import { Option } from './option';

type FilterConfigValue =
  | string
  | string[]
  | boolean[]
  | Option[]
  | Relationship[];

interface Tooltip {
  value: string;
  tooltip: string;
}

export interface FilterOption {
  value: string;
  label: string;
}

export interface BasicFilterConfig {
  source: string;
  type: FacetType;
  value?: FilterConfigValue;
  options?: any[];
  order?: string[];
  title?: string;
}

export interface FilterConfig {
  caseInsensitive?: boolean;
  type?: FacetType;
  label?: string;
  tootlip?: string;
  tooltips?: Tooltip[];
  description?: string;
  source: string;
  operation?: ApiFilterOperation;
  value: FilterConfigValue;
  options?: any[];
  filters?: Filter[];
  display?: FacetDisplay;
  displayedItems?: number;
  maxDisplayedItems?: number;
  order?: string[];
  orderBy?: string;
  when?: {
    apuType?: ApuType;
    all?: FacetAllItem[];
  };
}

export interface FavouriteQuery {
  icon: string;
  label: string;
  tooltip?: string;
  query?: string;
  type?: ApuType;
  filters?: FilterConfig[];
}

export interface FavouriteQueryEx extends FavouriteQuery {
  imageUrl?: string;
  backgroundColor?: string;
  columnSpan?: number;
  rowSpan?: number;
}

interface FacetInterval {
  from: string;
  fromText: string;
  to: string;
  toText: string;
}

interface FacetAllItem {
  apuType?: ApuType;
  filter?: string;
  value?: string;
}

export interface Facet {
  display: FacetDisplay;
  facets?: Facet[];
  intervals?: FacetInterval[];
  displayedItems: number;
  maxDisplayedItems: number;
  source: string;
  type: FacetType;
  order?: string[];
  orderBy?: string;
  tootlip?: string;
  tooltips?: Tooltip[];
  description?: string;
  when: {
    apuType?: ApuType;
    all?: FacetAllItem[];
  };
  title?: string;
}

export interface AggregationItem {
  key: string;
  value: string;
  [label: string]: string;
}

export interface AggregationItems {
  [name: string]: AggregationItem[];
}

export interface Relationship {
  field: string;
  value: string;
  name?: string;
}
