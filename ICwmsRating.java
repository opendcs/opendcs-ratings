package hec.data.cwmsRating;

import hec.data.IRating;

import java.util.Observer;

/**
 * Interface to be implemented by all CWMS-style rating objects
 *
 * @author Mike Perryman
 */
public interface ICwmsRating extends IRating {

	/**
	 * Registers an Observer object to receive updates when the IRatingCore object changes
	 * @param o The Observer object to receive updates
	 */
	public abstract void addObserver(Observer o);
	/**
	 * Unregisters an Observer object from receiving updates when the IRatingCore object changes
	 * @param o The Observer object to unregister
	 */
	public abstract void deleteObserver(Observer o);
	/**
	 * Retrieves the rating specification identifier.
	 * @return The rating specification identifier
	 */
	public abstract String getRatingSpecId();
	/**
	 * Retrieves the office identifier.
	 * @return The office identifier
	 */
	public abstract String getOfficeId();
	/**
	 * Sets the office identifier.
	 * @param officeId The office identifier
	 */
	public abstract void setOfficeId(String officeId);
	/**
	 * Sets the rating specification identifier.
	 * @param ratingSpecId The rating specification identifier
	 */
	public abstract void setRatingSpecId(String ratingSpecId);
	/**
	 * Retrieves the rating units identifier. These are the units of the underlying rating, which may be different than the
	 * data units, as long as valid unit conversions exist between rating units and data units.
	 * @return The rating units identifier
	 */
	public abstract String getRatingUnitsId();
	/**
	 * Sets the rating units identifier. These are the units of the underlying rating, which may be different than the
	 * data units, as long as valid unit conversions exist between rating units and data units.
	 * @param ratingUnitsId The rating units identifier.
	 */
	public abstract void setRatingUnitsId(String ratingUnitsId);
	/**
	 * Retrieves the data units identifier. These are the units expected for independent parameter(s) and generated for the dependent parameter.
	 * @return The data units identifier
	 */
	public abstract String getDataUnitsId();
	/**
	 * Sets the data units identifier. These are the units expected for independent parameter(s) and generated for the dependent parameter.
	 * @param dataUnitsId The data units identifier.
	 */
	public abstract void setDataUnitsId(String dataUnitsId);
	/**
	 * Retrieves whether the rating is active.
	 * @return Whether the rating is active
	 */
	public abstract boolean isActive();
	/**
	 * Sets whether the rating is active.
	 * @param active Whether the rating is active.
	 */
	public abstract void setActive(boolean active);
	/**
	 * Retrieves the description of the rating.
	 * @return The description of the rating
	 */
	public abstract String getDescription();
	/**
	 * Sets the description of the rating.
	 * @param description The description of the rating
	 */
	public abstract void setDescription(String description);
	/**
	 * Retrieves this rating as an XML string.
	 * @param indent The character(s) used for indentation in the XML string
	 * @param indentLevel The beginning indentation level for the XML string
	 * @return This rating as an XML string
	 */
	public abstract String toXmlString(CharSequence indent, int indentLevel) throws RatingException;

}