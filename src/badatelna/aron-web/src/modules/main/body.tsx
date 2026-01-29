import classNames from 'classnames';
import React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { parseYaml, useGet } from '../../common-utils';
import { SearchMain, useConfiguration } from '../../components';
import { OtherSource } from '../../components/configuration';
import { ApiUrl, Message } from '../../enums';
import { useAppStyles, useLayoutStyles, useSpacingStyles } from '../../styles';
import { FavouritesSection } from './favourites-section';
import { useStyles } from './styles';

export const Body: React.FC = () => {
  const classes = useStyles();
  const appClasses = useAppStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const { otherSources, otherSourcesIcon, favouritesStyle, favouritesSections, widerHomePage } =
    useConfiguration();

  const { formatMessage } = useIntl();

  const [favouriteQueriesText, loadingFavouriteQueries] = useGet<string>(
    ApiUrl.FAVORITE_QUERY,
    { textResponse: true }
  );

  const favouriteQueries: OtherSource[] | null = parseYaml(favouriteQueriesText);

  return (
    <div
      className={classNames(
        classes.mainBody,
        layoutClasses.flexCentered,
        spacingClasses.paddingVerticalBig
      )}
    >
      <div className={widerHomePage ? classes.mainBodyInnerWide : classes.mainBodyInner}>
        <h2 className={appClasses.screenReaderHidden}>
          <FormattedMessage id={Message.SEARCH} />
        </h2>
        <SearchMain />
        <div className={spacingClasses.paddingBottomBig} />
        <FavouritesSection
          label={formatMessage({ id: Message.FAVOURITE_QUERIES })}
          items={favouriteQueries}
          isLoading={loadingFavouriteQueries}
          style={favouritesStyle}
        />
        <FavouritesSection
          label={formatMessage({ id: Message.OTHER_SOURCES })}
          icon={otherSourcesIcon}
          items={otherSources}
          style={favouritesStyle}
        />
        {(favouritesSections || []).map((favouritesSection, index) => {
          return <FavouritesSection
          key={index}
          {...favouritesSection}
          />
        })}
      </div>
    </div>
  );
};
