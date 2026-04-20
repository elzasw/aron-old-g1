export const BASE_URL = `${process.env.URL_PREFIX || ''}`;
export const API_URL = `${BASE_URL}/api/aron`;
export const API_ORDERS_URL = `${BASE_URL}/api/orders`;

export enum ApiUrl {
  APU = '/apu',
  APU_PART_TYPE = '/apuPartType',
  APU_PART_ITEM_TYPE = '/apuPartItemType',
  FACETS = '/facets',
  FILE = '/file',
  REFERENCED_FILES = '/referencedfiles',
  HELP = '/help',
  ME = '/me',
  NEWS = '/news',
  RELATION = '/relation',
  FAVORITE_QUERY = '/favoriteQuery',
  PAGE_TEMPLATE = '/pageTemplate',
  PAGE_TEMPLATE_LOGO = '/pageTemplate/logo',
  PAGE_TEMPLATE_TOP_IMAGE = '/pageTemplate/topImage',
}

export enum SortMode {
  MIN = 'MIN',
  MAX = 'MAX',
}
