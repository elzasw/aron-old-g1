import React, { FC, useContext, createContext } from 'react';
import { AppHeaderItemCode, SearchOption, Message, ApuType } from '../../enums';
import { DaoNamePlacement } from '../../enums/dao-name-placement';

interface LicensePart {
  link?: string;
  name?: string;
  iconUrl?: string;
  separator?: boolean;
}

interface License {
  parts: LicensePart[];
  code: string;
  description?: string;
}

interface DaoFooterDef {
  copyrightText?: string;
  links?: DaoFooterLinkDef[];
  licenses?: License[];
}

interface DaoFooterLinkDef {
  text?: string;
  url?: string;
}

interface OtherSource {
  url?: string;
  label?: string;
  tooltip?: string;
  icon?: IconDef;
}

interface IconDef {
  icon?: string;
  background?: string;
  radius?: number;
  color?: string;
  padding?: number;
  size?: number;
}

export interface ConfigurationType {
  allowDetailExpand?: boolean;
  alternativeItemLabel?: boolean;
  compactAppHeader?: boolean;
  showAppLogo?: boolean;
  showAppTopImage?: boolean;
  showHeader?: boolean;
  showShareButtons?: boolean;
  showStandalonePartName?: boolean;
  showMainPageBreadcrumb?: boolean;
  showFindRecordsInFundButton?: boolean;
  showCitationFor?: ApuType[];
  headerItems?: AppHeaderItemCode[];
  headerItemColors?: Partial<Record<AppHeaderItemCode, string>>;
  searchOptions?: SearchOption[];
  localeCookieName?: string;
  localeOverride?: Record<string, Partial<Record<Message, string>>>;
  showMetadataInImageViewer?: boolean;
  disableDownloads?: boolean;
  daoFooter?: DaoFooterDef;
  treeHorizontalScroll?: boolean;
  daoNamePlacement?: DaoNamePlacement;
  hideThumbnails?: boolean;
  newsUrl?: string;
  helpUrl?: string;
  orderResultsByScore?: boolean;
  showIncrementalTree?: boolean;
  otherSources?: OtherSource[];
  otherSourcesIcon?: IconDef;
  useStructuredResults?: boolean;
  alternativeFavouritesStyle?: boolean;
  attachmentsOnTop?: boolean;
  refsOnTop?: string[];
  hideDescriptionFor?: string[];
  showOrders?: boolean;
}

