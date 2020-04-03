/**
 * 
 */
package hec.data.cwmsRating;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import hec.data.RatingException;
import hec.data.Units;
import hec.data.cwmsRating.io.IndependentValuesContainer;
import hec.heclib.util.HecTime;
import hec.io.Conversion;
import hec.io.TimeSeriesContainer;
import hec.io.TimeSeriesContainerAligner;

/**
 * @author Q0hecmdp
 *
 */
public class RatingUtil {
	
	public static XMLOutputter outputter = null;
	public static SAXBuilder saxBuilder = null;

	/**
	 * Generates an IndependentValuesContainer object from one or more TimeSeriesContainer objects
	 * @param tscs The TimeSeriesContainer independent parameter object(s)
	 * @param units The units that the IndependentValuesContainer values are to be generated in
	 * @param tz The time zone to use when converting times
	 * @param allowUnsafe Flag specifying whether to allow risky operations
	 * @param warnUnsafe Flag specifying whether to output messages about risky operations
	 * @return An IndependentValuesContainer object.
	 * @throws RatingException
	 */
	public static IndependentValuesContainer tscsToIvc(
			TimeSeriesContainer[] tscs, 
			String[] units,
			TimeZone tz,
			boolean allowUnsafe,
			boolean warnUnsafe) throws RatingException {
		IndependentValuesContainer ivc = new IndependentValuesContainer();
		int indParamCount = tscs.length;
		try {
			List<Integer> commonTimes = new Vector<Integer>();
			List<double[]> indVals = new Vector<double[]>();
			TimeSeriesContainerAligner tsca = new TimeSeriesContainerAligner(tscs);
			while (tsca.hasCurrent()) {
				if (tsca.getAlignedCount() == indParamCount) {
					commonTimes.add(tsca.getTime());
					double[] v = new double[indParamCount];
					for (int i = 0; i < indParamCount; ++i) {
						if (!tscs[i].units.equals(units[i])) {
							v[i] = Units.convertUnits(tsca.getValue(i), tscs[i].units, units[i]);
						}
						else {
							v[i] = tsca.getValue(i);
						}
					}
					indVals.add(v);
				}
				tsca.alignNext();
			}
			if (commonTimes.size() == 0) {
				throw new RatingException("No common times in TimeSeriesContainers.");
			}
			
			long[] valTimes = new long[commonTimes.size()];
			if (tz == null) {
				for (int i = 0; i < valTimes.length; ++i) {
					valTimes[i] = Conversion.toMillis(commonTimes.get(i));
				}
			}
			else {
				Calendar cal = Calendar.getInstance();
				cal.setTimeZone(tz);
				SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyyyy, HH:mm");
				HecTime t = new HecTime();
				for (int i = 0; i < valTimes.length; ++i) {
					t.set(commonTimes.get(i));
					cal.setTime(sdf.parse((t.dateAndTime(4))));
					valTimes[i] = cal.getTimeInMillis();
				}
			}
			ivc.valTimes = valTimes;
			ivc.indVals = new double[tscs.length][valTimes.length];
			for (int i = 0; i < ivc.indVals.length; ++i) {
				for (int j = 0; j < ivc.indVals[0].length; ++j) {
					ivc.indVals[i][j] = indVals.get(j)[i];
				}
			}
			return ivc;
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException) t;
			throw new RatingException(t);
		}
	}
	
	public static String jdomElementToText(Element element) {
		if (outputter == null) outputter = new XMLOutputter();
		synchronized(outputter) {
			return outputter.outputString(element);
		}
	}
	
	public static Element textToJdomElement(String text) throws RatingException {
		if (saxBuilder == null) saxBuilder = new SAXBuilder();
		try {
			synchronized(saxBuilder) {
				return saxBuilder.build(new StringReader(text)).getRootElement();
			}
		}
		catch (Exception e) {
			throw new RatingException(e);
		}
	}
}
