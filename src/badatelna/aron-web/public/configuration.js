var _configuration = {
  // allowDetailExpand: true,
  // alternativeItemLabel: false,
  // compactAppHeader: false,
  // orderResultsByScore: true,
  // showAppLogo: true,
  // showAppTopImage: false,
  // showHeader: true,
  // showShareButtons: false,
  // showStandalonePartName: true,
  // showMainPageBreadcrumb: true,
  // showFindRecordsInFundButton: false,
  // showCitationFor: [
  //   "ARCH_DESC",
  //   "COLLECTION",
  //   "ENTITY",
  //   "FINDING_AID",
  //   "INSTITUTION",
  //   "FUND",
  // ],
  // localeCookieName: "pll_language",
  // showMetadataInImageViewer: true,
  // disableDownloads: false,
  // headerItems: [
  //   "FUND",
  //   "ARCH_DESC",
  //   "FINDING_AID",
  //   "ENTITY",
  //   "ORIGINATOR",
  //   "INSTITUTION",
  //   "NEWS",
  //   "HELP",
  // ],
  // searchOptions: [
  //   "ARCH_DESC_DAO_ONLY",
  //   "ARCH_DESC",
  //   "FUND",
  //   "FUND_FA_ONLY",
  //   "FINDING_AID",
  //   "ENTITY",
  //   "ORIGINATOR",
  //   "INSTITUTION",
  // ],
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
  // localeOverride: {
  //   CZECH: {
  //     ARON_ENTITY: "Rejstříková hesla",
  //   },
  // },
  // treeHorizontalScroll: false,
  // daoNamePlacement: "top-right", // top-left, top-right, bottom-left, bottom-right
  // hideThumbnails: false,
  // newsUrl: 'https://www.seznam.cz',
  // helpUrl: 'https://www.google.cz',
  // showIncrementalTree: false,
  // otherSourcesIcon: {
  // icon: 'https://www.portafontium.eu/sites/default/files/logo.png',
  // background: '#609ed8',
  // radius: 8,
  // color: 'white',
  // padding: 5,
  // size: 20,
  // },
  // otherSources: [
  // {
  // icon: {
  //   icon: `charter.png`,
  //   background: '#609ED8',
  //   radius: 8,
  //   padding: 5,
  //   size: 25,
  // },
  //   label: 'Porta fontium',
  //   url: 'http://www.portafontium.eu',
  //   tooltip: 'Porta fontium',
  // },
  // {
  // icon: {
  //   icon: 'fas fa-file',
  //   background: '#609ED8',
  //      color: 'white',
  //   radius: 8,
  //   padding: 5,
  //   size: 25,
  // },
  //   label: 'Google',
  //   url: 'http://www.google.com',
  //   tooltip: 'Vyhledavac google',
  // },
  // ]
  // refsOnTop: ['FINDINGAID~REF'],
  // useStructuredResults: false,
  // headerItemColors: {
  // FUND: '#5f5bc2',
  // ARCH_DESC: '#79a7d1',
  // ENTITY: '#fdad3c',
  // },
  // hideDescriptionFor: ['ARCH_DESC'],
  // showOrders: false,
  // favouritesStyle: "TEXT",
  // devMode: false,
  // alternativeSearchType: true,
  // favouritesSections: [{
  //   label: "Často hledáte",
  //   items: [
  //     {
  //       backgroundImage: {
  //         url: `http://10.2.0.28/app/tiles/pozemkove_knihy.jpg`,
  //       },
  //       label: 'Pozemkové knihy',
  //       // url: 'http://www.portafontium.eu',
  //       type: "ARCH_DESC",
  //       filters: [{
  //         source: "UNIT_TYPE",
  //         value: ["pozemková kniha"]
  //       }],
  //       columnSpan: 1,
  //       rowSpan: 2,
  //     },
  //     {
  //       backgroundImage: {
  //         url: `http://10.2.0.28/app/tiles/pozemkove_katastry.jpg`,
  //         positionY: '0',
  //       },
  //       label: 'Pozemkové katastry',
  //       // url: 'http://www.portafontium.eu',
  //       type: "ARCH_DESC",
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
  //   style: "GRID",
  // }, {
  //     label: "Jiné zdroje",
  //     items: [
  //       {
  //         backgroundImage: {
  //           url: `http://10.2.0.28/app/tiles/matrika.jpg`,
  //         },
  //         label: 'Matriky',
  //         url: 'https://www.mza.cz/actapublica/matrika/hledani',
  //         columnSpan: 2,
  //         rowSpan: 2,
  //       },
  //       {
  //         backgroundImage: {
  //           url: `http://10.2.0.28/app/tiles/scitani_operat.jpg`,
  //         },
  //         label: 'Sčítací operáty',
  //         url: 'https://www.mza.cz/scitacioperaty/',
  //         columnSpan: 1,
  //       },
  //       {
  //         backgroundImage: {
  //           url: `http://10.2.0.28/app/tiles/indikacni_skica_kromeriz.jpg`,
  //         },
  //         label: 'Indikační skici',
  //         url: 'https://www.mza.cz/indikacniskici/skica',
  //         columnSpan: 2,
  //       },
  //       {
  //         backgroundImage: {
  //           url: `http://10.2.0.28/app/tiles/urbar.jpg`,
  //         },
  //         label: 'Urbáře',
  //         url: 'https://www.mza.cz/urbare/',
  //         columnSpan: 2,
  //       },
  //     ],
  //     style: "GRID",
  //   }],
  // widerHomePage: true,
  // alternativeSearchStyle: true,
};
