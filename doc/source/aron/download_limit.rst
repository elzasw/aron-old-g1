.. _ar_download_limit:

==============================
Omezení hromadného stahování
==============================

Digitalizáty jsou velké a aplikace sama žádné omezení neuplatňuje: odpoví na
každý požadavek, který dostane. Jeden uživatel, který stahuje celé knihy,
proto může zaplnit linku nebo obsadit server tak, že běžní badatelé snímky
nezobrazí. Ochranu je třeba nastavit na HTTP serveru před aplikací.

Dva druhy požadavků
======================

.. list-table::
   :widths: 15 40 45
   :header-rows: 1

   * - Druh
     - Cesty
     - Povaha
   * - Dlaždice
     - ``/api/aron/tile/{id}/…``, případně samostatný obrazový server
     - Mnoho malých požadavků v dávkách (otočení stránky, přiblížení)
       s přestávkami mezi nimi.
   * - Soubory
     - ``/api/aron/file/{id}`` a ``/api/aron/referencedfiles/{id}``
     - Celé soubory: stažení, ale také náhledy a obrázky stránek bez dlaždic.
       Velké soubory jsou dlouhé přenosy a z nich se hromadné stahování skládá.

Úplný obraz lze složit také z dlaždic, proto je třeba omezit oba druhy.

Omezení v Apache HTTPD
=========================

Apache HTTPD sám omezení podle klienta nemá. Modul ``mod_ratelimit`` zpomalí
jednotlivou odpověď (``rate-limit`` v KiB/s, ``rate-initial-burst`` pro část
odeslanou plnou rychlostí), ale nesčítá přenosy jednoho klienta. Zbytek doplní
modul třetí strany ``mod_qos``:

.. list-table::
   :widths: 40 60
   :header-rows: 1

   * - Direktiva
     - Omezení
   * - ``QS_SrvMaxConnPerIP``
     - Počet spojení z jedné adresy.
   * - ``QS_LocRequestLimitMatch``
     - Počet souběžných požadavků na cesty odpovídající vzoru.
   * - ``QS_LocRequestPerSecLimitMatch``
     - Počet požadavků za sekundu na cesty odpovídající vzoru.
   * - ``QS_LocKBytesPerSecLimitMatch``
     - Šířka pásma pro stahování z cest odpovídajících vzoru.
   * - ``QS_ErrorResponseCode``
     - Stavový kód odmítnutého požadavku; vhodný je ``429``.

Doporučený postup:

- Nejprve omezit počet souběžných přenosů souborů. Přenos přes proxy obsadí
  po celou dobu jedno vlákno Apache HTTPD.
- Velké soubory zpomalit až po prvních megabajtech, aby náhledy prošly plnou
  rychlostí.
- Hodnoty odvodit z provozního logu a po změně v logu sledovat, zda nejsou
  odmítáni běžní badatelé.
- Známé sítě (například badatelnu) z omezení vyjmout.

Mezipaměť prohlížeče pro dlaždice
====================================

Dlaždice by si měl prohlížeč pamatovat, jinak je při každém návratu na stránku
stahuje znovu. Hlavičky odpovědi lze ověřit příkazem::

    curl -I https://<adresa portálu>/api/aron/tile/<id>/image.dzi

Pokud odpověď obsahuje ``Cache-Control: no-store``, lze hlavičku pro dlaždice
přepsat modulem ``mod_headers``::

    <LocationMatch "^/api/aron/tile/">
      Header set Cache-Control "public, max-age=604800"
      Header unset Pragma
      Header unset Expires
    </LocationMatch>

Další možnosti
=================

Přehled dalších opatření (ověření, že návštěvník je člověk, umístění souborů
mimo portál, ochranná služba před portálem) je v administrátorské příručce
druhé generace portálu ARON, v kapitole *Limiting bulk downloading*.
