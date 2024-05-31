import React, { useState, useEffect, useMemo, useCallback, useContext, ReactNode } from 'react';
import { useParams, Link } from 'react-router-dom';
import { get, find, flatten, compact, isEmpty, sortBy } from 'lodash';
import classNames from 'classnames';
import DoubleArrowIcon from '@material-ui/icons/DoubleArrow';
import AccountTreeIcon from '@material-ui/icons/AccountTree';
import { ArrowLeft, ArrowRight, Search } from '@material-ui/icons';
import { FormattedMessage, useIntl } from 'react-intl';

import {
  ApiUrl,
  ModulePath,
  ApuPartItemEnum,
  Message,
  getNavigationItems,
  ApuType,
} from '../../enums';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import {
  useGet,
  getApu,
  sortByArray,
  useEvidenceNavigation,
  createUrlParams,
  createFiltersParam,
} from '../../common-utils';
import { DetailProps } from './types';
import {
  ApuEntity,
  ApuPartItem,
  ApuLocale,
  ApuPart,
  Dao,
} from '../../types';
import {
  findApuParts,
  filterApuPartTypes,
  getRelatedApusFilter,
  getParentBreadcrumbs,
} from './utils';
import { EvidenceDetailDaoDialog, Icon } from './evidence-detail-dao-dialog';
import { EvidenceDetailDao } from './evidence-detail-dao';
import { EvidenceDetailTree } from './evidence-detail-tree';
import { getPathByItem, useAppState } from '../../common-utils';
import { Module, Button, useConfiguration } from '../../components';
import { EvidenceDetailItem } from './evidence-detail-item';
import { EvidenceDetailAttachments } from './evidence-detail-attachments';
import { LocaleContext, NavigationContext } from '@eas/common-web';
import { EvidenceShareButtons } from './evidence-share-buttons';
import { EvidenceLayout, LayoutType } from './evidence-layout';
import { ActionsRenderProps, FileObject } from './evidence-detail-dao-dialog/types';
import { ApuPathParams, createApuDaoFileUrl } from './evidence';
import { getFiles } from './evidence-detail-dao-dialog/utils';
import { CitationDialog } from './citation-dialog';
import { EvidenceDetailIncrementalTree } from './evidence-detail-incremental-tree';

function EvidenceDao({
  customActionsLeft,
  customActionsRight,
  layoutType,
  items = [],
  apuInfo,
}: {
  customActionsLeft?: (props: ActionsRenderProps) => ReactNode;
  customActionsRight?: (props: ActionsRenderProps) => ReactNode;
  layoutType: LayoutType;
  items: Dao[];
  apuInfo: {
    name?: string;
    description?: string;
    id: string;
  };
}) {
  const { navigate } = useContext(NavigationContext);
  const { daoId, id, fileId } = useParams<ApuPathParams>();
  const [selectedDao, setSelectedDao] = useState<Dao | null>(null);
  const [selectedFile, setSelectedFile] = useState<FileObject | null>(null);

  useEffect(() => {
    setSelectedDao(null);
    setSelectedFile(null);
  }, [apuInfo.id])

  useEffect(() => {
    // ignore change when apu id does not match
    if (id !== apuInfo.id) {
      return;
    }

    let dao = selectedDao;

    if (selectedDao?.id !== daoId) {
      dao = items.find((dao) => dao.id === daoId) || null;
      setSelectedDao(dao);
    }

    if (!dao || !fileId || selectedFile?.id === fileId) {
      return;
    }

    const file = getFiles(dao).find((file) => file.id === fileId);
    if (file) {
      setSelectedFile(file);
    }
  }, [daoId, fileId])

  const file = selectedFile || getFiles(selectedDao || items[0])[0];
  const dao = selectedDao || items[0];

  return layoutType !== LayoutType.ONE_COLUMN ? <EvidenceDetailDaoDialog
    // items={items} 
    dao={dao}
    file={file}
    setItem={() => { }}
    embed={true}
    customActionsLeft={customActionsLeft}
    customActionsRight={customActionsRight}
    apuInfo={apuInfo}
  /> :
    <EvidenceDetailDao
      item={selectedDao}
      file={selectedFile}
      setItem={(dao) => {
        navigate(createApuDaoFileUrl(id, dao?.id))
      }}
      apuInfo={apuInfo}
      items={sortBy(items, 'order')}
    />
}

