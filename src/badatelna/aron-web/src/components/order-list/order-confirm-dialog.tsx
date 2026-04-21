import { useState, useContext } from 'react';
import { SnackbarContext, SnackbarVariant } from '@eas/common-web';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';
import DialogActions from '@material-ui/core/DialogActions';
import MUIButton from '@material-ui/core/Button';
import MUITextField from '@material-ui/core/TextField';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Close';
import makeStyles from '@material-ui/core/styles/makeStyles';
import { BadatelnaInfo, InstitutionInfo, OrderItemInfoEx } from './types';
import { ConfirmDialog } from './confirm-dialog';
import { API_ORDERS_URL } from '../../enums/api';
import {
  Chyba,
  NepripojenaObjednavka,
  ZpusobZpristupneniArchivalie,
} from '../../types/order';

export interface OrderConfirmDialogProps {
  open: boolean;
  onClose: () => void;
  institution: InstitutionInfo;
  badatelna?: BadatelnaInfo;
  orderItems: OrderItemInfoEx[];
}

const useStyles = makeStyles(() => ({
  title: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingRight: 8,
  },
  content: {
    display: 'flex',
    flexDirection: 'column',
    gap: 12,
    minWidth: 520,
  },
  label: {
    marginBottom: 2,
    fontSize: 13,
  },
  field: {
    display: 'flex',
    flexDirection: 'column',
  },
  actions: {
    padding: '8px 16px',
  },
}));

function formatCartContent(orderItems: OrderItemInfoEx[], institution: InstitutionInfo): string {
  return orderItems
    .map((item, index) => {
      const { archivalie } = item;
      const lines: string[] = [`--- ${index + 1} ---`];

      if (archivalie.formNazev) {
        lines.push(`${archivalie.formNazev} | ${institution.name}`);
      }

      const nadParts = [`Číslo listu NAD: ${archivalie.cisloNad}`];
      if (archivalie.dilciList) nadParts.push(`Dílčí list: ${archivalie.dilciList}`);
      if (archivalie.archivniIdentifikator) nadParts.push(`Inventární číslo: ${archivalie.archivniIdentifikator}`);
      lines.push(nadParts.join(' | '));

      if (archivalie.uplnyPopis) {
        lines.push(archivalie.uplnyPopis);
      } else if (archivalie.popis) {
        lines.push(`Obsah: ${archivalie.popis}`);
      }

      if (archivalie.url) {
        lines.push(`Přímý odkaz: ${archivalie.url}`);
      }

      return lines.join('\n');
    })
    .join('\n\n');
}

export function OrderConfirmDialog({
  open,
  onClose,
  institution,
  badatelna,
  orderItems,
}: OrderConfirmDialogProps) {
  const classes = useStyles();
  const { showSnackbar } = useContext(SnackbarContext);
  const institutionLabel = badatelna ? badatelna.name : institution.name;

  const savedContact = (() => {
    try { return JSON.parse(localStorage.getItem('orderContact') || '{}'); } catch { return {}; }
  })();

  const [name, setName] = useState<string>(savedContact.name || '');
  const [email, setEmail] = useState<string>(savedContact.email || '');
  const [phoneNumber, setPhoneNumber] = useState<string>(savedContact.phoneNumber || '');
  const [touched, setTouched] = useState({ name: false, email: false });
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [subject, setSubject] = useState('Předběžná rezervace archiválií');
  const [reserveDate, setReserveDate] = useState('');
  const [message, setMessage] = useState('');

  const cartContent = formatCartContent(orderItems, institution);

  function handleClose() {
    onClose();
  }

  const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());

  function handleSubmit() {
    if (!name.trim() || !email.trim() || !emailValid) {
      setTouched({ name: true, email: true });
      return;
    }
    setConfirmOpen(true);
  }

  async function doSubmit() {
    setConfirmOpen(false);
    const popisParts: string[] = [];
    if (reserveDate) popisParts.push(`Na kdy rezervovat: ${reserveDate}`);
    if (message) popisParts.push(message);

    const payload: NepripojenaObjednavka = {
      name,
      email,
      phoneNumber: phoneNumber || undefined,
      subjects: [subject],
      returnUrl: window.location.href,
      archiv: institution.code,
      badatelna: badatelna?.code,
      popis: popisParts.length > 0 ? popisParts.join('\n') : undefined,
      polozky: orderItems.map((item) => ({
        archivalie: item.archivalie,
        zpusobZpristupneni: item.type as ZpusobZpristupneniArchivalie,
      })),
    };

    localStorage.setItem('orderContact', JSON.stringify({ name, email, phoneNumber }));

    setSubmitError(null);
    const response = await fetch(`${API_ORDERS_URL}/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      try {
        const chyba: Chyba = await response.json();
        setSubmitError(chyba.zprava);
      } catch {
        setSubmitError(`Chyba ${response.status}`);
      }
      return;
    }

    showSnackbar('Objednávka byla úspěšně odeslána.', SnackbarVariant.SUCCESS);
    onClose();
  }

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle disableTypography className={classes.title}>
        <span>Rezervace archiválií</span>
        <IconButton size="small" onClick={handleClose}>
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <DialogContent className={classes.content}>
        <div className={classes.field}>
          <div className={classes.label}>* Vaše jméno a příjmení :</div>
          <MUITextField
            variant="outlined"
            size="small"
            value={name}
            onChange={(e) => setName(e.target.value)}
            error={touched.name && !name.trim()}
            helperText={touched.name && !name.trim() ? 'Povinné pole' : undefined}
          />
        </div>

        <div className={classes.field}>
          <div className={classes.label}>* Váš email :</div>
          <MUITextField
            variant="outlined"
            size="small"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={touched.email && (!email.trim() || !emailValid)}
            helperText={touched.email && !email.trim() ? 'Povinné pole' : touched.email && !emailValid ? 'Neplatný formát e-mailu' : undefined}
          />
        </div>

        <div className={classes.field}>
          <div className={classes.label}>Telefonní číslo :</div>
          <MUITextField
            variant="outlined"
            size="small"
            value={phoneNumber}
            onChange={(e) => setPhoneNumber(e.target.value)}
          />
        </div>

        <div className={classes.field}>
          <div className={classes.label}>Předmět zprávy :</div>
          <MUITextField
            variant="outlined"
            size="small"
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
          />
        </div>

        <div className={classes.field}>
          <div className={classes.label}>Na kdy rezervovat :</div>
          <MUITextField
            variant="outlined"
            size="small"
            value={reserveDate}
            onChange={(e) => setReserveDate(e.target.value)}
          />
        </div>

        <div className={classes.field}>
          <div className={classes.label}>Obsah košíku - {institutionLabel} :</div>
          <MUITextField
            variant="outlined"
            size="small"
            multiline
            rows={6}
            value={cartContent}
            InputProps={{ readOnly: true }}
          />
        </div>

        <div className={classes.field}>
          <div className={classes.label}>Text zprávy pro - {institutionLabel} :</div>
          <MUITextField
            variant="outlined"
            size="small"
            multiline
            rows={4}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
          />
        </div>
      </DialogContent>

      <DialogActions className={classes.actions}>
        {submitError && (
          <span style={{ color: 'red', flexGrow: 1, fontSize: 13 }}>{submitError}</span>
        )}
        <MUIButton variant="contained" color="primary" onClick={handleSubmit}>
          Odeslat
        </MUIButton>
      </DialogActions>
      <ConfirmDialog
        open={confirmOpen}
        message="Chcete odeslat objednávku?"
        onConfirm={doSubmit}
        onCancel={() => setConfirmOpen(false)}
      />
    </Dialog>
  );
}
