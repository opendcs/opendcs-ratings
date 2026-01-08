/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package hec.data.cwmsRating;


import hec.data.Units;
import hec.data.cwmsRating.io.IndependentValuesContainer;
import hec.heclib.util.HecTime;
import hec.io.Conversion;
import hec.io.TimeSeriesContainer;
import hec.io.TimeSeriesContainerAligner;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

final class RatingUtil {

    /**
     * Generates an IndependentValuesContainer object from one or more TimeSeriesContainer objects
     *
     * @param tscs        The TimeSeriesContainer independent parameter object(s)
     * @param units       The units that the IndependentValuesContainer values are to be generated in
     * @param tz          The time zone to use when converting times
     * @param allowUnsafe Flag specifying whether to allow risky operations
     * @param warnUnsafe  Flag specifying whether to output messages about risky operations
     * @return An IndependentValuesContainer object.
     * @throws RatingException
     */
    public static IndependentValuesContainer tscsToIvc(TimeSeriesContainer[] tscs, String[] units, TimeZone tz, boolean allowUnsafe,
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
                        } else {
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
            } else {
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
            ivc.indVals = indVals.toArray(new double[indVals.size()][]);
            return ivc;
        } catch (Throwable t) {
            if (t instanceof RatingException) {
                throw (RatingException) t;
            }
            throw new RatingException(t);
        }
    }
}
