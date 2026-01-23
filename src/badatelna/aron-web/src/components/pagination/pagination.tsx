import React, { useState } from 'react';
import classNames from 'classnames';
import Pagination from '@material-ui/lab/Pagination';
import { FormattedMessage } from 'react-intl';
import { KeyboardArrowLeft, KeyboardArrowRight } from '@material-ui/icons';
import MUIButton from '@material-ui/core/Button';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';

import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles/layout';
import { useSpacingStyles } from '../../styles';
import { pageSizeOptions, Message } from '../../enums';
import { Button } from '../../components';
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
  const [anchorElement, setAnchorElement] = useState<HTMLButtonElement | null>(null);

  if (!count) {
    return <></>;
  }

  function handleUpdatePageSize(size: number){
    onUpdatePageSize(size);
    setAnchorElement(null);
  }

  return (
    <div
      className={classNames(
        layoutClasses.flexSpaceBetween,
        layoutClasses.flexColumnPhone,
        spacingClasses.paddingSmall
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
        <div >
              <MUIButton
              onClick={({currentTarget}) => setAnchorElement(currentTarget)}
              variant="outlined"
              size="small"
              className={classes.pageSizeSelect}
              endIcon={<ArrowDropDownIcon/>}
              ><span style={{whiteSpace: "nowrap"}}>{pageSize.toString()}</span></MUIButton>
              <Menu
                open={Boolean(anchorElement)}
                anchorEl={anchorElement}
                onClose={() => setAnchorElement(null)}
                anchorOrigin={{vertical: "bottom", horizontal: "left"}}
                transformOrigin={{vertical: "top", horizontal: "left"}}
              >
                {pageSizeOptions.map(({name}) => {
                  return <MenuItem onClick={() => handleUpdatePageSize(name)} disableRipple={false}>{name}</MenuItem>
                })}
              </Menu>
        </div>
      </div>
    </div>
  );
}
