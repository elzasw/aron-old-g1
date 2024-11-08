import React, { useState, useEffect, ReactElement, ReactNode } from 'react';
import { getFile } from '../../common-utils';

interface ImageLoadProps {
  id?: string;
  alternativeImage: ReactNode;
  className: string;
  referencedFile?: boolean;
  url?: string;
}

export function ImageLoad({
  id,
  alternativeImage,
  className,
  referencedFile,
  url
}: ImageLoadProps): ReactElement {
  const [imgUrl, setImgUrl] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      if (!url) {
        const { blob } = await getFile(id, referencedFile);
        setImgUrl(blob ? URL.createObjectURL(blob) : null);
      } else {
        setImgUrl(url);
      }
    })();
  }, [id]);

  return (
    <div {...{ className }}>
      {imgUrl ? <img src={imgUrl} /> : alternativeImage}
    </div>
  );
}
