import { useCallback, useContext } from 'react';
import { useIntl } from 'react-intl';

import {
  NavigationContext,
  SnackbarContext,
  SnackbarVariant,
} from '@eas/common-web';

import { createUrlParams, createFiltersParam } from './navigation';
import { Message } from '../enums';

export function createEvidenceUrl(
  path: string,
  p: number | undefined, // page
  s: number | undefined, // pageSize
  q: string | undefined, // query
  f: any[] | undefined // filters
) {
  return `${path}${createUrlParams({
          p,
          s,
          q,
          f: createFiltersParam(f),
        })}`
}

export function useEvidenceNavigation() {
  const { formatMessage } = useIntl();

  const { navigate } = useContext(NavigationContext);

  const { showSnackbar } = useContext(SnackbarContext);

  const navigateTo = useCallback(
    (
      path: string,
      p: number | undefined, // page
      s: number | undefined, // pageSize
      q: string | undefined, // query
      f: any[] | undefined // filters
    ) => {
      const url = `${path}${createUrlParams({
        p,
        s,
        q,
        f: createFiltersParam(f),
      })}`;

      // TODO: find solution for long URL (save filters locally)
      if (url.length > 2010) {
        showSnackbar(
          formatMessage({ id: Message.URL_TOO_LONG }),
          SnackbarVariant.WARNING
        );
      } else {
        navigate(url);
      }
    },
    [formatMessage, navigate, showSnackbar]
  );

  return navigateTo;
}
