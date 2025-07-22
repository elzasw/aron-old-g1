import React from 'react';
import classNames from 'classnames';
import Pagination from '@material-ui/lab/Pagination';
import { FormattedMessage } from 'react-intl';
import { KeyboardArrowLeft, KeyboardArrowRight } from '@material-ui/icons';

import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles/layout';
import { useSpacingStyles } from '../../styles';
import { pageSizeOptions, Message } from '../../enums';
import { Select, Button } from '../../components';
import { useWindowSize } from '../../common-utils';

interface Props {
  page: number;
  pageSize: number;
  count?: number;
  onUpdatePage: (page: number) => void;
  onUpdatePageSize: (pageSize: number) => void;
}

export function PaginationEx({
  page,
  pageSize,
  count,
  onUpdatePage,
  onUpdatePageSize,
}: Props) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const { width } = useWindowSize();
  const maxPage = count ? Math.ceil(Math.min(count, 9999) / pageSize) : 0;

  if (!count) {
    return <></>;
  }

  return (
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
            onUpdatePage(newPage >= 0 ? newPage : 0);
          }}
          disabled={page <= 1}
        />
        <Pagination
          count={count ? Math.ceil(Math.min(count, 9999) / pageSize) : 0}
          size={width > 600 ? 'medium' : 'small'}
          siblingCount={width < 600 ? 0 : 1}
          color="primary"
          page={page}
          onChange={(_, page) => onUpdatePage(page)}
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
            onUpdatePage(newPage <= maxPage ? newPage : maxPage);
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
            onChange={({ id }) => onUpdatePageSize(id)}
            clearable={false}
          />
        </div>
      </div>
    </div>
  );
}
