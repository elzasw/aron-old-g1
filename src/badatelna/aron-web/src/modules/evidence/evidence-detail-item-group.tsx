import classNames from 'classnames';
import { compact } from 'lodash';
import React, { useEffect, useState } from 'react';
import { useIntl } from 'react-intl';
import { getApus } from '../../common-utils';
import { ApuPartItemDataType, Message } from '../../enums';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { ApuEntitySimplified } from '../../types';
import { EvidenceDetailItemValue } from './evidence-detail-item-value';
import { useStyles } from './styles';
import { useConfiguration } from '../../components';

interface EvidenceDetailItem {
  name?: string;
  type: ApuPartItemDataType;
  href?: string;
  value: string;
  code?: string;
}

function splitArrayIntoChunks<T>(arr: Array<T>, chunk: number) {
  const segmentedArray = [];
  for (let i=0; i < arr.length; i += chunk) {
    segmentedArray.push(arr.slice(i, i + chunk));
  }
  return segmentedArray;
}

const fetchApus = async (items: EvidenceDetailItem[], currentApus: ApuEntitySimplified[], itemLimit: number) => {
  const start = currentApus.length; // leave out existing apus
  const end = itemLimit || items.length;

  const apuList = [...items].slice(start, end).map((item) => item.value);
  const segmentedApuList = splitArrayIntoChunks(apuList, 100);
  const newApus = apuList.length > 0
    && await Promise.all(segmentedApuList.map((listSegment) => getApus(listSegment)))
    || [];
  return currentApus.concat(...compact(newApus));
}

export function EvidenceDetailItemGroup({
  apuId,
  items,
  initialItemLimit = 10,
}: {
  apuId?: string,
  items: EvidenceDetailItem[];
  initialItemLimit?: number;
}) {
  const { formatMessage } = useIntl();

  const { alternativeItemLabel } = useConfiguration();
  const classes = useStyles({ alternativeItemLabel });
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const type = items[0]?.type;
  const code = items[0]?.code;
  const name = items[0]?.name;
  const hasApus = type === ApuPartItemDataType.APU_REF;

  const [apus, setApus] = useState<ApuEntitySimplified[]>();
  const [apusLoading, setApusLoading] = useState(false);
  const [apusAllLoaded, setApusAllLoaded] = useState(!hasApus);
  const [showAllItems, setShowAllItems] = useState(false);

  useEffect(()=>{
    if(!hasApus || apus || apusLoading){return;}
    setApusLoading(true);
    (async () => {
      const newApus = await fetchApus(items, [], initialItemLimit);
      setApus(newApus);
      setApusLoading(false);
    })()
  }, [apuId])

  const handleShowAllItems = () => {
    if(apusAllLoaded){
      setShowAllItems(!showAllItems);
      return;
    }
    if(!apusAllLoaded && apus && !showAllItems){
      setApusLoading(true);
      (async () => {
        const newApus = await fetchApus(items, apus, items.length);
        setApus(newApus);
        setApusLoading(false);
        setApusAllLoaded(true);
        setShowAllItems(true);
      })()
    }
  }

  const labelClassName = classNames(
    classes.evidenceDetailItemText,
    classes.evidenceDetailItemLabel,
    spacingClasses.marginRight,
    spacingClasses.paddingRight
  );

  // hide the whole group when no apus in apu_ref
  if(hasApus && apus?.length === 0){
    return <></>;
  }

  return <div key={`${code}`} className={layoutClasses.flex}>
    <div
      className={classNames(
        labelClassName,
        classes.evidenceDetailItemLabelBorder,
        spacingClasses.paddingTopSmall
      )}
    >
      {name}
    </div>
    <div
      className={classNames(
        classes.evidenceDetailItemText,
        spacingClasses.paddingTopSmall
      )}
    >
      {(!hasApus || apus) && items.slice(0, showAllItems && apusAllLoaded ? items.length : initialItemLimit).map((item, i) =>
      {
          if(!hasApus){return <EvidenceDetailItemValue key={`${item.value}-${i}`} {...item}/>}
          const apu = apus?.find((apu)=> apu.id === item.value);
          if(apu){return <EvidenceDetailItemValue key={`${item.value}-${i}`} {...item} apu={apu}/>}
          return undefined;
        }
      )}

      {apusLoading && `${formatMessage({id: Message.LOADING})}...`}
      {items.length > initialItemLimit &&
        <div className={classes.evidenceDetailItemExpandButtonWrapper}>
        <button
          onClick={handleShowAllItems}
          className={classes.evidenceDetailItemExpandButton}
        >
          {showAllItems
              ? formatMessage({id: Message.APU_HIDE})
              : `${formatMessage({id: Message.APU_SHOW_ALL})} (${items.length})`}
        </button>
        </div>
        }
    </div>
  </div>
}
