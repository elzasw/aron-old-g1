import React, { useContext, useState } from 'react';
import classNames from 'classnames';
import { useIntl, FormattedMessage } from 'react-intl';
import GetAppIcon from '@material-ui/icons/GetApp';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@material-ui/icons/KeyboardArrowUp';

import { SnackbarContext, SnackbarVariant } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { DetailAttachmentsProps } from './types';
import { Message } from '../../enums';
import { downloadFile } from '../../common-utils';

export function EvidenceDetailCollapsibleAttachments({
  items,
  setLoading,
}: DetailAttachmentsProps) {
  const [isOpen, setIsOpen] = useState(items.length <= 5);
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const { showSnackbar } = useContext(SnackbarContext);

  const { formatMessage } = useIntl();

  return items.length ? (
    <div
      className={spacingClasses.marginTopBig}
      style={{
        margin: 0,
        border: '1px solid #ddd',
        padding: '8px',
        borderRadius: '8px',
        background: '#f8f8f8',
        marginBottom: '16px',
      }}
    >
      {true && (
        <div style={{ display: 'flex' }}>
          <h3
            style={{
              margin: 0,
              display: 'flex',
              alignItems: 'center',
              cursor: 'pointer',
            }}
            className={spacingClasses.marginBottomSmall}
            onClick={() => setIsOpen(!isOpen)}
          >
            <FormattedMessage id={Message.ATTACHMENTS} />
            &nbsp; ({items.length}) &nbsp;
            {isOpen ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
          </h3>
        </div>
      )}
      {isOpen && (
        <div
          className={classNames(
            layoutClasses.flexAlignCenter,
            layoutClasses.flexWrap
          )}
          style={{ marginTop: '10px' }}
        >
          {items.map(({ id, name, file }) => {
            const fileId = file?.file?.id;

            return (
              <div
                {...{
                  key: id,
                  className: classNames(
                    classes.attachment,
                    layoutClasses.flexCentered,
                    spacingClasses.paddingSmall,
                    spacingClasses.marginExtraSmall,
                    // spacingClasses.marginBottomSmall,
                    fileId && classes.attachmentDownload
                  ),
                  onClick: async () => {
                    if (fileId) {
                      setLoading(true);
                      const ok = await downloadFile(fileId, name);
                      if (!ok) {
                        showSnackbar(
                          formatMessage({ id: Message.ERROR_DOWNLOAD_FILE }),
                          SnackbarVariant.ERROR
                        );
                      }
                      setLoading(false);
                    }
                  },
                }}
              >
                <div
                  className={classNames(
                    classes.attachmentLabel,
                    spacingClasses.marginRight
                  )}
                >
                  {name}
                </div>
                {fileId ? <GetAppIcon /> : <></>}
              </div>
            );
          })}
        </div>
      )}
    </div>
  ) : (
    <></>
  );
}
