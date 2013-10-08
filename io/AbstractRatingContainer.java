package hec.data.cwmsRating.io;

import static hec.lang.Const.UNDEFINED_TIME;

/**
 * Container for AbstractRating data
 *
 * @author Mike Perryman
 */
public class AbstractRatingContainer {
	/**
	 * Office that owns the rating
	 */
	public String officeId = null;
	/**
	 * CWMS-style rating specification identifier
	 */
	public String ratingSpecId = null;
	/**
	 * CWMS-style rating units identifier
	 */
	public String unitsId = null;
	/**
	 * Flag specifying whether the rating is marked as active
	 */
	public boolean active = false;
	/**
	 * The earliest date/time that the rating is in effect
	 */
	public long effectiveDateMillis = UNDEFINED_TIME;
	/**
	 * The date/time that the rating was loaded into the database
	 */
	public long createDateMillis = UNDEFINED_TIME;
	/**
	 * Text description of the rating
	 */
	public String description = null;
	/**
	 * Fills another AbstractRatingContainer object with information from this one
	 * @param other The AbstractRatingContainer object to fill
	 */
	public void clone(AbstractRatingContainer other) {
		other.officeId = officeId;
		other.ratingSpecId = ratingSpecId;
		other.unitsId = unitsId;
		other.active = active;
		other.effectiveDateMillis = effectiveDateMillis;
		other.createDateMillis = createDateMillis;
		other.description = description;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		try {
			return String.format("%s/%s", officeId, ratingSpecId);
		}
		catch (Throwable t) {
			return super.toString();
		}
	}
	
	/**
	 * Intended to be overriden to allow sub-classes to return empty instances for cloning.
	 * @return
	 */
	public AbstractRatingContainer getInstance()
	{
		return new AbstractRatingContainer();
	}
}
