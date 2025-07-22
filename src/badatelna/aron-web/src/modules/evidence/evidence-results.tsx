import React, { useState, useEffect, useRef } from 'react';
import classNames from 'classnames';
import { FormattedMessage } from 'react-intl';
import { get } from 'lodash';
import { WarningRounded } from '@material-ui/icons';

import { useStyles } from './styles';
import {
  useSpacingStyles,
  colorYellow,
  colorYellowDark,
  colorYellowLight,
} from '../../styles';
import { ModulePath, Message, BASE_URL } from '../../enums';
import { Loading, useConfiguration, PaginationEx } from '../../components';
import {
  // useApiListSimple,
  useBaseGet,
  useApiListResultsSimple,
  EvidenceResultItem,
} from '../../common-utils';
import { useFilters } from './evidence-filters';
// import { ApuEntitySimplified } from '../../types';
import { Link } from 'react-router-dom';

interface ListConfigItemProperties {
  type: string;
  class: 'header' | 'text';
  color: 'string';
  size: number;
  bold: boolean;
  prefix: string;
  icon: string;
  separator?: string;
}

interface ListConfigTypeIcon {
  type: string;
  icon: string;
}

interface ListConfig {
  typeIcon: boolean;
  itemProperties: ListConfigItemProperties[];
  typeIcons: ListConfigTypeIcon[];
  typeIconSize: number;
  itemSeparator?: string;
}