export function EvidenceDetail({
  apuPartTypes,
  apuPartItemTypes,
}: DetailProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const configuration = useConfiguration();
  const locale = useContext(LocaleContext);

  const { appState, updateAppState } = useAppState();

  const { evidencePath } = appState;

  const navigateTo = useEvidenceNavigation();

  const [open, setOpen] = useState(false);
  const [showTree, setShowTree] = useState(true);
  const [showDescription, setShowDescription] = useState(true);

  const [loadingBasic, setLoading] = useState(false);

  const [archdescRootRefLoading, setArchdescRootRefLoading] = useState(false);

  const [archdescRootRef, setArchdescRootRef] = useState<ApuPartItem | null>(
    null
  );

  const [archdescRootRefItemId, setArchdescRootRefItemId] = useState<string>();

  const [isCitationOpen, setCitationOpen] = useState(false);

  const { id } = useParams();

  const url = `${ApiUrl.APU}/${id}/entity`;

  const [item, loadingItem] = useGet<ApuEntity>(url);

  const loading = loadingItem || loadingBasic;

  const findRoot = useCallback((root?: ApuEntity): ApuEntity | undefined => {
    return root?.parent ? findRoot(root.parent) : root;
  }, []);

  const [root, setRoot] = useState<ApuEntity | undefined>(findRoot(item));

  const { formatMessage } = useIntl();

  useEffect(() => {
    const rootItem = findRoot(item);
    if (rootItem && rootItem.id !== root?.id) {
      setRoot(rootItem);
    }
  }, [item, root, findRoot]);

  useEffect(() => {
    if (
      item &&
      item.parts &&
      !archdescRootRefLoading &&
      (!archdescRootRefItemId || item.id !== archdescRootRefItemId)
    ) {
      setArchdescRootRefItemId(item.id);

      let newArchdescRootRef: ApuPartItem | null = null;

      item.parts!.some((part) =>
        part.items.some((item: ApuPartItem) => {
          if (item.type === ApuPartItemEnum.ARCHDESC_ROOT_REF) {
            newArchdescRootRef = item;
            return true;
          }

          return false;
        })
      );

      if (
        newArchdescRootRef &&
        (!archdescRootRef || newArchdescRootRef!.id !== archdescRootRef.id)
      ) {
        const load = async () => {
          setArchdescRootRefLoading(true);

          setArchdescRootRef(
            (await getApu(newArchdescRootRef!.value))
              ? newArchdescRootRef
              : null
          );

          setArchdescRootRefLoading(false);
        };

        load();
      }
    }
  }, [
    item,
    apuPartTypes,
    apuPartItemTypes,
    archdescRootRef,
    archdescRootRefLoading,
    archdescRootRefItemId,
  ]);

  const getLocalizedName = (langs: ApuLocale[], defaultName: string) => {
    const lang = langs.find((lang) =>
      lang.lang.substr(0, 2) === locale.locale.name
    )
    return lang ? lang.text : defaultName;
  }

  const getApuPartItems = (part: ApuPart) => {
    return flatten(
      filterApuPartTypes(apuPartItemTypes, part.items)
        .sort((a, b) => a.viewOrder - b.viewOrder)
        .map(({ code, name, type, lang }) => {
          return compact(
            findApuParts(part.items, code).map((item) => {
              return item.type ===
                ApuPartItemEnum.ARCHDESC_ROOT_REF
                ? null
                : {
                  ...item,
                  code,
                  name: getLocalizedName(lang, name),
                  type,
                };
            })
          );
        }
        )
    )
  }

  const getApuParts = (item: ApuEntity) => {
    if (!item?.parts) { return [] }
    return sortByArray(
      flatten(
        item.parts.map((part) =>
          filterApuPartTypes(apuPartTypes, [part]).map(
            (apuPartType) => {
              return {
                ...apuPartType,
                name: getLocalizedName(apuPartType.lang, apuPartType.name),
                items: getApuPartItems(part),
              };
            }
          )
        )
      ),
      apuPartTypes,
      'code'
    )
  }

  const parts = useMemo(
    () => item ? getApuParts(item) : [],
    [item, apuPartTypes, apuPartItemTypes, locale]
  );

  const path = item ? getPathByItem(item) : undefined;
  const daos = item ? sortBy(item.digitalObjects, 'order') : [];
  const daosHaveFiles = !!daos.find((dao) => dao.files.length > 0);

  useEffect(() => {
    if (evidencePath && path) {
      updateAppState({ evidencePath: null });
    }
  }, [evidencePath, path, updateAppState]);

  return (
    <Module
      {...{
        items: [
          {
            path: evidencePath || path,
            label:
              find(
                getNavigationItems(configuration),
                (item) => item.path === (evidencePath || path)
              )?.label || '',
          },
          ...(item ? getParentBreadcrumbs(item.parent) : []),
          {
            label: item ? item.name : '...',
          },
        ],
      }}
    >
      <EvidenceLayout
        isLoading={loading}
        showTree={showTree}
        renderTree={
          item && path === ModulePath.ARCH_DESC && root ?
            () => <>
              {configuration.showIncrementalTree
                ? <EvidenceDetailIncrementalTree {...{ item, id: root.id, verticalResize: false }} />
                : <EvidenceDetailTree {...{ item, id: root.id, verticalResize: false }} />
              }
              <div className={spacingClasses.paddingBottom} />
            </>
            : undefined
        }
        renderDao={
          item && daos?.length > 0 && daosHaveFiles ?
            ({ layoutType }) => <EvidenceDao
              layoutType={layoutType}
              customActionsLeft={({ fullscreen }) => <>
                {!fullscreen && <Icon
                  onClick={() => setShowTree(!showTree)}
                  Component={showTree ? ArrowLeft : ArrowRight}
                  title={formatMessage({ id: showTree ? Message.TREE_HIDE : Message.TREE_SHOW })}
                />}
              </>}
              customActionsRight={({ fullscreen }) => <>
                {!fullscreen
                  && layoutType === LayoutType.THREE_COLUMN
                  && <Icon
                    onClick={() => setShowDescription(!showDescription)}
                    Component={showDescription ? ArrowRight : ArrowLeft}
                    title={formatMessage({ id: showDescription ? Message.DESCRIPTION_HIDE : Message.DESCRIPTION_SHOW })}
                  />}
              </>}
              apuInfo={{
                name: item.name,
                description: item.description,
                id: item.id,
              }}
              items={daos}
            /> : undefined
        }
        showDesc={showDescription || daos.length === 0}
        renderDesc={() =>
          <div style={{ minWidth: '300px' }} className={spacingClasses.paddingBig}>
            {item ? (
              <div
                className={classNames(
                  classes.evidenceDetailTop,
                  layoutClasses.flexSpaceBetweenBottom,
                  spacingClasses.marginBottom,
                )}
              >
                <div className={layoutClasses.flex}>
                  <div className={spacingClasses.paddingBottomSmall}>
                    <h3 className={spacingClasses.marginBottomSmall}>
                      {item.name}
                    </h3>
                    <Button
                      className={classes.findRelatedButton}
                      label={formatMessage({ id: Message.FIND_RELATED })}
                      outlined={true}
                      size="small"
                      onClick={() => {
                        navigateTo(
                          ModulePath.APU,
                          1,
                          10,
                          '',
                          getRelatedApusFilter(id, item.name)
                        );
                      }}
                    />
                    {item.description ? (
                      path === ModulePath.ARCH_DESC ||
                        path === ModulePath.ENTITY ? (
                          <h3
                            className={classNames(
                              classes.evidenceDetailDescription,
                              spacingClasses.marginBottomSmall
                            )}
                          >
                            {item.description}
                          </h3>
                        ) : (
                          <h4
                            className={classNames(
                              classes.evidenceDetailDescription,
                              spacingClasses.marginBottomSmall
                            )}
                          >
                            {item.description}
                          </h4>
                        )
                    ) : (
                        <></>
                      )}
                    {((configuration.showCitationFor && configuration.showCitationFor.length > 0) || configuration.showShareButtons) && <div className={classes.buttonsContainer}>
                      <EvidenceShareButtons item={item} />
                      {configuration.showCitationFor
                        && configuration.showCitationFor.indexOf(item.type) >= 0
                        && <>
                          <Button
                            className={classNames(classes.findRelatedButton, classes.citationButton)}
                            label={formatMessage({ id: Message.CITATION_CREATE })}
                            outlined={true}
                            size="small"
                            onClick={() => setCitationOpen(true)}
                          />
                          {isCitationOpen && <CitationDialog apuId={id} isOpen={isCitationOpen} onClose={() => setCitationOpen(false)} />}
                        </>}
                    </div>}
                    {archdescRootRef && item.type === ApuType.FUND ? (
                      <div className={spacingClasses.marginTop}>
                        <Link
                          to={{
                            pathname: `${ModulePath.APU}/${archdescRootRef.value}`,
                          }}
                          className={classNames(
                            classes.link,
                            !(item && path === ModulePath.ARCH_DESC && root) && classes.archdescRootLink,
                            layoutClasses.flexAlignCenter,
                            spacingClasses.marginRight
                          )}
                        >
                          <AccountTreeIcon
                            className={spacingClasses.marginRightSmall}
                          />
                          {get(
                            find(
                              apuPartItemTypes,
                              ({ code }) =>
                                code === ApuPartItemEnum.ARCHDESC_ROOT_REF
                            ),
                            'name',
                            formatMessage({
                              id: Message.TREE_VIEW_CURRENT_STATE,
                            })
                          )}
                        </Link>
                        {
                          configuration.showFindRecordsInFundButton &&
                          <Link
                            to={{
                              pathname: ModulePath.ARCH_DESC,
                              search: createUrlParams({ f: createFiltersParam([{ source: "FUND~REF", value: [{ id: item?.id, name: item?.name }] }]) }),
                            }}
                            className={classNames(
                              classes.link,
                              !(item && path === ModulePath.ARCH_DESC && root) && classes.searchLink,
                              layoutClasses.flexAlignCenter,
                            )}
                          >
                            <Search
                              className={spacingClasses.marginRightSmall}
                            />
                            <FormattedMessage id={Message.FIND_RECORDS_IN_FUND} />
                          </Link>
                        }
                      </div>
                    ) : (
                        <></>
                      )}
                  </div>
                </div>
                {configuration.allowDetailExpand && <DoubleArrowIcon
                  className={classNames(
                    classes.evidenceDetailTopIcon,
                    open && classes.evidenceDetailTopIconOpen,
                    spacingClasses.marginBottom
                  )}
                  onClick={() => setOpen(!open)}
                />}
              </div>
            ) : (
                <></>
              )}
            <div style={{ display: "flex", flexDirection: "column" }}>
              <div style={{ flexGrow: 1 }}>
                {parts
                  .map(({ items, ...part }) => ({
                    ...part,
                    items: items.filter(({ visible, value, href }) => visible && (value || href)),
                  }))
                  .filter(({ items }) => !isEmpty(items))
                  .map((partItem, index) => (
                    <EvidenceDetailItem
                      {...{
                        key: `${id}-${partItem.name}-${index}`,
                        ...partItem,
                        index,
                        open,
                        apuId: item?.id,
                      }}
                    />
                  ))}
              </div>
              <div style={{ flexShrink: 0 }}>
                {item && (
                  <EvidenceDetailAttachments
                    items={sortBy(item.attachments, 'order')}
                    setLoading={setLoading}
                  />
                )}
              </div>
            </div>
          </div>}
      />
    </Module>
  );
}
