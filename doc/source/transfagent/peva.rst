.. _ta_peva:

=======================
Komunikace s PEvA
=======================

PEvA II je celostátní evidence archiválií provozovaná Národním archivem.
Transformační agent z ní dokáže přebírat archivní soubory (listy NAD)
a archivní pomůcky a předávat je k publikaci do jádra. Kapitola popisuje,
jak tato komunikace probíhá. Nepopisuje převod jednotlivých údajů
do publikovaných jednotek popisu.

Transformační agent data z PEvA pouze čte, žádné údaje do ní nezapisuje.
Stažená data ukládá do :ref:`vstupní složky <ta_input>`, odkud jsou
zpracována stejným způsobem jako ostatní vstupy, a výsledek je zařazen
do fronty k odeslání do jádra.

.. uml::
    :caption: Přehled komunikace s PEvA
    :align: center

    cloud "PEvA" as PEVA

    rectangle "Transformační agent" {
      component "Synchronizace PEvA" as SYNC
      folder "Vstupní složka" as IN
      component "Zpracování vstupu" as PROC
      queue "Fronta k odeslání" as Q
    }

    database "Databáze agenta" as DB
    component "Jádro (AronCore)" as CORE

    SYNC -up-> PEVA : SOAP / HTTPS\n(pouze čtení)
    SYNC -right-> IN : stažená data (XML)
    SYNC -down-> DB : kurzor synchronizace
    IN -right-> PROC
    PROC -right-> Q : apusrc.xml
    PROC -down-> DB
    Q -right-> CORE


.. _ta_peva_conn:

Připojení
==========

Komunikace probíhá prostřednictvím webové služby PEvA (protokol SOAP)
přes HTTPS. Agent se přihlašuje jménem a heslem účtu zřízeného v PEvA
(WS-Security UsernameToken) a v každém požadavku předává identifikátor
archivu a uživatele, pod kterým dotaz provádí. Přebírána jsou data,
která má daný účet v PEvA dostupná.

Napojení na PEvA je aktivní jen tehdy, je-li v konfiguraci uvedena
adresa služby ``peva2.url``.

Pokud agent zajišťuje publikaci pro více archivů (například oblastní archiv
a jeho okresní archivy), uvede se hlavní účet a dále seznam dalších
archivů ``peva2.institutions``. Pro každý archiv ze seznamu je vytvořeno
samostatné připojení a synchronizace probíhá pro každé připojení nezávisle.
Není-li u archivu v seznamu uvedeno heslo, použije se heslo hlavního účtu.

Příklad konfigurace:

.. code-block:: yaml

    peva2:
      # adresa webové služby PEvA
      url: https://<adresa PEvA>/peva-external/ws/peva/
      # hlavní účet
      username: <uživatelské jméno>
      password: <heslo>
      userId: <identifikátor uživatele>
      institutionId: <UUID archivu v PEvA>
      # další archivy (nepovinné)
      institutions:
        - institutionId: <UUID archivu v PEvA>
          username: <uživatelské jméno>
      # interval synchronizace v sekundách
      interval: 3600
      # počet záznamů v jedné dávce
      batchSize: 100
      # průběžné přebírání změn včetně zrušených záznamů
      newStateChecking: true
      # přebírané agendy
      importFund: true
      importFindingAid: true
      findingAidProperties:
        # vyžádání chybějícího archivního souboru při importu pomůcky
        importMissingFund: true

