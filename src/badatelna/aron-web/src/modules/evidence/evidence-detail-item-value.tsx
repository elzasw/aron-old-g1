import React from 'react';
import { get } from 'lodash';
import { Link } from 'react-router-dom';

import { ModulePath, ApuPartItemDataType } from '../../enums';
import { useStyles } from './styles';
import { ApuEntitySimplified } from '../../types';
import { formatUnitDate } from '../../common-utils';
import { EvidenceJSONDisplay } from './evidence-json-display';
import { useIntl } from 'react-intl';

export function EvidenceDetailItemValue({
  value,
  type,
  href,
  apu,
  label,
}: {
  value: string;
  type: ApuPartItemDataType;
  href?: string;
  apu?: ApuEntitySimplified;
  label?: string;
}) {
  const classes = useStyles();
  const intl = useIntl();

  let result: string | JSX.Element = value;

  switch (type) {
    case ApuPartItemDataType.UNITDATE:
      result = formatUnitDate(value, intl);
      break;
    case ApuPartItemDataType.APU_REF:
      result = (
        <span>
          <Link
            to={{ pathname: `${ModulePath.APU}/${value}` }}
            className={classes.link}
          >
            {label?label:get(apu, 'name', 'Zobrazit APU')}
          </Link>
          {apu && !label && get(apu, 'description') ? (
            <span>, {get(apu, 'description')}</span>
          ) : (
              <></>
            )}
        </span>
      );
      break;
    case ApuPartItemDataType.LINK:
      result = (
        <a
          href={href || value}
          target="_blank"
          rel="noreferrer"
          className={classes.link}
        >
          {value || href}
        </a>
      );
      break;
    case ApuPartItemDataType.JSON:
      result = <EvidenceJSONDisplay jsonString={value} />;
      break;
  }

  return <span>{result}</span>;
}
