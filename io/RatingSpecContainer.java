package hec.data.cwmsRating.io;

import java.util.Arrays;

/**
 * Container class for RatingSpec objects
 *
 * @author Mike Perryman
 */
public class RatingSpecContainer extends RatingTemplateContainer {
	/**
	 * The rating specification identifier
	 */
	public String specId = null;
	/**
	 * The office that owns the rating
	 */
	public String officeId = null;
	/**
	 * The location portion of the specification identifier
	 */
	public String locationId = null;
	/**
	 * The specification version portio of the specification identifier
	 */
	public String specVersion = null;
	/**
	 * The agency that maintains the identified rating
	 */
	public String sourceAgencyId = null;
	/**
	 * The rating method for handling dates that lie between effective dates of the ratings
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String inRangeMethod = null;
	/**
	 * The rating method for handling dates that are earlier than the earliest effective date of the ratings
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String outRangeLowMethod = null;
	/**
	 * The rating method for handling dates that are later than the latest effective date of the ratings
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String outRangeHighMethod = null;
	/**
	 * Specifies whether ratings under this specification are active
	 */
	public boolean active = false;
	/**
	 * Specifies whether to automatically update ratings under this specification when new ratings are available
	 */
	public boolean autoUpdate = false;
	/**
	 * Specifies whether to automatically mark ratings under this specification as active when auto-updated
	 */
	public boolean autoActivate = false;
	/**
	 * Specifies whether to automatically apply any rating extensions for ratings under this specification when auto-updated
	 */
	public boolean autoMigrateExtensions = false;
	/**
	 * Usgs-style rounding specifications, one for each independent parameter.
	 * @see hecjavadev.hec.data.UsgsRounder.java
	 */
	public String[] indRoundingSpecs = null;
	/**
	 * Usgs-style rounding specification for the independent parameter.
	 * @see hecjavadev.hec.data.UsgsRounder.java
	 */
	public String depRoundingSpec = null;
	/**
	 * The description of this rating specification
	 */
	public String specDescription = null;

	/**
	 * Copies the data from this object into the specified RatingSpecContainer
	 * @param other The RatingSpecContainer object to receive the copy
	 */
	public void clone(RatingSpecContainer other) {
		super.clone(other);
		other.specId = specId;
		other.officeId = officeId;
		other.locationId = locationId;
		other.specVersion = specVersion;
		other.sourceAgencyId = sourceAgencyId;
		other.inRangeMethod = inRangeMethod;
		other.outRangeLowMethod = outRangeLowMethod;
		other.outRangeHighMethod = outRangeHighMethod;
		other.active = active;
		other.autoUpdate = autoUpdate;
		other.autoActivate = autoActivate;
		other.autoMigrateExtensions = autoMigrateExtensions;
		other.indRoundingSpecs = Arrays.copyOf(indRoundingSpecs, indRoundingSpecs.length);
		other.depRoundingSpec = depRoundingSpec;
		other.specDescription = specDescription;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		try {
			return String.format("%s/%s", officeId, specId);
		}
		catch (Throwable t) {
			return ((Object)this).toString();
		}
	}

}
