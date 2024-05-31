import { Fullscreen, FullscreenExit, GetApp as GetAppIcon, InfoOutlined, LockOpen, Lock, Tune, ExploreOutlined, Explore } from "@material-ui/icons";
import classNames from 'classnames';
import { findIndex } from 'lodash';
import React, { useEffect, useRef, useState, useContext } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { downloadFileByUrl, createUrlParams } from '../../../common-utils';
import { useConfiguration } from '../../../components';
import { ImageViewer, ImageViewerExposedFunctions } from '../../../components/file-viewer/image-viewer';
import { Message, API_URL } from '../../../enums';
import { useLayoutStyles, useSpacingStyles } from '../../../styles';
import { useKeyPress } from "../../../utils/useKeyPress";
import { ToolbarButton } from './icon';
import { useStyles } from './styles';
import { Toolbar } from './toolbar';
import { ImageSettingsWindow } from "./image-settings";
import { DetailDaoDialogProps, FileObject } from './types';
import { getExistingFile, getFiles } from './utils';
import { useParams } from "react-router-dom";
import { NavigationContext } from "@eas/common-web";
import { createApuDaoFileUrl, ApuPathParams } from "../evidence";
import { DaoNamePlacement } from "../../../enums/dao-name-placement";
import { ImageList } from "./image-list";

const FULLSCREEN = "fullscreen"

const getObjectFromUrlParams = (map: URLSearchParams) => {
  const object: Record<string, string> = {};
  map.forEach((value, key) => {
    object[key] = value.toString();
  })
  return object;
}

const getDaoPlacementStyle = (placement?: DaoNamePlacement) => {
  if (!placement) { placement = DaoNamePlacement.TOP_RIGHT }

  const placementStyle = {
    [DaoNamePlacement.TOP_LEFT]: { top: 0, left: 0 },
    [DaoNamePlacement.TOP_RIGHT]: { top: 0, right: 0 },
    [DaoNamePlacement.BOTTOM_RIGHT]: { bottom: 0, right: 0 },
    [DaoNamePlacement.BOTTOM_LEFT]: { bottom: 0, left: 0 },
  }[placement];

  if (!placementStyle) { throw `Undefined placement name: ${placement}` }

  return placementStyle || {};
}

