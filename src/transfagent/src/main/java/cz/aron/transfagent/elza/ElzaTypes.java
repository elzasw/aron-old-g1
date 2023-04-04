package cz.aron.transfagent.elza;

import java.util.HashMap;
import java.util.Map;

/**
 * Seznam typu/constant v Elza
 *
 *
 */
public abstract class ElzaTypes {

    public static final String NM_MAIN = "NM_MAIN";
    public static final String NM_MINOR = "NM_MINOR";
    public static final String NM_DEGREE_PRE = "NM_DEGREE_PRE";
    public static final String NM_DEGREE_POST = "NM_DEGREE_POST";

    public static final String NM_SUP_GEN = "NM_SUP_GEN";
    public static final String NM_SUP_CHRO = "NM_SUP_CHRO";
    public static final String NM_SUP_GEO = "NM_SUP_GEO";
    public static final String NM_SUP_DIFF = "NM_SUP_DIFF";
    public static final String NM_SUP_PRIV = "NM_SUP_PRIV";

    public static final String NM_TYPE = "NM_TYPE";
    public static final String NT_ACRONYM = "NT_ACRONYM";

    public static final String NM_SUPS[] = {
            NM_SUP_GEN, NM_SUP_CHRO, NM_SUP_GEO
    };    
    
    public static final String IDN_TYPE = "IDN_TYPE";
    public static final String IDN_VALUE = "IDN_VALUE";

    // Stručná charakteristika
    public static final String BRIEF_DESC = "BRIEF_DESC";

    // Administrativní zařazení
    public static final String GEO_ADMIN_CLASS = "GEO_ADMIN_CLASS";

    public static final String CORP_PURPOSE = "CORP_PURPOSE";
    public static final String FOUNDING_NORMS = "FOUNDING_NORMS";
    public static final String HISTORY = "HISTORY";
    public static final String GENEALOGY = "GENEALOGY";
    public static final String BIOGRAPHY = "BIOGRAPHY";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String SCOPE_NORMS = "SCOPE_NORMS";
    public static final String CORP_STRUCTURE = "CORP_STRUCTURE";
    public static final String SOURCE_INFO = "SOURCE_INFO";
    public static final String SOURCE_LINK = "SOURCE_LINK";

    public static final String REL_ENTITY = "REL_ENTITY";

    public static final String CRE_CLASS = "CRE_CLASS";
    public static final String CRE_DATE = "CRE_DATE";
    public static final String CRE_TYPE = "CRE_TYPE";
    public static final Map<String, String> creTypeMap = new HashMap<>();
    static {
        creTypeMap.put("CRT_ESTAB", "vznik zřízením/založením");
        creTypeMap.put("CRT_COMMENCEMENT", "vznik zahájením činnosti");
        creTypeMap.put("CRT_PREDECESSOR", "vznik změnou předchůdce");
        creTypeMap.put("CRT_RECORDSENTRY", "vznik zápisem do evidence");
        creTypeMap.put("CRT_UNSPECIFIED", "nespecifikovaný vznik");
        creTypeMap.put("CRT_CREATION", "vytvoření");
        creTypeMap.put("CRT_EDITION", "vydání");
        creTypeMap.put("CRT_FIRSTREALIZATION", "první realizace");
        creTypeMap.put("CRT_VALIDITYBEGIN", "počátek platnosti");
    }
    public static final String EXT_CLASS = "EXT_CLASS";
    public static final String EXT_DATE = "EXT_DATE";
    public static final String EXT_TYPE = "EXT_TYPE";
    public static final Map<String, String> extTypeMap = new HashMap<>();
    static {
        // extTypeMap
    }

    public static final String ZP2015_EXTRA_UNITS = "ZP2015_EXTRA_UNITS";
    public static final String ZP2015_UNIT_SUBTYPE = "ZP2015_UNIT_SUBTYPE";
    public static final String ZP2015_RECORD_TYPE = "ZP2015_RECORD_TYPE";
    public static final String ZP2015_LANGUAGE = "ZP2015_LANGUAGE";
    public static final String ZP2015_UNIT_DATE = "ZP2015_UNIT_DATE";
    public static final String ZP2015_DATE_OTHER = "ZP2015_DATE_OTHER";
    public static final String ZP2015_ENTITY_ROLE = "ZP2015_ENTITY_ROLE";
    public static final String ZP2015_OTHER_ID = "ZP2015_OTHER_ID";
    public static final String ZP2015_STORAGE_ID = "ZP2015_STORAGE_ID";

    public static final String ZP2015_ORIGINATOR = "ZP2015_ORIGINATOR";
    public static final String ZP2015_POSITION = "ZP2015_POSITION";
    public static final String ZP2015_ORIENTATION = "ZP2015_ORIENTATION";
    public static final String ZP2015_ITEM_MAT = "ZP2015_ITEM_MAT";
    public static final String ZP2015_PART = "ZP2015_PART";

    public static final String ZP2015_WRITING = "ZP2015_WRITING";
    public static final String ZP2015_CORROBORATION = "ZP2015_CORROBORATION";
    public static final String ZP2015_IMPRINT_COUNT = "ZP2015_IMPRINT_COUNT";
    public static final String ZP2015_IMPRINT_ORDER = "ZP2015_IMPRINT_ORDER";
    public static final String ZP2015_LEGEND = "ZP2015_LEGEND";
    public static final String ZP2015_MOVIE_LENGTH = "ZP2015_MOVIE_LENGTH";
    public static final String ZP2015_RECORD_LENGTH = "ZP2015_RECORD_LENGTH";
    public static final String ZP2015_ITEM_LINK = "ZP2015_ITEM_LINK";
    public static final String ZP2015_DAO_LINK = "ZP2015_DAO_LINK";
    public static final String ZP2015_RELATED_UNITS_LINK = "ZP2015_RELATED_UNITS_LINK";
    
