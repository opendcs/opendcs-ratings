package hec.data.cwmsRating.io;

import static hec.lang.Const.UNDEFINED_DOUBLE;
import hec.data.RatingException;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

/**
 * Data container class for RatingValue
 *
 * @author Mike Perryman
 */
public class RatingValueContainer {
	/**
	 * The independent value
	 */
	public double indValue = UNDEFINED_DOUBLE;
	/**
	 * The dependent value - should be UNDEFINED_DOUBLE if depTable is non-null
	 */
	public double depValue = UNDEFINED_DOUBLE;
	/**
	 * The rating value note, if any
	 */
	public String note = null;
	/**
	 * The dependent rating table - should be null if depValue != UNDEFINED_DOUBLE
	 */
	public TableRatingContainer depTable = null;
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		do {
			if (!(obj instanceof RatingValueContainer)) break;
			RatingValueContainer other = (RatingValueContainer)obj;
			if (other.indValue != indValue) break;
			if (other.depValue != depValue) break;
			if ((other.note == null) != (note == null)) break;
			if (note != null) {
				if (!other.note.equals(note)) break;
			}
			if ((other.depTable == null) != (depTable == null)) break;
			if (depTable != null) {
				if (!other.depTable.equals(depTable)) break;
			}
			result = true;
		} while(false);
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode() 
				+ new Double(indValue).hashCode()
				+ new Double(depValue).hashCode()
				+ (note == null ? 0 : note.hashCode())
				+ (depTable == null ? 0 : depTable.hashCode());
	}
	/**
	 * Fills a specified RatingValueContainer object with information from this one
	 * @param other The RatingValueContainer object to fill
	 */
	public void clone(RatingValueContainer other) {
		other.indValue = indValue;
		other.depValue = depValue;
		other.note     = note;
		if (depTable == null) {
			other.depTable = null;
		}
		else {
			other.depTable = new TableRatingContainer();
			depTable.clone(other.depTable);
		}
	}
	/**
	 * Generates RatingValueContainer array from tables of points, notes, and methods
	 * @param points The table of points. This has width of number of independent parameters + 1 and depth of total number dependent values.
	 *               The leftmost column is the 1st independent parameter and changes most slowly with depth.  The rightmost column is the
	 *               dependent parameter and normally changes with each row of depth.
	 * @param notes Notes (depth-only) associated with each dependent value
	 * @param inRangeMethods  The in-range independent interpolation methods (width only). Should be one for each independent parameter
	 * @param outRangeLowMethods  The out-range-low independent interpolation methods (width only). Should be one for each independent parameter
	 * @param outRangeHighMethods  The out-range-high independent interpolation methods (width only). Should be one for each independent parameter
	 * @return The generated RatingValueContainer array
	 */
	public static RatingValueContainer[] makeContainers(
			double[][] points, 
			String[] notes, 
			String[] inRangeMethods,
			String[] outRangeLowMethods,
			String[] outRangeHighMethods) {
		
		return makeContainers(
				points, 
				notes, 
				0, 
				points.length, 
				0, 
				points[0].length, 
				inRangeMethods, 
				outRangeLowMethods, 
				outRangeHighMethods);
	}
	/**
	 * Generates RatingValueContainer array from tables of points, notes, and methods
	 * @param points The table of points. This has width of number of independent parameters + 1 and depth of total number dependent values.
	 *               The leftmost column is the 1st independent parameter and changes most slowly with depth.  The rightmost column is the
	 *               dependent parameter and normally changes with each row of depth.
	 * @param notes Notes (depth-only) associated with each dependent value
	 * @param top   The depth offset (offset from the top) to use as the first row (set to 0 when calling from outside)
	 * @param depth The number of rows to process (set to total depth when calling from outside)
	 * @param left  The width offset (offset from the left) to use as the first column (set to 0 when calling from outside) 
	 * @param width The number of columns to process (set to total width when calling from outside)
	 * @param inRangeMethods  The in-range independent interpolation methods (width only). Should be one for each independent parameter
	 * @param outRangeLowMethods  The out-range-low independent interpolation methods (width only). Should be one for each independent parameter
	 * @param outRangeHighMethods  The out-range-high independent interpolation methods (width only). Should be one for each independent parameter
	 * @return The generated RatingValueContainer array
	 */
	public static RatingValueContainer[] makeContainers(
			double[][] points, 
			String[] notes, 
			int top, 
			int depth, 
			int left, 
			int width,
			String[] inRangeMethods,
			String[] outRangeLowMethods,
			String[] outRangeHighMethods) {
		RatingValueContainer[] rvcs = null;
		if (width == 2) {
			//------------------------//
			// only one ind parameter //
			//------------------------//
			rvcs = new RatingValueContainer[depth];
			for (int i = 0; i < depth; ++i) {
				rvcs[i] = new RatingValueContainer();
				rvcs[i].indValue = points[top+i][left+0];
				rvcs[i].depValue = points[top+i][left+1];
				if (notes != null) rvcs[i].note = notes[top+i];
			}
		}
		else {
			//-----------------------------//
			// more than one ind parameter //
			//-----------------------------//
			//-----------------------------------------------------------------//
			// determine the number of values of the 1st independent parameter //
			//-----------------------------------------------------------------//
			double lastIndValue = points[top][left];
			List<Integer> breakpoints = new ArrayList<Integer>();
			breakpoints.add(0);
			for (int i = 1; i < depth; ++i) {
				if (points[top+i][left] != lastIndValue) {
					breakpoints.add(i);
					lastIndValue = points[top+i][left]; 
				}
			}
			int count = breakpoints.size();
			rvcs = new RatingValueContainer[count];
			for (int i = 0; i < count; ++i) {
				//--------------------------------------------------------------//
				// each value of 1st ind parameter gets its own container array //
				//--------------------------------------------------------------//
				rvcs[i] = new RatingValueContainer();
				rvcs[i].indValue = points[top+breakpoints.get(i)][left];
				rvcs[i].depTable = new TableRatingContainer();
				int sub_depth;
				if (i == count - 1) {
					sub_depth = depth - breakpoints.get(i);
				}
				else {
					sub_depth = breakpoints.get(i+1) - breakpoints.get(i);
				}
				if (inRangeMethods      != null) rvcs[i].depTable.inRangeMethod      = inRangeMethods[left+1];
				if (outRangeLowMethods  != null) rvcs[i].depTable.outRangeLowMethod  = outRangeLowMethods[left+1];
				if (outRangeHighMethods != null) rvcs[i].depTable.outRangeHighMethod = outRangeHighMethods[left+1];
				//--------------------------------------------------------------------------------------//
				// make the container array by recursing on the appropriate subset of the original data //
				//--------------------------------------------------------------------------------------//
				rvcs[i].depTable.values = makeContainers(
					points, 
					notes, 
					top+breakpoints.get(i), 
					sub_depth, 
					left+1, 
					width-1, 
					inRangeMethods,
					outRangeLowMethods,
					outRangeHighMethods);
			}
		}
		return rvcs;
	}
	/**
	 * Generates RatingValueContainer array from an XML node
	 * @param ratingElement The XML rating node as an org.jdom.Element object
	 * @return The generated RatingValueContainer array
	 * @throws RatingException
	 */
	public static RatingValueContainer[] makeContainers(Element ratingElement, String pointsElementsName) throws RatingException {
		
		double[][] points = null;
		String[] notes = null;
		int width = -1; 
		int depth = 0;
		//---------------------------------//
		// determine the shape of the data //
		//---------------------------------//
		List pointsElems = ratingElement.getChildren(pointsElementsName);
		if (pointsElems.size() == 0) return null;
		for (Object pointsObj : pointsElems) {
			Element pointsElem = (Element)pointsObj;
			if (width == -1) {
				width = pointsElem.getChildren("other-ind").size(); // number of ind params - 1
			}
			if (pointsElem.getChildren("other-ind").size() != width) {
				throw new RatingException("Inconsistent number of independent parameters");
			}
			depth += pointsElem.getChildren("point").size();
		}
		width += 2; // now number of all params
		//------------------------------------//
		// parse the XML data into the arrays //
		//------------------------------------//
		points = new double[depth][];
		for (int i = 0; i < depth; ++i) points[i] = new double[width];
		double[] otherInds = new double[width-2];
		int row = 0;
		String note;
		for (Object obj : pointsElems) {
			Element pointsElem = (Element)obj;
			int col = 0;
			for (Object otherIndObj : pointsElem.getChildren("other-ind")) {
				Element otherIndElem = (Element)otherIndObj;
				int pos = Integer.parseInt(otherIndElem.getAttributeValue("position"));
				if (pos != col+1) {
					throw new RatingException(String.format("Expected position %d, got %d on %s", col+1, pos, otherIndElem.toString()));
				}
				otherInds[col++] = Double.parseDouble(otherIndElem.getAttributeValue("value"));
			}
			for (Object pointObj : pointsElem.getChildren("point")) {
				Element pointElem = (Element)pointObj;
				for (col = 0; col < width-2; ++col) points[row][col] = otherInds[col];
				points[row][width-2] = Double.parseDouble(pointElem.getChildTextTrim("ind"));
				points[row][width-1] = Double.parseDouble(pointElem.getChildTextTrim("dep"));
				note = pointElem.getChildTextTrim("note");
				if (note != null) {
					if (notes == null) notes = new String[depth];
					notes[row] = note;
				}
				++row;
			}
		}
		return makeContainers(points, notes, null, null, null);
	}
	public String toXml(CharSequence prefix, CharSequence indent, StringBuilder sb) {
		return toXml(prefix, indent, null, sb);
	}
	
	public String toXml(CharSequence prefix, CharSequence indent, List<Double> otherIndParams, StringBuilder sb) {
		if (depTable == null) {
			sb.append(prefix).append(indent).append("<point>\n");
			sb.append(prefix).append(indent).append(indent).append("<ind>").append(indValue).append("</ind>\n");
			sb.append(prefix).append(indent).append(indent).append("<dep>").append(depValue).append("</dep>\n");
			if (note != null) sb.append(prefix).append(indent).append(indent).append("<note>").append(note).append("</note>\n");
			sb.append(prefix).append(indent).append("</point>\n");
		}
		else {
			if (otherIndParams == null) {
				otherIndParams = new ArrayList<Double>();
			}
			otherIndParams.add(indValue);
			if (depTable.values[0].depTable == null) {
				sb.append(prefix).append("<rating-points>\n");
				for (int i = 0; i < otherIndParams.size(); ++i) {
					sb.append(prefix).append(indent).append("<other-ind position=\"").append(i+1).append("\" value=\"").append(otherIndParams.get(i)).append("\"/>\n");
				}
			}
			for (RatingValueContainer rvc : depTable.values) {
				rvc.toXml(prefix, indent, otherIndParams, sb);
			}
			otherIndParams.remove(otherIndParams.size()-1);
			if (depTable.values[0].depTable == null) {
				sb.append(prefix).append("</rating-points>\n");
			}
		}
		return sb.toString();
	}
			
}
