import React, { useState, useEffect } from 'react';
import classNames from 'classnames';
import { FormattedMessage } from 'react-intl';
import { get } from 'lodash';
import { WarningRounded } from "@material-ui/icons";

import { useStyles } from './styles';
import { useSpacingStyles, colorYellow, colorYellowDark, colorYellowLight } from '../../styles';
import { ModulePath, Message } from '../../enums';
import { Loading, useConfiguration, PaginationEx } from '../../components';
import { useApiListSimple } from '../../common-utils';
import { useFilters } from './evidence-filters';
import { ApuEntitySimplified } from '../../types';
import { Link } from 'react-router-dom';

function ItemsList() {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();

  const { orderResultsByScore } = useConfiguration();
  const sortBy = orderResultsByScore === true ? [
    {
      field: 'name',
      type: 'SCORE',
      order: 'DESC',
      sortMode: 'MIN',
    },
  ] :
    [
      {
        field: 'name',
        type: 'FIELD',
        order: 'ASC',
        sortMode: 'MIN',
      },
    ];

  const {
    page,
    pageSize,
    apiFilters,
    updatePage,
    updatePageSize,
  } = useFilters();

  const [result, loading] = useApiListSimple({
    filters: apiFilters,
    sort: sortBy,
    offset: (page - 1) * pageSize,
    size: pageSize,
    source: "evidence-list"
  });

  const [infoEnabled, setInfoEnabled] = useState(false);

  useEffect(() => {
    if (loading) {
      setInfoEnabled(true);
    }
  }, [loading]);

  const items: ApuEntitySimplified[] = result?.items || [];
  const count = get(result, 'count');

  const maxPage = count ? Math.ceil(Math.min(count, 9999) / pageSize) : 0;

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
            {items.length &&
              <PaginationEx
              page={page}
              pageSize={pageSize}
              onUpdatePageSize={updatePageSize}
              onUpdatePage={updatePage}
              count={count}
              />
             }
            {items.map(({ id, name, description }) => (
              <Link key={id} to={`${ModulePath.APU}/${id}`}
                className={classNames(
                  classes.listItem,
                  spacingClasses.marginBottomSmall,
                  spacingClasses.padding,
                  !description && classes.listItemTitleOnly
                )}
              >
                <h4>{name || 'Neznámé'}</h4>
                {description && <p>{description}</p>}
              </Link>
            ))}
            {page === maxPage && count && count > 9999 && <div
              style={{
                width: '100%',
                padding: `0.8rem 1.25rem`,
                backgroundColor: colorYellowLight,
                borderRadius: "4px",
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
              <WarningRounded />&nbsp; <i><FormattedMessage id={Message.SEARCH_SHORTENED_RESULT_MESSAGE} /></i>
            </div>}
            {items.length &&
              <PaginationEx
              page={page}
              pageSize={pageSize}
              onUpdatePageSize={updatePageSize}
              onUpdatePage={updatePage}
              count={count}
              />
             }
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

export function EvidenceList() {
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
