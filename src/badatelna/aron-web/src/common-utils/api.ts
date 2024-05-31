import { useState, useEffect } from 'react';
import contentDisposition from 'content-disposition';
import { get } from 'lodash';

import { useFetch, ApiFilterOperation } from '@eas/common-web';

import { API_URL, ApiUrl } from '../enums';
import { downloadFileFromUrl } from './file';
import { Filter, ApuEntity, AggregationItems, ApuEntitySimplified } from '../types';

type Options = any;

export const createUrl = (url: string) => `${API_URL}${url}`;

function useApi<T>(url: string, options: Options = {}) {
  return useFetch<T>(createUrl(url), {
    ...options,
    ...(options.json
      ? {
          body: JSON.stringify(options.json),
        }
      : {}),
  });
}

export function useGet<T>(url: string, options: Options = {}) {
  return useApi<T>(url, options);
}

export function usePost<T>(url: string, options: Options = {}) {
  return useApi<T>(url, {
    method: 'POST',
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    ...options,
  });
}

export interface ApiListResponse {
  aggregations: AggregationItems;
  count: number;
  items: ApuEntity[];
  searchAfter: unknown | null;
}

export interface AggregationConfig {
  family: string;
  aggregator: string;
  name: string;
  field: string;
  size?: number;
  format?: string;
}

export interface ApiListSimplifiedResponse {
  aggregations: AggregationItems;
  count: number;
  items: ApuEntitySimplified[];
  searchAfter: unknown | null;
}

interface SortConfig {
  field: string;
  type: string;
  order: string;
  sortMode: string;
}

interface ApiListProps {
  filters?: Filter[];
  sort?: SortConfig[];
  offset?: number;
  size?: number;
  aggregations?: AggregationConfig[];
  flipDirection?: boolean;
  simplified?: boolean;
  source?: string;
}

export const transformFilters = (filters:Filter[]):Filter[] => {
  return filters.map((filter) => {
    if(filter.filters){
      return {
        ...filter,
        filters: transformFilters(filter.filters)
      };
    }
    // remove name from institution ref value
    else if(
      filter.field === "FUND~INST~REF" 
      || filter.field === "INST~REF"
    ){
      const value = filter.value?.split("|")[0] || undefined;
      return {
        ...filter,
        value,
      }
    }
    if(filter.caseInsensitive){
        if(typeof filter.value === "string"){
          return {
            ...filter,
            value: filter.value.toLowerCase()
          }
        }
    }
    return filter;
  })
}

export async function getApiList(aggregations: AggregationConfig[], filters: Filter[], callSource?: string) {
  return await fetch(createUrl(`${ApiUrl.APU}/listview?listType=${callSource}`), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      size: 0,
      aggregations,
      filters: transformFilters(filters),
    }),
  });
}

export function useApiList({
  filters = [], 
  sort = [], 
  offset, 
  size = -1, 
  aggregations = [],
  flipDirection = false,
  source,
}: ApiListProps) {
  const json = { 
    aggregations, 
    filters: transformFilters(filters), 
    sort, 
    offset, 
    flipDirection, 
    size 
  };
  return usePost<ApiListResponse>(`${ApiUrl.APU}/list?listType=${(source)?.toUpperCase()}`, { json });
}

export function useApiListSimple({
  filters = [], 
  sort = [], 
  offset, 
  size = -1, 
  aggregations = [],
  flipDirection = false,
  source,
}: ApiListProps) {
  const json = { 
    aggregations, 
    filters: transformFilters(filters), 
    sort, 
    offset, 
    flipDirection, 
    size,
  };
  return usePost<ApiListSimplifiedResponse>(`${ApiUrl.APU}/listview?listType=${(source)?.toUpperCase()}`, { json });
}

export function useApiListViewSimple({
  filters = [], 
  sort = [], 
  offset, 
  size = -1, 
  aggregations = [],
  flipDirection = false,
  source,
}: ApiListProps) {
  const json = { 
    aggregations, 
    filters: transformFilters(filters), 
    sort, 
    offset, 
    flipDirection, 
    size,
  };
  return usePost<ApiListResponse>(`${ApiUrl.APU}/listsimple?listType=${(source)?.toUpperCase()}`, { json });
};

export const getApu = async (id: string):Promise<ApuEntitySimplified | null> => {
  try {
    const response = await fetch(createUrl(`${ApiUrl.APU}/${id}/view`));

    return response.ok ? await response.json() : null;
  } catch (error) {
    console.log(error);
    return null;
  }
};

export const getApus = async (ids: string[]):Promise<ApuEntitySimplified[] | null> => {
  try {
    const query = ids.join(','); 
    const response = await fetch(createUrl(`${ApiUrl.APU}/views?ids=${query}`));
    return response.ok ? await response.json() : null;
  } catch (error) {
    console.log(error);
    return null;
  }
};

