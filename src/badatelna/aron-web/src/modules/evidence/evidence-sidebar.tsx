import React, { useState, useRef, useCallback } from 'react';
import classNames from 'classnames';
import { cloneDeep, isArray } from 'lodash';
import IconButton from '@material-ui/core/IconButton';
import ChevronRight from '@material-ui/icons/ChevronRight';
import { useIntl } from 'react-intl';
import { DialogHandle, useEventCallback } from '@eas/common-web';

import { Button, Search, Loading } from '../../components';
import { useStyles } from './styles';
import {
  useSpacingStyles,
  // appHeaderHeight,
  // breadcrumbsHeight,
} from '../../styles';
import { SidebarProps } from './types';
import { FilterComponent } from './utils';
import { FilterConfig } from '../../types';
import { Message, FacetDisplay, ModulePath } from '../../enums';
import { FilterDialog } from './sidebar-content';
import { useFilters } from './evidence-filters';
import {
  // useHeight,
  // iOS,
  useEvidenceNavigation,
  useSearchOptions,
} from '../../common-utils';

export const EvidenceSidebar: React.FC<SidebarProps> = ({
  apuPartItemTypes,
  facets,
  path,
}) => {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();

  const { formatMessage } = useIntl();

  const {
    loading,
    initialized,
    query,
    filters,
    apiFilters,
    apiFiltersOnly,
    updateQuery,
    updateFilter,
    updateFilters,
  } = useFilters();

  const [visible, setVisible] = useState(false);
  const [dialogKey, setDialogKey] = useState(false);
  const [dialogFilters, setDialogFilters] = useState<FilterConfig[]>([]);

  const navigateTo = useEvidenceNavigation();

  const dialogRef = useRef<DialogHandle>(null);

  const searchOptions = useSearchOptions();

  const handleAllFilters = useEventCallback(() => {
    setDialogFilters(cloneDeep(filters));
    setTimeout(() => dialogRef.current?.open());
  });

  const handleRemoveFilters = useEventCallback(() => {
    updateFilters([]);
  });

  const handleDialogClose = () => {
    setTimeout(() => setDialogKey(!dialogKey));
  };

  const onSearch = useCallback(
    ({ query }: { query: string }) => {
      updateQuery(query);
    },
    [updateQuery]
  );

  const onChange = useCallback(
    (f: FilterConfig) => {
      updateFilter(f);
    },
    [updateFilter]
  );

  // const browserHeight = useHeight();

  // const isIOS = iOS();

  // const height = `calc(${
  //   isIOS ? `${browserHeight}px` : '100vh'
  // } - ${appHeaderHeight} - ${breadcrumbsHeight} - 1px)`;

  return (
    <div
      // style={{ height }}
      className={classNames(classes.sidebar, visible && classes.sidebarVisible)}
    >
      <div
        className={classNames(
          classes.visibleButton,
          spacingClasses.paddingVerticalSmall
        )}
        onClick={() => setVisible(!visible)}
      >
        <IconButton size="small" color="primary">
          <ChevronRight
            className={
              visible
                ? classes.toggleSidebarButtonOpen
                : classes.toggleSidebarButtonClose
            }
          />
        </IconButton>
      </div>
      <div
        className={classNames(classes.sidebarContent, {
          [classes.sidebarContentVisible]: visible,
        })}
      >
        <Loading {...{ loading: !initialized || loading }} />
        {initialized && (
          <div className={spacingClasses.paddingBig}>
            <div className={spacingClasses.paddingBottom}>
              <Search value={query} onSearch={onSearch} />
            </div>
            {path === ModulePath.APU ? (
              <div className={classes.tooManyresults}>
                <h3>{formatMessage({ id: Message.TOO_MANY_RESULTS })}</h3>
                <p>
                  {formatMessage({ id: Message.TRY_REFINING_YOUR_SEARCH_AREA })}
                </p>
                {searchOptions.map(
                  ({ path, name, filters: searchOptionFilters }) => (
                    <Button
                      key={name}
                      contained={true}
                      label={name}
                      onClick={() =>
                        navigateTo(path, 1, 10, query, [
                          ...filters,
                          ...(searchOptionFilters || []),
                        ])
                      } // doplneni filtru ze searchOptions o prave pouzivane filtry
                      className={classNames(
                        classes.sidebarButton,
                        spacingClasses.marginBottom
                      )}
                    />
                  )
                )}
              </div>
            ) : (
              <></>
            )}
            {filters
              .filter(
                ({ display, value }) =>
                  display !== FacetDisplay.DETAIL ||
                  (typeof value === 'string'
                    ? value
                    : isArray(value)
                      ? value.length
                      : false)
              )
              .map((f) => (
                <FilterComponent
                  {...{
                    ...f,
                    key: f.source,
                    onChange,
                    apuPartItemTypes,
                    apiFilters,
                  }}
                />
              ))}
            {filters.length ? (
              <div className={spacingClasses.paddingTop}>
                <Button
                  contained={true}
                  label={formatMessage({ id: Message.ALL_FILTERS })}
                  color="primary"
                  onClick={handleAllFilters}
                  className={classNames(
                    classes.sidebarButton,
                    spacingClasses.marginBottom
                  )}
                />
                <Button
                  contained={true}
                  label={formatMessage({ id: Message.REMOVE_FILTERS })}
                  onClick={handleRemoveFilters}
                  disabled={!apiFiltersOnly.length}
                  className={classes.sidebarButton}
                />
                <FilterDialog
                  ref={dialogRef}
                  key={`${dialogKey}`}
                  filters={dialogFilters}
                  onClose={handleDialogClose}
                  {...{
                    apuPartItemTypes,
                    facets,
                    path,
                  }}
                />
              </div>
            ) : (
              <></>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