Význam voleb:

 * ``url`` - adresa webové služby PEvA
 * ``username``, ``password`` - přihlašovací údaje hlavního účtu
 * ``userId``, ``institutionId`` - identifikátor uživatele a archivu v PEvA,
   předávané v každém požadavku
 * ``institutions`` - seznam dalších archivů, každý s vlastním ``institutionId``,
   ``username`` a případně ``password``
 * ``interval`` - interval mezi synchronizacemi v sekundách (výchozí hodnota 3600)
 * ``batchSize`` - počet záznamů vyžádaných v jednom dotazu (výchozí hodnota 100)
 * ``newStateChecking`` - způsob synchronizace popsaný v této kapitole,
   je nutné jej zapnout
 * ``importFund`` - zapíná přebírání archivních souborů (ve výchozím stavu vypnuto)
 * ``importFindingAid`` - zapíná přebírání archivních pomůcek (ve výchozím stavu vypnuto)
 * ``findingAidProperties.importMissingFund`` - viz :ref:`ta_peva_findingaids`
 * ``soapLogging`` - zapíná zápis odesílaných a přijímaných zpráv do logu,
   vhodné jen pro hledání závad


.. _ta_peva_sync:

Průběh synchronizace
=====================

Synchronizace se spouští v pravidelném intervalu (``peva2.interval``).
V každém běhu jsou postupně zpracovány zapnuté agendy, nejprve archivní
soubory a po nich archivní pomůcky, a to pro každé nastavené připojení.

Agent se dotazuje na seznam záznamů seřazený podle času poslední změny.
Záznamy přebírá po dávkách (``peva2.batchSize``). S každou dávkou vrací
PEvA kurzor, tedy pozici, na které má další dotaz pokračovat. Agent kurzor
po zpracování každé dávky uloží do své databáze, samostatně pro každou
agendu a každé připojení. Běh končí ve chvíli, kdy PEvA nevrátí žádný
další záznam.

Kurzor zůstává uložen i mezi jednotlivými běhy. Další běh proto naváže
tam, kde předchozí skončil, a převezme jen záznamy, které byly v PEvA
mezitím změněny nebo nově vytvořeny. Při prvním běhu, kdy kurzor ještě
neexistuje, jsou převzaty všechny dostupné záznamy.

Agent si v dotazu vyžádá i záznamy, které byly v PEvA zrušeny nebo
zneplatněny. Díky tomu se o zrušení dozví a může publikovaný záznam
z jádra odstranit.

.. uml::
    :caption: Cyklus synchronizace
    :align: center

    participant "Synchronizace PEvA" as SYNC
    database "Databáze agenta" as DB
    participant "PEvA" as PEVA
    collections "Vstupní složka" as IN

    loop jednou za nastavený interval
      loop pro každou zapnutou agendu a každé připojení
        SYNC -> DB : načtení kurzoru
        DB --> SYNC : searchAfter
        loop dokud PEvA vrací záznamy
          SYNC -> PEVA : dotaz na seznam\n(size, searchAfter, řazení podle času změny,\nvčetně zrušených záznamů)
          PEVA --> SYNC : dávka záznamů, nový searchAfter
          SYNC -> IN : uložení záznamů
          SYNC -> DB : uložení kurzoru
        end
      end
    end

Pokud komunikace s PEvA selže (například pro nedostupnost služby), běh
se přeruší a agent jej po krátké prodlevě zopakuje od posledního uloženého
kurzoru. Již převzaté dávky se znovu nestahují.

Přílohy ani digitalizáty nejsou z PEvA přebírány.


.. _ta_peva_funds:

Import archivních souborů
==========================

Přebírání archivních souborů se zapíná volbou ``peva2.importFund``.

Agent se dotáže na seznam změněných listů NAD (operace ``listNadSheet``).
Pro každý list ze seznamu si následně vyžádá:

 * úplný list NAD (operace ``getNadSheet``),
 * vnitřní a vnější změny evidované k listu (operace ``listInternalChange``
   a ``listExternalChange``).

Jde-li o dílčí list, načte agent také jeho hlavní list, aby zjistil číslo
NAD. Změny se u dílčích listů nepřebírají.

Získaná data agent uloží do vstupní složky :file:`input/fund`. Pro každý
list NAD vznikne podsložka :file:`pevafund-<identifikátor>` se souborem
:file:`pevafund-<identifikátor>.zip`, který obsahuje:

 * :file:`pevafund-<identifikátor>.xml` - list NAD ve formátu PEvA,
 * :file:`internal_changes.xml` - vnitřní změny,
 * :file:`external_changes.xml` - vnější změny.

