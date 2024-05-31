import { SnackbarContext, SnackbarVariant } from '@eas/common-web';
import React, { useContext } from 'react';
import { useIntl } from 'react-intl';
import Button from '@material-ui/core/Button';
import ButtonGroup from '@material-ui/core/ButtonGroup';
import CircularProgress from '@material-ui/core/CircularProgress';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';
import DialogActions from '@material-ui/core/DialogActions';
import { Message } from '../../enums';
import { useGet } from '../../common-utils';
import { useStyles } from './styles';

export interface Props {
  apuId: string;
  isOpen: boolean;
  onClose: () => void;
}

export interface Citation {
  citation?: string;
  error?: string;
}

export function CitationDialog({ apuId, isOpen, onClose }: Props) {
  const [citation] = useGet<Citation>(`/script/citation/${apuId}`);

  const { showSnackbar } = useContext(SnackbarContext);
  const { formatMessage } = useIntl();
  const classes = useStyles();

  async function handleCopyToClipboard() {
    try {
      if (citation && citation.citation) {
        await navigator.clipboard.writeText(citation.citation);
        showSnackbar(formatMessage({ id: Message.COPY_TEXT_SUCCESS }), SnackbarVariant.INFO, true);
      } else {
        throw Error("Citation is not defined");
      }
    } catch (error) {
      showSnackbar(formatMessage({ id: Message.COPY_TEXT_ERROR }), SnackbarVariant.ERROR, false);
      throw error;
    }
  }

  return <Dialog open={isOpen} onClose={onClose}>
    <DialogTitle>
      Citace
    </DialogTitle>
    <DialogContent>
      <div className={classes.citationText}>
        {citation && citation.citation}
        {!citation && <div style={{ display: 'flex', justifyContent: 'center' }}>
          <CircularProgress />
        </div>}
      </div>
    </DialogContent>
    <DialogActions>
      <ButtonGroup>
        {window.isSecureContext && citation && <Button size="small" onClick={handleCopyToClipboard}>{formatMessage({ id: Message.COPY_TEXT })}</Button>}
        <Button size="small" onClick={onClose}>{formatMessage({ id: Message.CLOSE })}</Button>
      </ButtonGroup>
    </DialogActions>
  </Dialog>
}
