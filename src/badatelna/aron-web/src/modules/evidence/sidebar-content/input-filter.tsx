import React, { ReactElement, useState, useCallback, useEffect } from 'react';
import { debounce } from 'lodash';

import { TextField, Tooltip } from '../../../components';
import { useStyles } from './styles';
import { useStyles as useEvidenceStyles } from '../styles';
import { useSpacingStyles } from '../../../styles';
import { InputFilterProps, InputFilterType } from '.';
import { useGetCountInput } from './utils';

export function InputFilter({
  value,
  onChange,
  source,
  label,
  tooltip,
  description,
  inDialog,
  apiFilters,
  filterType = InputFilterType.FULLTEXT,
}: InputFilterProps): ReactElement {
  const classes = useStyles();
  const classesEvidence = useEvidenceStyles();
  const classesSpacing = useSpacingStyles();

  const [inputValue, setInputValue] = useState(value);

  const updateFilterValue = useCallback(
    (newFilterValue: string) => {
      onChange({
        source,
        value: newFilterValue,
      });
    },

    [onChange, source]
  );

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const updateFilterValueDebounced = useCallback(
    debounce(updateFilterValue, 700),
    [updateFilterValue]
  );

  const handleChange = (text: string) => {
    setInputValue(text);
    updateFilterValueDebounced(text);
  };

  useEffect(() => {
    if (value !== inputValue) {
      setInputValue(value);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  const [result, loading] = useGetCountInput(source, value, apiFilters, filterType);

  return (
    <>
      <div className={classes.filterTitle}>
        <Tooltip title={tooltip}>
          <span>
            <span className={classesSpacing.marginRightSmall}>{label}</span>
            {inputValue && !loading ? (
              <span className={classesEvidence.itemsCount}>
                <span>(</span>
                {result?.count || 0}
                <span>)</span>
              </span>
            ) : (
              <></>
            )}
          </span>
        </Tooltip>
      </div>
      {inDialog && description ? (
        <div className={classes.filterDescription}>{description}</div>
      ) : (
        <></>
      )}
      <TextField
        {...{
          className: classes.inputFilterTextField,
          size: 'small',
          variant: 'outlined',
          value: inputValue || '',
          onChange: handleChange,
        }}
      />
    </>
  );
}
