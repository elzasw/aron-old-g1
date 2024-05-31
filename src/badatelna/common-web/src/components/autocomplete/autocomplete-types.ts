import { DomainObject, ScrollableSource } from 'common/common-types';
import { ReactNode, ComponentType } from 'react';

export interface AutocompleteSource<OPTION> extends ScrollableSource<OPTION> {
  setSearchQuery: (q: string) => void;
  loadDetail: (item: OPTION) => Promise<OPTION>;
}

export interface AutocompleteProps<OPTION extends DomainObject> {
  disabled?: boolean;
  value: OPTION | OPTION[] | null | undefined;
  onChange: (value: OPTION | OPTION[] | null) => void;

  source: AutocompleteSource<OPTION>;
  clearable?: boolean;

  multiple?: boolean;
  showTooltip?: boolean;

  ItemComponent?: ComponentType<{ item: OPTION; children: ReactNode }>;
  labelMapper?: (option: OPTION, type: 'FIELD' | 'OPTION') => string;
  tooltipMapper?: (option: OPTION) => ReactNode;

  DisabledComponent?: ComponentType<{
    value: string | null;
    disabled?: boolean;
  }>;
  closeOnSelect?: boolean;
}
