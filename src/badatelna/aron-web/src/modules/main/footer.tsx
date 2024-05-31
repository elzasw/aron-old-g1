import React from 'react';
import classNames from 'classnames';
import { FormattedMessage } from 'react-intl';
import { useStyles } from './styles';
import { useSpacingStyles } from '../../styles';
import { Message } from '../../enums';
import { Props } from './types';

export const Footer = ({ pageTemplate }: Props) => {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();

  if (!pageTemplate?.homepage?.footerCenter && !pageTemplate?.homepage?.footerRight) {
    return <></>;
  }

  return (
    <footer className={classes.mainFooter}>
      <div className={spacingClasses.padding}>
        <div className={classes.mainFooterInner}>
          {pageTemplate ? (
            <div className={classes.mainFooterSections}>
              {[
                pageTemplate.homepage.footerCenter,
                pageTemplate.homepage.footerRight,
              ].map((item) => (
                item ? <div
                  key={item}
                  className={classNames(
                    classes.mainFooterSection,
                    spacingClasses.padding
                  )}
                  dangerouslySetInnerHTML={{ __html: item }}
                /> : <></>
              ))}
            </div>
          ) : (
              <div className={classes.mainFooterSections}>
                {[
                  {
                    title: Message.BASIC_INFORMATION,
                    content: [
                      <p>
                        ARchiv ONline je webová aplikace{' '}
                        <a href="https://vychodoceskearchivy.cz/">
                          Státního oblastního archivu v Zámrsku
                      </a>{' '}
                      sloužící ke zpřístupnění popisu archiválií a jejich
                      digitalizátů.
                    </p>,
                      <p>
                        Copyright &copy; 2021 Státní oblastní archiv v Zámrsku
                    </p>,
                    ],
                  },
                  {
                    title: Message.CONTACT,
                    content: [<p>webmaster@ahapa.cz</p>],
                  },
                ].map(({ title, content }) => (
                  <div
                    key={title}
                    className={classNames(
                      classes.mainFooterSection,
                      spacingClasses.padding
                    )}
                  >
                    <h2
                      className={classNames(
                        classes.mainFooterTitle,
                        spacingClasses.marginSmall
                      )}
                    >
                      <FormattedMessage id={title} />
                    </h2>
                    {content.map((c: any, i: number) => (
                      <p
                        key={`${i}-${i}`}
                        className={classNames(
                          classes.mainFooterText,
                          spacingClasses.marginNone
                        )}
                      >
                        {c}
                      </p>
                    ))}
                  </div>
                ))}
              </div>
            )}
        </div>
      </div>
    </footer>
  );
};
