import { SnackbarContext, SnackbarVariant } from '@eas/common-web';
import {
  Close as CloseIcon,
  GetApp as GetAppIcon, Menu as MenuIcon,
  RotateLeft,
  RotateRight,
  ZoomIn as ZoomInIcon,
  ZoomOut as ZoomOutIcon,
  ZoomOutMap as ZoomOutMapIcon,
  Share as ShareIcon,
} from "@material-ui/icons";
import { KeyboardArrowLeft, KeyboardArrowRight, DoubleArrowSharp, FirstPage, LastPage } from "@material-ui/icons";
import classNames from 'classnames';
import { get } from 'lodash';
import React, { useContext, useState } from 'react';
import { useIntl } from 'react-intl';
import { downloadFile } from '../../../common-utils';
import { Loading, useConfiguration } from '../../../components';
import { Message } from '../../../enums';
import { useLayoutStyles, useSpacingStyles } from '../../../styles';
import { useStyles } from './styles';
import { ToolbarProps } from './types';
import { getExistingFile } from './utils';
import { ToolbarButton } from './icon';

const PAGE_SIZE = 10;

export function Toolbar({
  noView,
  noAction,
  selectIndex,
  selectRelative,
  totalItemCount,
  zoomIn,
  zoomOut,
  reset,
  rotateLeft,
  rotateRight,
  previousEnabled,
  nextEnabled,
  previousDisabled,
  nextDisabled,
  zoomInDisabled,
  zoomOutDisabled,
  itemIndex,
  item,
  setItem,
  open,
  setOpen,
  file,
  showCloseButton = true,
  customActionsLeft,
  customActionsRight,
  customActionsCenter,
}: ToolbarProps) {
  const classes = useStyles();
  const { disableDownloads } = useConfiguration();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const { showSnackbar } = useContext(SnackbarContext);

  const { formatMessage } = useIntl();

  const [loading, setLoading] = useState(false);

  const isReferencedFile = !!file?.published?.name;
  const fileDownload = get(getExistingFile(file, true), isReferencedFile ? 'id' : 'file.id');

  return (
    <div className={classes.daoDialogToolbar}>
      <Loading loading={loading} />
      <div
        className={classNames(
          classes.daoDialogToolbarInner,
          layoutClasses.flexSpaceBetween
        )}
      >
        {showCloseButton &&
          <ToolbarButton
            {...{
              title: formatMessage({ id: Message.CLOSE }),
              Component: CloseIcon,
              onClick: () => setItem(null),
            }}
          />
        }
        {customActionsLeft}
        <div style={{ flex: 1 }} />
        <div className={classNames(layoutClasses.flex, layoutClasses.flexWrap)}>
          <div className={classNames(layoutClasses.flex, layoutClasses.flexGrow1, layoutClasses.flexCentered, layoutClasses.flexWrap, spacingClasses.paddingSmall)}>
            {[
              {
                key: "first-page",
                title: Message.FIRST_PAGE,
                Component: FirstPage,
                onClick: () => selectIndex(0),
                visible: !noView && previousEnabled,
                disabled: previousDisabled,
              },
              {
                key: "jump-n-backward",
                title: Message.JUMP_PAGES_BACKWARD,
                titleOptions: { number: PAGE_SIZE },
                Component: DoubleArrowSharp,
                onClick: () => selectRelative(-PAGE_SIZE),
                visible: !noView && previousEnabled,
                disabled: previousDisabled,
                style: { transform: "rotate(180deg)" }
              },
              {
                key: "previous-page",
                title: Message.PREVIOUS_PAGE,
                Component: KeyboardArrowLeft,
                onClick: () => selectRelative(-1),
                visible: !noView && previousEnabled,
                disabled: previousDisabled,
              },
              {
                key: "page-input",
                Component: (props: any) => {
                  const [inputValue, setInputValue] = useState<number | undefined>(itemIndex + 1);
                  return <>
                    <form
                      onSubmit={(e) => {
                        e.preventDefault();
                        if (inputValue) {
                          selectIndex(inputValue - 1);
                        }
                      }}
                      className={classNames(classes.toolbarPageForm)}
                    >
                      <input
                        className={classNames(classes.toolbarInput)}
                        value={inputValue}
                        onChange={(e) => {
                          e.stopPropagation();
                          const numValue = parseInt(e.currentTarget.value, 10);
                          if (e.currentTarget.value == "") {
                            setInputValue(undefined);
                          }
                          else if ((Number.isInteger(numValue) && numValue > 0 && numValue <= totalItemCount)) {
                            setInputValue(numValue);
                          }
                        }}
                      /> / {totalItemCount}
                    </form>
                  </>
                },
                onClick: () => selectRelative(1),
                visible: !noView && nextEnabled,
                disabled: nextDisabled,
              },
              {
                key: "next-page",
                title: Message.NEXT_PAGE,
                Component: KeyboardArrowRight,
                onClick: () => selectRelative(1),
                visible: !noView && nextEnabled,
                disabled: nextDisabled,
              },
              {
                key: "jump-n-forward",
                title: Message.JUMP_PAGES_FORWARD,
                titleOptions: { number: PAGE_SIZE },
                Component: DoubleArrowSharp,
                onClick: () => selectRelative(PAGE_SIZE),
                visible: !noView && nextEnabled,
                disabled: nextDisabled,
              },
              {
                key: "last-page",
                title: Message.LAST_PAGE,
                Component: LastPage,
                onClick: () => selectIndex(totalItemCount - 1),
                visible: !noView && nextEnabled,
                disabled: nextDisabled,
              },
            ].map(
              ({ visible, title, key, titleOptions, ...icon }) =>
                visible && (
                  <ToolbarButton
                    {...{
                      key,
                      ...icon,
                      title: title ? formatMessage({ id: title }, titleOptions) : "",
                    }}
                  />
                )
            )}
          </div>
          <div className={classNames(layoutClasses.flex, layoutClasses.flexGrow1, layoutClasses.flexCentered, layoutClasses.flexWrap, spacingClasses.paddingSmall)}>
            {[
              {
                key: "rotate-left",
                title: Message.ROTATE_LEFT,
                Component: RotateLeft,
                onClick: rotateLeft,
                visible: !noView && !noAction,
                disabled: zoomInDisabled,
              },
              {
                key: "rotate-right",
                title: Message.ROTATE_RIGHT,
                Component: RotateRight,
                onClick: rotateRight,
                visible: !noView && !noAction,
                disabled: zoomInDisabled,
              },
              {
                key: "zoom-in",
                title: Message.ZOOM_IN,
                Component: ZoomInIcon,
                onClick: zoomIn,
                visible: !noView && !noAction,
                disabled: zoomInDisabled,
              },
              {
                key: "zoom-out",
                title: Message.ZOOM_OUT,
                Component: ZoomOutIcon,
                onClick: zoomOut,
                visible: !noView && !noAction,
                disabled: zoomOutDisabled,
              },
              {
                key: "center",
                title: Message.CENTER,
                Component: ZoomOutMapIcon,
                onClick: reset,
                visible: !noView && !noAction,
              },
              {
                key: "copy-link",
                title: Message.COPY_LINK,
                Component: ShareIcon,
                onClick: async () => {
                  try {
                    await navigator.clipboard.writeText(location.toString());
                    showSnackbar(formatMessage({ id: Message.COPY_LINK_SUCCESS }), SnackbarVariant.INFO, true);
                  } catch (error) {
                    showSnackbar(formatMessage({ id: Message.COPY_LINK_ERROR }), SnackbarVariant.ERROR, false);
                    throw error;
                  }
                },
                visible: !noView && !noAction && window.isSecureContext,
              },
              {
                key: "download",
                title: Message.DOWNLOAD,
                Component: GetAppIcon,
                onClick: async () => {
                  setLoading(true);
                  const ok = await downloadFile(
                    fileDownload,
                    `dao_${item.id}.jpg`,
                    isReferencedFile,
                  );
                  if (!ok) {
                    showSnackbar(
                      formatMessage({ id: Message.ERROR_DOWNLOAD_FILE }),
                      SnackbarVariant.ERROR
                    );
                  }
                  setLoading(false);
                },
                visible: fileDownload,
                disabled: disableDownloads ? true : loading,
              },
              // {
              //   title: Message.DOWNLOAD_ALL,
              //   Component: InsertDriveFileIcon,
              //   onClick: () => null,
              // },
            ].map(
              ({ visible, title, key, ...icon }) =>
                visible && (
                  <ToolbarButton
                    {...{
                      key,
                      ...icon,
                      title: formatMessage({ id: title }),
                    }}
                  />
                )
            )}
            {customActionsCenter}
          </div>
        </div>
        <div style={{ flex: 1 }} />
        {customActionsRight}
        {setOpen && <ToolbarButton
          {...{
            title: '',
            Component: MenuIcon,
            onClick: () => setOpen(!open),
            className: classes.daoDialogMenu,
          }}
        />}
      </div>
    </div>
  );
}
