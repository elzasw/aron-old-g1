import { find, sortBy } from 'lodash';
import { DaoBundleType } from '../../../enums';
import { Dao, DaoFile } from '../../../types';
import { FileObject } from './types';

export const getExistingFile = (
  fileObject: FileObject,
  publishedFirst = false
) => {
  if (fileObject) {
    const { tile, published, thumbnail } = fileObject;

    return (
      (publishedFirst ? published : tile) ||
      (publishedFirst ? tile : published) ||
      thumbnail ||
      null
    );
  }

  return null;
};

export const getFilesByType = (item: Dao, bundleType: DaoBundleType) =>
  sortBy(
    item.files.filter(({ type }) => type === bundleType),
    'order'
  );

export const getFileByOrder = (files: DaoFile[], order: number) =>
  find(files, (file) => file.order === order);

export const getFiles = (item: Dao) => {
  if (!item.files || !item.files.length) {
    return [];
  }

  const publishedItems = getFilesByType(item, DaoBundleType.PUBLISHED);
  const thumbnails = getFilesByType(item, DaoBundleType.THUMBNAIL);
  const tiles = getFilesByType(item, DaoBundleType.TILE);

  const files: FileObject[] = [];

  let i = 1;
  let flag = true;

  while (flag) {
    const published = getFileByOrder(publishedItems, i);
    const thumbnail = getFileByOrder(thumbnails, i);
    const tile = getFileByOrder(tiles, i);

    if (published || thumbnail || tile) {
      files.push({
        id: tile?.id || published?.id || thumbnail?.id || '',
        order: tile?.order || published?.order || thumbnail?.order || 0,
        tile,
        published,
        thumbnail,
        selected: tile?.selected || published?.selected || thumbnail?.selected,
      });

      i++;
    } else {
      flag = false;
    }
  }

  const sortedFiles = files.sort((fileA, fileB) => fileA.order - fileB.order);

  return sortedFiles;
};
