import React, { ReactElement } from 'react';
import { identity } from 'lodash';

import { Autocomplete as EasAutocomplete, AutocompleteSource } from '@eas/common-web';

import { Option } from '../../types';

interface Props {
  value: Option | Option[] | null;
  onChange: (value: Option | Option[] | null) => void;
  options: Option[];
  multiple?: boolean;
  onQueryChange: (q: string) => void;
  loading: boolean;
}

export function Autocomplete({
  options,
  value,
  onChange,
  onQueryChange,
  loading,
  multiple = false,
}: Props): ReactElement {
  const source: AutocompleteSource<Option> = {
    setSearchQuery: onQueryChange,
    hasNextPage: () => false,
    isDataValid: () => true,
    setParams: () => null,
    getParams: () => ({}),
    loadMore: () => new Promise<void>(() => null),
    loadDetail: identity,
    loading,
    reset: () => null,
    items: options,
    count: options.length,
    setLoading: () => loading,
  };
  return <EasAutocomplete {...{ value, onChange, source, multiple, closeOnSelect: true }} />;
}
