package cz.aron.transfagent.elza.convertor;

import static cz.aron.transfagent.elza.convertor.UnitDateConvertorConsts.CENTURY;
import static cz.aron.transfagent.elza.convertor.UnitDateConvertorConsts.DATE;
import static cz.aron.transfagent.elza.convertor.UnitDateConvertorConsts.DATE_TIME;
import static cz.aron.transfagent.elza.convertor.UnitDateConvertorConsts.DEFAULT_INTERVAL_DELIMITER;
import static cz.aron.transfagent.elza.convertor.UnitDateConvertorConsts.ESTIMATED_TEMPLATE;
import static cz.aron.transfagent.elza.convertor.UnitDateConvertorConsts.YEAR;
import static cz.aron.transfagent.elza.convertor.UnitDateConvertorConsts.YEAR_MONTH;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.commons.lang3.BooleanUtils;

import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;

/**
 * Konvertor pro sprváné zobrazování UnitDate podle formátu.
 *
 * @author Martin Šlapa
 * @since 6.11.2015
 */
public class UnitDateConvertor {

    /**
     * Výraz pro detekci stolení
     */
    public static final String EXP_CENTURY = "(\\d+)((st)|(\\.[ ]?st\\.))";

    /**
     * Šablona pro století
     */
    public static final String CENTURY_TEMPLATE = "%d. st.";

    /**
     * Výraz pro rok
     */
    public static final String EXP_YEAR = "(-?\\d{1,4})";

    /**
     * Formát datumu
     */
    public static final String FORMAT_DATE = "d.M.u";

    /**
     * Formát datumu s časem
     */
    public static final String FORMAT_DATE_TIME = "d.M.u H:mm:ss";

    /**
     * Formát datumu s časem
     */
    public static final String FORMAT_DATE_TIME_WITHOUT_SEC = "d.M.u H:mm";

    /**
     * Formát roku s měsícem
     */
    public static final String FORMAT_YEAR_MONTH = "M.u";

    /**
     * Šablona pro interval rok-rok
     */
    public static final String DEFAULT_INTERVAL_DELIMITER_TEMPLATE = "%s-%s";

    /**
     * Šablona pro interval den - den
     */
    public static final String DEFAULT_INTERVAL_DAY_DELIMITER_TEMPLATE = "%s - %s";
    	
	/**
	 * Šablona pro interval rok-rok
	 */
    public static final String INTERVAL_YEAR_YEAR_DELIMITER_TEMPLATE = "%s–%s";
    
	/**
	 * Šablona pro ostatní interval
	 */
    public static final String INTERVAL_DEFAULT_DELIMITER_TEMPLATE = "%s – %s";
        
    /**
     * Oddělovač pro interval, který vyjadřuje odhad
     */
    public static final String ESTIMATE_INTERVAL_DELIMITER = "/";

    /**
     * Šablona pro interval, který vyjadřuje odhad
     */
    public static final String ESTIMATE_INTERVAL_DELIMITER_TEMPLATE = "%s/%s";

    /**
     * Když druhý rok v intervalu je negativní
     */
    public static final String SECOND_YEAR_IS_NEGATIVE = "--";

    /**
     * Suffix př. n. l.
     */
    public static final String PR_N_L = " př. n. l.";

    /**
     * Záporná reprezentace v ISO formátu.
     */
    public static final String BC_ISO = "-";

    private static final DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern(FORMAT_DATE);
    private static final DateTimeFormatter FORMATTER_DATE_TIME = DateTimeFormatter.ofPattern(FORMAT_DATE_TIME);
    private static final DateTimeFormatter FORMATTER_YEAR_MONTH = DateTimeFormatter.ofPattern(FORMAT_YEAR_MONTH);

    /**
     * Detekce, zda-li se jedná o interval
     * Interval existuje, pokud je nalezen oddělovač '/' nebo '-', ale vylučujeme situace:
     * Intervaly:
     *      1900-1912
     *      1900/1912
     *      -7-2
     *      -7/-2
     *      -7--2
     *      -1.3.7--14.10.2
     *      [-10--8]
     * Samostatne:
     *      -12.3.44
     *      -18
     *      [-20]
     *
     * @param input vstupní řetězec
     * @return true - jedná se o interval
     */
    private static boolean isInterval(final String input) {
        if (input.contains(ESTIMATE_INTERVAL_DELIMITER)) {
            return true; // 1900/1902
        }
        String dateString = input;
        if (input.startsWith("-")) {
            dateString = dateString.substring(1); // vyloučit -8
        } else if (input.startsWith("[-")) {
            dateString = dateString.substring(2); // vyloučit [-8]
        }

        return dateString.contains(DEFAULT_INTERVAL_DELIMITER);
    }

    /**
     * Provede konverzi formátu do textové podoby.
     * 
     * @param unitdate
     * @return String
     * 
     *  @deprecated use {@link #convertToString2(DescriptionItemUnitDate)} lepe fomatuje intervaly
     */
    @Deprecated
    public static String convertToString(final DescriptionItemUnitDate unitDate) {

        String format = unitDate.getD().getFmt();

        if (isInterval(format)) {
            return convertInterval(format, unitDate);
        }
        return convertToken(format, unitDate.getD().getF(), unitDate.getD().isFe());
    }
    
