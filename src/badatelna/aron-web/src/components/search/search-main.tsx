import MUIButton from '@material-ui/core/Button';
import IconButton from '@material-ui/core/IconButton';
import SearchIcon from '@material-ui/icons/Search';
import classNames from 'classnames';
import React, { useState } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { Link } from 'react-router-dom';
import { createEvidenceUrl, useEvidenceNavigation, useSearchOptions } from '../../common-utils';
import { Message, ModulePath } from '../../enums';
import { useAppStyles, useLayoutStyles, useSpacingStyles } from '../../styles';
import { Button } from '../button';
import { ClickableSelection, ClickableSelectionAlt } from '../clickable-selection';
import { useConfiguration } from '../configuration';
import { TextField } from '../text-field';
import { useStyles } from './styles';


export function SearchMain() {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const appClasses = useAppStyles();
  const { alternativeSearchType } = useConfiguration();
  const navigateTo = useEvidenceNavigation();

  const { formatMessage } = useIntl();

  // const { navigate } = useContext(NavigationContext);

  const searchOptions = useSearchOptions();

  const [searchValue, setSearchValue] = useState<string>('');
  const [selectedOptions, setSelectedOptions] = useState<any[]>([]);
  const [placeholder, setPlaceholder] = useState<Message>( Message.SEARCH_NO_FILTER );

  const handleInputChange = (
    value: string | React.ChangeEvent<HTMLTextAreaElement | HTMLInputElement>
  ) => {
    if(value == ''){
      handleSearchReset();
      return;
    }
      typeof value === 'string' && setSearchValue(value)
  };

  const handleSearchSubmit = (_searchValue: string = searchValue) => {
    const selectedPath = selectedOptions[0]?.path;
    const path = selectedPath || ModulePath.APU;

    if (searchValue || selectedPath) {
      navigateTo(path, 1, 10, searchValue, selectedOptions[0]?.filters);
    }
  };
  const handleSearch = () => {
    handleSearchSubmit(searchValue);
  }
  const handleSearchReset = () => {
    handleSearchSubmit('');
    setSearchValue('');
  }

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

  return (
    <div className={classes.search}>
      <div
        className={classNames(
          classes.searchInner,
          classes.searchBigInner,
          layoutClasses.flexCentered
        )}
      >
        <div className={classNames(classes.searchTextFieldWrapper, layoutClasses.flexCentered)}>
          <TextField
            className={classNames(classes.searchTextField, searchValue && classes.searchTextInserted)}
            value={searchValue}
            onChange={handleInputChange}
            placeholder={formatMessage({
              id: placeholder,
            })}
            InputProps={{
              startAdornment: (
                <SearchIcon className={classes.searchIcon} color="disabled" />
              ),
            }}
            onKeyPress={(e) => {
              if (e.key === 'Enter') {
                handleSearch();
              }
            }}
          />
          {(searchValue) &&
            <IconButton
              aria-label='delete'
              onClick={handleSearchReset}
              type='button'
              size='small'
              className={classes.searchResetButton}
            >
              <span className='fas fa-times'/>
            </IconButton>
          }
        </div>
        <Button
          className={classes.searchButton}
          label={formatMessage({ id: Message.SEARCH_BTN })}
          color="primary"
          contained={true}
          onClick={handleSearch}
        />
      </div>
        <div
          className={classNames(
            classes.searchAdvancedContainer,
            layoutClasses.flex,
            spacingClasses.paddingTopSmall
          )}
        >
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
          <div style={{flexShrink: 0, display: "flex", justifyContent: "flex-end"}} >
            <Link
              to={createEvidenceUrl(ModulePath.ARCH_DESC)}
              // className={classes.searchAdvanced}
              // style={{borderRadius: "4px", margin: "4px"}}
            >
              <MUIButton
                // onClick={() => navigate(ModulePath.ARCH_DESC)}
                className={classes.searchAdvanced}
                // label={formatMessage({ id: Message.ADVANCED_SEARCH })}
                size="medium"
                style={{borderRadius: "4px", margin: "4px"}}
              >
                {formatMessage({ id: Message.ADVANCED_SEARCH })}
              </MUIButton>
            </Link>
            {/*<MUIButton
              onClick={() => navigate(ModulePath.ARCH_DESC)}
              className={classes.searchAdvanced}
              // label={formatMessage({ id: Message.ADVANCED_SEARCH })}
              size="medium"
              style={{borderRadius: "4px", margin: "4px"}}
            >
              {formatMessage({ id: Message.ADVANCED_SEARCH })}
            </MUIButton>*/}
          </div>
        </div>
    </div>
  );
}
