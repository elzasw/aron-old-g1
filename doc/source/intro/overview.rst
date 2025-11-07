.. _intr_overview:

=============================
Základní přehled
=============================

Systém umožňuje zpřístupnit
údaje od jednotkách publikace, digitální objekty 
a přílohy jednotek publikace.

Druhy zpřístupněných jednotek publikace jsou:
 - informace o archivu (jako instituci)
 - archivní soubor
 - archivní pomůcka
 - jednotka popisu (a archiválie)
 - přístupový bod

Formou samostatné stránky je možné zobrazit 
novinky, či přejít na nápovědu.

Digitální objekty
=====================

Digitálním objektem (DAO) je obvykle digitalizát 
v některém z podporovaných formátů. Digitální objekty 
jsou odkazované z jednotek publikace a lze je samostatně zobrazit.
Digitální objekty je možné samostatně stahovat.

Podporované formáty digitalizátů:
 - JPG (image/jpeg)
 - PNG (image/png)
 - JPEG2000 (image/jp2)
 - TIFF (image/tiff)

 .. _intr_overview_comps:

Komponenty
=============

Publikační systém je tvořen dvěma komponentami:

 - :ref:`ta_ta` - komponenta zajišťující přípravu dat pro publikaci a jejich vložení
 - :ref:`ar` - publikační jádro zajišťující indexaci a poskytování dat uživatelům

Následující schéma ukazuje příklad instalace aplikace na virtuální server a jednotlivé komponenty 
tvořící aplikaci. Součástí schématu je také propojení na další komponenty.

.. plantuml::
    :caption: Příklad způsobu instalace
    :scale: 70%
    :align: center
    
    node "Virtuální server" {
      portin "HTTPS/443"
      component HTTPD [
        Apache Httpd nebo NGINX
        ----
        Certifikáty pro HTTPS
        Reverzní proxy
      ]
      "HTTPS/443" --> HTTPD
      component AronCore [
        AronCore
        ----
        Java 11
      ]
      component TransformAgent [
        Transformační agent
        ----
        Java 11
      ]
      database Databáze [
         Databáze
         ----
         PostgreSQL 12+
      ]
      component ElasticSearch [
        ElasticSearch  
      ]
      HTTPD -right-> AronCore : HTTP/8080      
      AronCore --> Databáze
      AronCore --> ElasticSearch
      TransformAgent -left-> AronCore
    } 

    node "Elza server" {
      component Elza [
        Elza
        ----
        archivní popis, archivní entity
      ]
    }

    cloud PEvA [
        PEvA
        ----
        Součást Národního Archivního Portálu
    ]

    TransformAgent -right-> Elza : HTTPS
    TransformAgent -down-> PEvA : HTTPS



Poměrně obvyklou variantou k uvedenému příkladu je řešení, 
kdy webový server (Apache HTTPD, NGINX, IIS) je instalován 
na jiném serveru a na virtuálním serveru je nainstalována 
jen aplikace Aron. V takovém případě je server čistě 
aplikační a uživatelé k němu přistupují prostřednictvím samostatného 
webového serveru na nějž je 
směrováno URL, např. **https://aron.organizace.cz**.

.. _intr_overview_hw:

Hardwarové požadavky
=======================

Hardwarové požadavky vycházejí z předpokládané a následně reálně 
zátěže dané instance aplikace. Jsou závislé na rozsahu 
publikovaných dat, jejich struktuře a také na míře jejich využívání.

Uváděné minimální hodnoty jsou určené pro málo zatížené instance, 
např. pro testování a vývoj. Doporučené hodnoty jsou určeny pro produkční 
prostředí s běžnou zátěží odpovídající menšímu specializovanému archivu.

=================================== ========= ==========
Požadavky                           Minimální Doporučené
=================================== ========= ==========
Velikost RAM                        8GB       16GB
Počet jader CPU                     4         8
Dostupné místo na disku (aplikace)  4GB       16GB
Dostupné místo na disku (data)      10GB      20GB
Místo pro databázi a index          15GB      60GB
=================================== ========= ==========

Součástí uváděných hardwarových požadavků není velikost prostoru pro digitální objekty a 
jejich náhledy. Pokud jsou náhledy vytvářeny na straně Aron a jsou staticky 
předgenerovány, je nutné počítat s dvojnásobnou velikostí diskového prostoru 
než je jejich výchozí podoba. V případě využití samostatného ImageServeru
je prostor definován požadavky daného produktu a velikostí cache.

Zátěž je dána zejména těmito faktory:

 - množství zpřístupněných jednotek popisu
 - rozsah poskytovaných digitálních objektů (obrázků)
 - míra zájmu veřejnosti o zpřístupněná data
