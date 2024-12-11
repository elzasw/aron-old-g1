package cz.aron.transfagent.peva;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.apux._2020.Part;
import cz.aron.peva2.wsdl.UniversalTimeRange;
import cz.aron.transfagent.transformation.CoreTypes;

public class Peva2Utils {
	
	private static final String EXP_CENTURY = "(\\d+)((st)|(\\.[ ]?st\\.))";
	 
	private static final Pattern CENTURY_PATTERN = Pattern.compile(EXP_CENTURY);
	
	public static void fillDateRange(UniversalTimeRange timeRange, Part partFundInfo, ApuSourceBuilder apusBuilder) {    	
    	String timeRangeFrom = timeRange.getTimeRangeFrom();
    	String timeRangeTo = timeRange.getTimeRangeTo();
    	
    	String fromFormat = "Y";
    	String toFormat = "Y";
    	
    	var itemDateRange = new ItemDateRange();    	
    	if (timeRangeFrom!=null) {
    		var fromDate = getDate(timeRangeFrom);
    		if (isCentury(fromDate)) {
    			fromFormat = "C";
				itemDateRange.setF(getCentury(fromDate, true));
    		} else {
    			itemDateRange.setF(fromDate);
    		}
        	itemDateRange.setFe(isEstimate(timeRangeFrom));    		
    	}    	
    	if (timeRangeTo!=null) {
    		var toDate = getDate(timeRangeTo);
    		if (isCentury(toDate)) {
    			toFormat = "C";
				itemDateRange.setTo(getCentury(toDate, false));
    		} else {
    			itemDateRange.setTo(toDate);
    		}
        	itemDateRange.setToe(isEstimate(timeRangeTo));    		
    	}    	
		itemDateRange.setFmt(fromFormat + "-" + toFormat);
    	itemDateRange.setType(CoreTypes.UNIT_DATE);
    	itemDateRange.setVisible(true);    	
    	ApuSourceBuilder.addDateRange(partFundInfo, itemDateRange);
    }
    
    public static String getDate(String date) {
    	if (isEstimate(date)) {
    		return date.substring(1,date.length()-1);
    	} else {
    		return date;
    	}
    }
    
    public static String getCentury(String date, boolean start) {
		Matcher matcher = CENTURY_PATTERN.matcher(date);    		
        if (matcher.find()) {
            int c = Integer.parseInt(matcher.group(1));
            if (start) {
            	return ""+((c-1)*100+1);	
            } else {
            	return ""+(c*100);
            }            
        } else {
            throw new IllegalStateException("Invalid century format");
        }    		
    }
    
    public static boolean isEstimate(String date) {
    	return date.startsWith("[");
    }
    
    public static boolean isCentury(String date) {
    	return date.endsWith(" st.");
    }

    public static String getAsString(UniversalTimeRange timeRange) {    	
    	StringJoiner sj = new StringJoiner(" ");    	    	
    	if (StringUtils.isNotBlank(timeRange.getPrior())) {
    		sj.add("("+timeRange.getPrior()+")");    		
    	}    	    	    	
    	var timeRangeFrom = timeRange.getTimeRangeFrom();
    	var timeRangeTo = timeRange.getTimeRangeTo();
    	if (timeRangeFrom!=null) {
    		if (timeRangeTo!=null&&!timeRangeFrom.equals(timeRangeTo)) {
    			sj.add(timeRangeFrom+ "-" + timeRangeTo);
    		} else {
    			sj.add(timeRangeFrom);
    		}
    	}
    	if (StringUtils.isNotBlank(timeRange.getPosterior())) {
    		sj.add("("+timeRange.getPosterior()+")");
    	}
    	return sj.toString();
    }
    
    private static Pattern pattern = Pattern.compile("(\\\r[^\n])|(\\\r$)");
    
    /**
     * Opravi samostatne \r na \r\n
     * @param original vstupni retezec
     * @return opraveny retezec
     */
	public static String correctLineSeparators(String original) {
		if (original == null) {
			return null;
		}
		var matcher = pattern.matcher(original);
		if (!matcher.find()) {
			return original;
		}
		int lastIndex = 0;
		StringBuilder output = new StringBuilder();
		do {
			output.append(original, lastIndex, matcher.start()).append("\r\n");
			if (matcher.end()-matcher.start()>1) {
				output.append(original, matcher.end() - 1, matcher.end());
			}
			lastIndex = matcher.end();
		} while (matcher.find());
		if (lastIndex < original.length()) {
			output.append(original, lastIndex, original.length());
		}
		return output.toString();
	}
	
	
	private static final Map<String,String> CAM_CODE_MAP = new HashMap<>();
	