Identifikátor je sestaven z kódu archivu, čísla NAD a čísla dílčího listu,
například :file:`pevafund-jafa100000010_1001___`.

Složku následně zpracuje stejná komponenta, která zpracovává
:ref:`archivní soubory z jiných zdrojů <ta_input_funds>`. Vytvoří
jednotku publikace, vstup přesune do složky :file:`data` a zařadí jej
do fronty k odeslání do jádra. Opakovaně převzatý list NAD aktualizuje
již existující archivní soubor.

Archivní soubor lze zpracovat jen tehdy, je-li již naimportován archiv
(instituce), kterému patří. Dílčí list je zpracován až po svém hlavním listu.
Vstup, který zatím zpracovat nelze, zůstává ve vstupní složce a agent
se jej pokouší zpracovat znovu.

.. uml::
    :caption: Import archivního souboru
    :align: center

    participant "Synchronizace PEvA" as SYNC
    participant "PEvA" as PEVA
    collections "Vstupní složka\ninput/fund" as IN
    participant "Zpracování vstupu" as PROC
    database "Databáze agenta" as DB
    participant "Jádro" as CORE

    SYNC -> PEVA : listNadSheet
    PEVA --> SYNC : dávka listů NAD
    loop pro každý list NAD
      SYNC -> PEVA : getNadSheet
      PEVA --> SYNC : list NAD
      opt dílčí list
        SYNC -> PEVA : getNadSheet (hlavní list)
        PEVA --> SYNC : hlavní list
      end
      SYNC -> PEVA : listInternalChange, listExternalChange
      PEVA --> SYNC : vnitřní a vnější změny
      SYNC -> IN : pevafund-<identifikátor>.zip
    end

    PROC -> IN : načtení složky
    alt platný archivní soubor
      PROC -> DB : uložení, zařazení do fronty
      opt odkazovaní původci a místa dosud nepřevzatí
        PROC -> PEVA : getOriginator, getGeoObject
        PEVA --> PROC : původce, geografický objekt
      end
      PROC -> CORE : odeslání archivního souboru
    else archivní soubor vyřazený z evidence
      PROC -> DB : označení jako smazaný, zařazení do fronty
      PROC -> CORE : odstranění archivního souboru
    end

Původci a místa vzniku
-----------------------

Archivní soubor odkazuje na své původce a místa vzniku. Pokud odkazovaný
původce nebo geografický objekt dosud nebyl převzat, agent si jej z PEvA
vyžádá jednotlivě (operace ``getOriginator`` a ``getGeoObject``).

Vyřazení z evidence
--------------------

Vnitřní a vnější změny slouží k rozpoznání, že archivní soubor již
v archivu není. Archivní soubor je označen jako smazaný a z jádra
je odstraněn, pokud je list NAD ve stavu přesunuto, nebo pokud je
k němu evidována provedená změna, kterou zaniká celý archivní soubor:

 * vnější změna - úbytek delimitací celého archivního souboru, vydání
   vlastníkovi v ČR, vydání do zahraničí,
 * vnitřní změna - zničení nebo přehodnocení, nedohledání, přesun celého
   archivního souboru do jiného souboru.

Společně s archivním souborem vyřazeným takovou změnou jsou odstraněny
i jeho archivní popis a archivní pomůcky. Pomůcka, která se vztahuje
i k jinému platnému archivnímu souboru, zůstává zachována.

Souběh se systémem Elza
------------------------

Archivní popis přebíraný ze systému Elza se připojuje k archivnímu souboru
převzatému z PEvA. Tam, kde archivní soubory dodává PEvA, je proto vhodné
vypnout automatické zakládání chybějících archivních souborů při importu
archivního popisu, viz :ref:`ta_input_archdescs`.