	/**
	 * Konverze intervalu.
	 *
	 * @param format   vstupní formát
	 * @param unitdate doplňovaný objekt
	 * @return výsledný řetězec
	 */
    private static String convertInterval(final String format, final DescriptionItemUnitDate unitdate) {

        String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);

        if (data.length != 2) {
            throw new IllegalStateException("Neplatný interval: " + format);
        }

        boolean bothEstimate = BooleanUtils.isTrue(unitdate.getD().isFe() && BooleanUtils.isTrue(unitdate.getD().isToe()));

        String template = bothEstimate? ESTIMATE_INTERVAL_DELIMITER_TEMPLATE : DEFAULT_INTERVAL_DELIMITER_TEMPLATE;  
        String dateFrom = convertToken(data[0], unitdate.getD().getF(), !bothEstimate && unitdate.getD().isToe());
        String dateTo = convertToken(data[1], unitdate.getD().getTo(), !bothEstimate && unitdate.getD().isToe());

        return String.format(template, dateFrom, dateTo);
    }


    
    /**
     * Provede konverzi formátu do textové podoby.
     * 
     * @param unitdate
     * @return String
     */

    public static String convertToString2(final DescriptionItemUnitDate unitDate) {
    	String format = unitDate.getD().getFmt();

        if (isInterval(format)) {
            return convertInterval2(format, unitDate);
        }
        return convertToken(format, unitDate.getD().getF(), unitDate.getD().isFe());
    }
    
	/**
	 * Konverze intervalu.
	 *
	 * @param format   vstupní formát
	 * @param unitdate doplňovaný objekt
	 * @return výsledný řetězec
	 */
    private static String convertInterval2(final String format, final DescriptionItemUnitDate unitdate) {

        String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);

        if (data.length != 2) {
            throw new IllegalStateException("Neplatný interval: " + format);
        }

        String template;
        boolean bothEstimate = BooleanUtils.isTrue(unitdate.getD().isFe() && BooleanUtils.isTrue(unitdate.getD().isToe()));
        if (bothEstimate) {
        	template = ESTIMATE_INTERVAL_DELIMITER_TEMPLATE;
        } else if ("Y-Y".equals(unitdate.getD().getFmt())) {
        	template = INTERVAL_YEAR_YEAR_DELIMITER_TEMPLATE;
        } else {
			template = INTERVAL_DEFAULT_DELIMITER_TEMPLATE;
		}  
        String dateFrom = convertToken(data[0], unitdate.getD().getF(), !bothEstimate && unitdate.getD().isToe());
        String dateTo = convertToken(data[1], unitdate.getD().getTo(), !bothEstimate && unitdate.getD().isToe());
        return String.format(template, dateFrom, dateTo);
    }


    /**
     * Konverze tokenu - výrazu.
     *
     * @param format        vstupní formát
     * @param unitdate      doplňovaný objekt
     * @param first         zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    private static String convertToken(final String format, final String value, final boolean estimated) {

        String result;
        boolean addEstimate = estimated;

        LocalDateTime date;
        try {
            date = LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("Chyba při analýze datum: " + value, e);
        }
        switch (format) {
            case CENTURY:
            	int century = (date.getYear()+99) / 100; 
                result = String.format(CENTURY_TEMPLATE, century);
                break;
            case YEAR:
                result = String.valueOf(date.getYear());
                break;
            case YEAR_MONTH:
                result = moveMinusToDayDate(FORMATTER_YEAR_MONTH.format(date));
                break;
            case DATE:
                result = moveMinusToDayDate(FORMATTER_DATE.format(date));
                break;
            case DATE_TIME:
                result = moveMinusToDayDate(FORMATTER_DATE_TIME.format(date));
                break;
            default:
                throw new IllegalStateException("Neexistující formát: " + format);
        }

        if (addEstimate) {
            result = String.format(ESTIMATED_TEMPLATE, result);
        }

        return result;
    }

    /**
     * Přesunutí znaménka mínus z roku na začátek data 
     * 
     * @param s datum, např 1.2.-1024
     * @return String, např -1.2.1024
     */
    private static String moveMinusToDayDate(final String s) {
        String[] parts = s.split("\\.");
        switch (parts.length) {
        // jen rok
        case 1:
            return s;
        // měsíc a rok
        case 2:
            if (!parts[1].startsWith("-")) {
                return s;
            }
            return String.format("-%s.%s", parts[0], parts[1].substring(1));
        // den, měsíc a rok
        case 3:
            if (!parts[2].startsWith("-")) {
                return s;
            }
            return String.format("-%s.%s.%s", parts[0], parts[1], parts[2].substring(1));  
        default:
            throw new IllegalStateException("Chyba formátu data: " + s);
        }
    }

}