function ItemsList() {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();
  const firstElementRef = useRef<HTMLDivElement>(null);

  const { orderResultsByScore } = useConfiguration();
  const sortBy =
    orderResultsByScore === true
      ? [
          {
            field: 'name',
            type: 'SCORE',
            order: 'DESC',
            sortMode: 'MIN',
          },
        ]
      : [
          {
            field: 'name',
            type: 'FIELD',
            order: 'ASC',
            sortMode: 'MIN',
          },
        ];

  const { page, pageSize, apiFilters, updatePage, updatePageSize } =
    useFilters();

  const [result, loading] = useApiListResultsSimple({
    filters: apiFilters,
    sort: sortBy,
    offset: (page - 1) * pageSize,
    size: pageSize,
    source: 'evidence-list',
  });

  const [listConfig /* , loadingListConfig */] =
    useBaseGet<ListConfig>('/app/config.json');

  const [infoEnabled, setInfoEnabled] = useState(false);

  useEffect(() => {
    if (loading) {
      setInfoEnabled(true);
    }
  }, [loading]);

  const items: EvidenceResultItem[] = result?.items || [];
  const count = get(result, 'count');

  const maxPage = count ? Math.ceil(Math.min(count, 9999) / pageSize) : 0;

  function handleUpdatePage(page: number) {
    firstElementRef.current?.scrollIntoView({ behavior: 'smooth' });
    setTimeout(() => updatePage(page), 100);
  }

  function handleUpdatePageSize(page: number) {
    firstElementRef.current?.scrollIntoView({ behavior: 'smooth' });
    setTimeout(() => updatePageSize(page), 100);
  }

  const renderItem = ({ tn, l, id, t }: EvidenceResultItem) => {
    const typeIcon = listConfig?.typeIcons.find(({ type }) => type === t);
    const typeIconSize = listConfig?.typeIconSize || 24;
    return (
      <Link
        key={id}
        to={`${ModulePath.APU}/${id}`}
        className={classNames(
          classes.listItem,
          spacingClasses.marginBottomSmall
          // !description && classes.listItemTitleOnly
        )}
        style={{ display: 'flex' }}
      >
        {typeIcon && (
          <div
            style={{
              flexGrow: 0,
              flexShrink: 0,
              padding: '16px',
              // borderRight: '1px solid #ddd',
              paddingRight: '0px',
              marginRight: `${typeIconSize / 2}px`,
              background: 'hsl(210, 20%, 30%)',
            }}
          >
            <img
              src={`${BASE_URL}/app/${typeIcon.icon}`}
              style={{
                width: `${typeIconSize}px`,
                marginRight: `-${typeIconSize / 2}px`,
              }}
            />
          </div>
        )}
        <div style={{ flexGrow: 1, padding: '24px' }}>
          {l.map((lineItems) => {
            return (
              <div style={{ display: 'flex', flexWrap: 'wrap' }}>
                {lineItems.map(({ t, v }, index) => {
                  const typeProperties = listConfig?.itemProperties.find(
                    ({ type }) => type === t
                  );

                  const style: React.CSSProperties = {
                    display: 'flex',
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    margin: '0',
                  };
                  const valueStyle: React.CSSProperties = {
                    // whiteSpace: 'nowrap',
                  };
                  const linkStyle: React.CSSProperties = {};

                  if (typeProperties) {
                    style.fontSize = `${1 + (typeProperties.size - 1) * 0.1}em`;
                    style.color = typeProperties.color;
                    style.fontWeight = typeProperties.bold ? 'bold' : 'normal';
                  }

                  linkStyle.color = 'currentColor';

                  const _element =
                    typeProperties?.class === 'header' ? 'h4' : 'span';
                  return (
                    <_element style={style}>
                      {typeProperties?.icon && (
                        <img
                          style={{ height: '1.4em', marginRight: '4px' }}
                          src={`${BASE_URL}/app/${typeProperties?.icon}`}
                        />
                      )}
                      <span style={{ whiteSpace: 'pre' }}>
                        {typeProperties?.prefix}
                      </span>
                      {v?.map(({ v: value, id }, index) => (
                        <>
                          {id ? (
                            <Link
                              style={{
                                ...linkStyle,
                                ...valueStyle,
                                marginRight:
                                  index < v.length - 1 &&
                                  typeProperties?.separator === undefined
                                    ? '4px'
                                    : 0,
                              }}
                              to={`${ModulePath.APU}/${id}`}
                            >
                              {value}
                            </Link>
                          ) : (
                            <span
                              style={{
                                ...valueStyle,
                                marginRight:
                                  index < v.length - 1 &&
                                  typeProperties?.separator === undefined
                                    ? '4px'
                                    : 0,
                              }}
                            >
                              {value}
                            </span>
                          )}
                          {index < v.length - 1 && (
                            <span style={{ whiteSpace: 'pre' }}>
                              {typeProperties?.separator || ' '}
                            </span>
                          )}
                        </>
                      ))}
                      {index < lineItems.length - 1 && (
                        <span style={{ whiteSpace: 'pre' }}>
                          {listConfig?.itemSeparator || ' | '}
                        </span>
                      )}
                    </_element>
                  );
                })}
              </div>
            );
          })}
        </div>
        {tn && (
          <div style={{ padding: '24px', paddingLeft: 0 }}>
            <img
              src={tn}
              style={{
                height: '100%',
                maxHeight: '120px',
                maxWidth: '192px',
                flexGrow: 0,
                borderRadius: '8px',
              }}
            />
          </div>
        )}
      </Link>
    );
  };

  return (
    <div className={classes.list}>
      {loading || items.length ? (
        <>
          <Loading {...{ loading }} />
          <div ref={firstElementRef} />
          <PaginationEx
            page={page}
            pageSize={pageSize}
            count={count}
            onUpdatePage={updatePage}
            onUpdatePageSize={updatePageSize}
          />
          <div
            className={classNames(
              spacingClasses.paddingSmall,
              spacingClasses.paddingBottomSmall
            )}
          >
            {items.map((item) => renderItem(item))}
            {page === maxPage && count && count > 9999 && (
              <div
                style={{
                  width: '100%',
                  padding: `0.8rem 1.25rem`,
                  backgroundColor: colorYellowLight,
                  borderRadius: '4px',
                  color: colorYellowDark,
                  border: `1px solid ${colorYellow}`,
                  display: 'flex',
                  alignItems: 'center',
                }}
                className={classNames(
                  spacingClasses.marginBottomSmall,
                  classes.listItemTitleOnly
                )}
              >
                <WarningRounded />
                &nbsp;{' '}
                <i>
                  <FormattedMessage
                    id={Message.SEARCH_SHORTENED_RESULT_MESSAGE}
                  />
                </i>
              </div>
            )}
            <PaginationEx
              page={page}
              pageSize={pageSize}
              count={count}
              onUpdatePage={handleUpdatePage}
              onUpdatePageSize={handleUpdatePageSize}
            />
          </div>
        </>
      ) : infoEnabled ? (
        <div className={classNames(classes.listEmpty, spacingClasses.padding)}>
          <FormattedMessage id={Message.NO_ITEMS_FOUND} />
        </div>
      ) : (
        <></>
      )}
    </div>
  );
}

export function EvidenceResults() {
  const { initialized, loading } = useFilters();
  const classes = useStyles();

  return (
    <div className={classes.listContainer}>
      {initialized ? (
        <ItemsList />
      ) : (
        <Loading {...{ loading: !initialized || loading }} />
      )}
    </div>
  );
}