export const useGetOptionsBySource = (
  source: string,
  query: string,
  isApuRef: boolean,
  apiFilters: Filter[]
) => {
  
  const [result, loading] = isApuRef ? useGetReferenceOptionsBySource(source, query, apiFilters) :
                                       useGetFieldOptionsBySource(source, query, apiFilters);
  
  return [result, loading];
};

const useGetFieldOptionsBySource = (
  source: string,
  query: string,
  apiFilters: Filter[]
) => {

  const field = `${source}~ID~LABEL`;
  const fieldLabel = `${source}~LABEL`;
  const filters = transformFilters(apiFilters);

  const [result, loading] = useApiListSimple({
    size: 0,
    aggregations: [
      {
        family: 'BUCKET',
        aggregator: 'TERMS',
        name: 'items',
        field,
      },
    ],
    filters: [
      ...filters.filter(
        ({ filters }) =>
          !filters || !filters.length || filters[0].field !== source
      ),
      ...(query
        ? [
            {
              field: fieldLabel,
              operation: ApiFilterOperation.CONTAINS,
              value: query,
            },
          ]
        : []),
    ],
    source: "get-options-by-source"
  });
  return [get(result, 'aggregations.items', []), loading];
}

const useGetReferenceOptionsBySource = (
  source: string,
  query: string,
  apiFilters: Filter[]
) => {
  //const field = `${source}~ID~LABEL`;
  const fieldLabel = `${source}~LABEL`;
  const filters = transformFilters(apiFilters);

  const [postResult, postLoading] = usePost(`${ApiUrl.APU}/list`+`?listType=GET-OPTIONSREL-BY_SOURCE_${(source)?.toUpperCase()}`, {
    json: {
      size: 0,
      aggregations: [
        {
        name: 'items',
        family: 'BUCKET',
        aggregator: 'NESTED',
        path: 'rels',
        aggregations: [
          {
            family: 'BUCKET',
            aggregator: 'FILTER',
            name: 'relsFilterAgg',
            filter:
              {
                  operation: ApiFilterOperation.AND,
                  filters: [
                    {
                      field: 'rels.type',
                      operation: ApiFilterOperation.EQ,
                      value: source,
                      nestedQueryEnabled: false,
                    },
                    {
                      operation: ApiFilterOperation.FTXF,
                      field: 'rels.label',
                      value: query,
                      nestedQueryEnabled: false,
                    },
                  ],
                },              
            aggregations: [
              {
                family: 'BUCKET',
                aggregator: 'TERMS',
                name: 'idLabel',
                field: 'rels.idLabel',
              },
            ],
          },
        ]
        },
      ],
      filters: [
        ...filters.filter(
          ({ filters }) =>
            !filters || !filters.length || filters[0].field !== source
        ),
        ...(query
          ? [
              {
                field: fieldLabel,
                operation: ApiFilterOperation.FTXF,
                value: query,
              },
            ]
          : []),
      ],
    source: "get-options-by-source"      
    }
  });

  const fields =
  (postResult as any)?.aggregations?.items[0]?.aggregations
    ?.relsFilterAgg[0]?.aggregations
    ?.idLabel || [];
  return [fields, postLoading];

}

export const useGetMatchingName = (query: string, group?: string) =>
  useApiListSimple({
      filters: [
        group
          ? {
              operation: ApiFilterOperation.AND,
              filters: [
                {
                  operation: ApiFilterOperation.FTXF,
                  field: 'name',
                  value: query,
                },
                {
                  operation: ApiFilterOperation.EQ,
                  field: 'incomingRelTypeGroups',
                  value: group,
                },
              ],
            }
          : {
              operation: ApiFilterOperation.FTXF,
              field: 'name',
              value: query,
            },
      ],
      size: 10,
    source: "get-matching-name" 
  });

