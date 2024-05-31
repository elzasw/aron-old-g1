import React from 'react';
import { find, isEmpty, get, compact, isArray, map, orderBy } from 'lodash';
import { useIntl } from 'react-intl';

import {
  FacetType,
  FacetDisplay,
  Message,
  ApuPartItemDataType,
} from '../../enums';
import {
  getApuPartItemType,
  // parseApuRefOptionId,
  parseApuRefOptionLabel,
  ApiListResponse,
  getApiList,
  AggregationConfig,
} from '../../common-utils';
import {
  Facet,
  FilterConfig,
  ApuPartItemType,
  AggregationItems,
  BasicFilterConfig,
  Filter,
  AggregationItem,
  Relationship,
  Option,
} from '../../types';
import { filterApiFilters, createApiFilters } from './utils';
import { TextWithCount } from '../../components';

const isFilterWithOptions = (type: FacetType) => {
  switch (type) {
    case FacetType.ENUM:
    case FacetType.ENUM_SINGLE:
      return true;
    default:
      return false;
  }
};

const getEnumOptions = async (
  source: string,
  apuPartItemTypes: ApuPartItemType[],
  filterConfig: FilterConfig[],
  additionalFilters: Filter[],
):Promise<ApiListResponse> => {
  const aggregations: AggregationConfig[] = [
        {
          family: 'BUCKET',
          aggregator: 'TERMS',
          name: source,
          field:
            getApuPartItemType(apuPartItemTypes, source) ===
            ApuPartItemDataType.APU_REF
              ? `${source}~ID~LABEL`
              : source,
          size: 9999,
        },
      ]

  const filters = filterApiFilters([
        ...additionalFilters,
        ...createApiFilters(filterConfig.filter((f) => f.source !== source)),
      ]);

  const response = await getApiList(aggregations, filters, source);

  return await response.json();
};

export const getEnumsOptions = async (
  sources: string[],
  apuPartItemTypes: ApuPartItemType[],
  filters: FilterConfig[],
  additionalFilters: Filter[],
):Promise<AggregationItems> => {
  try {
    const promises = sources.map((source) =>
      getEnumOptions(source, apuPartItemTypes, filters, additionalFilters)
    );

    const responses = await Promise.all(promises);
    let result:AggregationItems = {};
    compact(responses).forEach((response)=>{
      result = {...result, ...response.aggregations}
    })

    return result;
  } catch (e) {
    console.error(e);
    return {};
  }
};

export function useMapFilters() {
  const { formatMessage } = useIntl();

  const parseEnumOptions = (
    item: FilterConfig,
    enumsOptions: AggregationItems,
    apuPartItemTypes: ApuPartItemType[]
  ) => {
    const isApuRef =
      getApuPartItemType(apuPartItemTypes, item.source) ===
      ApuPartItemDataType.APU_REF;

    switch (item.type) {
      case FacetType.ENUM:
        return orderBy(
          map(get(enumsOptions, item.source, []), (optionItem: any) => {
            const labelString = isApuRef
              ? parseApuRefOptionLabel(optionItem.key)
              : optionItem.key;

            return {
              label: (
                <TextWithCount text={labelString} count={optionItem.value} />
              ),
              labelString,
              value: optionItem.key,
              count: parseInt(optionItem.value, 10),
            };
          }),
          item.orderBy === 'ASC' ? ['labelString'] : ['count'],
          item.orderBy === 'ASC' ? ['asc'] : ['desc']
        ).map(({ label, labelString, value }) => ({
          label,
          labelString,
          value,
        }));
      default:
        return [];
    }
  };

  const mapFilters = async (
    facets: Facet[],
    apuPartItemTypes: ApuPartItemType[],
    filters: FilterConfig[],
    additionalFilters: Filter[],
  ): Promise<FilterConfig[]> => {
    const merged: BasicFilterConfig[] = [...facets];

    // add filter definition from URL filters
    filters.forEach((item) => {
      const isSpecialFilter =
        item.type === FacetType.DAO_ONLY ||
        item.type === FacetType.RELATED_APUS;

      if (
        item.type &&
        (isSpecialFilter ||
          (item.source &&
            !find(facets, ({ source }) => source === item.source)))
      ) {
        const newItem = {
          source: FacetType.RELATED_APUS,
          ...(item as any),
        };

        // special filter is first
        if (isSpecialFilter) {
          merged.unshift(newItem);
        } else {
          merged.push(newItem);
        }
      }
    });

    const mapped = merged.map((item) => {
      const apuPartItemType = find(apuPartItemTypes, { code: item.source })
      return {
        display: FacetDisplay.ALWAYS,
        displayedItems: 0,
        maxDisplayedItems: 0,
        orderBy: 'FREQ',
        ...item,
        label:
          item.type === FacetType.DAO_ONLY
            ? formatMessage({ id: Message.DAO_ONLY })
            : item.type === FacetType.RELATED_APUS
            ? `${formatMessage({ id: Message.RELATED_TO })}: ${item.value}`
            : item.title 
            ? item.title 
            : apuPartItemType?.name || '_',
        caseInsensitive: apuPartItemType?.caseInsensitive || false,
        value:
          item.value ||
          find(filters, ({ source }) => source === item.source)?.value ||
          (item.type === FacetType.FULLTEXT || item.type === FacetType.FULLTEXTF || item.type === FacetType.CONSTANT ? '' : []),
      };
    });

    const enumsOptions = await getEnumsOptions(
      merged
        .filter((item) => item.type === FacetType.ENUM)
        .map((item) => item.source),
      apuPartItemTypes,
      mapped,
      additionalFilters
    );

    // add selected, but unavailable enum options 
    filters.forEach((filter)=>{
      const availableOptions = enumsOptions[filter.source] ? [...enumsOptions[filter.source]] : [];
      const values = isArray(filter.value) ? filter.value : [filter.value];
      const emptyValues:AggregationItem[] = [];

      values.forEach((value: boolean | string | Relationship | Option)=>{
        if(!availableOptions.find((f) => f.key === value || f.key.toString().startsWith(value.toString()))){
          emptyValues.push({
            key: value.toString(),
            value: "0",
          })
        }
      })
      availableOptions.unshift(...emptyValues);
      enumsOptions[filter.source] = availableOptions;
    })

    // add options
    return mapped
      .map((item) => {
        const options =
          item.options ||
          parseEnumOptions(item, enumsOptions, apuPartItemTypes);

        return {
          ...item,
          options: (isArray(item.order) && item.order.length
            ? [
                ...compact(
                  item.order.map((orderItem) =>
                    find(
                      options,
                      (o) =>
                        orderItem === o.value || orderItem === o.labelString
                    )
                  )
                ),
                ...options.filter(
                  (o) =>
                    !find(
                      item.order,
                      (orderItem) =>
                        orderItem === o.value || orderItem === o.labelString
                    )
                ),
              ]
            : options
          ).map((option) => {
            const tooltip = find(
              (item as any).tooltips,
              ({ value }) =>
                value === option.value || value === option.labelString
            )?.tooltip;

            return { ...option, ...(tooltip ? { tooltip } : {}) };
          }),
        };
      })
      .filter(
        ({ type, options }) => !isFilterWithOptions(type) || !isEmpty(options)
      );
  };

  return mapFilters;
}
