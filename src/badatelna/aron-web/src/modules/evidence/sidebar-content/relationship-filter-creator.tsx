import React, { ReactElement, useState, useCallback } from 'react';
import LinearProgress from '@material-ui/core/LinearProgress';
import { get, isArray, isEmpty } from 'lodash';
import { FormattedMessage } from 'react-intl';

import { Autocomplete, Button, TextWithCount } from '../../../components';
import { Option } from '../../../types';
import {
  useGetEntityRelationships,
  useGetMatchingName,
  getApuPartItemName,
} from '../../../common-utils';
import { ClickableSelection } from '../../../components/clickable-selection/clickable-selection';
import { useStyles } from './styles';
import { useSpacingStyles } from '../../../styles';
import { Message } from '../../../enums';
import { RelationshipFilterCreatorProps } from '.';

export function RelationshipFilterCreator({
  onChange,
  apuPartItemTypes,
  apiFilters,
  group,
}: RelationshipFilterCreatorProps): ReactElement {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();

  const [searchValue, setSearchValue] = useState<Option | null>(null);
  const [query, setQuery] = useState('');

  const [resultAutocomplete, loadingAutocomplete] = useGetMatchingName(
    query,
    group
  );
  const [resultRelationships, loadingRelationships] = useGetEntityRelationships(
    searchValue?.id,
    apiFilters,
    group
  );
  const [selectedRelationships, setSelectedRelationships] = useState<
    {
      name: string;
      field: string;
    }[]
  >();

  const updateFilterValue = useCallback(
    (fields: string[]) => {
      setSelectedRelationships(undefined);
      onChange(
        fields.map((field: string) => ({
          field,
          value: searchValue?.id ? searchValue.id : searchValue?.name,
          name: searchValue?.name,
        }))
      );
    },
    [onChange, searchValue?.id, searchValue?.name]
  );

  const options = resultRelationships.map(
    (relationship: { key: string; value: string }) => ({
      field: relationship.key,
      name: (
        <TextWithCount
          text={getApuPartItemName(apuPartItemTypes, relationship.key)}
          count={parseInt(relationship.value)}
        />
      ),
    })
  );

  return (
    <div className={classes.relationshipFilterCreatorWrapper}>
      <div className={classes.relationshipFilterLabel}>
        <FormattedMessage id={Message.NEW_FILTER} />
      </div>
      <Autocomplete
        {...{
          value: searchValue,
          onChange: (o: Option | Option[] | null) =>
            setSearchValue(o && !isArray(o) ? o : null),
          loading: loadingAutocomplete,
          onQueryChange: setQuery,
          options: get(resultAutocomplete, 'items', []),
        }}
      />
      {searchValue &&
        (loadingRelationships ? (
          <LinearProgress color="primary" />
        ) : !isEmpty(resultRelationships) ? (
          <>
            <div className={classes.relationshipFilterLabel}>
              <FormattedMessage id={Message.RELATIONSHIP_TYPE_CONSTRAINT} />
            </div>
            <ClickableSelection
              options={options}
              onChange={setSelectedRelationships}
            />
            <Button
              contained
              color="primary"
              size="small"
              label={<FormattedMessage id={Message.ADD_TO_FILTER} />}
              className={spacingClasses.marginTopSmall}
              disabled={isEmpty(selectedRelationships)}
              onClick={() =>
                updateFilterValue(
                  !selectedRelationships || isEmpty(selectedRelationships)
                    ? []
                    : selectedRelationships.map((r) => r.field)
                )
              }
            />
          </>
        ) : (
          <FormattedMessage id={Message.RELATIONSHIP_NOT_FOUND} />
        ))}
    </div>
  );
}