    public static final String ZP2015_APPLIED_RESTRICTION = "ZP2015_APPLIED_RESTRICTION";

    public static final Map<String, String> unitTypeMap = new HashMap<>();
    static {
        unitTypeMap.put("ZP2015_UNIT_TYPE_LIO", "listina do roku 1850");
        unitTypeMap.put("ZP2015_UNIT_TYPE_LIP", "listina po roce 1850");
        unitTypeMap.put("ZP2015_UNIT_TYPE_UKN", "úřední kniha");
        unitTypeMap.put("ZP2015_UNIT_TYPE_RKP", "rukopis");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PPR", "podací protokol");
        unitTypeMap.put("ZP2015_UNIT_TYPE_IND", "index");
        unitTypeMap.put("ZP2015_UNIT_TYP_ELE", "elench");
        unitTypeMap.put("ZP2015_UNIT_TYPE_REP", "repertář");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KTT", "kartotéka");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PEC", "pečetidlo");
        unitTypeMap.put("ZP2015_UNIT_TYPE_RAZ", "razítko");
        unitTypeMap.put("ZP2015_UNIT_TYPE_OTD", "samostatná pečeť, odlitek pečeti, otisk typáře");
        unitTypeMap.put("ZP2015_UNIT_TYPE_OTC", "kopie otisku");
        unitTypeMap.put("ZP2015_UNIT_TYPE_MAP", "mapa");
        unitTypeMap.put("ZP2015_UNIT_TYPE_ATL", "atlas");
        unitTypeMap.put("ZP2015_UNIT_TYPE_TVY", "technický výkres");
        unitTypeMap.put("ZP2015_UNIT_TYPE_GLI", "grafický list");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KRE", "kresba");
        unitTypeMap.put("ZP2015_UNIT_TYPE_FSN", "fotografie na papírové podložce");
        unitTypeMap.put("ZP2015_UNIT_TYPE_FSD", "fotografická deska");
        unitTypeMap.put("ZP2015_UNIT_TYPE_LFI", "listový film");
        unitTypeMap.put("ZP2015_UNIT_TYPE_SFI", "svitkový film");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KIN", "kinofilm");
        unitTypeMap.put("ZP2015_UNIT_TYPE_MF", "mikrofilm");
        unitTypeMap.put("ZP2015_UNIT_TYPE_MFS", "mikrofiš");
        unitTypeMap.put("ZP2015_UNIT_TYPE_FAL", "fotoalbum");
        unitTypeMap.put("ZP2015_UNIT_TYPE_DFO", "digitální fotografie");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KZA", "kinematografický záznam");
        unitTypeMap.put("ZP2015_UNIT_TYPE_ZZA", "zvukový záznam");
        unitTypeMap.put("ZP2015_UNIT_TYPE_TIO", "tisk do roku 1800");
        unitTypeMap.put("ZP2015_UNIT_TYPE_TIP", "tisk po roce 1800");
        unitTypeMap.put("ZP2015_UNIT_TYPE_POH", "pohlednice");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PKT", "plakát");
        unitTypeMap.put("ZP2015_UNIT_TYPE_CPA", "cenný papír");
        unitTypeMap.put("ZP2015_UNIT_TYPE_STO", "štoček");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PNP", "předmět numizmatické povahy");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PFP", "předmět faleristické povahy");
        unitTypeMap.put("ZP2015_UNIT_TYPE_JIN", "jiná");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KTT_BOX", "kartotéční zásuvka");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KTT_ITEM", "kartotéční lístek");
    }

    public static final Map<String, String> extraUnitTypeMap = new HashMap<>();
    static {
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_LIO",
                             "listina do roku 1850");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_LIP",
                             "listina po roce 1850");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_UKN",
                             "úřední kniha");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_RKP",
                             "rukopis");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_PPR",
                             "podací protokol");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_IND",
                             "index");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_ELE",
                             "elench");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_REP",
                             "repertář");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_KTT",
                             "kartotéka");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_PEC",
                             "pečetidlo");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_RAZ",
                             "razítko");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_OTD",
                             "samostatné pečeť, odlitek pečeti a otisk typáře");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_MAP", "mapa");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_ATL", "atlas");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_TVY", "technický výkres");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_GLI", "grafický list");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_KRE", "kresba");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_FSN", "fotografie na papírové podložce");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_FSD", "fotografická deska");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_LFI", "listový film");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_SFI", "svitkový film");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_KIN", "kinofilm");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_MF", "mikrofilm");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_MFS", "mikrofiš");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_FAL", "fotoalbum");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_DFO", "digitální fotografie");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_KZA", "kinematografický záznam");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_ZZA", "zvukový záznam");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_TIO", "tisk do roku 1800");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_TIP", "tisky po roce 1800");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_POH", "pohlednice");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_PKT", "plakát");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_CPA", "cenný papír");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_STO", "štoček");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_PNP", "předmět numizmatické povahy");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_PFP", "předmět faleristické povahy");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_JIN", "jiná");
    }

    public static Map<String, String> subtypeMap = new HashMap<>();
    static {
        subtypeMap.put("ZP2015_UNIT_SUBTYPE_MTR", "matrika");
        subtypeMap.put("ZP2015_UNIT_SUBTYPE_HDB", "hudebnina");
        subtypeMap.put("ZP2015_UNIT_SUBTYPE_STV", "vyznamenání / cena");
        subtypeMap.put("ZP2015_UNIT_SUBTYPE_PA", "právní akt");
        subtypeMap.put("ZP2015_UNIT_SUBTYPE_STVR", "udělení / propůjčení vyznamenání / ceny");
        subtypeMap.put("ZP2015_UNIT_SUBTYPE_JMF", "jmenování / ustanovení do funkce</name");
    }

    public static Map<String, String> recordTypeMap = new HashMap<>();
    static {
        recordTypeMap.put("ZP2015_RECORD_TYPE_BORN", "záznamy narozených");
        recordTypeMap.put("ZP2015_RECORD_TYPE_DIED", "záznamy zemřelých");
        recordTypeMap.put("ZP2015_RECORD_TYPE_MARRIED", "záznamy oddaných");
        recordTypeMap.put("ZP2015_RECORD_TYPE_INDEX_BORN", "rejstřík narozených");
        recordTypeMap.put("ZP2015_RECORD_TYPE_INDEX_DIED", "rejstřík zemřelých");
        recordTypeMap.put("ZP2015_RECORD_TYPE_INDEX_MARRIED", "rejstřík oddaných");
    }

    public static Map<String, String> roleSpecMap = new HashMap<>();
    static {
        roleSpecMap.put("ZP2015_ENTITY_ROLE_1", "ENTITY_ROLE_1");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_2", "ENTITY_ROLE_2");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_4", "ENTITY_ROLE_4");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_5", "ENTITY_ROLE_5");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_10", "ENTITY_ROLE_10");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_6", "ENTITY_ROLE_6");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_7", "ENTITY_ROLE_7");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_8", "ENTITY_ROLE_8");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_9", "ENTITY_ROLE_9");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_11", "ENTITY_ROLE_11");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_3", "ENTITY_ROLE_3");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_12", "ENTITY_ROLE_12");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_13", "ENTITY_ROLE_13");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_14", "ENTITY_ROLE_14");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_15", "ENTITY_ROLE_15");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_16", "ENTITY_ROLE_16");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_17", "ENTITY_ROLE_17");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_18", "ENTITY_ROLE_18");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_19", "ENTITY_ROLE_19");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_20", "ENTITY_ROLE_20");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_21", "ENTITY_ROLE_21");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_22", "ENTITY_ROLE_22");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_23", "ENTITY_ROLE_23");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_24", "ENTITY_ROLE_24");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_25", "ENTITY_ROLE_25");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_26", "ENTITY_ROLE_26");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_27", "ENTITY_ROLE_27");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_28", "ENTITY_ROLE_28");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_29", "ENTITY_ROLE_29");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_30", "ENTITY_ROLE_30");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_31", "ENTITY_ROLE_31");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_32", "ENTITY_ROLE_32");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_33", "ENTITY_ROLE_33");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_34", "ENTITY_ROLE_34");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_35", "ENTITY_ROLE_35");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_36", "ENTITY_ROLE_36");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_37", "ENTITY_ROLE_37");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_38", "ENTITY_ROLE_38");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_39", "ENTITY_ROLE_39");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_40", "ENTITY_ROLE_40");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_41", "ENTITY_ROLE_41");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_42", "ENTITY_ROLE_42");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_43", "ENTITY_ROLE_43");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_44", "ENTITY_ROLE_44");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_45", "ENTITY_ROLE_45");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_46", "ENTITY_ROLE_46");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_47", "ENTITY_ROLE_47");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_48", "ENTITY_ROLE_48");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_49", "ENTITY_ROLE_49");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_50", "ENTITY_ROLE_50");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_51", "ENTITY_ROLE_51");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_52", "ENTITY_ROLE_52");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_53", "ENTITY_ROLE_53");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_54", "ENTITY_ROLE_54");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_55", "ENTITY_ROLE_55");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_56", "ENTITY_ROLE_56");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_57", "ENTITY_ROLE_57");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_58", "ENTITY_ROLE_58");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_59", "ENTITY_ROLE_59");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_60", "ENTITY_ROLE_60");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_61", "ENTITY_ROLE_61");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_62", "ENTITY_ROLE_62");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_63", "ENTITY_ROLE_63");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_64", "ENTITY_ROLE_64");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_65", "ENTITY_ROLE_65");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_66", "ENTITY_ROLE_66");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_67", "ENTITY_ROLE_67");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_68", "ENTITY_ROLE_68");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_69", "ENTITY_ROLE_69");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_70", "ENTITY_ROLE_70");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_71", "ENTITY_ROLE_71");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_72", "ENTITY_ROLE_72");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_73", "ENTITY_ROLE_73");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_74", "ENTITY_ROLE_74");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_75", "ENTITY_ROLE_75");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_76", "ENTITY_ROLE_76");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_77", "ENTITY_ROLE_77");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_78", "ENTITY_ROLE_78");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_79", "ENTITY_ROLE_79");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_80", "ENTITY_ROLE_80");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_81", "ENTITY_ROLE_81");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_82", "ENTITY_ROLE_82");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_83", "ENTITY_ROLE_83");
    }

    public static Map<String, String> dateOtherMap = new HashMap<>();
    static {
        dateOtherMap.put("ZP2015_DATE_OF_CONTENT", "DATE_OF_CONTENT");
        dateOtherMap.put("ZP2015_DATE_DECLARED", "DATE_DECLARED");
        dateOtherMap.put("ZP2015_DATE_ORIG", "DATE_ORIG");
        dateOtherMap.put("ZP2015_DATE_OF_COPY", "DATE_OF_COPY");
        dateOtherMap.put("ZP2015_DATE_SEALING", "DATE_SEALING");
        dateOtherMap.put("ZP2015_DATE_ACT_PUBLISHING", "DATE_ACT_PUBLISHING");
        dateOtherMap.put("ZP2015_DATE_INSERT", "DATE_INSERT");
        dateOtherMap.put("ZP2015_DATE_MOLD_CREATION", "DATE_MOLD_CREATION");
        dateOtherMap.put("ZP2015_DATE_USAGE", "DATE_USAGE");
        dateOtherMap.put("ZP2015_DATE_PUBLISHING", "DATE_PUBLISHING");
        dateOtherMap.put("ZP2015_DATE_MAP_UPDATE", "DATE_MAP_UPDATE");
        dateOtherMap.put("ZP2015_DATE_CAPTURING", "DATE_CAPTURING");
        dateOtherMap.put("ZP2015_DATE_RECORDING", "DATE_RECORDING");
        dateOtherMap.put("ZP2015_DATE_AWARDING", "DATE_AWARDING");
        dateOtherMap.put("ZP2015_DATE_AWARD_CER", "DATE_AWARD_CER");
        dateOtherMap.put("ZP2015_DATE_WITHDRAWAL", "DATE_WITHDRAWAL");
    }

    /**
     * Zjednodussene indexovani jine datace
     */
    public static Map<String, String> dateOtherMapIndex = new HashMap<>();
    static {
        dateOtherMapIndex.put("ZP2015_DATE_DECLARED", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_ORIG", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_OF_COPY", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_SEALING", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_ACT_PUBLISHING", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_INSERT", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_MOLD_CREATION", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_USAGE", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_PUBLISHING", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_MAP_UPDATE", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_CAPTURING", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_RECORDING", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_AWARDING", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_AWARD_CER", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_WITHDRAWAL", "DATE_OTHER");
    }

    public static Map<String, String> languageTypeMap = new HashMap<>();
    static {
        languageTypeMap.put("ZP2015_LANGUAGE_1", "blíže neurčený indoevropský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_2", "blíže neurčený baltský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_3", "litevština");
        languageTypeMap.put("ZP2015_LANGUAGE_4", "lotyština");
        languageTypeMap.put("ZP2015_LANGUAGE_5", "blíže neurčený germánský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_6", "afrikánština");
        languageTypeMap.put("ZP2015_LANGUAGE_7", "alsaština");
        languageTypeMap.put("ZP2015_LANGUAGE_8", "angličtina");
        languageTypeMap.put("ZP2015_LANGUAGE_9", "dánština");
        languageTypeMap.put("ZP2015_LANGUAGE_10", "dolnoněmčina");
        languageTypeMap.put("ZP2015_LANGUAGE_11", "faerština");
        languageTypeMap.put("ZP2015_LANGUAGE_12", "fríština");
        languageTypeMap.put("ZP2015_LANGUAGE_13", "holandština (nizozemština)");
        languageTypeMap.put("ZP2015_LANGUAGE_14", "islandština");
        languageTypeMap.put("ZP2015_LANGUAGE_15", "jidiš");
        languageTypeMap.put("ZP2015_LANGUAGE_16", "judendeutsch");
        languageTypeMap.put("ZP2015_LANGUAGE_17", "lucemburština");
        languageTypeMap.put("ZP2015_LANGUAGE_18", "němčina");
        languageTypeMap.put("ZP2015_LANGUAGE_19", "norština");
        languageTypeMap.put("ZP2015_LANGUAGE_20", "švédština");
        languageTypeMap.put("ZP2015_LANGUAGE_21", "blíže neurčený keltský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_22", "bretonština");
        languageTypeMap.put("ZP2015_LANGUAGE_23", "irština");
        languageTypeMap.put("ZP2015_LANGUAGE_24", "skotská gaelština");
        languageTypeMap.put("ZP2015_LANGUAGE_25", "velština");
        languageTypeMap.put("ZP2015_LANGUAGE_26", "blíže neurčený románský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_27", "francouzština");
        languageTypeMap.put("ZP2015_LANGUAGE_28", "galicijština");
        languageTypeMap.put("ZP2015_LANGUAGE_29", "italština");
        languageTypeMap.put("ZP2015_LANGUAGE_30", "katalánština");
        languageTypeMap.put("ZP2015_LANGUAGE_31", "korsičtina");
        languageTypeMap.put("ZP2015_LANGUAGE_32", "latina");
        languageTypeMap.put("ZP2015_LANGUAGE_33", "moldavština");
        languageTypeMap.put("ZP2015_LANGUAGE_34", "okcitánština");
        languageTypeMap.put("ZP2015_LANGUAGE_35", "portugalština");
        languageTypeMap.put("ZP2015_LANGUAGE_36", "rétorománština");
        languageTypeMap.put("ZP2015_LANGUAGE_37", "rumunština");
        languageTypeMap.put("ZP2015_LANGUAGE_38", "španělština");
        languageTypeMap.put("ZP2015_LANGUAGE_39", "blíže neurčený slovanský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_40", "běloruština");
        languageTypeMap.put("ZP2015_LANGUAGE_41", "bosenština");
        languageTypeMap.put("ZP2015_LANGUAGE_42", "bulharština");
        languageTypeMap.put("ZP2015_LANGUAGE_43", "čeština");
        languageTypeMap.put("ZP2015_LANGUAGE_44", "chorvatština");
        languageTypeMap.put("ZP2015_LANGUAGE_45", "kašubština");
        languageTypeMap.put("ZP2015_LANGUAGE_46", "lužická srbština dolní (dolnolužičtina)");
        languageTypeMap.put("ZP2015_LANGUAGE_47", "lužická srbština horní (hornolužičtina)");
        languageTypeMap.put("ZP2015_LANGUAGE_48", "makedonština");
        languageTypeMap.put("ZP2015_LANGUAGE_49", "polština");
        languageTypeMap.put("ZP2015_LANGUAGE_50", "rusínština");
        languageTypeMap.put("ZP2015_LANGUAGE_51", "ruština");
        languageTypeMap.put("ZP2015_LANGUAGE_52", "slovenština");
        languageTypeMap.put("ZP2015_LANGUAGE_53", "slovinština");
        languageTypeMap.put("ZP2015_LANGUAGE_54", "srbochorvatština");
        languageTypeMap.put("ZP2015_LANGUAGE_55", "srbština");
        languageTypeMap.put("ZP2015_LANGUAGE_56", "staroslověnština, církevní slovanština");
        languageTypeMap.put("ZP2015_LANGUAGE_57", "ukrajinština");
        languageTypeMap.put("ZP2015_LANGUAGE_58", "blíže neurčený indoevropský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_59", "albánština");
        languageTypeMap.put("ZP2015_LANGUAGE_60", "arménština");
        languageTypeMap.put("ZP2015_LANGUAGE_61", "řečtina");
        languageTypeMap.put("ZP2015_LANGUAGE_62", "blíže neurčený uralský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_63", "estonština");
        languageTypeMap.put("ZP2015_LANGUAGE_64", "finština");
        languageTypeMap.put("ZP2015_LANGUAGE_65", "maďarština");
        languageTypeMap.put("ZP2015_LANGUAGE_66", "livština");
        languageTypeMap.put("ZP2015_LANGUAGE_67", "baskičtina");
        languageTypeMap.put("ZP2015_LANGUAGE_68", "blíže neurčený afroasijský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_69", "blíže neurčený semitský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_70", "(novo)hebrejština");
        languageTypeMap.put("ZP2015_LANGUAGE_71", "amharština");
        languageTypeMap.put("ZP2015_LANGUAGE_72", "arabština");
        languageTypeMap.put("ZP2015_LANGUAGE_73", "aramejština");
        languageTypeMap.put("ZP2015_LANGUAGE_74", "hauština");
        languageTypeMap.put("ZP2015_LANGUAGE_75", "maltština");
        languageTypeMap.put("ZP2015_LANGUAGE_76", "stará hebrejština");
        languageTypeMap.put("ZP2015_LANGUAGE_77", "akkadština");
        languageTypeMap.put("ZP2015_LANGUAGE_78", "assyrština");
        languageTypeMap.put("ZP2015_LANGUAGE_79", "babylonština");
        languageTypeMap.put("ZP2015_LANGUAGE_80", "chetitština");
        languageTypeMap.put("ZP2015_LANGUAGE_81", "sumerština");
        languageTypeMap.put("ZP2015_LANGUAGE_82", "egyptština");
        languageTypeMap.put("ZP2015_LANGUAGE_83", "koptština");
        languageTypeMap.put("ZP2015_LANGUAGE_84", "blíže neurčený nigerokonžský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_85", "svahilština");
        languageTypeMap.put("ZP2015_LANGUAGE_86", "blíže neurčený altajský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_87", "japonština");
        languageTypeMap.put("ZP2015_LANGUAGE_88", "korejština");
        languageTypeMap.put("ZP2015_LANGUAGE_89", "blíže neurčený mongolský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_90", "mongolština");
        languageTypeMap.put("ZP2015_LANGUAGE_91", "blíže neurčený turkický jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_92", "azerbajdžánština");
        languageTypeMap.put("ZP2015_LANGUAGE_93", "baškirština");
        languageTypeMap.put("ZP2015_LANGUAGE_94", "gagauzština");
        languageTypeMap.put("ZP2015_LANGUAGE_95", "hazarština");
        languageTypeMap.put("ZP2015_LANGUAGE_96", "kyrgyzština");
        languageTypeMap.put("ZP2015_LANGUAGE_97", "krymská tatarština");
        languageTypeMap.put("ZP2015_LANGUAGE_98", "tatarština");
        languageTypeMap.put("ZP2015_LANGUAGE_99", "turečtina");
        languageTypeMap.put("ZP2015_LANGUAGE_100", "turkménština");
        languageTypeMap.put("ZP2015_LANGUAGE_101", "ujgurština");
        languageTypeMap.put("ZP2015_LANGUAGE_102", "uzbečtina");
        languageTypeMap.put("ZP2015_LANGUAGE_103", "blíže neurčený austroasijský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_104", "santalština");
        languageTypeMap.put("ZP2015_LANGUAGE_105", "khmérština");
        languageTypeMap.put("ZP2015_LANGUAGE_106", "vietnamština");
        languageTypeMap.put("ZP2015_LANGUAGE_107", "blíže neurčený austronéský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_108", "indonéština");
        languageTypeMap.put("ZP2015_LANGUAGE_109", "malajština");
        languageTypeMap.put("ZP2015_LANGUAGE_110", "blíže neurčený drávidský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_111", "kannadština (též kannarština)");
        languageTypeMap.put("ZP2015_LANGUAGE_112", "malajalámština");
        languageTypeMap.put("ZP2015_LANGUAGE_113", "tamilština");
        languageTypeMap.put("ZP2015_LANGUAGE_114", "telugština");
        languageTypeMap.put("ZP2015_LANGUAGE_115", "blíže neurčený indoárijský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_116", "asámština");
        languageTypeMap.put("ZP2015_LANGUAGE_117", "bengálština");
        languageTypeMap.put("ZP2015_LANGUAGE_118", "Bihárštiina (maithilština)");
        languageTypeMap.put("ZP2015_LANGUAGE_119", "dógrí");
        languageTypeMap.put("ZP2015_LANGUAGE_120", "gudžarátština");
        languageTypeMap.put("ZP2015_LANGUAGE_121", "hindština");
        languageTypeMap.put("ZP2015_LANGUAGE_122", "kašmírština");
        languageTypeMap.put("ZP2015_LANGUAGE_123", "konkánština");
        languageTypeMap.put("ZP2015_LANGUAGE_124", "maráthština");
        languageTypeMap.put("ZP2015_LANGUAGE_125", "pahárské jazyky (nepálština)");
        languageTypeMap.put("ZP2015_LANGUAGE_126", "pandžábština");
        languageTypeMap.put("ZP2015_LANGUAGE_127", "romština");
        languageTypeMap.put("ZP2015_LANGUAGE_128", "sanskrt");
        languageTypeMap.put("ZP2015_LANGUAGE_129", "sindština");
        languageTypeMap.put("ZP2015_LANGUAGE_130", "sinhálština");
        languageTypeMap.put("ZP2015_LANGUAGE_131", "urdština");
        languageTypeMap.put("ZP2015_LANGUAGE_132", "urijština");
        languageTypeMap.put("ZP2015_LANGUAGE_133", "blíže neurčený indoíránský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_134", "(novo)perština");
        languageTypeMap.put("ZP2015_LANGUAGE_135", "kurdština");
        languageTypeMap.put("ZP2015_LANGUAGE_136", "paštština");
        languageTypeMap.put("ZP2015_LANGUAGE_137", "stará perština");
        languageTypeMap.put("ZP2015_LANGUAGE_138", "střední perština");
        languageTypeMap.put("ZP2015_LANGUAGE_139", "tádžičtina");
        languageTypeMap.put("ZP2015_LANGUAGE_140", "blíže neurčený kartvelský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_141", "gruzínština");
        languageTypeMap.put("ZP2015_LANGUAGE_142", "blíže neurčený severokavkazský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_143", "abcházština");
        languageTypeMap.put("ZP2015_LANGUAGE_144", "blíže neurčený tajsko-kadajský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_145", "laoština");
        languageTypeMap.put("ZP2015_LANGUAGE_146", "thajština");
        languageTypeMap.put("ZP2015_LANGUAGE_147", "blíže neurčený tibetskočínský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_148", "barmština");
        languageTypeMap.put("ZP2015_LANGUAGE_149", "bodo");
        languageTypeMap.put("ZP2015_LANGUAGE_150", "bhútánština");
        languageTypeMap.put("ZP2015_LANGUAGE_151", "manípurština");
        languageTypeMap.put("ZP2015_LANGUAGE_152", "čínština");
        languageTypeMap.put("ZP2015_LANGUAGE_153", "tibetština");
        languageTypeMap.put("ZP2015_LANGUAGE_154", "blíže neurčený aleutský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_155", "grónština");
        languageTypeMap.put("ZP2015_LANGUAGE_156", "blíže neurčený indiánský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_157", "blíže neurčený");
        languageTypeMap.put("ZP2015_LANGUAGE_158", "blíže neurčený izolovaný jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_159", "blíže neurčený kreolský jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_160", "blíže neurčený umělý jazyk");
        languageTypeMap.put("ZP2015_LANGUAGE_161", "esperanto");
        languageTypeMap.put("ZP2015_LANGUAGE_162", "ido");
        languageTypeMap.put("ZP2015_LANGUAGE_163", "interlingua");
        languageTypeMap.put("ZP2015_LANGUAGE_164", "interlingue");
        languageTypeMap.put("ZP2015_LANGUAGE_165", "volapük");
        languageTypeMap.put("ZP2015_LANGUAGE_166", "slovio");
    }
    public static Map<String, String> otherIdMap = new HashMap<>();
    static {
        otherIdMap.put("ZP2015_OTHERID_SIG_ORIG", "OTHERID_SIG_ORIG");
        otherIdMap.put("ZP2015_OTHERID_SIG", "OTHERID_SIG");
        otherIdMap.put("ZP2015_OTHERID_STORAGE_ID", "OTHERID_STORAGE_ID");
        otherIdMap.put("ZP2015_OTHERID_CJ", "OTHERID_CJ");
        otherIdMap.put("ZP2015_OTHERID_DOCID", "OTHERID_DOCID");
        otherIdMap.put("ZP2015_OTHERID_FORMAL_DOCID", "OTHERID_FORMAL_DOCID");
        otherIdMap.put("ZP2015_OTHERID_ADDID", "OTHERID_ADDID");
        otherIdMap.put("ZP2015_OTHERID_OLDSIG", "OTHERID_OLDSIG");
        otherIdMap.put("ZP2015_OTHERID_OLDSIG2", "OTHERID_OLDSIG2");
        otherIdMap.put("ZP2015_OTHERID_OLDID", "OTHERID_OLDID");
        otherIdMap.put("ZP2015_OTHERID_INVALID_UNITID", "OTHERID_INVALID_UNITID");
        otherIdMap.put("ZP2015_OTHERID_INVALID_REFNO", "OTHERID_INVALID_REFNO");
        otherIdMap.put("ZP2015_OTHERID_PRINTID", "OTHERID_PRINTID");
        otherIdMap.put("ZP2015_OTHERID_PICID", "OTHERID_PICID");
        otherIdMap.put("ZP2015_OTHERID_NEGID", "OTHERID_NEGID");
        otherIdMap.put("ZP2015_OTHERID_CDID", "OTHERID_CDID");
        otherIdMap.put("ZP2015_OTHERID_ISBN", "OTHERID_ISBN");
        otherIdMap.put("ZP2015_OTHERID_ISSN", "OTHERID_ISSN");
        otherIdMap.put("ZP2015_OTHERID_ISMN", "OTHERID_ISMN");
        otherIdMap.put("ZP2015_OTHERID_MATRIXID", "OTHERID_MATRIXID");
    }
    public static Map<String, String> otherIdNameMap = new HashMap<>();
    static {
        otherIdNameMap.put("ZP2015_OTHERID_SIG_ORIG", "sign. pův.");
        otherIdNameMap.put("ZP2015_OTHERID_SIG", "sign.");
        otherIdNameMap.put("ZP2015_OTHERID_STORAGE_ID", "ukl. znak");
        otherIdNameMap.put("ZP2015_OTHERID_CJ", "č. j.");
        otherIdNameMap.put("ZP2015_OTHERID_DOCID", "zn. sp.");
        otherIdNameMap.put("ZP2015_OTHERID_FORMAL_DOCID", "č. vl.");
        otherIdNameMap.put("ZP2015_OTHERID_ADDID", "č. př.");
        otherIdNameMap.put("ZP2015_OTHERID_OLDSIG", "nepl. sign.");
        otherIdNameMap.put("ZP2015_OTHERID_OLDSIG2", "sp. znak");
        otherIdNameMap.put("ZP2015_OTHERID_OLDID", "nepl. inv. č.");
        otherIdNameMap.put("ZP2015_OTHERID_INVALID_UNITID", "neplatné ukládací číslo");
        otherIdNameMap.put("ZP2015_OTHERID_INVALID_REFNO", "nepl. ref. ozn.");
        otherIdNameMap.put("ZP2015_OTHERID_PRINTID", "poř. č.");
        otherIdNameMap.put("ZP2015_OTHERID_PICID", "nakl. č.");
        otherIdNameMap.put("ZP2015_OTHERID_NEGID", "č. neg.");
        otherIdNameMap.put("ZP2015_OTHERID_CDID", "č. prod.");
        otherIdNameMap.put("ZP2015_OTHERID_ISBN", "ISBN");
        otherIdNameMap.put("ZP2015_OTHERID_ISSN", "ISSN");
        otherIdNameMap.put("ZP2015_OTHERID_ISMN", "ISMN");
        otherIdNameMap.put("ZP2015_OTHERID_MATRIXID", "matriční číslo");
    }

    public static Map<String, String> otherIdIndexMap = new HashMap<>();
    static {
        otherIdIndexMap.put("ZP2015_OTHERID_SIG_ORIG", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_SIG", "OTHER_ID_PROC_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_STORAGE_ID", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_CJ", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_DOCID", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_FORMAL_DOCID", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_ADDID", "OTHER_ID_PROC_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_OLDSIG", "OTHER_ID_PROC_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_OLDSIG2", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_OLDID", "OTHER_ID_PROC_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_INVALID_UNITID", "OTHER_ID_PROC_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_INVALID_REFNO", "UNIT_ID_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_PRINTID", "OTHER_ID_PROC_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_PICID", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_NEGID", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_CDID", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_ISBN", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_ISSN", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_ISMN", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_MATRIXID", "OTHER_ID_ORIG_INDEX");
    }

    public static Map<String, String> relEntityMap = new HashMap<>();
    static {
        relEntityMap.put("RT_AUTHOR", "RT_AUTHOR");
        relEntityMap.put("RT_AUTHOROFCHANGE", "RT_AUTHOROFCHANGE");
        relEntityMap.put("RT_GRANDMOTHER", "RT_GRANDMOTHER");
        relEntityMap.put("RT_BROTHER", "RT_BROTHER");
        relEntityMap.put("RT_HECOUSIN", "RT_HECOUSIN");
        relEntityMap.put("RT_WHOLE", "RT_WHOLE");
        relEntityMap.put("RT_CEREMONY", "RT_CEREMONY");
        relEntityMap.put("RT_MEMBERORG", "RT_MEMBERORG");
        relEntityMap.put("RT_RELATIONS", "RT_RELATIONS");
        relEntityMap.put("RT_GRANDFATHER", "RT_GRANDFATHER");
        relEntityMap.put("RT_DOCUMENT", "RT_DOCUMENT");
        relEntityMap.put("RT_ENTITYEND", "RT_ENTITYEND");
        relEntityMap.put("RT_ENTITYBIRTH", "RT_ENTITYBIRTH");
        relEntityMap.put("RT_ENTITYDEATH", "RT_ENTITYDEATH");
        relEntityMap.put("RT_ENTITYRISE", "RT_ENTITYRISE");
        relEntityMap.put("RT_ENTITYORIGIN", "RT_ENTITYORIGIN");
        relEntityMap.put("RT_ENTITYEXTINCTION", "RT_ENTITYEXTINCTION");
        relEntityMap.put("RT_FUNCTION", "RT_FUNCTION");
        relEntityMap.put("RT_GEOSCOPE", "RT_GEOSCOPE");
        relEntityMap.put("RT_MILESTONE", "RT_MILESTONE");
        relEntityMap.put("RT_ISPART", "RT_ISPART");
        relEntityMap.put("RT_ISMEMBER", "RT_ISMEMBER");
        relEntityMap.put("RT_GENUSMEMBER", "RT_GENUSMEMBER");
        relEntityMap.put("RT_OTHERNAME", "RT_OTHERNAME");
        relEntityMap.put("RT_ANCESTOR", "RT_ANCESTOR");
        relEntityMap.put("RT_ACTIVITYCORP", "RT_ACTIVITYCORP");
        relEntityMap.put("RT_LIQUIDATOR", "RT_LIQUIDATOR");
        relEntityMap.put("RT_OWNER", "RT_OWNER");
        relEntityMap.put("RT_HOLDER", "RT_HOLDER");
        relEntityMap.put("RT_HUSBAND", "RT_HUSBAND");
        relEntityMap.put("RT_WIFE", "RT_WIFE");
        relEntityMap.put("RT_MOTHER", "RT_MOTHER");
        relEntityMap.put("RT_PLACE", "RT_PLACE");
        relEntityMap.put("RT_VENUE", "RT_VENUE");
        relEntityMap.put("RT_PLACEEND", "RT_PLACEEND");
        relEntityMap.put("RT_PLACESTART", "RT_PLACESTART");
        relEntityMap.put("RT_SUPCORP", "RT_SUPCORP");
        relEntityMap.put("RT_SUPTERM", "RT_SUPTERM");
        relEntityMap.put("RT_SENIOR", "RT_SENIOR");
        relEntityMap.put("RT_SUCCESSOR", "RT_SUCCESSOR");
        relEntityMap.put("RT_ACTIVITYFIELD", "RT_ACTIVITYFIELD");
        relEntityMap.put("RT_STUDYFIELD", "RT_STUDYFIELD");
        relEntityMap.put("RT_AWARD", "RT_AWARD");
        relEntityMap.put("RT_ORGANIZER", "RT_ORGANIZER");
        relEntityMap.put("RT_FATHER", "RT_FATHER");
        relEntityMap.put("RT_PARTNER", "RT_PARTNER");
        relEntityMap.put("RT_SHEPARTNER", "RT_SHEPARTNER");
        relEntityMap.put("RT_SUBTERM", "RT_SUBTERM");
        relEntityMap.put("RT_NAMEDAFTER", "RT_NAMEDAFTER");
        relEntityMap.put("RT_LASTKMEMBER", "RT_LASTKMEMBER");
        relEntityMap.put("RT_FIRSTKMEMBER", "RT_FIRSTKMEMBER");
        relEntityMap.put("RT_PREDECESSOR", "RT_PREDECESSOR");
        relEntityMap.put("RT_AFFILIATION", "RT_AFFILIATION");
        relEntityMap.put("RT_SISTER", "RT_SISTER");
        relEntityMap.put("RT_SHECOUSIN", "RT_SHECOUSIN");
        relEntityMap.put("RT_RESIDENCE", "RT_RESIDENCE");
        relEntityMap.put("RT_COMPEVENT", "RT_COMPEVENT");
        relEntityMap.put("RT_RELATED", "RT_RELATED");
        relEntityMap.put("RT_RELATEDTERM", "RT_RELATEDTERM");
        relEntityMap.put("RT_COLLABORATOR", "RT_COLLABORATOR");
        relEntityMap.put("RT_SCHOOLMATE", "RT_SCHOOLMATE");
        relEntityMap.put("RT_UNCLE", "RT_UNCLE");
        relEntityMap.put("RT_SCHOOL", "RT_SCHOOL");
        relEntityMap.put("RT_BROTHERINLAW", "RT_BROTHERINLAW");
        relEntityMap.put("RT_SISTERINLAW", "RT_SISTERINLAW");
        relEntityMap.put("RT_CATEGORY", "RT_CATEGORY");
        relEntityMap.put("RT_THEME", "RT_THEME");
        relEntityMap.put("RT_AUNT", "RT_AUNT");
        relEntityMap.put("RT_FATHERINLAW", "RT_FATHERINLAW");
        relEntityMap.put("RT_MOTHERINLAW", "RT_MOTHERINLAW");
        relEntityMap.put("RT_TEACHER", "RT_TEACHER");
        relEntityMap.put("RT_GRANTAUTH", "RT_GRANTAUTH");
        relEntityMap.put("RT_STORAGE", "RT_STORAGE");
        relEntityMap.put("RT_LOCATION", "RT_LOCATION");
        relEntityMap.put("RT_OBJECT", "RT_OBJECT");
        relEntityMap.put("RT_ACTIVITIES", "RT_ACTIVITIES");
        relEntityMap.put("RT_FOUNDER", "RT_FOUNDER");
        relEntityMap.put("RT_EMPLOYER", "RT_EMPLOYER");
        relEntityMap.put("RT_ROOF", "RT_ROOF");
        relEntityMap.put("RT_REPRESENT", "RT_REPRESENT");
        relEntityMap.put("RT_NAMECHANGE", "RT_NAMECHANGE");
        relEntityMap.put("RT_MENTION", "RT_MENTION");
        relEntityMap.put("RT_ORIGINATOR", "RT_ORIGINATOR");
        relEntityMap.put("RT_GEOPARTNER", "RT_GEOPARTNER");
    }
    
    public static Map<String, String> APPLIED_ACCESS_RESTRICT_MAP = new HashMap<>();
    static {
        APPLIED_ACCESS_RESTRICT_MAP.put("ZP2015_APPLIED_RESTRICTION_DAO","Digitální archivní objekt není přístupný k nahlížení.");
        APPLIED_ACCESS_RESTRICT_MAP.put("ZP2015_ZP2015_APPLIED_RESTRICTION_DAO_INPERS_ONLY","Digitální archivní objekt je přístupný k nahlížení pouze v prostorách badatelny.");
        APPLIED_ACCESS_RESTRICT_MAP.put("ZP2015_APPLIED_RESTRICTION_ARCHMAT","Originál archiválie není přístupný k nahlížení. K nahlížení je přístupná pouze kopie archiválie.");
        APPLIED_ACCESS_RESTRICT_MAP.put("ZP2015_APPLIED_RESTRICTION_ARCHMAT2","Archiválie a její kopie v analogové podobě nejsou přístupné k nahlížení.");
        APPLIED_ACCESS_RESTRICT_MAP.put("ZP2015_APPLIED_RESTRICTION_ABSTRACT","Obsah nezveřejněn z důvodu omezení přístupnosti.");
        APPLIED_ACCESS_RESTRICT_MAP.put("ZP2015_APPLIED_RESTRICTION_LIMITED","Uplatněno omezení přístupnosti - zobrazený archivní popis není úplný.");
        APPLIED_ACCESS_RESTRICT_MAP.put("ZP2015_APPLIED_RESTRICTION_ARCHENTITY","Uplatněno omezení přístupnosti - zobrazený archivní popis není úplný.");
    }

}
