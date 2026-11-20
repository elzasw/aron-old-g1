package cz.aron.transfagent.peva;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import cz.aron.apux._2020.ItemDateRange;
import cz.aron.peva2.wsdl.UniversalTimeRange;

public class Peva2UtilsTest {

	@Test
	public void testCorrectLineSeparators() {		
		assertEquals(Peva2Utils.correctLineSeparators("\rahoj"),"\r\nahoj");
		assertEquals(Peva2Utils.correctLineSeparators("ah\roj"),"ah\r\noj");
		assertEquals(Peva2Utils.correctLineSeparators("\rahoj\r"),"\r\nahoj\r\n");
		assertEquals(Peva2Utils.correctLineSeparators("\r\nahoj\r\n"),"\r\nahoj\r\n");
	}
	
	@Test
	public void testParseDating() {		
		compareIdr(Peva2Utils.parseDating("1.1.2000", "type"),"2000-01-01T00:00:00","2000-01-01T23:59:59","D-D");				
		compareIdr(Peva2Utils.parseDating("1928", "type"),"1928-01-01T00:00:00","1928-12-31T23:59:59","Y-Y");
		compareIdr(Peva2Utils.parseDating("1.2.1996", "type"),"1996-02-01T00:00:00","1996-02-01T23:59:59","D-D");
		compareIdr(Peva2Utils.parseDating("1990/2000", "type"),"1990-01-01T00:00:00","2000-12-31T23:59:59","Y-Y");
		compareIdr(Peva2Utils.parseDating("1.1.1990/2000", "type"),"1990-01-01T00:00:00","2000-12-31T23:59:59","D-Y");
		compareIdr(Peva2Utils.parseDating("1990/1.1.2000", "type"),"1990-01-01T00:00:00","2000-01-01T23:59:59","Y-D");
		compareIdr(Peva2Utils.parseDating("1.1.1990/1.1.2000", "type"),"1990-01-01T00:00:00","2000-01-01T23:59:59","D-D");
	}
	
	@Test
	public void testCreateItemDateRange() {		
		compareIdr(Peva2Utils.createItemDateRange(createUniversalTimeRange("1900","1901")),"1900-01-01T00:00:00","1901-12-31T23:59:59","Y-Y");
		compareIdr(Peva2Utils.createItemDateRange(createUniversalTimeRange("26.4.1740","26.4.1740")),"1740-04-26T00:00:00","1740-04-26T23:59:59","D-D");
		compareIdr(Peva2Utils.createItemDateRange(createUniversalTimeRange("10. st.","10. st.")),"0901-01-01T00:00:00","1000-12-31T23:59:59","C-C");
		compareIdr(Peva2Utils.createItemDateRange(createUniversalTimeRange("1841","31.12.1945-31.12.1945")),"1841-01-01T00:00:00","1945-12-31T23:59:59","Y-D");
		compareIdr(Peva2Utils.createItemDateRange(createUniversalTimeRange("1574","1979-2000")),"1574-01-01T00:00:00","2000-12-31T23:59:59","Y-Y");
		compareIdr(Peva2Utils.createItemDateRange(createUniversalTimeRange("1799/1800","1799/1800")),"1799-01-01T00:00:00","1800-12-31T23:59:59","Y-Y");
		compareIdr(Peva2Utils.createItemDateRange(createUniversalTimeRange("1574","1979/2000")),"1574-01-01T00:00:00","2000-12-31T23:59:59","Y-Y");
		compareIdr(Peva2Utils.createItemDateRange(createUniversalTimeRange("1.1.1799/1800","1841")),"1799-01-01T00:00:00","1841-12-31T23:59:59","D-Y");
		compareIdr(Peva2Utils.createItemDateRange(createUniversalTimeRange("1799-1800","1900")),"1799-01-01T00:00:00","1900-12-31T23:59:59","Y-Y");
	}
	
	@Test
	public void testAsString() {
		assertEquals(Peva2Utils.getAsString(createUniversalTimeRange("1900",null)),"1900");
		assertEquals(Peva2Utils.getAsString(createUniversalTimeRange("1900","1900")),"1900");
		assertEquals(Peva2Utils.getAsString(createUniversalTimeRange("1900","1901")),"1900–1901");
		assertEquals(Peva2Utils.getAsString(createUniversalTimeRange("26.4.1740",null)),"26.4.1740");
		assertEquals(Peva2Utils.getAsString(createUniversalTimeRange("26.4.1740","26.4.1740")),"26.4.1740");
		assertEquals(Peva2Utils.getAsString(createUniversalTimeRange("26.4.1740","27.4.1740")),"26.4.1740 – 27.4.1740");
		
		assertEquals(Peva2Utils.getAsString(createUniversalTimeRange("1740","27.4.1740")),"1740 – 27.4.1740");
		
		assertEquals(Peva2Utils.getAsString(createUniversalTimeRange("1900","1901","1800","2000")),"(1800) 1900–1901 (2000)");
	}
	
	private UniversalTimeRange createUniversalTimeRange(String from, String to) {		
		var utRange = new UniversalTimeRange();
		utRange.setTimeRangeFrom(from);
		utRange.setTimeRangeTo(to);
		return utRange;				
	}
	
	private UniversalTimeRange createUniversalTimeRange(String from, String to, String prior, String posterior) {		
		var utRange = new UniversalTimeRange();
		utRange.setTimeRangeFrom(from);
		utRange.setTimeRangeTo(to);
		utRange.setPrior(prior);
		utRange.setPosterior(posterior);
		return utRange;				
	}
	
	private void compareIdr(ItemDateRange idr, String f, String t, String fmt) {
		assertEquals(idr.getF(), f);
		assertEquals(idr.getTo(), t);
		assertEquals(idr.getFmt(), fmt);
	}
	
}
