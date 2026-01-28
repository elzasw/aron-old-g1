import React, { createContext, FC, useContext } from 'react';
import { AppHeaderItemCode, SearchOption } from '../../enums';
import { DaoNamePlacement } from '../../enums/dao-name-placement';
import { FavouritesStyle } from './enums';
import { ConfigurationType } from './types';

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
  // favouritesSections: [{
  //   label: "Často hledáte",
  //   items: [
  //     {
  //       imageUrl: `http://10.2.0.28/app/tiles/pozemkove_knihy.jpg`,
  //       label: 'Pozemkové knihy',
  //       // url: 'http://www.portafontium.eu',
  //       type: ApuType.ARCH_DESC,
  //       filters: [{
  //         source: "UNIT_TYPE",
  //         value: ["pozemková kniha"]
  //       }],
  //       columnSpan: 2,
  //       rowSpan: 2,
  //     },
  //     {
  //       imageUrl: `http://10.2.0.28/app/tiles/pozemkove_katastry.jpg`,
  //       label: 'Pozemkové katastry',
  //       // url: 'http://www.portafontium.eu',
  //       type: ApuType.ARCH_DESC,
  //       filters: [{
  //         source: "REGISTRY_TYPE",
  //         value: ["rejstřík obecných pojmů"]
  //       },{
  //         source: "REG_TERM~REF",
  //           value: [{
  //             id: "e6d9e04c-dcb2-430e-94fb-1d3448e16640",
  //             name: "pozemkové katastry (soupisy pozemkového majetku)"
  //           }]
  //       }],
  //       columnSpan: 2,
  //       // rowSpan: 2,
  //     },
  //   ],
  //   style: FavouritesStyle.GRID,
  // }, {
  //     label: "Jiné zdroje",
  //     items: [
  //       {
  //         imageUrl: `http://10.2.0.28/app/tiles/matrika.jpg`,
  //         label: 'Matriky',
  //         url: 'https://www.mza.cz/actapublica/matrika/hledani',
  //         tooltip: 'Matriky',
  //         columnSpan: 2,
  //         rowSpan: 2,
  //       },
  //       {
  //         imageUrl: `http://10.2.0.28/app/tiles/scitani_operat.jpg`,
  //         label: 'Sčítací operáty',
  //         url: 'https://www.mza.cz/scitacioperaty/',
  //         columnSpan: 2,
  //       },
  //       {
  //         imageUrl: `http://10.2.0.28/app/tiles/indikacni_skica_kromeriz.jpg`,
  //         label: 'Indikační skici',
  //         url: 'https://www.mza.cz/indikacniskici/skica',
  //         columnSpan: 2,
  //       },
  //       {
  //         imageUrl: `http://10.2.0.28/app/tiles/urbar.jpg`,
  //         label: 'Urbáře',
  //         url: 'https://www.mza.cz/urbare/',
  //         columnSpan: 2,
  //       },
  //     ],
  //     style: FavouritesStyle.GRID,
  //   }],
  otherSources: [
    // {
    //   // icon: {
    //   //   icon: `icon.svg`,
    //   //   background: '#609ED8',
    //   //   // radius: 8,
    //   //   // color: 'white',
    //   //   // padding: 5,
    //   //   // size: 25,
    //   // },
    //   icon: `icon.svg`,
    //   imageUrl: `logo.png`,
    //   // imageUrl:'https://fastly.picsum.photos/id/998/500/754.jpg?hmac=pIA45S-7hqp_DWSpHfMewgs5Nhj_8S4E8xprYQB-rxk' ,
    //   label: 'Porta fontium',
    //   // url: 'http://www.portafontium.eu',
    //   tooltip: 'Porta fontium',
    //   type: ApuType.ARCH_DESC,
    //   filters: [{
    //     source: "UNIT~TYPE",
    //     value: ["index"]
    //   }]
    // },
    // {
    //   icon: `https://www.portafontium.eu/sites/default/files/logo.png`,
    //   label: 'Porta fontium',
    //   url: 'http://www.portafontium.eu',
    //   tooltip: 'Porta fontium',
    // },
    // {
    //   icon: { icon: 'fas fa-building' },
    //   label: 'Google',
    //   url: 'http://www.google.com',
    //   tooltip: 'Vyhledavac google',
    // },
    // {
    //   icon: { icon: 'fas fa-house-user' },
    //   label: 'Google',
    //   url: 'http://www.google.com',
    //   tooltip: 'Vyhledavac google',
    // },
  //   {
  //     icon: { icon: 'far fa-map' },
  //     label: 'Google mapy',
  //     url: 'http://maps.google.com',
  //     tooltip: 'Mapove zdroje',
  //   },
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
  alternativeSearchType: false,
  favouritesStyle: FavouritesStyle.GRID,
  widerHomePage: false,
  devMode: false,
  attachmentsOnTop: false,
  refsOnTop: [
    // 'FINDINGAID~REF'
  ],
  hideDescriptionFor: [
    // 'ARCH_DESC'
  ],
  showOrders: false,
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
