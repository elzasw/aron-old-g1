import React, { useState, useMemo } from 'react';
import classNames from 'classnames';
import { useIntl } from 'react-intl';
import CircularProgress from '@material-ui/core/CircularProgress';
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router-dom';

import { Tooltip, useLocalStorage } from '@eas/common-web';

import { Search, useConfiguration, Icon, FavouritesStyle } from '../../components';
import { ModulePath, Message, ApiUrl } from '../../enums';
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
import { FavouriteQuery, FilterConfig, FavouriteQueryEx } from '../../types';
import { replace } from 'lodash';

// function _getRandomInt(min: number, max: number) {
//   // const random = Math.random();
//   // const number = random >= 0.5 ? Math.ceil(random * max) + min : Math.floor(random * max) + min;
//   // return Math.min(number, max);
//   return Math.floor(Math.random() * (max - min + 1)) + min
//   // return Math.floor(Math.random() * max) + min;
// }

function getRandomInt(min: number, max: number, target: number = 0, strength: number = 0) {
  const random = Math.random();
  const range = max - min;
  const targetNormalized = (target - min) / range;

  const biased = random * (1 - strength) + targetNormalized * strength;

  return Math.floor(biased * (range + 1)) + min;
}

function getRandomColor() {
  const h = getRandomInt(0, 360, 180, 0.1);
  const s = getRandomInt(20, 80, 50, 0.1);
  const l = getRandomInt(20, 50, 35, 0.1);
  return `hsl(${h}, ${s}%, ${l}%)`
}

function getRandomIcon() {
  const icons = [
    "fas fa-book",
    "fas fa-building",
    "fas fa-map",
    "fas fa-house-user",
    "fas fa-address-card",
    "fas fa-user",
    "fas fa-users",
    "fas fa-user-edit",
    "fas fa-laptop-house",
    "fas fa-globe-americas",
    "fas fa-horse-head",
    "fas fa-fish",
    "fas fa-dog",
    "fas fa-frog",
    "fas fa-film",
    "fas fa-photo-video",
    "fas fa-sitemap",
    "fas fa-graduation-cap",
  ]
  return icons[getRandomInt(0, icons.length - 1)];
}

function randomizeFavourites(favouriteQueries: FavouriteQuery[]):FavouriteQueryEx[] {
  return favouriteQueries.filter(() => getRandomInt(0, 1)).map((query) => {
    const hasLongText =  query.label.length > 20 || (query.tooltip?.length || 0) > 40;
    const colSpan = hasLongText ? getRandomInt(1, 2) : 1;
    const rowSpan = hasLongText && colSpan === 1 ? 2 : 1;
    const max = 2;

    const randomColumn = colSpan < max ? getRandomInt(colSpan, 2, 1, 0.4) : colSpan;
    const randomRow = rowSpan < max ? getRandomInt(rowSpan, 2, 1, 0.4) : rowSpan;

    return {
      ...query,
      icon: getRandomIcon(),
      imageUrl: getRandomInt(0, 1) ? `https://picsum.photos/500/${getRandomInt(400, 500)}` : undefined,
      columnSpan: randomColumn,
      rowSpan: randomRow,
      backgroundColor: getRandomColor(),
    }
  })
}

