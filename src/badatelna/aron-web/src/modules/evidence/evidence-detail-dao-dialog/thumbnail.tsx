import classNames from 'classnames';
import React from 'react';
import { ImageLoad } from '../../../components/image-load';
import { useStyles } from './styles';
import { FileObject } from './types';

export function Thumbnail({
  file,
  index,
  isActive,
  onClick = () => console.error('"onClick" not defined in Thumbnail'),
}: {
  file: FileObject;
  index: number;
  isActive?: boolean;
  onClick?: (file: FileObject) => void;
}) {
  const classes = useStyles();
  const name = file.published?.metadata?.find((item) => item.type === "name")?.value;
  const isReferencedFile = !!file?.thumbnail?.name;

  return (
    <div
      {...{
        key: file.id,
      }}
      onClick={() => !isActive && onClick(file)}
      className={classNames(
        isActive && classes.daoDialogSectionPartActive,
        classes.daoThumbnailContainer
      )}
    >
      <div
        title={name}
        className={classes.daoThumbnailTitle}
      >
        {index + 1}
        {name && ` - ${name}`}
      </div>
      <ImageLoad
        key={file.id}
        id={isReferencedFile ? file?.thumbnail?.id : file?.thumbnail?.file?.id}
        referencedFile={isReferencedFile}
        alternativeImage={<div />}
        className={classNames(classes.daoThumbnail)}
      />
    </div>
  );
}


