import { BASE_URL } from '../../../enums';

export interface Props {
  thumbnailUrl?: string;
  thumbnailLink?: string;
}

export function Thumbnail({ thumbnailUrl, thumbnailLink }: Props) {
  if (!thumbnailUrl) {
    return null;
  }
  const isAbsoluteUrl = /(http|https):\/\//g.test(thumbnailUrl || '');
  // const isFileName = /.\.[\w\d,\-,_]+$/g.test(thumbnailUrl || '');
  const _thumbnailUrl = isAbsoluteUrl
    ? thumbnailUrl
    : `${BASE_URL}/app/${thumbnailUrl}`;

  if (thumbnailLink) {
    return (
      <a
        href={thumbnailLink}
        onClick={(e) => e.stopPropagation()}
        style={{ padding: '24px', paddingLeft: 0 }}
      >
        <img
          src={_thumbnailUrl}
          style={{
            height: '100%',
            maxHeight: '120px',
            maxWidth: '192px',
            flexGrow: 0,
            borderRadius: '8px',
          }}
        />
      </a>
    );
  }

  return (
    <span style={{ padding: '24px', paddingLeft: 0 }}>
      <img
        src={_thumbnailUrl}
        style={{
          height: '100%',
          maxHeight: '120px',
          maxWidth: '192px',
          flexGrow: 0,
          borderRadius: '8px',
        }}
      />
    </span>
  );
}
