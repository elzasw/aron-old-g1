import { ApiFilterOperation } from '@eas/common-web';
import { useIntl } from 'react-intl';
import { useConfiguration } from '../components';
import { FacetType, Message, ModulePath, SearchOption } from '../enums';

interface SearchOptionFilter {
    source: string;
    type?: FacetType;
    operation?: ApiFilterOperation;
    value: (boolean | string)[];
}

interface SearchOptionType {
    path: string;
    name: string;
    filters?: SearchOptionFilter[];
}

export function useSearchOptions() {
  const { formatMessage } = useIntl();
  const configuration = useConfiguration();

  if(!configuration.searchOptions){return []}

  const searchOptions:Partial<Record<SearchOption, SearchOptionType>> = {
    INSTITUTION: {
      path: ModulePath.INSTITUTION,
      name: formatMessage({id: Message.INSTITUTION}),
    },
    FUND: {
      path: ModulePath.FUND,
      name: formatMessage({id: Message.FUND}),
    },
    FUND_FA_ONLY: {
      path: ModulePath.FUND,
      name: formatMessage({id: Message.FUND_FA_ONLY}),
      filters: [
        {
          source: 'FINDINGAID~FLAG',
          value: ['Ano'],
        },
      ],
    },
    FINDING_AID: {
      path: ModulePath.FINDING_AID,
      name: formatMessage({id: Message.FINDING_AID}),
    },
    ARCH_DESC: {
      path: ModulePath.ARCH_DESC,
      name: formatMessage({id: Message.ARCH_DESC}),
    },
    ENTITY: {
      path: ModulePath.ENTITY,
      name: formatMessage({id: Message.ENTITY}),
    },
    ORIGINATOR: {
      path: ModulePath.ORIGINATOR,
      name: formatMessage({id: Message.ORIGINATORS}),
    },
    ARCH_DESC_DAO_ONLY: {
      path: ModulePath.ARCH_DESC,
      name: formatMessage({ id: Message.ARCH_DESC_DAO_ONLY }),
      filters: [
        // {
        //   source: 'containsDigitalObjects',
        //   type: FacetType.DAO_ONLY,
        //   operation: ApiFilterOperation.EQ,
        //   value: [true],
        // },
        {
          source: 'DIGITAL',
          value: ['Ano'],
        },
      ],
    }
  };

  const items:SearchOptionType[] = [];

  configuration.searchOptions.forEach((itemCode)=>{
      const item = searchOptions[itemCode];
      if(item){
        items.push(item);
      }
    }
  )

  return items;
}
