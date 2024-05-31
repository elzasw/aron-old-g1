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
}: {
  value: string;
  type: ApuPartItemDataType;
  href?: string;
  apu?: ApuEntitySimplified;
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
            {get(apu, 'name', 'Zobrazit APU')}
          </Link>
          {apu && get(apu, 'description') ? (
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
