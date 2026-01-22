import MUIButton from '@material-ui/core/Button';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import { messages } from './messages';
import { ZpusobZpristupneni } from './types';
import { useState } from 'react';

export interface Props {
  value: ZpusobZpristupneni | null;
  onChange: (type: ZpusobZpristupneni) => void;
}

const types = [
  ZpusobZpristupneni.NAHLIZENI,
  ZpusobZpristupneni.REPLIKA,
  ZpusobZpristupneni.OVERENI_SHODY,
];

export function AccessTypeSelect({value, onChange}:Props) {
  const [anchorElement, setAnchorElement] = useState<HTMLButtonElement | null>(null);

  function handleChange(type: ZpusobZpristupneni) {
    setAnchorElement(null);
    onChange(type);
  }

  return <>
    <MUIButton
    onClick={({currentTarget}) => setAnchorElement(currentTarget)}
    variant="outlined"
    size="small"
    endIcon={<ArrowDropDownIcon/>}
    ><span style={{whiteSpace: "nowrap"}}>{value ? messages[value] : ""}</span></MUIButton>
    <Menu
      open={Boolean(anchorElement)}
      anchorEl={anchorElement}
      onClose={() => setAnchorElement(null)}
      anchorOrigin={{vertical: "bottom", horizontal: "left"}}
      transformOrigin={{vertical: "top", horizontal: "left"}}
    >
      {types.map((type) => {
        return <MenuItem onClick={() => handleChange(type)} disableRipple={false}>{messages[type]}</MenuItem>
      })}
    </Menu>
  </>
}
