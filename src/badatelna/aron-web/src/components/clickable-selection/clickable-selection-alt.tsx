import React, { ReactElement, useState } from 'react';
import { Checkbox } from '@eas/common-web';
import Radio from '@material-ui/core/Radio';

import { useStyles } from './styles';
import classNames from 'classnames';

interface Props {
  options: { name: any }[];
  onChange: (o: any[]) => void;
  radio?: boolean;
}

export function ClickableSelectionAlt({
  options,
  onChange,
  radio = false,
}: Props): ReactElement {
  const classes = useStyles();

  const [selectedOptions, setSelectedOptions] = useState<number[]>([]);
  const handleOptionsChange = (value: boolean, index: number) => {
    const newOptions = value
      ? radio
        ? [index]
        : [...selectedOptions, index]
      : radio
      ? []
      : selectedOptions.filter((o) => o !== index);
    setSelectedOptions(newOptions);
    onChange(options.filter((_, i) => newOptions.includes(i)));
  };
  return (
    <div className={classes.containerAlt}>
      {options.map((option, index) => (
        <button
          key={index}
          className={
            classNames(
              classes.optionAlt,
              selectedOptions.includes(index) && classes.selected,
              radio && classes.radioOption,
            )}
          onClick={() =>
            handleOptionsChange(!selectedOptions.includes(index), index)
          }
        >
          {radio ? (
            <Radio
              size="small"
              color="primary"
              style={{display: "none"}}
              checked={selectedOptions.includes(index)}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                handleOptionsChange(e.target.checked, index)
              }
            />
          ) : (
            <Checkbox
              value={selectedOptions.includes(index)}
              onChange={(isChecked: null | boolean) =>
                handleOptionsChange(isChecked || false, index)
              }
            />
          )}

          <span>{option.name}</span>
        </button>
      ))}
    </div>
  );
}