export const Body: React.FC = () => {
  const classes = useStyles();
  const appClasses = useAppStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const { otherSources, otherSourcesIcon, favouritesStyle: _favouritesStyle, alternativeSearchType, devMode } =
    useConfiguration();
  const [favouritesStyle, setFavouritesStyle] = useLocalStorage("favouritesStyle", _favouritesStyle || FavouritesStyle?.TEXT);
  const [randomize, setRandomize] = useLocalStorage("randomizeFavourites", false);

  const favouritesStyles = [
    FavouritesStyle.TEXT,
    FavouritesStyle.FILLED,
    FavouritesStyle.GRID,
  ]

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

  const _favouriteQueries: FavouriteQuery[] | null = parseYaml(
    favouriteQueriesText
  )?.map(({ filters, ...q }: FavouriteQuery) => ({
    ...q,
    filters: filters
      ? filters.map(({ source, ...f }: FilterConfig) => ({
          ...f,
          source: replace(source, '_', '~'),
        }))
      : [],
  }));

  const favouriteQueries = useMemo(() => {
    if (randomize && _favouriteQueries) {
      const randomFavs = randomizeFavourites(_favouriteQueries);
      return randomFavs;
    } else {
      return _favouriteQueries?.map((query) => {
        const hasLongText =  query.label.length > 20 || (query.tooltip?.length || 0) > 40;
        const colSpan = hasLongText ? getRandomInt(1, 2) : 1;
        const rowSpan = hasLongText && colSpan === 1 ? 2 : 1;
        const max = 2;

        return {
          ...query,
          columnSpan: colSpan < max ? getRandomInt(colSpan, 2, 1, 0.5) : colSpan,
          rowSpan: rowSpan < max ? getRandomInt(rowSpan, 2, 1, 0.5) : rowSpan,
        }
      }) as FavouriteQueryEx[] | null;
    }
  }, [randomize, favouriteQueriesText]);

  console.log(favouriteQueries);

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

  function handleCycleFavouritesStyle() {
    let index = favouritesStyles.indexOf(favouritesStyle);
    if (index >= favouritesStyles.length - 1) { index = 0; }
    else { index++; }

    setFavouritesStyle(favouritesStyles[index]);
  }

  function handleToggleRandomize() {
    setRandomize(!randomize);
  }

  const favouritesClasses = {
    [FavouritesStyle.TEXT]: {
      mainBodyInner: classes.mainBodyInner,
      main: classes.mainFavourite,
      icon: classes.mainFavouriteIcon,
      item: classes.mainFavouriteItem,
    },
    [FavouritesStyle.FILLED]: {
      mainBodyInner: classes.mainBodyInner,
      main: classes.mainFavourite,
      icon: classes.mainFavouriteIconAlt,
      item: classes.mainFavouriteItemAlt,
    },
    [FavouritesStyle.GRID]: {
      mainBodyInner: classes.mainBodyInnerGrid,
      main: classes.mainFavouriteGrid,
      icon: classes.mainFavouriteIconGrid,
      item: classes.mainFavouriteItemGrid,
      background: classes.mainFavouriteItemGridBackground,
      label: classes.mainFavouriteLabelGrid,
      labelWrapper: classes.mainFavouriteLabelWrapperGrid,
    }
    }[favouritesStyle]

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
        {alternativeSearchType && <div>
          <h3 className={appClasses.screenReaderHidden}>
            <FormattedMessage id={Message.SEARCH_CHANGE_TYPE} />
          </h3>
          <ClickableSelectionAlt
            radio={true}
            options={searchOptions}
            onChange={updateSelection}
          />
        </div>}
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
        {!alternativeSearchType && <div>
          <h3 className={appClasses.screenReaderHidden}>
            <FormattedMessage id={Message.SEARCH_CHANGE_TYPE} />
          </h3>
          <ClickableSelection
            radio={true}
            options={searchOptions}
            onChange={updateSelection}
          />
        </div>}
        <div className={spacingClasses.paddingBottomBig} />
        {devMode && <div style={{ position: "fixed", left: 0, bottom: 0, opacity: 0.2, height: 20 }}>
          <button className={classes.hiddenSettingsButton} onClick={handleCycleFavouritesStyle}></button>
          <button className={classes.hiddenSettingsButton} onClick={handleToggleRandomize}></button>
        </div>}
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
                (favouriteQueries || []).map(
                  ({ icon, label, tooltip, type, query, filters, imageUrl, columnSpan, rowSpan, backgroundColor }) => {
                    const hasImage = imageUrl != undefined;

                    return <Tooltip key={label} title={favouritesStyle === FavouritesStyle.GRID ? undefined : tooltip}>
                      <Link
                        to={createEvidenceUrl(
                          type ? getPathByType(type) : ModulePath.APU,
                          1,
                          10,
                          query,
                          filters
                        )}
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
                            backgroundImage: imageUrl && `url('${imageUrl}')`,
                            backgroundColor: backgroundColor,
                          }}
                          className={favouritesClasses.background}
                        ></div>}
                        {(!hasImage || favouritesStyle !== FavouritesStyle.GRID) && <span
                          className={
                            favouritesClasses.icon
                          }
                        >
                          <span className={icon} />
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
                      </Link>
                    </Tooltip>
                  }
                )
              )}
            </div>
          </>
        )}
        {otherSources && otherSources.length > 0 && (
          <>
            <h3 className={spacingClasses.marginTopBig}>
              <Icon
                icon={otherSourcesIcon?.icon}
                background={otherSourcesIcon?.background}
                radius={otherSourcesIcon?.radius}
                color={otherSourcesIcon?.color}
                size={otherSourcesIcon?.size}
                padding={otherSourcesIcon?.padding}
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
              {(otherSources || []).map(({ icon, label, url, tooltip, imageUrl }, index) => {
                return (
                  <Tooltip key={index} title={favouritesStyle === FavouritesStyle.GRID ? undefined : tooltip}>
                    <a
                      href={url}
                      target="blank"
                      style={{
                        gridColumn: `span ${randomize ? getRandomInt(1, 2) : 1}`,
                        gridRow: `span ${randomize ? getRandomInt(1, 2) : 1}`,
                        // background: hasImage ? undefined : getRandomColor(),
                      }}
                      className={classNames(
                        layoutClasses.flexAlignCenter,
                        spacingClasses.marginBottomSmall,
                        classes.otherSourceLink,
                        favouritesClasses.item
                      )}
                    >
                      {favouritesStyle === FavouritesStyle.GRID && <div style={{
                        backgroundImage: imageUrl && `url('${imageUrl}')`,
                        backgroundColor: randomize ? getRandomColor() : undefined,
                      }} className={favouritesClasses.background}></div>}
                      {(!imageUrl || favouritesStyle !== FavouritesStyle.GRID) && <span className={ favouritesClasses.icon } >
                        <Icon
                          background={icon?.background}
                          radius={icon?.radius}
                          icon={icon?.icon}
                          color={icon?.color}
                          size={icon?.size}
                          padding={icon?.padding}
                        />
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
                    </a>
                  </Tooltip>
                );
              })}
            </div>
          </>
        )}
      </div>
    </div>
  );
};
