import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';
import DialogActions from '@material-ui/core/DialogActions';
import MUIButton from '@material-ui/core/Button';

export interface ConfirmDialogProps {
  open: boolean;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({ open, message, onConfirm, onCancel }: ConfirmDialogProps) {
  return (
    <Dialog open={open} onClose={onCancel} maxWidth="xs" fullWidth>
      <DialogTitle>Potvrzení</DialogTitle>
      <DialogContent>{message}</DialogContent>
      <DialogActions>
        <MUIButton onClick={onCancel}>Zrušit</MUIButton>
        <MUIButton variant="contained" color="primary" onClick={onConfirm}>
          Potvrdit
        </MUIButton>
      </DialogActions>
    </Dialog>
  );
}
