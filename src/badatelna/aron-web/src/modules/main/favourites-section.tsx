import CircularProgress from '@material-ui/core/CircularProgress';
import classNames from "classnames";
import { replace } from "lodash";
import { createEvidenceUrl, getPathByType } from "../../common-utils";
import { HomeTile, Icon, IconDef, OtherSource } from "../../components";
import { FavouritesStyle, ModulePath } from "../../enums";
import { useLayoutStyles, useSpacingStyles } from "../../styles";
import { FilterConfig } from "../../types";
import { useStyles } from "./styles";

export interface Props {
  label?: string;
  items?: OtherSource[] | null;
  style?: FavouritesStyle;
  icon?: IconDef | string;
  isLoading?: boolean;
}

export function FavouritesSection({
  label,
  items = [],
  style = FavouritesStyle.TEXT,
  icon,
  isLoading,
}:Props) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const _favouritesClasses = {
    [FavouritesStyle.TEXT]: {
      main: classes.mainFavourite,
    },
    [FavouritesStyle.FILLED]: {
      main: classes.mainFavourite,
    },
    [FavouritesStyle.GRID]: {
      main: classes.mainFavouriteGrid,
    }
  }
  const sectionClasses = _favouritesClasses[style];

  function replaceSourceInItems(items: OtherSource[] = []) {
    return items.map(({ filters, ...q }) => ({
        ...q,
        filters: filters
          ? filters.map(({ source, ...f }: FilterConfig) => ({
              ...f,
              source: replace(source, '_', '~'),
            }))
          : [],
      })
    )
  }

  if ((items || []).length === 0 && !isLoading) {
    return <></>
  }

  return <section>
    {label && <h3 className={spacingClasses.marginTopBig}>
      <Icon
        icon={icon}
        className={classes.otherSourcesIcon}
      />
      &nbsp;
      {label}
    </h3>}
    <div
      className={classNames(
        layoutClasses.flex,
        layoutClasses.flexWrap,
        spacingClasses.paddingBottomBig,
        sectionClasses.main
      )}
    >
      {isLoading && <CircularProgress />}
      {!isLoading && replaceSourceInItems(items || []).map(({ url, type, query, filters, ...itemInfo }, index) => {
        return (
          <HomeTile
          {...itemInfo}
          url={url || createEvidenceUrl(
            type ? getPathByType(type) : ModulePath.APU,
            1,
            10,
            query,
            filters
          )}
          key={index}
          favouritesStyle={style}
          />
        );
      })}
    </div>
  </section>
}
