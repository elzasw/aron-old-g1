import { Tooltip } from "@eas/common-web";
// import Tooltip from '@material-ui/core/Tooltip';
// import { Tooltip } from "../tooltip";
import { Link } from "react-router-dom";

import classNames from "classnames";
import { useLayoutStyles, useSpacingStyles } from "../../styles";
import { useStyles } from "./styles";
import { Icon } from "../icon";
import { FavouritesStyle, BASE_URL } from "../../enums";
import { IconDef } from "../configuration";
import { forwardRef, PropsWithChildren } from "react";
import { BackgroundImageDef } from "../configuration/types";

export interface Props{
  icon?: IconDef | string;
  label?: string;
  tooltip?: string;
  url?: string;
  localUrl?: string;
  backgroundImage?: BackgroundImageDef;
  columnSpan?: number;
  rowSpan?: number;
  backgroundColor?: string;
  favouritesStyle?: FavouritesStyle;
}

interface UniversalLinkProps extends React.AnchorHTMLAttributes<HTMLAnchorElement> {
  url?: string, isLocal?: boolean
}

const UniversalLink = forwardRef<HTMLAnchorElement, PropsWithChildren<UniversalLinkProps>>(
  ({ url, isLocal, ...otherProps }: PropsWithChildren<UniversalLinkProps>, ref) => {
  if (!url) {
    return <a {...otherProps}/>
  }

  const isAbsoluteUrl = /(http|https):\/\//g.test(url || '');
  // const _isLocal = isLocal || /.\.[\w\d,\-,_]+$/g.test(url || '');

  if (isAbsoluteUrl) {
    return <a ref={ref} {...otherProps} href={url}>{otherProps.children}</a>
  }
  else if (!isAbsoluteUrl) {
    return <Link ref={ref} {...otherProps} to={url}>{otherProps.children}</Link>
  }

  console.error(`Incorrect url: ${url}`)
  return <a {...otherProps}>{url}</a>
})

export function HomeTile({
  icon,
  label,
  tooltip,
  url,
  backgroundImage,
  columnSpan,
  rowSpan,
  backgroundColor,
  favouritesStyle = FavouritesStyle.TEXT
}: Props) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  // const hasImage = imageUrl != undefined;

  const imageUrl = backgroundImage?.url

  const isAbsoluteImageUrl = /(http|https):\/\//g.test(imageUrl || '');
  const _imageUrl = isAbsoluteImageUrl
    ? imageUrl
    : `${BASE_URL}/app/${imageUrl}`


  const favouritesClasses = {
    [FavouritesStyle.TEXT]: {
      icon: classes.mainFavouriteIcon,
      item: classes.mainFavouriteItem,
    },
    [FavouritesStyle.FILLED]: {
      icon: classes.mainFavouriteIconAlt,
      item: classes.mainFavouriteItemAlt,
    },
    [FavouritesStyle.GRID]: {
      icon: classes.mainFavouriteIconGrid,
      item: classes.mainFavouriteItemGrid,
      background: classes.mainFavouriteItemGridBackground,
      label: classes.mainFavouriteLabelGrid,
      labelWrapper: classes.mainFavouriteLabelWrapperGrid,
    }
    }[favouritesStyle]

  return <Tooltip title={favouritesStyle !== FavouritesStyle.GRID ? tooltip : undefined}>
    <UniversalLink
      url={url}
      style={{
        gridColumn: columnSpan && `span ${columnSpan}`,
        gridRow: rowSpan && `span ${rowSpan}`,
      }}
      className={classNames(
        layoutClasses.flexAlignCenter,
        spacingClasses.marginBottomSmall,
        favouritesClasses.item,
      )}
    >
      {favouritesStyle === FavouritesStyle.GRID && <div
        style={{
          backgroundImage: _imageUrl && `url('${_imageUrl}')`,
          backgroundColor: backgroundColor,
          backgroundPositionX: backgroundImage?.positionX,
          backgroundPositionY: backgroundImage?.positionY,
        }}
        className={favouritesClasses.background}
      ></div>}
      {icon && <span className={favouritesClasses.icon} >
        <Icon icon={icon} />
      </span>}
      <div className={favouritesClasses.labelWrapper}>
        <div>
          <span className={favouritesClasses.label}>{label}</span>
        </div>
        {tooltip && favouritesStyle === FavouritesStyle.GRID && <div>
          <span
            style={{ fontWeight: 'normal', fontSize: '1em' }}
            className={favouritesClasses.label}
          >
            <i>{tooltip}</i>
          </span>
        </div>}
      </div>
    </UniversalLink>
  </Tooltip>
}