const defaultConfiguration: ConfigurationType = {
  allowDetailExpand: true,
  alternativeItemLabel: false,
  compactAppHeader: false,
  showAppLogo: true,
  showAppTopImage: false,
  showHeader: true,
  showShareButtons: false,
  showStandalonePartName: true,
  showMainPageBreadcrumb: true,
  showFindRecordsInFundButton: false,
  showCitationFor: [
    // ApuType.FINDING_AID,
    // ApuType.FUND,
    // ApuType.ENTITY,
    // ApuType.ARCH_DESC,
    // ApuType.COLLECTION,
    // ApuType.INSTITUTION,
  ],
  disableDownloads: false,
  // localeCookieName: "pll_language",
  headerItems: [
    AppHeaderItemCode.FUND,
    AppHeaderItemCode.ARCH_DESC,
    AppHeaderItemCode.FINDING_AID,
    AppHeaderItemCode.ENTITY,
    AppHeaderItemCode.NEWS,
    AppHeaderItemCode.HELP,
  ],
  headerItemColors: {
    // [AppHeaderItemCode.FUND]: '#5f5bc2',
    // [AppHeaderItemCode.ARCH_DESC]: '#79a7d1',
    // [AppHeaderItemCode.ENTITY]: '#fdad3c',
  },
  searchOptions: [
    SearchOption.ARCH_DESC_DAO_ONLY,
    SearchOption.ARCH_DESC,
    SearchOption.FUND,
    SearchOption.FUND_FA_ONLY,
    SearchOption.FINDING_AID,
    SearchOption.ENTITY,
  ],
  // localeOverride: {
  //     CZECH: {
  //         ARON_ENTITY: "Rejstříková hesla",
  //     },
  // }
  // daoFooter: {
  //     licenses: [
  //         {
  //             code: 'default',
  //             parts: [
  //                 {
  //                     name:
  //                         'Držitel licence: Literární archiv Památníku národního písemnictví, ' +
  //                         new Date().getFullYear(),
  //                     separator: true,
  //                 },
  //                 { name: 'Smluvní podmínky', link: 'https://www.google.com' },
  //                 { name: 'Reprografické služby', link: 'https://www.google.com' },
  //             ],
  //         },
  //         {
  //             code: 'bolzano',
  //             description:
  //                 'Tento dokument z archivního fondu Bolzano Bernard byl digitalizován ve spolupráci Památníku národního písemnictví a Filosofického ústavu AV ČR prostřednictvím výzkumné skupiny PragMatika (grantový projekt GA ČR 23-06540M). Licence CC BY-NC 4.0. Metadata byla vytvořena ve spolupráci Památníku národního písemnictví a Filosofického ústavu AV ČR prostřednictvím výzkumné skupiny PragMatika (grantový projekt GA ČR 23-06540M). Licence CC BY-NC 4.0',
  //             parts: [
  //                 {
  //                     name: 'Licence',
  //                     link: 'https://www.muzeumliteratury.cz/sluzby-reprograficke',
  //                 },
  //                 {
  //                     iconUrl:
  //                         'https://mirrors.creativecommons.org/presskit/buttons/88x31/svg/by-nc.svg',
  //                     link: 'https://creativecommons.org/licenses/by-nc/4.0/legalcode.cs',
  //                 },
  //             ],
  //         },
  //     ],
  // },
  treeHorizontalScroll: false,
  daoNamePlacement: DaoNamePlacement.TOP_RIGHT,
  hideThumbnails: false,
  showIncrementalTree: true,
  // newsUrl: 'https://www.seznam.cz',
  // helpUrl: 'https://www.google.cz',
  orderResultsByScore: true,
  otherSources: [
    {
      icon: {
        icon: `charter.png`,
        background: '#609ED8',
        // radius: 8,
        // color: 'white',
        // padding: 5,
        // size: 25,
      },
      label: 'Porta fontium',
      url: 'http://www.portafontium.eu',
      tooltip: 'Porta fontium',
    },
    // {
    //   icon: `https://www.portafontium.eu/sites/default/files/logo.png`,
    //   label: 'Porta fontium',
    //   url: 'http://www.portafontium.eu',
    //   tooltip: 'Porta fontium',
    // },
    // {
    //   icon: 'fas fa-file',
    //   label: 'Google',
    //   url: 'http://www.google.com',
    //   tooltip: 'Vyhledavac google',
    // },
  ],
  // otherSourcesIcon: {
  //   icon: 'https://www.portafontium.eu/sites/default/files/logo.png',
  //   background: '#609ed8',
  //   radius: 8,
  //   color: 'white',
  //   padding: 5,
  //   size: 20,
  // },
  useStructuredResults: false,
  alternativeFavouritesStyle: false,
  attachmentsOnTop: false,
  refsOnTop: [
    // 'FINDINGAID~REF'
  ],
  hideDescriptionFor: [
    // 'ARCH_DESC'
  ],
  showOrders: true,
};

const ConfigurationContext =
  createContext<ConfigurationType>(defaultConfiguration);

export const useConfiguration = () => useContext(ConfigurationContext);

export const ConfigurationProvider: FC = ({ children }) => {
  let configuration = { ...defaultConfiguration };
  try {
    configuration = {
      ...configuration,
      //@ts-ignore load configuration from global variable
      ...(_configuration ? _configuration : {}),
    };
  } catch {
    configuration = { ...defaultConfiguration };
  }
  return (
    <ConfigurationContext.Provider value={configuration}>
      {children}
    </ConfigurationContext.Provider>
  );
};
