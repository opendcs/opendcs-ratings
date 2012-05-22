package hec.data.cwmsRating;

import static hec.lang.Const.*;

import java.util.Arrays;
import java.util.Map.Entry;

/**
 * RatingSet type used specifically for shifts to UsgsStreamTableRating objects
 * @author Mike Perryman
 */
public class UsgsStageShiftsRatingSet extends RatingSet {
	/**
	 * The effective date of the base rating to which these shifts apply
	 */
	protected long baseRatingEffectiveDate = UNDEFINED_LONG;
	/**
	 * Public Constructor
	 * @param baseRatingEffectiveDate The effective date of the base rating
	 * @param shifts The shifts
	 * @throws RatingException
	 */
	public UsgsStageShiftsRatingSet(long baseRatingEffectiveDate, TableRating[] shifts)
			throws RatingException {
		super(null, shifts);
		this.baseRatingEffectiveDate = baseRatingEffectiveDate;
		if (baseRatingEffectiveDate > shifts[0].getEffectiveDate()) {
			throw new RatingException("Shift effective date is earlier than base rating effective date.");
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingSet#rate(double[][], long[])
	 */
	@Override
	public double[] rate(double[][] valueSets, long[] valueTimes)
			throws RatingException {
		
		if (valueSets.length != valueTimes.length) {
			throw new RatingException("Values and times have different lengths");
		}
		for (int i = 0; i < valueSets.length; ++i) {
			if (valueSets[i].length != 1) {
				throw new RatingException("Only a single independent parameter value is allowed.");
			}
		}
		double[] shifts = new double[valueSets.length];
		if (activeRatings.size() == 0) {
			Arrays.fill(shifts, 0);
		}
		else {
			for (int i = 0; i < valueTimes.length; ++i) {
				if (valueTimes[i] < baseRatingEffectiveDate) {
					shifts[i] = 0;
				}
				else if (valueTimes[i] > activeRatings.lastKey()) {
					shifts[i] = activeRatings.lastEntry().getValue().rate(valueSets[i][0]);
				}
				else {
					Entry<Long, AbstractRating> loEntry = activeRatings.floorEntry(valueTimes[i]);
					Entry<Long, AbstractRating> hiEntry = activeRatings.ceilingEntry(valueTimes[i]);
					if (hiEntry == null) {
						shifts[i] = activeRatings.lastEntry().getValue().rate(valueSets[i][0]);
					}
					else {
						if (loEntry != null && loEntry.equals(hiEntry)) {
							shifts[i] = loEntry.getValue().rate(valueSets[i][0]);
						}
						else {
							double x1, y1, x2, y2;
							if (loEntry == null) {
								x1 = (double)baseRatingEffectiveDate;
								y1 = 0.;
							}
							else {
								x1 = (double)loEntry.getKey();
								y1 = loEntry.getValue().rate(valueSets[i][0]);
							}
							x2 = (double)hiEntry.getKey();
							y2 = hiEntry.getValue().rate(valueSets[i][0]);
							shifts[i] = y1 + ((double)valueTimes[i] - x1) / (x2 - x1) * (y2 - y1); 
						}
					}
				}
			}
		}
		return shifts;
	}

	
	
}
