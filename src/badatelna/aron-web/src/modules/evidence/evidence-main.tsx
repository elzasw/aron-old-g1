import React from 'react';
import { FormattedMessage } from 'react-intl';

import { Module, useConfiguration } from '../../components';
import { Message, ModulePath } from '../../enums';
import { useStyles } from './styles';
import { MainProps } from './types';
import { FiltersProvider } from './evidence-filters';
import { EvidenceResults } from './evidence-results';
import { EvidenceList } from './evidence-list';
import { EvidenceSidebar } from './evidence-sidebar';

export function EvidenceMain({
  modulePath: path,
  label,
  facets,
  apuPartItemTypes,
}: MainProps) {
  const classes = useStyles();
  const { useStructuredResults } = useConfiguration();

  let textId: Message | null = null;

  switch (path) {
    case ModulePath.ARCH_DESC:
      textId = Message.TEXT_ARCH_DESC;
      break;
    case ModulePath.ENTITY:
      textId = Message.TEXT_ENTITY;
      break;
    case ModulePath.FINDING_AID:
      textId = Message.TEXT_FINDING_AID;
      break;
    case ModulePath.FUND:
      textId = Message.TEXT_FUND;
      break;
  }

  return (
    <Module
      {...{
        path,
        items: [{ label }],
        ...(textId
          ? {
            toolbar: (
              <div className={classes.evidenceTextInfo}>
                <FormattedMessage id={textId} />
              </div>
            ),
          }
          : {}),
      }}
    >
      <FiltersProvider {...{ path, facets, apuPartItemTypes }}>
        <EvidenceSidebar {...{ apuPartItemTypes, facets, path }} />
        {useStructuredResults ? <EvidenceResults /> : <EvidenceList />}
      </FiltersProvider>
    </Module>
  );
}