export const useGetEntityRelationships = (
  id = '',
  apiFilters: Filter[],
  group?: string
) => {
  const [postResult, postLoading] = usePost(`${ApiUrl.APU}/list`+`?listType=GET-ENTITY-RELATIONSHIPS_${(group)?.toUpperCase()}`, {
    json: {
      size: 0,
      aggregations: [
        {
          family: 'BUCKET',
          aggregator: 'FILTER',
          name: 'apuFilterAgg',
          filter: {
            operation: ApiFilterOperation.OR,
            filters: apiFilters,
          },
          aggregations: [
            {
              name: 'nestedAgg',
              family: 'BUCKET',
              aggregator: 'NESTED',
              path: 'rels',
              aggregations: [
                {
                  family: 'BUCKET',
                  aggregator: 'FILTER',
                  name: 'relsFilterAgg',
                  filter: group
                    ? {
                        operation: ApiFilterOperation.AND,
                        filters: [
                          {
                            field: 'rels.targetId',
                            operation: ApiFilterOperation.EQ,
                            value: id,
                            nestedQueryEnabled: false,
                          },
                          {
                            operation: ApiFilterOperation.EQ,
                            field: 'rels.groups',
                            value: group,
                            nestedQueryEnabled: false,
                          },
                        ],
                      }
                    : {
                        field: 'rels.targetId',
                        operation: ApiFilterOperation.EQ,
                        value: id,
                        nestedQueryEnabled: false,
                      },
                  aggregations: [
                    {
                      family: 'BUCKET',
                      aggregator: 'TERMS',
                      name: 'relsTypeAgg',
                      field: 'rels.type',
                    },
                  ],
                },
              ],
            },
          ],
        },
      ],
    },
  });

  const fields =
    (postResult as any)?.aggregations?.apuFilterAgg[0]?.aggregations
      ?.nestedAgg[0]?.aggregations?.relsFilterAgg[0]?.aggregations
      ?.relsTypeAgg || [];
  return [fields, postLoading];
};

export const getFileByUrl = async (url: string) => {
  try {
    const response = await fetch(createUrl(url));

    if (response.ok) {
      const cd = response.headers.get('content-disposition');

      let name;

      if (cd) {
        const filename = contentDisposition.parse(cd).parameters.filename;

        name = filename && filename != 'null' ? filename : undefined;
      }

      return {
        blob: await response.blob(),
        name,
      };
    }
  } catch (error) {
    console.log(error);
  }

  return {};
};

export const getFile = async (id?: string, referencedFile?: boolean) => {
  if (id) {
    return await getFileByUrl(`${referencedFile ? ApiUrl.REFERENCED_FILES : ApiUrl.FILE}/${id}`);
  }

  return {};
};

export const downloadFile = async (
  id: string,
  filename?: string,
  referencedFile?: boolean,
  blank?: boolean,
) => {
  const { blob, name } = await getFile(id, referencedFile);

  if (blob) {
    downloadFileFromUrl(URL.createObjectURL(blob), name || filename, blank);
    return true;
  } else {
    return false;
  }
};

const useGetDateRangeLimit = (
  filters: Filter[],
  field: string
): [number | null, number | null, boolean] => {
  const [response, loading] = useApiListViewSimple({
      size: 0,
      aggregations: [
        {
          family: 'METRIC',
          aggregator: 'MAX',
          name: 'maxH',
          field:  `${field}~H`,
          format: 'yyyy'
        },
        {
          family: 'METRIC',
          aggregator: 'MIN',
          name: 'minL',
          field:  `${field}~L`,
          format: 'yyyy'
        }
      ],
      // remove current field from filters
      filters: filters.filter(
        ({ field: filterField }) => field !== filterField
      ),
    source: (`get-date-range_${field}`).toUpperCase()
  });

  const items = get(response, 'aggregations', null);

  const getFiniteNumber = (value: String): number|null => {
    const num = Number(value);
    return isFinite(num)?num:null;
  } 

  const minValue = !loading && items && items['minL']
        ? getFiniteNumber(items['minL'][0]['asString'])
        :null;

  const maxValue = !loading && items && items['maxH']
        ? getFiniteNumber(items['maxH'][0]['asString'])
        :null;

  return [minValue, maxValue, loading];
};

export const useGetRangeFilterInterval = (
  filters: Filter[],
  field: string
): [[number | null, number | null], boolean] => {
  const [result, setResult] = useState<[number | null, number | null]>([
    null,
    null,
  ]);
  const [minValue, maxValue, loading] = useGetDateRangeLimit(filters, field);
  useEffect(() => {
    if (!loading) {
      setResult([minValue || null, maxValue || null]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading]);
  return [result, loading];
};

export const getPageTemplateLogo = async () => {
  try {
    const response = await fetch(createUrl(ApiUrl.PAGE_TEMPLATE_LOGO), {
      headers: new Headers({
        'Content-Type': 'image/svg+xml',
      }),
    });

    return response.ok ? URL.createObjectURL(await response.blob()) : null;
  } catch (error) {
    console.log(error);
  }

  return null;
};

export const getPageTemplateTopImage = async () => {
  try {
    const response = await fetch(createUrl(ApiUrl.PAGE_TEMPLATE_TOP_IMAGE), {
      headers: new Headers({
        'Content-Type': 'image/png',
      }),
    });

    return response.ok ? URL.createObjectURL(await response.blob()) : null;
  } catch (error) {
    console.log(error);
  }

  return null;
};
