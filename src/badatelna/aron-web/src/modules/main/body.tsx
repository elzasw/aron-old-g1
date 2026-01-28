import React, { useState } from 'react';
import classNames from 'classnames';
import { useIntl } from 'react-intl';
import CircularProgress from '@material-ui/core/CircularProgress';
import { FormattedMessage } from 'react-intl';

import { OtherSource } from '../../components/configuration';

import { Search, useConfiguration, Icon } from '../../components';
import { ModulePath, Message, ApiUrl, FavouritesStyle } from '../../enums';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles, useAppStyles } from '../../styles';
import {
  getPathByType,
  useGet,
  parseYaml,
  useEvidenceNavigation,
  useSearchOptions,
  createEvidenceUrl,
} from '../../common-utils';
import { ClickableSelection, ClickableSelectionAlt } from '../../components/clickable-selection/';
import { FilterConfig } from '../../types';
import { replace } from 'lodash';
import { HomeTile } from '../../components/home-tile/home-tile';

// function getRandomInt(min: number, max: number, target: number = 0, strength: number = 0) {
//   const random = Math.random();
//   const range = max - min;
//   const targetNormalized = (target - min) / range;

//   const biased = random * (1 - strength) + targetNormalized * strength;

//   return Math.floor(biased * (range + 1)) + min;
// }

// function getRandomColor() {
//   const h = getRandomInt(0, 360, 180, 0.1);
//   const s = getRandomInt(20, 80, 50, 0.1);
//   const l = getRandomInt(20, 50, 35, 0.1);
//   return `hsl(${h}, ${s}%, ${l}%)`
// }

// function getRandomIcon() {
//   const icons = [
//     "fas fa-book",
//     "fas fa-building",
//     "fas fa-map",
//     "fas fa-house-user",
//     "fas fa-address-card",
//     "fas fa-user",
//     "fas fa-users",
//     "fas fa-user-edit",
//     "fas fa-laptop-house",
//     "fas fa-globe-americas",
//     "fas fa-horse-head",
//     "fas fa-fish",
//     "fas fa-dog",
//     "fas fa-frog",
//     "fas fa-film",
//     "fas fa-photo-video",
//     "fas fa-sitemap",
//     "fas fa-graduation-cap",
//   ]
//   return icons[getRandomInt(0, icons.length - 1)];
// }

// function randomizeFavourites(favouriteQueries: OtherSource[]):OtherSource[] {
//   return favouriteQueries.filter(() => getRandomInt(0, 1)).map((query) => {
//     const hasLongText =  (query.label?.length || "") > 20 || (query.tooltip?.length || 0) > 40;
//     const colSpan = hasLongText ? getRandomInt(1, 2) : 1;
//     const rowSpan = hasLongText && colSpan === 1 ? 2 : 1;
//     const max = 2;

//     const randomColumn = colSpan < max ? getRandomInt(colSpan, 2, 1, 0.4) : colSpan;
//     const randomRow = rowSpan < max ? getRandomInt(rowSpan, 2, 1, 0.4) : rowSpan;

//     return {
//       ...query,
//       icon: { icon: getRandomIcon() },
//       imageUrl: getRandomInt(0, 1) ? `https://picsum.photos/500/${getRandomInt(400, 500)}` : undefined,
//       columnSpan: randomColumn,
//       rowSpan: randomRow,
//       backgroundColor: getRandomColor(),
//     }
//   })
// }

export const Body: React.FC = () => {
  const classes = useStyles();
  const appClasses = useAppStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const { otherSources, otherSourcesIcon, favouritesStyle, alternativeSearchType, favouritesSections } =
    useConfiguration();

  const { formatMessage } = useIntl();

  const navigateTo = useEvidenceNavigation();

  const searchOptions = useSearchOptions();

  const [selectedOptions, setSelectedOptions] = useState<any[]>([]);
  const [placeholder, setPlaceholder] = useState<Message>(
    Message.SEARCH_NO_FILTER
  );

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

  const updateSelection = (value: any[]) => {
    const current = value[0];
    let placeholder: Message = Message.SEARCH_NO_FILTER;

    if (current) {
      switch (current.path) {
        case ModulePath.ARCH_DESC:
          placeholder = current.filters
            ? Message.SEARCH_ARCH_DESC_DAO_ONLY
            : Message.SEARCH_ARCH_DESC;
          break;
        case ModulePath.ENTITY:
          placeholder = Message.SEARCH_ENTITY;
          break;
        case ModulePath.FINDING_AID:
          placeholder = Message.SEARCH_FINDING_AID;
          break;
        case ModulePath.FUND:
          placeholder = current.filters
            ? Message.SEARCH_FUND_FA_ONLY
            : Message.SEARCH_FUND;
          break;
        case ModulePath.ORIGINATOR:
          placeholder = Message.SEARCH_ORIGINATOR;
          break;
        case ModulePath.INSTITUTION:
          placeholder = Message.SEARCH_INSTITUTION;
          break;
        default:
          break;
      }
    }

    setSelectedOptions(value);
    setPlaceholder(placeholder);
  };

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
        <Search
          main={
            searchOptions.filter((so) => so.path === ModulePath.ARCH_DESC)
              .length > 0
          }
          placeholder={placeholder}
          onSearch={({ query }) => {
            const selectedPath = selectedOptions[0]?.path;
            const path = selectedPath || ModulePath.APU;

            if (query || selectedPath) {
              navigateTo(path, 1, 10, query, selectedOptions[0]?.filters);
            }
          }}
        />
        <div>
          <h3 className={appClasses.screenReaderHidden}>
            <FormattedMessage id={Message.SEARCH_CHANGE_TYPE} />
          </h3>
          {alternativeSearchType ?
            <ClickableSelectionAlt
              radio={true}
              options={searchOptions}
              onChange={updateSelection}
            />
            : <ClickableSelection
            radio={true}
            options={searchOptions}
            onChange={updateSelection}
          />}
        </div>
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
