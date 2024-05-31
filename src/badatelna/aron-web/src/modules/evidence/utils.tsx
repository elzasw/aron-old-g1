import React from 'react';
import {
  find,
  isEmpty,
  flattenDeep,
  reverse,
  compact,
  isArray,
  omit,
} from 'lodash';

import { FacetType, ModulePath } from '../../enums';
import { getTypeByPath } from '../../common-utils';
import {
  Facet,
  FilterConfig,
  Relationship,
  Filter,
  ApuEntity,
  Option,
} from '../../types';
import { ApiFilterOperation } from '@eas/common-web';
import {
  SelectionFilter,
  RangeFilter,
  InputFilter,
  AutocompleteFilter,
  RelationshipFilter,
  yearInISO,
} from './sidebar-content';
import { InputFilterType } from "./sidebar-content"

export const findApuParts = <EntityItem extends {type: string}>(items: EntityItem[], code: string) =>
  items.filter(({ type }) => type === code);

export const filterApuPartTypes = <Item extends {code: string}, EntityItem extends {type: string}>(items: Item[], entityItems: EntityItem[]) =>
  items.filter(({ code }) => findApuParts(entityItems, code).length);

export const FilterComponent = (props: any) => {
  let CFilter: React.ReactType;
  switch (props.type) {
    case FacetType.ENUM:
    case FacetType.ENUM_SINGLE:
      CFilter = SelectionFilter;
      break;
    case FacetType.UNITDATE:
      CFilter = RangeFilter;
      break;
    case FacetType.CONSTANT:
      return <InputFilter filterType={InputFilterType.CONSTANT} {...props} />;
    case FacetType.FULLTEXT:
      CFilter = InputFilter;
      break;
    case FacetType.FULLTEXTF:
      return <InputFilter filterType={InputFilterType.FULLTEXT_FIELD} {...props} />;
    case FacetType.MULTI_REF:
      CFilter = AutocompleteFilter;
      break;
    case FacetType.MULTI_REF_EXT:
      CFilter = RelationshipFilter;
      break;
    default:
      // eslint-disable-next-line react/display-name
      CFilter = ({ label }) => <div>{label}</div>;
      break;
  }

  return <CFilter {...props} />;
};

export const getRelatedApusFilter = (id: string, name: string) => [
  {
    type: FacetType.RELATED_APUS,
    operation: ApiFilterOperation.AND,
    value: name,
    filters: [
      {
        operation: ApiFilterOperation.NOT,
        filters: [
          {
            field: 'id',
            operation: ApiFilterOperation.EQ,
            value: id,
          },
        ],
      },
      {
        operation: ApiFilterOperation.AKF,
        value: id,
      },
    ],
  },
];

export const filterFacets = (facets: Facet[], path: ModulePath): Facet[] =>
  facets.filter(
    (facet: Facet) =>
      !facet.when.apuType || facet.when.apuType === getTypeByPath(path)
  );

const clearSource = (source: string) => source.replace(/~|_/, '');

export const filterMappedFilters = (
  filters: FilterConfig[],
  path: ModulePath
) => {
  const flattenedFilters = flattenDeep(
    filters.map((f) => (f.filters ? f.filters : (f as any)))
  );

  const filteredFilters = filters.filter((filterConfig: FilterConfig) => {
    const { when } = filterConfig;

    return (
      !when ||
      !when.all ||
      when.all.every(({ apuType, filter, value }) => {
        if (apuType) {
          return apuType === getTypeByPath(path);
        }

        if (filter) {
          const foundFilter = find(
            flattenedFilters,
            ({ source }) =>
              source && clearSource(source) === clearSource(filter)
          );

          return (
            foundFilter &&
            (foundFilter.value === value ||
              (isArray(foundFilter.value) &&
                find(
                  foundFilter.value,
                  (v) =>
                    v === value ||
                    find(
                      foundFilter.options,
                      (o) => o && v === o.value && value === o.labelString // check if condition value is equal to option label
                    )
                )))
          );
        }

        return true;
      })
    );
  });

  return filteredFilters;
};

export const facetsContainRelationships = (
  facets: Facet[],
  path: ModulePath
): boolean =>
  !isEmpty(
    find(
      facets.filter((f: Facet) => f.when.apuType === getTypeByPath(path)),
      { type: FacetType.MULTI_REF_EXT }
    )
  );