.. _ta_peva_findingaids:

Import archivních pomůcek
==========================

Přebírání archivních pomůcek se zapíná volbou ``peva2.importFindingAid``.

Agent se dotáže na seznam změněných pomůcek (operace ``listFindingAid``).
Seznam obsahuje úplné záznamy pomůcek, další dotazy na jednotlivé pomůcky
proto nejsou potřeba. Pomůcka, která není přiřazena k žádnému listu NAD,
se nepřebírá.

Každou pomůcku agent uloží do vstupní složky :file:`input/findingaids`
do podsložky :file:`pevafa-<kód archivu>_<identifikátor pomůcky>` jako
soubor :file:`pevafa-<kód archivu>_<identifikátor pomůcky>.xml` ve formátu PEvA.

Při zpracování složky agent vyhledá archivní soubory, ke kterým se pomůcka
vztahuje. Hledá je mezi již naimportovanými archivními soubory téhož archivu
podle identifikátoru listu NAD.

 * Je-li nalezen alespoň jeden archivní soubor, vytvoří se jednotka publikace
   s odkazem na nalezené archivní soubory, vstup se přesune do složky
   :file:`data` a zařadí se do fronty k odeslání do jádra.
 * Není-li nalezen žádný, pomůcka zůstává ve vstupní složce a agent se ji
   pokouší zpracovat znovu. Je-li zapnuta volba
   ``peva2.findingAidProperties.importMissingFund``, agent si chybějící
   archivní soubory vyžádá z PEvA (operace ``getNadSheet``). Ty jsou
   naimportovány způsobem popsaným v části :ref:`ta_peva_funds` a pomůcka
   je zpracována poté.

Archivní soubory jsou zpracovávány přednostně před pomůckami. Při běžné
synchronizaci jsou tedy archivní soubory k dispozici dříve, než dojde
na zpracování pomůcek, které na ně odkazují.

Stejně jako u archivních souborů musí být již naimportován archiv (instituce),
kterému pomůcka patří.

.. uml::
    :caption: Import archivní pomůcky
    :align: center

    participant "Synchronizace PEvA" as SYNC
    participant "PEvA" as PEVA
    collections "Vstupní složka\ninput/findingaids" as IN
    participant "Zpracování vstupu" as PROC
    database "Databáze agenta" as DB
    participant "Jádro" as CORE

    SYNC -> PEVA : listFindingAid
    PEVA --> SYNC : dávka archivních pomůcek
    loop pro každou pomůcku
      SYNC -> IN : pevafa-<kód archivu>_<identifikátor>.xml
    end

    PROC -> IN : načtení složky
    PROC -> DB : vyhledání archivních souborů pomůcky
    alt archivní soubor nalezen, pomůcka platná
      PROC -> DB : uložení, zařazení do fronty
      PROC -> CORE : odeslání archivní pomůcky
    else archivní soubor nalezen, pomůcka neplatná
      PROC -> DB : označení jako smazaná, zařazení do fronty
      PROC -> CORE : odstranění archivní pomůcky
    else archivní soubor chybí
      note over PROC, IN : pomůcka zůstává ve vstupní složce\na zpracuje se později
      opt zapnuto importMissingFund
        SYNC -> PEVA : getNadSheet
        PEVA --> SYNC : list NAD
        SYNC -> IN : uložení do input/fund
      end
    end

Změny a zrušení
----------------

Změní-li se pomůcka v PEvA, je při další synchronizaci převzata znovu
a již existující pomůcka je aktualizována.

Pomůcka, která je v PEvA označena jako neplatná, je označena jako smazaná
a z jádra je odstraněna. Pomůcka je odstraněna také tehdy, je-li z evidence
vyřazen archivní soubor, ke kterému se vztahuje (viz :ref:`ta_peva_funds`).

Pomůcky převzaté z PEvA a pomůcky převzaté ze systému Elza jsou na sobě
nezávislé a vzájemně se nepárují.
