import React, { useState, useEffect } from 'react';
import classNames from 'classnames';
import Pagination from '@material-ui/lab/Pagination';
import { FormattedMessage } from 'react-intl';
import { get } from 'lodash';
import { KeyboardArrowLeft, KeyboardArrowRight, WarningRounded } from "@material-ui/icons";

import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles/layout';
import { useSpacingStyles, colorRed, colorWhite } from '../../styles';
import { ModulePath, pageSizeOptions, Message } from '../../enums';
import { Select, Loading, useConfiguration, Button } from '../../components';
import { useWindowSize, useApiListSimple } from '../../common-utils';
import { useFilters } from './evidence-filters';
import { ApuEntitySimplified } from '../../types';
import { Link } from 'react-router-dom';

function ItemsList() {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
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

  const { width } = useWindowSize();

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
            {items.map(({ id, name, description }) => (
              <Link key={id} to={`${ModulePath.APU}/${id}`}
                className={classNames(
                  classes.listItem,
                  spacingClasses.marginBottomSmall,
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
                backgroundColor: colorRed,
                borderRadius: "4px",
                color: colorWhite,
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
            {items.length ? (
              <div
                className={classNames(
                  layoutClasses.flexSpaceBetween,
                  layoutClasses.flexColumnPhone,
                  spacingClasses.paddingTopSmall
                )}
              >
                <div style={{ display: "flex" }}>
                  <Button
                    className={width > 600 ? classes.paginationButton : classes.paginationButton_small}
                    label={<><KeyboardArrowLeft style={{ height: "0.8em" }} /><KeyboardArrowLeft style={{ marginLeft: "-17px", height: "0.8em" }} /></>}
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
                    className={width > 600 ? classes.paginationButton : classes.paginationButton_small}
                    label={<><KeyboardArrowRight style={{ height: "0.8em" }} /><KeyboardArrowRight style={{ marginLeft: "-17px", height: "0.8em" }} /></>}
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