export const convertRelationshipsToFilter = (
  relationships: Relationship[]
): Filter | undefined =>
  relationships && {
    operation: ApiFilterOperation.OR,
    filters: relationships.map(({ field, value }: Relationship) => ({
      operation: ApiFilterOperation.EQ,
      field,
      value,
    })),
  };

export const getParentBreadcrumbs = (parent?: ApuEntity) => {
  const breadcrumbs = [];

  let current = parent;

  while (current) {
    breadcrumbs.push({
      path: `${ModulePath.APU}/${current.id}`,
      label: current.description || current.name || 'Unknown',
    });

    current = current.parent;
  }

  return reverse(breadcrumbs);
};

export const getPathSpecificFilters = (path: ModulePath) => {
    switch (path) {
        case ModulePath.ORIGINATOR:
            return [{
                field: "AE~ORIGINATOR",
                operation: ApiFilterOperation.EQ,
                value: "ANO",
            }];        
        default:
            return [];
    }
}

export const createApiFilters = (filters: FilterConfig[]) => {
  return filters.length
    ? compact(
        filters.map(({ source, operation, value, type, filters, caseInsensitive }) => {
          const createFilter = (params: Filter) => ({
            field: source,
            ...(filters ? { filters } : {}),
            ...params,
            caseInsensitive,
          });

          if (type === FacetType.UNITDATE) {
            return isArray(value) && value.length >= 2 && value[0] && value[1]
              ? {
                  field: source,
                  operation: ApiFilterOperation.RANGE,
                  gte: yearInISO(Number(value[0]), true),
                  lte: yearInISO(Number(value[1])),
                }
              : null;
          }

          if (type === FacetType.MULTI_REF) {
            if (isArray(value) && value.length) {
              const optionValues = value as Option[];

              if ('id' in optionValues[0]) {
                return {
                  operation: ApiFilterOperation.OR,
                  filters: optionValues.map(({ id }) => ({
                    field: source,
                    operation: ApiFilterOperation.CONTAINS,
                    value: id,
                    caseInsensitive,
                  })),
                };
              }
            }

            return null;
          }

          if (type === FacetType.MULTI_REF_EXT) {
            return isArray(value) && value.length
              ? {
                  operation: ApiFilterOperation.OR,
                  filters: (value as Relationship[]).map((relationship) =>
                    createFilter({
                      field: relationship.field,
                      operation: operation || ApiFilterOperation.EQ,
                      value: relationship.value,
                    })
                  ),
                }
              : null;
          }

          return isArray(value)
            ? value.length > 1
              ? {
                  operation: ApiFilterOperation.OR,
                  filters: (value as string[]).map((v) =>
                    createFilter({
                      operation: operation || ApiFilterOperation.EQ,
                      value: v,
                    })
                  ),
                }
              : value[0]
              ? createFilter({
                  operation: operation || ApiFilterOperation.EQ,
                  value: value[0],
                })
              : null
            : createFilter({
                operation:
                  operation ||
                  (type === FacetType.FULLTEXT
                    ? ApiFilterOperation.CONTAINS
                    : (type === FacetType.FULLTEXTF ? ApiFilterOperation.FTXF : ApiFilterOperation.EQ)),
                value,
              });
        })
      )
    : [];
};

export const filterApiFilters = (filters?: Filter[]): Filter[] =>
  filters
    ? compact(
        filters.map(({ filters, ...item }) => {
          const isAndOrNot =
            item.operation === ApiFilterOperation.AND ||
            item.operation === ApiFilterOperation.OR ||
            item.operation === ApiFilterOperation.NOT;

          const filtersOk = filterApiFilters(filters).length;

          return (item.value && !isAndOrNot) ||
            (isAndOrNot && filtersOk) ||
            item.operation === ApiFilterOperation.RANGE ||
            item.operation === ApiFilterOperation.NOT_NULL
            ? {
                ...omit(item, isAndOrNot ? ['field', 'value'] : []),
                ...(filtersOk ? { filters: filterApiFilters(filters) } : {}),
              }
            : null;
        })
      )
    : [];
