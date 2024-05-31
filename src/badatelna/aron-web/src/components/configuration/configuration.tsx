import React, { FC, useContext, createContext } from 'react';
import { AppHeaderItemCode, SearchOption, Message, ApuType } from '../../enums';
import { DaoNamePlacement } from '../../enums/dao-name-placement';

interface DaoFooterDef {
    copyrightText?: string;
    links?: DaoFooterLinkDef[];
}

interface DaoFooterLinkDef {
    text?: string;
    url?: string;
}

interface OtherSource {
    icon?: string;
    url?: string;
    label?: string;
    tooltip?: string;
}

export interface ConfigurationType {
    allowDetailExpand?: boolean,
    alternativeItemLabel?: boolean;
    compactAppHeader?: boolean;
    showAppLogo?: boolean,
    showAppTopImage?: boolean,
    showHeader?: boolean;
    showShareButtons?: boolean;
    showStandalonePartName?: boolean;
    showMainPageBreadcrumb?: boolean;
    showFindRecordsInFundButton?: boolean;
    showCitationFor?: ApuType[];
    headerItems?: AppHeaderItemCode[];
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
    searchOptions: [
        SearchOption.ARCH_DESC_DAO_ONLY,
        SearchOption.ARCH_DESC,
        SearchOption.FUND,
        SearchOption.FINDING_AID,
        SearchOption.ENTITY,
    ],
    // localeOverride: {
    //     CZECH: {
    //         ARON_ENTITY: "Rejstříková hesla",
    //     },
    // }
    // daoFooter: {
    //   copyrightText: 'Držitel licence: Literární archiv Památníku národního písemnictví, ' + new Date().getFullYear(),
    //   links: [{
    //     text: 'Smluvní podmínky',
    //     url: 'https://www.google.com',
    //   }]
    // },
    treeHorizontalScroll: false,
    daoNamePlacement: DaoNamePlacement.TOP_RIGHT,
    hideThumbnails: false,
    showIncrementalTree: true,
    // newsUrl: 'https://www.seznam.cz',
    // helpUrl: 'https://www.google.cz',
    orderResultsByScore: true,
    otherSources: undefined,
}

const ConfigurationContext = createContext<ConfigurationType>(defaultConfiguration)

export const useConfiguration = () => useContext(ConfigurationContext)

export const ConfigurationProvider: FC = ({
    children
}) => {
    let configuration = { ...defaultConfiguration }
    try {
        configuration = {
            ...configuration,
            //@ts-ignore load configuration from global variable
            ...(_configuration ? _configuration : {})
        }
    } catch {
        configuration = { ...defaultConfiguration }
    }
    return <ConfigurationContext.Provider value={configuration}>
        {children}
    </ConfigurationContext.Provider>
}
