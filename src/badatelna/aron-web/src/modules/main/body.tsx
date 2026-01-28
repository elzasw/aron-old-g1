import CircularProgress from '@material-ui/core/CircularProgress';
import classNames from 'classnames';
import { replace } from 'lodash';
import React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import {
    createEvidenceUrl, getPathByType,
    parseYaml, useGet
} from '../../common-utils';
import { Icon, SearchMain, useConfiguration } from '../../components';
import { OtherSource } from '../../components/configuration';
import { HomeTile } from '../../components/home-tile/home-tile';
import { ApiUrl, FavouritesStyle, Message, ModulePath } from '../../enums';
import { useAppStyles, useLayoutStyles, useSpacingStyles } from '../../styles';
import { FilterConfig } from '../../types';
import { useStyles } from './styles';

export const Body: React.FC = () => {
  const classes = useStyles();
  const appClasses = useAppStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const { otherSources, otherSourcesIcon, favouritesStyle, favouritesSections } =
    useConfiguration();

  const { formatMessage } = useIntl();

  const [favouriteQueriesText, loadingFavouriteQueries] = useGet<string>(
    ApiUrl.FAVORITE_QUERY,
    { textResponse: true }
  );

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

  const favouriteQueries: OtherSource[] | null = parseYaml(favouriteQueriesText);

  const _favouritesClasses = {
    [FavouritesStyle.TEXT]: {
      mainBodyInner: classes.mainBodyInner,
      main: classes.mainFavourite,
    },
    [FavouritesStyle.FILLED]: {
      mainBodyInner: classes.mainBodyInner,
      main: classes.mainFavourite,
    },
    [FavouritesStyle.GRID]: {
      mainBodyInner: classes.mainBodyInnerGrid,
      main: classes.mainFavouriteGrid,
    }
  }

  const favouritesClasses = _favouritesClasses[favouritesStyle || FavouritesStyle.TEXT];

  return (
    <div
      className={classNames(
        classes.mainBody,
        layoutClasses.flexCentered,
        spacingClasses.paddingVerticalBig
      )}
    >
      <div className={favouritesClasses.mainBodyInner}>
        <div className={spacingClasses.paddingBottomBig} />
        <h2 className={appClasses.screenReaderHidden}>
          <FormattedMessage id={Message.SEARCH} />
        </h2>
        <div className={spacingClasses.paddingBottomBig} />
        <SearchMain />
        <div className={spacingClasses.paddingBottomBig} />
        {(favouriteQueries || loadingFavouriteQueries) && (
          <>
            <h3 className={spacingClasses.marginTopBig}>
              {formatMessage({ id: Message.FAVOURITE_QUERIES })}
            </h3>
            <div
              className={classNames(
                layoutClasses.flex,
                layoutClasses.flexWrap,
                spacingClasses.paddingBottomBig,
                favouritesClasses.main
              )}
            >
              {loadingFavouriteQueries ? (
                <CircularProgress />
              ) : (
                replaceSourceInItems(favouriteQueries || []).map(
                  ({ type, query, filters, url, ...itemInfo }, index) => {
                    return <HomeTile
                    {...itemInfo}
                    favouritesStyle={favouritesStyle}
                    key={index}
                    url={url || createEvidenceUrl(
                      type ? getPathByType(type) : ModulePath.APU,
                      1,
                      10,
                      query,
                      filters
                    )}
                    />
                  }
                )
              )}
            </div>
          </>
        )}
        {(otherSources || []).length > 0 && (
          <>
            <h3 className={spacingClasses.marginTopBig}>
              <Icon
                icon={otherSourcesIcon}
                className={classes.otherSourcesIcon}
              />
              &nbsp;
              {formatMessage({ id: Message.OTHER_SOURCES })}
            </h3>
            <div
              className={classNames(
                layoutClasses.flex,
                layoutClasses.flexWrap,
                spacingClasses.paddingBottomBig,
                favouritesClasses.main
              )}
            >
              {replaceSourceInItems(otherSources || []).map(({ url, type, query, filters, ...itemInfo }, index) => {
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
                    favouritesStyle={favouritesStyle}
                  />
                );
              })}
            </div>
          </>
        )}
        {(favouritesSections || []).map(({style = FavouritesStyle.TEXT, icon, items, label}) => {
          const sectionClasses = _favouritesClasses[style];
          return <section>
            <h3 className={spacingClasses.marginTopBig}>
              <Icon
                icon={icon}
                className={classes.otherSourcesIcon}
              />
              &nbsp;
              {label}
            </h3>
            <div
              className={classNames(
                layoutClasses.flex,
                layoutClasses.flexWrap,
                spacingClasses.paddingBottomBig,
                sectionClasses.main
              )}
            >
              {replaceSourceInItems(items || []).map(({ url, type, query, filters, ...itemInfo }, index) => {
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
        })}
      </div>
    </div>
  );
};
