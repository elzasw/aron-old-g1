import React, { ReactElement, useState } from 'react';
import MUIButton from '@material-ui/core/Button';

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
        <MUIButton
          key={index}
          className={
            classNames(
              classes.optionAlt,
              selectedOptions.includes(index) && classes.selected,
            )}
          onClick={() =>
            handleOptionsChange(!selectedOptions.includes(index), index)
          }
          size="small"
        >
          {option.name}
        </MUIButton>
      ))}
    </div>
  );
}