	static {
		CAM_CODE_MAP.put("CRC_BIRTH", CoreTypes.CRC_BIRTH_DATE);
		CAM_CODE_MAP.put("CRC_RISE", CoreTypes.CRC_RISE_DATE);
		CAM_CODE_MAP.put("CRC_BEGINSCOPE", CoreTypes.CRC_BEGINSCOPE_DATE);
		CAM_CODE_MAP.put("CRC_FIRSTMBIRTH", CoreTypes.CRC_FIRSTMBIRTH_DATE);
		CAM_CODE_MAP.put("CRC_FIRSTWMENTION", CoreTypes.CRC_FIRSTWMENTION_DATE);
		CAM_CODE_MAP.put("CRC_ORIGIN", CoreTypes.CRC_ORIGIN_DATE);
		CAM_CODE_MAP.put("CRC_BEGINVALIDNESS", CoreTypes.CRC_BEGINVALIDNESS_DATE);
	}
	
	public static String transformCamCode(String camCode) {
		var converted = CAM_CODE_MAP.get(camCode);
		if (converted!=null) {
			return converted;
		} else {
			return camCode;
		}
	}

	private static Pattern DATE_PATTERN = Pattern.compile("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d+)$");
	private static Pattern DATE_PATTERN_YEAR = Pattern.compile("^\\d+$");
	
	/**
	 * Prevede datum z formatu [dd.mm.]yyyy nebo [dd.mm.]yyyy/[dd.mm.]yyyy na ItemDateRange
	 * @param date
	 * @param type typ nastavovany do vraceneho ItemDateRange
	 * @return ItemDateRange
	 */	
	public static ItemDateRange parseDating(String date, String type) {
		var idr = new ItemDateRange();
		idr.setType(type);
		idr.setVisible(true);
		var splitted = date.split("/");
		var format = new StringBuilder();
		if (splitted.length == 1) {
			if (!fillDate(splitted[0], idr, true, true, format)) {
				return null;
			}
			idr.setFmt(format.toString());
			return idr;
		} else if (splitted.length == 2) {
			if (!fillDate(splitted[0], idr, true, false, format)) {
				return null;
			}
			if (!fillDate(splitted[1], idr, false, true, format)) {
				return null;
			}
			idr.setFmt(format.toString());
			return idr;
		}
		return null;
	}

	private static boolean fillDate(String date, ItemDateRange idr, boolean fillStart, boolean fillEnd, StringBuilder formatBuilder) {
		var localDate = tryParseYearDate(date);
		if (localDate != null) {
			if (fillStart) {
				setStartYear(localDate, idr);
				formatBuilder.append("Y");
			}
			if (fillEnd) {
				setEndYear(localDate, idr);
				formatBuilder.append("-Y");
			}
			return true;
		} else {
			localDate = tryParseYearMonthDayDate(date);
			if (localDate != null) {
				if (fillStart) {
					setStartYearMonthDay(localDate, idr);
					formatBuilder.append("D");
				}
				if (fillEnd) {
					setEndYearMonthDay(localDate, idr);
					formatBuilder.append("-D");
				}
				return true;
			}
		}
		return false;
	}
	
	private static void setStartYear(LocalDate localDate, ItemDateRange idr) {
		idr.setF(localDate.with(TemporalAdjusters.firstDayOfYear()).atTime(0, 0, 0)
				.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		idr.setFe(false);
	}

	private static void setEndYear(LocalDate localDate, ItemDateRange idr) {
		idr.setTo(localDate.with(TemporalAdjusters.lastDayOfYear()).atTime(23, 59, 59).toString());
		idr.setToe(false);
	}	
	
	private static void setStartYearMonthDay(LocalDate localDate, ItemDateRange idr) {
		idr.setF(localDate.atTime(0, 0, 0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		idr.setFe(false);
	}
	
	private static void setEndYearMonthDay(LocalDate localDate, ItemDateRange idr) {
		idr.setTo(localDate.atTime(23, 59, 59).toString());
		idr.setToe(false);
	}
	
	private static LocalDate tryParseYearDate(String date) {
		var matcher = DATE_PATTERN_YEAR.matcher(date);
		if (matcher.matches()) {
			return LocalDate.of(Integer.parseInt(matcher.group(0)), 1, 1);
		}
		return null;
	}
	
	private static LocalDate tryParseYearMonthDayDate(String date) {
		var matcher = DATE_PATTERN.matcher(date);
		if (matcher.matches()) {
			return LocalDate.of(Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(2)),
					Integer.parseInt(matcher.group(1)));
		}
		return null;
	}

}
