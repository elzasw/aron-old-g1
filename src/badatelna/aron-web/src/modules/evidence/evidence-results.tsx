import React, { useState, useEffect } from 'react';
import classNames from 'classnames';
import Pagination from '@material-ui/lab/Pagination';
import { FormattedMessage } from 'react-intl';
import { get } from 'lodash';
import {
  KeyboardArrowLeft,
  KeyboardArrowRight,
  WarningRounded,
} from '@material-ui/icons';

import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles/layout';
import {
  useSpacingStyles,
  colorYellow,
  colorYellowDark,
  colorYellowLight,
} from '../../styles';
import { ModulePath, pageSizeOptions, Message, BASE_URL } from '../../enums';
import { Select, Loading, useConfiguration, Button } from '../../components';
import {
  useWindowSize,
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
}

function ItemsList() {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

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

  const [listConfig, loadingListConfig] =
    useBaseGet<ListConfig>('/app/config.json');
  console.log('#lc', listConfig, loadingListConfig);

  const [infoEnabled, setInfoEnabled] = useState(false);

  const { width } = useWindowSize();

  useEffect(() => {
    if (loading) {
      setInfoEnabled(true);
    }
  }, [loading]);

  const items: EvidenceResultItem[] = result?.items || [];
  const count = get(result, 'count');

  const maxPage = count ? Math.ceil(Math.min(count, 9999) / pageSize) : 0;

  const renderItem = ({ tn, l, id, t }: EvidenceResultItem) => {
    const typeIcon = listConfig?.typeIcons.find(({ type }) => type === t);
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
          <div style={{ flexGrow: 0, flexShrink: 0, marginRight: '6px' }}>
            <img
              src={`${BASE_URL}/app/${typeIcon.icon}`}
              style={{ width: `${listConfig?.typeIconSize}px` }}
            />
          </div>
        )}
        <div style={{ flexGrow: 1 }}>
          {l.map((lineItems) => {
            return (
              <div>
                {lineItems.map(({ t, v }) => {
                  const typeProperties = listConfig?.itemProperties.find(
                    ({ type }) => type === t
                  );

                  const style: React.CSSProperties = {
                    display: 'flex',
                    alignItems: 'center',
                  };
                  const linkStyle: React.CSSProperties = {};

                  if (typeProperties) {
                    style.fontSize = `${1 + (typeProperties.size - 1) * 0.1}em`;
                    style.color = typeProperties.color;
                    style.fontWeight = typeProperties.bold ? 'bold' : 'normal';
                    linkStyle.color = 'currentColor';
                  }
                  console.log('#er', t, v, l, items);
                  const _element =
                    typeProperties?.class === 'header' ? 'h4' : 'span';
                  return (
                    <_element style={style}>
                      {typeProperties?.icon && (
                        <img
                          style={{ height: '1.4em' }}
                          src={`${BASE_URL}/app/${typeProperties?.icon}`}
                        />
                      )}
                      {typeProperties?.prefix}
                      {v?.map(({ v, id }) =>
                        id ? (
                          <Link
                            style={linkStyle}
                            to={`${ModulePath.APU}/${id}`}
                          >
                            {v}
                          </Link>
                        ) : (
                          <span>{v}</span>
                        )
                      )}
                    </_element>
                  );
                })}
              </div>
            );
          })}
        </div>
        {tn && (
          <div>
            <img
              src={tn}
              style={{
                height: '100%',
                maxHeight: '80px',
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
          <div
            className={classNames(
              spacingClasses.padding,
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
            {items.length ? (
              <div
                className={classNames(
                  layoutClasses.flexSpaceBetween,
                  layoutClasses.flexColumnPhone,
                  spacingClasses.paddingTopSmall
                )}
              >
                <div style={{ display: 'flex' }}>
                  <Button
                    className={
                      width > 600
                        ? classes.paginationButton
                        : classes.paginationButton_small
                    }
                    label={
                      <>
                        <KeyboardArrowLeft style={{ height: '0.8em' }} />
                        <KeyboardArrowLeft
                          style={{ marginLeft: '-17px', height: '0.8em' }}
                        />
                      </>
                    }
                    onClick={() => {
                      const newPage = page - 10;
                      updatePage(newPage >= 0 ? newPage : 0);
                    }}
                    disabled={page <= 1}
                  />
                  <Pagination
                    count={
                      count ? Math.ceil(Math.min(count, 9999) / pageSize) : 0
                    }
                    size={width > 600 ? 'medium' : 'small'}
                    siblingCount={width < 600 ? 0 : 1}
                    color="primary"
                    page={page}
                    onChange={(_, page) => updatePage(page)}
                    className={spacingClasses.marginBottomSmall}
                  />
                  <Button
                    className={
                      width > 600
                        ? classes.paginationButton
                        : classes.paginationButton_small
                    }
                    label={
                      <>
                        <KeyboardArrowRight style={{ height: '0.8em' }} />
                        <KeyboardArrowRight
                          style={{ marginLeft: '-17px', height: '0.8em' }}
                        />
                      </>
                    }
                    onClick={() => {
                      const newPage = page + 10;
                      updatePage(newPage <= maxPage ? newPage : maxPage);
                    }}
                    disabled={page >= maxPage}
                  />
                </div>
                <div
                  className={classNames(
                    classes.listPageNumber,
                    spacingClasses.marginBottomSmall
                  )}
                >
                  <span>
                    <FormattedMessage id={Message.PER_PAGE} />
                    :&nbsp;&nbsp;&nbsp;
                  </span>
                  <div className={classes.pageSizeSelect}>
                    <Select
                      value={{ id: pageSize.toString(), name: pageSize }}
                      options={pageSizeOptions}
                      onChange={({ id }) => updatePageSize(id)}
                      clearable={false}
                    />
                  </div>
                </div>
              </div>
            ) : (
              <></>
            )}
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
