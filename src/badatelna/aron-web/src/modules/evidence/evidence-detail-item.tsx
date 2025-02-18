import Tooltip from '@material-ui/core/Tooltip';
import classNames from 'classnames';
import React, { useEffect, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { usePrevious } from '../../common-utils';
import { useConfiguration } from '../../components';
import { ApuPartItemDataType, ApuPartViewType, Message } from '../../enums';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { EvidenceDetailItemGroup } from './evidence-detail-item-group';
import { EvidenceDetailItemValue } from './evidence-detail-item-value';
import { useStyles } from './styles';

export function EvidenceDetailItem({
  apuId,
  name,
  viewType,
  items,
  open: outterOpen,
  index,
  value,
}: {
  apuId?: string;
  name: string;
  viewType: ApuPartViewType;
  items: any[];
  open: boolean;
  index: number;
  value?: string;
}) {
  const configuration = useConfiguration();
  const classes = useStyles({alternativeItemLabel: configuration.alternativeItemLabel});
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const [open, setOpen] = useState(viewType === ApuPartViewType.STANDALONE);

  const previousOutterOpen = usePrevious(outterOpen);

  useEffect(() => {
    if (
      viewType === ApuPartViewType.GROUPED &&
      previousOutterOpen !== undefined &&
      previousOutterOpen !== outterOpen
    ) {
      setOpen(outterOpen);
    }
  }, [viewType, outterOpen, previousOutterOpen]);

  const labelClassName = value ? classNames(
    classes.evidenceDetailItemText,
    classes.evidenceDetailItemLabel,
    spacingClasses.marginRight,
    spacingClasses.paddingRight
  ) : classes.evidenceDetailSectionTitle;

  const isGrouped = viewType === ApuPartViewType.GROUPED;

  const renderGroupedItems = () => {
    return <div
      className={classNames(
        classes.evidenceDetailItemText,
        classes.bold,
        open && spacingClasses.paddingBottomSmall
      )}
      style={configuration.allowDetailExpand ? { cursor: 'pointer' } : {}}
      onClick={() => configuration.allowDetailExpand && setOpen(!open)}
    >
      {items
        .filter(({ type }) => type !== ApuPartItemDataType.APU_REF)
        .map(({ value, type }, i) => (
          <React.Fragment key={`${value}-${i}`}>
            <EvidenceDetailItemValue {...{ value, type }} />{' '}
          </React.Fragment>
        ))}
    </div>
  }

  const groupItemsByCode = (items:any[]):[Record<string,any[]>, string[]] => {
    const groupedItems:Record<string, any[]> = {};
    const groupOrder:string[] = [];
    items.forEach((item)=>{
      const code:string = item.code;
      const existingGroup = groupedItems[code];
      if(!existingGroup){
        groupOrder.push(code)
        groupedItems[code] = [item];
      } else {
        groupedItems[code].push(item);
      }
    })
    return [groupedItems, groupOrder];
  }

  const [groupedItems, groupOrder] = groupItemsByCode(items);


  return (
    <div
      className={classNames(
        classes.evidenceDetailItem,
        spacingClasses.paddingTopSmall,
        index && classes.evidenceDetailItemNotFirst
      )}
    >
      <div
        className={classNames(
          layoutClasses.flex,
          (!isGrouped || !open) && spacingClasses.paddingBottomSmall
        )}
      >
        {(isGrouped || configuration.showStandalonePartName) && 
            <div
                className={classNames(
                    labelClassName,
                    isGrouped && classes.evidenceDetailItemLabelBorder
                )}
            >
                {name}
            </div>
        }
        {isGrouped && (
          configuration.allowDetailExpand ? <Tooltip
            title={
              <FormattedMessage
                id={open ? Message.CLICK_TO_COLLAPSE : Message.CLICK_TO_EXPAND}
              />
            }
          >
            {renderGroupedItems()}
          </Tooltip> : renderGroupedItems()
        )}
      </div>
      {open ? (
        <div className={spacingClasses.paddingBottomSmall}>
          {groupOrder.map((groupName, i) => {
            const items = groupedItems[groupName];
            return <EvidenceDetailItemGroup 
              {...{
                items,
                apuId,
                key: `${apuId}-${groupName}-${i}`
              }} 
              />
          }
          )}
        </div>
      ) : (
        <></>
      )}
    </div>
  );
}
