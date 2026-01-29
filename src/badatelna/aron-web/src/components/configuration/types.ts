import { ApuType, AppHeaderItemCode, SearchOption, Message, FavouritesStyle } from "../../enums";
import { FilterConfig } from "../../types";
import { DaoNamePlacement } from "../../enums/dao-name-placement";

export interface LicensePart {
  link?: string;
  name?: string;
  iconUrl?: string;
  separator?: boolean;
}

export interface License {
  parts: LicensePart[];
  code: string;
  description?: string;
}

export interface DaoFooterDef {
  copyrightText?: string;
  links?: DaoFooterLinkDef[];
  licenses?: License[];
}

export interface DaoFooterLinkDef {
  text?: string;
  url?: string;
}

export interface OtherSource {
  url?: string;
  label?: string;
  tooltip?: string;
  icon?: IconDef | string;
  backgroundImage?: BackgroundImageDef;
  backgroundColor?: string;
  columnSpan?: number;
  rowSpan?: number;
  query?: string;
  type?: ApuType;
  filters?: FilterConfig[];
}

export interface BackgroundImageDef {
  url: string;
  positionX: string;
  positionY: string;
}

export interface IconDef {
  icon?: string;
  background?: string;
  radius?: number;
  color?: string;
  padding?: number;
  size?: number;
}

interface FavouritesSection{
  label?: string;
  items: OtherSource[];
  style?: FavouritesStyle;
  icon?: IconDef | string;
}

type PrimaryColor = {
  main: string;
  dark: string;
  light: string;
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
  alternativeSearchType?: boolean;
  favouritesStyle?: FavouritesStyle;
  devMode?: boolean;
  attachmentsOnTop?: boolean;
  refsOnTop?: string[];
  hideDescriptionFor?: string[];
  showOrders?: boolean;
  favouritesSections?: FavouritesSection[];
  widerHomePage?: boolean;
  alternativeSearchStyle?: boolean;
  primaryColor?: PrimaryColor;
}
