import React, { useState } from 'react';
import classNames from 'classnames';
import { useIntl } from 'react-intl';
import CircularProgress from '@material-ui/core/CircularProgress';
import { FormattedMessage } from 'react-intl';

import { Tooltip } from '@eas/common-web';

import { Search, useConfiguration } from '../../components';
import { ModulePath, Message, ApiUrl } from '../../enums';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles, useAppStyles } from '../../styles';
import {
  getPathByType,
  useGet,
  parseYaml,
  useEvidenceNavigation,
  useSearchOptions,
} from '../../common-utils';
import { ClickableSelection } from '../../components/clickable-selection/';
import { FavouriteQuery, FilterConfig } from '../../types';
import { replace } from 'lodash';

export const Body: React.FC = () => {
  const classes = useStyles();
  const appClasses = useAppStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const { otherSources } = useConfiguration();

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

  const favouriteQueries: FavouriteQuery[] | null = parseYaml(
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
          placeholder = Message.SEARCH_FUND;
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

  return (
    <div
      className={classNames(
        classes.mainBody,
        layoutClasses.flexCentered,
        spacingClasses.paddingVerticalBig
      )}
    >
      <div className={classes.mainBodyInner}>
        <div className={spacingClasses.paddingBottomBig} />
        <h2 className={appClasses.screenReaderHidden}>
          <FormattedMessage id={Message.SEARCH}/>
        </h2>
        <Search
          main={searchOptions.filter(so => so.path === ModulePath.ARCH_DESC).length > 0}
          placeholder={placeholder}
          onSearch={({ query }) => {
            const selectedPath = selectedOptions[0]?.path;
            const path = selectedPath || ModulePath.APU;

            if (query || selectedPath) {
              navigateTo(path, 1, 10, query, selectedOptions[0]?.filters);
            }
          }}
        />
        <h3 className={appClasses.screenReaderHidden}>
          <FormattedMessage id={Message.SEARCH_CHANGE_TYPE}/>
        </h3>
        <ClickableSelection
          radio={true}
          options={searchOptions}
          onChange={updateSelection}
        />
        {(favouriteQueries || loadingFavouriteQueries) && (
          <>
            <h3 className={spacingClasses.marginTopBig}>
              {formatMessage({ id: Message.FAVOURITE_QUERIES })}
            </h3>
            <div
              className={classNames(
                classes.mainFavourite,
                layoutClasses.flex,
                layoutClasses.flexWrap,
                spacingClasses.paddingBottomBig
              )}
            >
              {loadingFavouriteQueries ? (
                <CircularProgress />
              ) : (
                  (favouriteQueries || []).map(
                    ({ icon, label, tooltip, type, query, filters }) => (
                      <Tooltip key={label} title={tooltip}>
                        <div
                          onClick={() =>
                            navigateTo(
                              type ? getPathByType(type) : ModulePath.APU,
                              1,
                              10,
                              query,
                              filters
                            )
                          }
                          className={classNames(
                            layoutClasses.flexAlignCenter,
                            spacingClasses.marginBottomSmall
                          )}
                        >
                          <span className={classes.mainFavouriteIcon}>
                            <span className={icon} />
                          </span>
                          &nbsp;&nbsp;{label}
                        </div>
                      </Tooltip>
                    )
                  )
                )}
            </div>
          </>
        )}
        {otherSources && otherSources.length > 0 && (
          <>
            <h3 className={spacingClasses.marginTopBig}>
              {formatMessage({ id: Message.OTHER_SOURCES })}
            </h3>
            <div
              className={classNames(
                classes.mainFavourite,
                layoutClasses.flex,
                layoutClasses.flexWrap,
                spacingClasses.paddingBottomBig
              )}
            >
              {(
                (otherSources || []).map(
                  ({ icon, label, url, tooltip }) => (
                    <Tooltip key={label} title={tooltip}>
                      <a
                        href={url}
                        target="blank"
                        className={classNames(
                          layoutClasses.flexAlignCenter,
                          spacingClasses.marginBottomSmall,
                          classes.otherSourceLink,
                        )}
                      >
                        <span className={classes.mainFavouriteIcon}>
                          <span className={icon} />
                        </span>
                        &nbsp;&nbsp;{label}
                      </a>
                    </Tooltip>
                  )
                )
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
};
