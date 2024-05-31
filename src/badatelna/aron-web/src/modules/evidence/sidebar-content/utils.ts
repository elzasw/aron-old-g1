import { isEmpty, compact } from 'lodash';

import { useApiListSimple } from '../../../common-utils';
import { Filter } from '../../../types';
import { ApiFilterOperation } from '@eas/common-web';
import { DEFAULT_RANGE } from '../../../enums';
import { InputFilterType } from '.';

export const toStrRange = (numberRange: [number, number]): [string, string] => [
  numberRange[0].toString(),
  numberRange[1].toString(),
];

export const isOutsideRange = (value: string) => {
  const parsed = parseInt(value);

  return (
    !parsed ||
    isEmpty(value) ||
    DEFAULT_RANGE[0] > parsed ||
    DEFAULT_RANGE[1] < parsed
  );
};

export const yearInISO = (value: number | null, isGTE = false) =>
  value
    ? `${`0000${value}`.slice(-4)}-${
        isGTE ? `01-01T00:00:00.000Z` : `12-31T23:59:59.999Z`
      }`
    : null;

export const useGetCountRange = (
  field: string,
  value: [number, number],
  apiFilters: Filter[]
) =>
  useApiListSimple({
      size: 0,
      filters: compact([
        // remove current field from filters
        ...apiFilters.filter((item) => item.field !== field),
        {
          field,
          operation: ApiFilterOperation.RANGE,
          gte: yearInISO(Math.max(value[0], DEFAULT_RANGE[0]), true),
          lte: yearInISO(Math.min(value[1], DEFAULT_RANGE[1])),
        },
      ]),
      source: "get-count-range",
  });

export const useGetCountInput = (
  field: string,
  value: string,
  apiFilters: Filter[], 
  filterType?: InputFilterType,
) => {

  const operation = 
    filterType===InputFilterType.FULLTEXT?ApiFilterOperation.FTX:
    filterType===InputFilterType.FULLTEXT_FIELD?ApiFilterOperation.FTXF:
    filterType===InputFilterType.CONSTANT?ApiFilterOperation.EQ
    : ApiFilterOperation.CONTAINS;

  const filter = apiFilters.find((item) => item.field === field);
  return useApiListSimple({
      size: 0,
      filters: compact([
        // remove current field from filters
        ...apiFilters.filter((item) => item.field !== field),
        ...(value
          ? [{
                field,
                operation,
                value,
                caseInsensitive: filter?.caseInsensitive,
              }]
          : []),
      ]),
      source: "get-count-input",
  });
}
