

/**
 * Metadatov� z�znam archiv�lie.
 * @export
 * @interface ArchivalieInfo
 */
export interface ArchivalieInfo {
    /**
     * Jednozna�n� identifikace archivn�ho souboru v r�mci archivu.
     * @type {number}
     * @memberof ArchivalieInfo
     */
    cisloNad: number;
    /**
     * Identifik�tor ��sti archivn�ho souboru, jednozna�n� v r�mci cisloNad.
     * @type {number}
     * @memberof ArchivalieInfo
     */
    dilciList?: number;
    /**
     * Ukl�dac� jednotka/ukl�dac� ��slo. Uv�d� se u analogov�ch archiv�li�.
     * @type {string}
     * @memberof ArchivalieInfo
     */
    ukladaciJednotka?: string;
    /**
     * Identifik�tor AIP (ulo�en� v p��slu�n�m digit�ln�m archivu).
     * @type {string}
     * @memberof ArchivalieInfo
     */
    aipId?: string;
    /**
     * Identifikace jednotky popisu dle p��slu�n�ch pravidel popisu.
     * @type {string}
     * @memberof ArchivalieInfo
     */
    archivniIdentifikator?: string;
    /**
     * Jin� ozna�en� jednotky popisu umo��uj�c� jej� identifikaci.
     * @type {string}
     * @memberof ArchivalieInfo
     */
    jineOznaceni?: string;
    /**
     * Textov� popis polo�ky objedn�vky.
     * @type {string}
     * @memberof ArchivalieInfo
     */
    popis?: string;
    /**
     * Form�ln� n�zev na archiv�lii.
     * @type {string}
     * @memberof ArchivalieInfo
     */
    formNazev?: string;
    /**
     * Slou�en� prvky Obsah/Regest nad�azen�ch �rovn� z�znamu do nejvy��� s�rie.
     * @type {string}
     * @memberof ArchivalieInfo
     */
    uplnyPopis?: string;
    /**
     * Odkaz URL dan� jednotky popisu.
     * @type {string}
     * @memberof ArchivalieInfo
     */
    url?: string;
    /**
     * Datace vzniku archiv�lie.
     * @type {string}
     * @memberof ArchivalieInfo
     */
    datace?: string;
}

/**
 * Strukturovan� popis chyby.
 * @export
 * @interface Chyba
 */
export interface Chyba {
    /**
     * K�d chyby pro jej� p�esn�j�� ur�en�.
     * @type {string}
     * @memberof Chyba
     */
    kod?: string;
    /**
     * U�ivatelsky srozumiteln� popis chyby.
     * @type {string}
     * @memberof Chyba
     */
    zprava: string;
    /**
     * Technick� popis chyby, nap��klad zachycen� v�jimka.
     * @type {string}
     * @memberof Chyba
     */
    techDetail?: string;
}

/**
 * Objedn�vka archiv�li� bez ur�en� osoby.
 * 
 * Objedn�vka je ur�ena pro konkr�tn� archiv.
 * Obsahuje URL nutn� pro p�esm�rov�n� po�adavku po p�ipojen� objedn�vky k osob�.
 * @export
 * @interface NepripojenaObjednavka
 */
export interface NepripojenaObjednavka {
    /**
     * Jm�no objednatele.
     * @type {string}
     * @memberof NepripojenaObjednavka
     */
    name: string;
    /**
     * E-mailov� adresa objednatele.
     * @type {string}
     * @memberof NepripojenaObjednavka
     */
    email: string;
    /**
     * Telefonní číslo objednatele.
     * @type {string}
     * @memberof NepripojenaObjednavka
     */
    phoneNumber?: string;
    /**
     * P�edm�ty e-mailov�ch zpr�v zas�lan�ch v r�mci objedn�vky.
     * @type {Array<string>}
     * @memberof NepripojenaObjednavka
     */
    subjects?: Array<string>;
    /**
     * N�vratov� URL, kter� bude zavol�no po p�ipojen� objedn�vky k osob�.
     * @type {string}
     * @memberof NepripojenaObjednavka
     */
    returnUrl: string;
    /**
     * Ur�en� archivu jeho� archiv�lie jsou v objedn�vce po�adov�ny.
     * 
     * Jedn� se o identifik�tor archivu ze seznamu archiv�.
     * Objedn�vka je v�dy ur�ena jen pro jeden archiv.
     * @type {string}
     * @memberof NepripojenaObjednavka
     */
    archiv: string;
    /**
     * Ur�en� badatelna archivu.
     * 
     * Jedn� se o identifik�tor badatelny, kter� odpov�d� registrovan�
     * badateln� v modulu PAR.
     * @type {string}
     * @memberof NepripojenaObjednavka
     */
    badatelna?: string;
    /**
     * Textov� popis objedn�vky (nestrukturovan� hodnota).
     * @type {string}
     * @memberof NepripojenaObjednavka
     */
    popis?: string;
    /**
     * Pole polo�ek objedn�vky.
     * @type {Array<PolozkaObjednavky>}
     * @memberof NepripojenaObjednavka
     */
    polozky?: Array<PolozkaObjednavky>;
}

/**
 * Omezen� na zp��stupn�n� archiv�lie.
 * - NAHLIZENI_DAO_JEN_V_BADATELNE - Digit�ln� archivn� objekt p��stupn� jen v badateln�
 * - NEPRISTUPNY_ORIGINAL - Nep��stupn� origin�l analogov� archiv�lie
 * @export
 */
export const OmezeniZpristupneni = {
    NahlizeniDaoJenVBadatelne: 'NAHLIZENI_DAO_JEN_V_BADATELNE',
    NepristupnyOriginal: 'NEPRISTUPNY_ORIGINAL'
} as const;
export type OmezeniZpristupneni = typeof OmezeniZpristupneni[keyof typeof OmezeniZpristupneni];

/**
 * Po�adovan� archiv�lie.
 * @export
 * @interface PolozkaObjednavky
 */
export interface PolozkaObjednavky {
    /**
     * 
     * @type {ArchivalieInfo}
     * @memberof PolozkaObjednavky
     */
    archivalie: ArchivalieInfo;
    /**
     * 
     * @type {ZpusobZpristupneniArchivalie}
     * @memberof PolozkaObjednavky
     */
    zpusobZpristupneni: ZpusobZpristupneniArchivalie;
    /**
     * 
     * @type {OmezeniZpristupneni}
     * @memberof PolozkaObjednavky
     */
    omezeniZpristupneni?: OmezeniZpristupneni;
}


/**
 * Zp�sob zp��stupn�n� archiv�lie.
 * - NAHLIZENI - nahl�en� archiv�lie
 * - REPLIKA - poskytnut� repliky �i reprodukce dle povahy archiv�lie
 * - OVERENI_SHODY - poskytnut� s ov��en�m shody
 * @export
 */
export const ZpusobZpristupneniArchivalie = {
    Nahlizeni: 'NAHLIZENI',
    Replika: 'REPLIKA',
    OvereniShody: 'OVERENI_SHODY'
} as const;
export type ZpusobZpristupneniArchivalie = typeof ZpusobZpristupneniArchivalie[keyof typeof ZpusobZpristupneniArchivalie];

