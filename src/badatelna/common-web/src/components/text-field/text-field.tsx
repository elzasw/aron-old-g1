import React, {
  ChangeEvent,
  forwardRef,
  useRef,
  RefObject,
  useLayoutEffect,
  useState,
} from 'react';
import clsx from 'clsx';
import MuiTextField from '@material-ui/core/TextField';
import InputAdornment from '@material-ui/core/InputAdornment';
import { useEventCallback } from 'utils/event-callback-hook';
import { TextFieldProps } from './text-field-types';
import { useStyles } from './text-field-styles';
import { Tooltip } from 'components/tooltip/tooltip';
import { useComponentSize } from 'utils/component-size';
import { useDebounce } from 'use-debounce/lib';

export const TextField = forwardRef<HTMLDivElement, TextFieldProps>(
  function TextField(
    {
      form,
      id,
      name,
      onChange,
      disabled,
      value,
      onBlur,
      onFocus,
      onKeyDown,
      startAdornment,
      endAdornment,
      onChangeEvent,
      error = false,
      type = 'text',
      autocomplete,
      inputProps,
    }: TextFieldProps,
    ref
  ) {
    // fix undefined value
    value = value ?? null;

    // prepare value for MUI component
    value = value ?? '';

    const handleChange = useEventCallback(
      (e: ChangeEvent<HTMLInputElement>) => {
        if (onChangeEvent !== undefined) {
          onChangeEvent(e);
        }

        if (onChange !== undefined) {
          const value = e.target.value;
          onChange(value !== '' ? value : null);
        }
      }
    );

    const classes = useStyles();

    const inputRef = useRef<HTMLInputElement>(null);
    const [useTooltip, setUseTooltip] = useState(false);

    const { width } = useComponentSize(inputRef);
    const [debouncedWidth] = useDebounce(width, 500);

    useLayoutEffect(() => {
      setUseTooltip(isEllipsisActive(inputRef));
    }, [value, debouncedWidth]);

    startAdornment = startAdornment && (
      <InputAdornment position="start">{startAdornment}</InputAdornment>
    );
    endAdornment = endAdornment && (
      <InputAdornment position="end">{endAdornment}</InputAdornment>
    );

    const content = (
      <MuiTextField
        InputProps={{
          classes,
          startAdornment,
          endAdornment,
        }}
        inputProps={{
          form,
          ref: inputRef,
          ...inputProps,
        }}
        className={clsx({ [classes.disabled]: disabled })}
        id={id}
        name={name}
        ref={ref}
        disabled={disabled}
        value={value}
        onChange={handleChange}
        onBlur={onBlur}
        onFocus={onFocus}
        onKeyDown={onKeyDown}
        error={error}
        spellCheck={true}
        fullWidth={true}
        lang="cs"
        type={type}
        autoComplete={autocomplete}
        variant="outlined"
        size="small"
      />
    );

    return useTooltip ? (
      <Tooltip title={value} placement="top-start" type="HOVER">
        {content}
      </Tooltip>
    ) : (
      <>{content}</>
    );
  }
);

function isEllipsisActive(e: RefObject<HTMLInputElement>) {
  const current = e.current;
  return current !== null ? current.offsetWidth < current.scrollWidth : false;
}