export function EvidenceDetailDaoDialog({
  // item,
  // items,
  dao,
  file,
  setItem,
  embed = false,
  customActionsLeft,
  customActionsRight,
  customActionsCenter,
  apuInfo,
  showInfo,
}: DetailDaoDialogProps) {
  const files = getFiles(dao);
  const viewerRef = useRef<ImageViewerExposedFunctions>(null);
  const daoDialogElement = useRef<HTMLDivElement>(null);

  const { formatMessage } = useIntl();
  const {
    showMetadataInImageViewer,
    daoFooter,
    daoNamePlacement,
    hideThumbnails,
  } = useConfiguration();

  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const urlParams = getObjectFromUrlParams(new URLSearchParams(location.search));
  const isFullscreen = urlParams[FULLSCREEN] === "true";

  const [open, setOpen] = useState(false);
  const [showMetadata, setShowMetadata] = useState(false);
  const [fullscreen, setFullscreen] = useState(!embed || isFullscreen);
  const [preserveViewportState, setPreserveViewportState] = useState<boolean | undefined>();
  const [brightness, setBrightness] = useState(100);
  const [contrast, setContrast] = useState(100);
  const [imageSettingsOpen, setImageSettingsOpen] = useState(false);
  const [showNavigator, setShowNavigator] = useState(false);

  const { id, daoId, fileId } = useParams<ApuPathParams>();
  const { navigate } = useContext(NavigationContext);

  useEffect(() => {
    if (!daoId || !fileId) {
      navigate(createApuDaoFileUrl(apuInfo.id, dao.id, file.id), true);
    }
  }, [dao, file])

  useEffect(() => {
    navigate(`${createApuDaoFileUrl(id, daoId, fileId)}${createUrlParams({ ...urlParams, [FULLSCREEN]: fullscreen && embed })}`, true);
  }, [fullscreen])

  const fileIndex = findIndex(files, ({ id }) => id === file.id);

  const existingFile = getExistingFile(file);

  const metadataId = file?.published?.id;
  const metadata = file?.published?.metadata;

  const isTile = !!(file && file.tile);
  const fileUuid = isTile ? existingFile?.id : existingFile?.file.id

  useKeyPress([{ key: "ArrowUp" }/* , { key: "ArrowLeft" }  - conflict with page number input */], (event) => {
    event.preventDefault();
    handleSelectRelative(-1);
  }, daoDialogElement.current);

  useKeyPress([{ key: "ArrowDown" }/* , { key: "ArrowRight" }  - conflict with page number input */], (event) => {
    event.preventDefault();
    handleSelectRelative(1);
  }, daoDialogElement.current);

  useKeyPress([{ key: "Home" }], (event) => {
    event.preventDefault();
    handleShowFirst();
  }, daoDialogElement.current);

  useKeyPress([{ key: "End" }], (event) => {
    event.preventDefault();
    handleShowLast();
  }, daoDialogElement.current);

  useKeyPress([{ key: "PageUp" }], (event) => {
    event.preventDefault();
    handleSelectRelative(-10);
  }, daoDialogElement.current);

  useKeyPress([{ key: "PageDown" }], (event) => {
    event.preventDefault();
    handleSelectRelative(10);
  }, daoDialogElement.current);

  const handleClickThumbnail = (file: FileObject) => {
    navigate(`${createApuDaoFileUrl(apuInfo.id, dao.id, file.id)}${createUrlParams(urlParams)}`);
    if (open) { setOpen(false); }
  }

  const handleShowMetadata = () => {
    if (metadata?.length) { setShowMetadata(!showMetadata) }
  }

  const showFileByIndex = (index: number) => navigate(createApuDaoFileUrl(apuInfo.id, dao.id, files[index]?.id));

  const handleSelectRelative = (step: number) => {
    if (fileIndex + step >= files.length) {
      handleShowLast();
    }
    else if (fileIndex + step < 0) {
      handleShowFirst();
    }
    else {
      showFileByIndex(fileIndex + step)
    }
  };

  const handleShowFirst = () => showFileByIndex(0);
  const handleShowLast = () => showFileByIndex(files.length - 1);
  const handlePreserveViewportChange = (preserveViewportState: boolean) => setPreserveViewportState(preserveViewportState);

  const createUrlsFromFiles = (files: FileObject[]) => {
    return files.map((file) => {
      const _file = getExistingFile(file);
      const isTile = !!(file && file.tile);
      const uuid = isTile ? _file?.id : _file?.file.id
      return file.tile?.referencedFile?.startsWith("http") ? file.tile.referencedFile : `${API_URL}/tile/${uuid}/image.dzi`
    })
  }

  return (
    <div tabIndex={0} ref={daoDialogElement} className={classNames(
      classes.daoDialog,
      fullscreen && classes.daoDialogFixed,
      layoutClasses.flexColumn,
    )}>
      <>
        {viewerRef.current &&
          <Toolbar
            {...{
              zoomIn: () => viewerRef.current?.zoomIn(),
              zoomOut: () => viewerRef.current?.zoomOut(),
              zoomInDisabled: false,
              zoomOutDisabled: false,
              rotateLeft: () => viewerRef.current?.rotateLeft(),
              rotateRight: () => viewerRef.current?.rotateRight(),
              reset: () => viewerRef.current?.reset(),
              previousEnabled: true,
              nextEnabled: true,
              previousDisabled: fileIndex <= 0,
              nextDisabled: fileIndex < 0 || fileIndex >= files.length - 1,
              selectRelative: handleSelectRelative,
              selectIndex: showFileByIndex,
              totalItemCount: files.length,
              itemIndex: fileIndex,
              item: dao,
              setItem,
              open,
              setOpen: hideThumbnails ? undefined : setOpen,
              file,
              showCloseButton: !embed,
              customActionsLeft: customActionsLeft?.({ fullscreen }),
              customActionsRight: customActionsRight?.({ fullscreen }),
              customActionsCenter: <>
                {<ToolbarButton
                  Component={preserveViewportState ? Lock : LockOpen}
                  title={formatMessage({ id: Message.PRESERVE_VIEW })}
                  onClick={() => { viewerRef.current?.togglePreserveViewport() }}
                />}
                {
                  <div style={{ position: "relative" }}>
                    <ToolbarButton
                      Component={Tune}
                      title={formatMessage({ id: Message.IMAGE_SETTINGS })}
                      onClick={() => { setImageSettingsOpen(!imageSettingsOpen); }}
                    />
                    {imageSettingsOpen &&
                      <div style={{
                        position: "absolute",
                        background: "#000a",
                        padding: "15px 20px",
                        right: 0,
                        color: "white",
                        width: "250px",
                        borderRadius: "5px",
                      }}>
                        <ImageSettingsWindow {...{
                          brightness,
                          contrast,
                          onBrightnessChange: (level) => setBrightness(level),
                          onContrastChange: (level) => setContrast(level),
                        }} />
                      </div>
                    }
                  </div>}
                {<ToolbarButton
                  Component={showNavigator ? Explore : ExploreOutlined}
                  title={formatMessage({ id: Message.SHOW_NAVIGATOR })}
                  onClick={() => { setShowNavigator(!showNavigator) }}
                />}
                {showMetadataInImageViewer && <ToolbarButton Component={InfoOutlined} title={"info"} onClick={handleShowMetadata} />}
                {embed && <ToolbarButton
                  Component={fullscreen ? FullscreenExit : Fullscreen}
                  onClick={() => setFullscreen(!fullscreen)}
                  title={formatMessage({ id: fullscreen ? Message.FULLSCREEN_EXIT : Message.FULLSCREEN })}
                />}
                {customActionsCenter?.({ fullscreen })}
              </>,
            }}
          />
        }
        <div className={classNames(layoutClasses.flexGrow1, layoutClasses.flex)}>
          {!hideThumbnails && <div
            className={classNames(
              classes.daoDialogSide,
              open && classes.daoDialogSideOpen
            )}
          >
            <ImageList files={files} activeFile={file} onClick={handleClickThumbnail} />
          </div>}
          <div
            className={classNames(
              classes.daoDialogSection,
              layoutClasses.flexGrow1,
            )}
            style={{ position: "relative" }}
          >
            {files.length && fileUuid ? (
              <div style={{ width: "100%", height: "100%", filter: `brightness(${brightness / 100}) contrast(${contrast / 100})` }}>
                <ImageViewer
                  ref={viewerRef}
                  parentId={dao.id}
                  urls={createUrlsFromFiles(files)}
                  page={files.findIndex((file) => {
                    return fileUuid === file?.tile?.id || fileUuid === file?.id || fileUuid === file?.published?.id;
                  })}
                  onPreserveViewportChange={handlePreserveViewportChange}
                  showNavigator={showNavigator}
                />
              </div>
            ) : (
                <div
                  className={classNames(
                    classes.daoDialogNoFiles,
                    layoutClasses.flexCentered
                  )}
                >
                  <FormattedMessage id={Message.NO_FILES_TO_DISPLAY} />
                </div>
              )}
            <div
              className={classes.daoDialogFloatingOverlay}
              style={{ ...getDaoPlacementStyle(daoNamePlacement), padding: '10px' }}
            >
              {showInfo && <>
                <div className={classes.bold}>
                  {apuInfo.name}
                </div>
                {apuInfo.description && <div style={{
                  marginTop: '5px',
                }}>
                  {apuInfo.description.length > 150
                    ? `${apuInfo.description?.slice(0, 150)}...`
                    : apuInfo.description
                  }
                </div>}
              </>
              }
              <div style={{ marginTop: '5px' }}>
                {fileIndex + 1}/{files.length} - {file.published?.metadata?.find((item) => item.type === "name")?.value}
              </div>
            </div>
            {daoFooter &&
              <div
                className={classes.daoDialogFloatingOverlay}
                style={{ bottom: 0, right: 0 }}
              >
                <span style={{
                  paddingRight: '10px',
                  borderRight: '1px solid currentColor',
                  margin: '0 5px'
                }}>
                  {daoFooter.copyrightText}
                </span>
                {daoFooter.links?.map((link, index) => {
                  return <a
                    key={index}
                    target='_'
                    style={{
                      margin: '0 5px',
                      color: 'white'
                    }}
                    href={link.url}
                  >
                    {link.text}
                  </a>
                })}
              </div>
            }
          </div>
          {showMetadata && <div
            className={classNames(
              classes.daoDialogSection,
              classes.daoDialogMetadataContainer,
            )}
          >
            <div
              className={classNames(
                classes.daoDialogMetadata,
                spacingClasses.paddingSmall
              )}
            >
              <div
                onClick={() =>
                  downloadFileByUrl(
                    `/digitalObjectFile/${metadataId}/metadata/csv`,
                    `dao_${dao.id}_metadata.csv`
                  )}
                className={classes.daoDialogMetadataButton}
              >
                <GetAppIcon className={classNames(classes.icon)} />
                {formatMessage({ id: Message.DOWNLOAD_METADATA })}
              </div>
              <div className={layoutClasses.flex}>
                <div className={classes.bold}>{formatMessage({ id: Message.METADATA })}</div>
              </div>
              <div>
                {metadata?.map(({ id, value, type }) => (
                  <div {...{ key: id, className: layoutClasses.flex }}>
                    <div
                      className={classNames(
                        classes.bold,
                        classes.daoDialogMetadataLabel
                      )}
                    >
                      {type}:
                  </div>
                    <div>{value}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>}
        </div>
      </>
    </div>
  );
}
