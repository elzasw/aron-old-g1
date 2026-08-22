.. _ta_input:

=======================
Vstupní složka
=======================


Vstupní složka je hlavním datovým vstupem transformačního agenta. 
Do vstupní složky jsou nahrávány soubory tvořící pro jednotlivé typy 
jednotek publikace. Cesta k této složce je součástí konfigurace. 
Transformační agent v pravidelných intervalech monitoruje vstupní složku
(adresář :file:`Input`). V případě zjištění nového vstupu je tento zpracován. 
Úspěšně zpracovaný vstup je přesunut do složky :file:`data`. Vstup, který se 
nepodaří zpracovat z libovolného důvodu je přesunut do složky :file:`error`,
případně zůstane přímo ve vstupní složce.

Interval provádění kontrol je možné určit v konfiguraci.

Všechny druhy vstupů mají shodnou základní strukturu. Odlišuje se formát 
vstupních souborů a jejich pojmenování. 

Při zpracování vstupu je vytvářen protokol o průběhu zpracování a zapisován 
do souboru :file:`protokol.txt`. Při startu transformačního agenta dojde 
ke kontrole existence podřízených složek. Pokud některé neexistují, 
tak jsou automaticky vytvořeny.

.. _ta_input_direct:

Direct
=========

Přímý datový vstup ve formátu jádra (schéma ``http://www.aron.cz/apux/2020``). 
Hlavní soubor datového vstupu Direct musí být pojmenován :file:`apux-<identifikátor>.xml`. 
Případné přílohy jsou jsou uloženy v podsložce files, kde jednotlivý soubor 
je identifikován svým UUID.

Komponenta soubor načte.Provede ověření prvků popisu a ty, které odkazují 
na přístupový bod přidá mezi dostupné přístupové body. Pokud XML obsahuje 
odkazy na DAO, tak přidá tyto DAO do fronty ke zpracování. Následně je XML 
zařazeno do fronty k odeslání.

.. _ta_input_inst:

Institutions
==============

Datový vstup obsahující data o institucích. Data se nahrávají pomocí XML, 
které může obsahovat informace o 1..n institucích. Soubor se musí jmenovat 
:file:`institutions-<identifikátor>.xml`. Soubor je v nativním formátu Elza.

Při zpracování souboru jsou importovány informace o instituci uvedené v 
identifikátoru v názvu souboru.


.. _ta_input_funds:

Funds
===========

Informace o archivních souborech. V jednotlivé datové podsložce musí být soubor 
:file:`fund-<identifikace>.xml`. Soubor je v nativním formátu Elza.

.. _ta_input_archdescs:

Archdesc
===========

Informace o jednotkách popisu. V jednotlivé datové podsložce musí být soubor 
:file:`archdesc-<identifikace>.xml`. Soubor je ve formátu dle schématu Elza.

Pokud archivní popis odkazuje na archivní soubor nebo instituci, které
v transformačním agentovi neexistují, nelze vstup zpracovat ani při dalším
pokusu. Takový vstup je proto přesunut do složky :file:`error`, důvod je
zapsán do souboru :file:`protokol.txt` a zpracování se již neopakuje.
Pokud data pocházejí z publikace Elzy, je publikace v Elze označena jako
chybná (stav ``PUBLISH_ERROR``) včetně uvedeného důvodu.
