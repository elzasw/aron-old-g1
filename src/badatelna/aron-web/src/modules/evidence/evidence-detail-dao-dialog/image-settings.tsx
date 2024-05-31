import { Replay } from "@material-ui/icons";
import classNames from 'classnames';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { Message } from '../../../enums';
import { useLayoutStyles, useSpacingStyles } from '../../../styles';
import { useStyles } from './styles';
import Slider from "@material-ui/core/Slider";

export const ImageSettingsWindow = ({
  brightness = 100,
  contrast = 100,
  onBrightnessChange,
  onContrastChange,
}: {
  brightness: number;
  contrast: number;
  onBrightnessChange: (brightness: number) => void;
  onContrastChange: (contrast: number) => void;
}) => {
  const styles = useStyles();
  const layoutStyles = useLayoutStyles();
  const spacingStyles = useSpacingStyles();

  return <div className={classNames(styles.imageSettingsWrapper)} >
    <div className={classNames(layoutStyles.flexBottom)}>
      <FormattedMessage id={Message.BRIGHTNESS} />
      <Replay
        className={classNames(spacingStyles.marginLeftSmall, styles.imageSettingsButton)}
        style={{ visibility: brightness !== 100 ? "visible" : "hidden" }}
        onClick={() => { onBrightnessChange(100) }}
      />
    </div>
    <div>
      <Slider
        min={0}
        max={200}
        value={brightness}
        valueLabelDisplay={"auto"}
        onChange={(_, number) => { !Array.isArray(number) && onBrightnessChange(number) }}
      />
    </div>
    <div className={classNames(layoutStyles.flexBottom)}>
      <FormattedMessage id={Message.CONTRAST} />
      <Replay
        className={classNames(spacingStyles.marginLeftSmall, styles.imageSettingsButton)}
        style={{ visibility: contrast !== 100 ? "visible" : "hidden" }}
        onClick={() => { onContrastChange(100) }}
      />
    </div>
    <div>
      <Slider
        min={0}
        max={200}
        value={contrast}
        valueLabelDisplay={"auto"}
        onChange={(_, number) => { !Array.isArray(number) && onContrastChange(number) }}
      />
    </div>
  </div>
}
