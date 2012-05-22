package hec.data.cwmsRating.io;

import java.util.Arrays;
/**
 * Container class for RatingTemplate objects
 *
 * @author Mike Perryman
 */
public class RatingTemplateContainer {
	/**
	 * The text identifier of the rating template
	 */
	public String templateId = null;
	/**
	 * The parameters portion of the template identifier
	 */
	public String parametersId = null;
	/**
	 * The independent parameters of the rating template
	 */
	public String[] indParams = null;
	/**
	 * The dependent parameter of the rating template
	 */
	public String depParam = null;
	/**
	 * The version portion of the template identifier
	 */
	public String templateVersion = null;
	/**
	 * The rating method for handling independent parameter values that lie between values in the rating tables, one for each independent parameter
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String[] inRangeMethods = null;
	/**
	 * The rating method for handling independent parameter values that are less than the least values in the rating tables, one for each independent parameter
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String[] outRangeLowMethods = null;
	/**
	 * The rating method for handling independent parameter values that are greater than the greatest values in the rating tables, one for each independent parameter
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String[] outRangeHighMethods = null;
	/**
	 * The description of the rating template
	 */
	public String templateDescription = null;
	/**
	 * Copies the data from this object into the specified RatingTemplateContainer
	 * @param other The RatingTemplateContainer object to receive the copy
	 */
	public void clone(RatingTemplateContainer other) {
		other.templateId = templateId;
		other.parametersId = parametersId;
		other.indParams = new String[indParams.length];
		for (int i = 0; i < indParams.length; ++i) other.indParams[i] = indParams[i];
		other.depParam = depParam;
		other.templateVersion = templateVersion;
		other.inRangeMethods = Arrays.copyOf(inRangeMethods, inRangeMethods.length);
		other.outRangeLowMethods = Arrays.copyOf(outRangeLowMethods, outRangeLowMethods.length);
		other.outRangeHighMethods = Arrays.copyOf(outRangeHighMethods, outRangeHighMethods.length);
		other.templateDescription = templateDescription;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		try {
			return templateId;
		}
		catch(Throwable t) {
			return super.toString();
		}
	}
}
