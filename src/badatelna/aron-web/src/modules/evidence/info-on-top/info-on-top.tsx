import React, { useState, useEffect } from 'react';
import classNames from 'classnames';

import { useStyles } from '../styles';
import { useLayoutStyles, useSpacingStyles } from '../../../styles';
import { ApuPartItemDataType, ModulePath } from '../../../enums';
import { useConfiguration } from '../../../components';
import {
  ApuDisplayPart,
  ApuDisplayPartItem,
  ApuEntitySimplified,
} from '../../../types';
import { Link } from 'react-router-dom';
import { getApus } from '../../../common-utils';

export interface Props {
  parts: ApuDisplayPart[];
  setLoading?: (loading: boolean) => void;
}

export function InfoOnTop({ parts }: Props) {
  // const [] = useState(parts.length <= 5);
  const [apus, setApus] = useState<ApuEntitySimplified[]>([]);
  const [itemsOnTop, setItemsOnTop] = useState<ApuDisplayPartItem[]>([]);
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const configuration = useConfiguration();

  useEffect(() => {
    (async () => {
      const refsOnTop: ApuDisplayPartItem[] = [];
      if (configuration.refsOnTop) {
        parts.forEach(({ items }) => {
          const refs = items.filter(({ code }) =>
            configuration.refsOnTop?.includes(code)
          );
          if (refs.length > 0) {
            refsOnTop.push(...refs);
          }
        });
      }
      const apuIds = refsOnTop
        .filter(({ type }) => type === ApuPartItemDataType.APU_REF)
        .map(({ value }) => value);
      const apus = await getApus(apuIds);

      const resultItems = refsOnTop.filter(({ type, value }) => {
        if (type !== ApuPartItemDataType.APU_REF) {
          return true;
        }
        return (apus || []).find((apu) => apu.id === value);
      });

      setApus(apus || []);
      setItemsOnTop(resultItems);
    })();
  }, [parts]);

  if (!configuration.refsOnTop || configuration.refsOnTop.length == 0) {
    return null;
  }

  if (itemsOnTop.length == 0) {
    return null;
  }

  return (
    <>
      {configuration.refsOnTop.map((_code) => {
        const _refs = itemsOnTop.filter(({ code }) => code === _code);
        if (_refs.length === 0) {
          return;
        }

        return (
          <div>
            <div>
              <b>{_refs[0].name}</b>
            </div>
            <div>
              {_refs.map(({ value, type }) =>
                type === ApuPartItemDataType.APU_REF ? (
                  <Link
                    to={{
                      pathname: `${ModulePath.APU}/${value}`,
                    }}
                    className={classNames(
                      classes.link,
                      classes.archdescRootLink,
                      layoutClasses.flexAlignCenter,
                      spacingClasses.marginRight
                    )}
                  >
                    {apus.find(({ id }) => id === value)?.name || value}
                  </Link>
                ) : (
                  <div>{value}</div>
                )
              )}
            </div>
          </div>
        );
      })}
    </>
  );

  // return items.length ? (
  //   <div
  //     className={spacingClasses.marginTopBig}
  //     style={{
  //       margin: 0,
  //       border: '1px solid #ddd',
  //       padding: '8px',
  //       borderRadius: '8px',
  //       background: '#f8f8f8',
  //       marginBottom: '16px',
  //     }}
  //   >
  //     {true && (
  //       <div style={{ display: 'flex' }}>
  //         <h3
  //           style={{
  //             margin: 0,
  //             display: 'flex',
  //             alignItems: 'center',
  //             cursor: 'pointer',
  //           }}
  //           className={spacingClasses.marginBottomSmall}
  //           onClick={() => setIsOpen(!isOpen)}
  //         >
  //           <FormattedMessage id={Message.ATTACHMENTS} />
  //           &nbsp; ({items.length}) &nbsp;
  //           {isOpen ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
  //         </h3>
  //       </div>
  //     )}
  //     {isOpen && (
  //       <div
  //         className={classNames(
  //           layoutClasses.flexAlignCenter,
  //           layoutClasses.flexWrap
  //         )}
  //         style={{ marginTop: '10px' }}
  //       >
  //         {items.map(({ id, name, file }) => {
  //           const fileId = file?.file?.id;
  //
  //           return (
  //             <div
  //               {...{
  //                 key: id,
  //                 className: classNames(
  //                   classes.attachment,
  //                   layoutClasses.flexCentered,
  //                   spacingClasses.paddingSmall,
  //                   spacingClasses.marginExtraSmall,
  //                   // spacingClasses.marginBottomSmall,
  //                   fileId && classes.attachmentDownload
  //                 ),
  //                 onClick: async () => {
  //                   if (fileId) {
  //                     setLoading(true);
  //                     const ok = await downloadFile(fileId, name);
  //                     if (!ok) {
  //                       showSnackbar(
  //                         formatMessage({ id: Message.ERROR_DOWNLOAD_FILE }),
  //                         SnackbarVariant.ERROR
  //                       );
  //                     }
  //                     setLoading(false);
  //                   }
  //                 },
  //               }}
  //             >
  //               <div
  //                 className={classNames(
  //                   classes.attachmentLabel,
  //                   spacingClasses.marginRight
  //                 )}
  //               >
  //                 {name}
  //               </div>
  //               {fileId ? <GetAppIcon /> : <></>}
  //             </div>
  //           );
  //         })}
  //       </div>
  //     )}
  //   </div>
  // ) : (
  //   <></>
  // );
}
