package hec.data.cwmsRating.io;

import hec.data.RatingException;
import hec.data.VerticalDatumException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.TableRating;
import hec.io.VerticalDatumContainer;
import hec.util.TextUtil;

import org.jdom.Element;
import org.jdom.output.XMLOutputter;

/**
 * Data container class for TableRating objects
 *
 * @author Mike Perryman
 */
public class TableRatingContainer extends AbstractRatingContainer {
	/**
	 * The rating values
	 */
	public RatingValueContainer[] values = null;
	/**
	 * The extension values
	 */
	public RatingValueContainer[] extensionValues = null;
	/**
	 * The rating method for handling independent parameter values that lie between values in the rating table
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String inRangeMethod = null;
	/**
	 * The rating method for handling independent parameter values that are less than the least values in the rating table
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String outRangeLowMethod = null;
	/**
	 * The rating method for handling independent parameter values that are greater than the greatest values in the rating table
	 * @see hec.data.cwmsRating.RatingConst.RatingMethod
	 */
	public String outRangeHighMethod = null;
	/**
	 * Fills another TableRatingContainer object with information from this one
	 * @param other The TableRatingContainer object to fill
	 */
	@Override
	public void clone(AbstractRatingContainer other) {
		if (!(other instanceof TableRatingContainer)) {
			throw new IllegalArgumentException("Clone-to object must be a TableRatingContainer.");
		}
		super.clone(other);
		TableRatingContainer trc = (TableRatingContainer)other;
		trc.inRangeMethod = inRangeMethod;
		trc.outRangeLowMethod = outRangeLowMethod;
		trc.outRangeHighMethod = outRangeHighMethod;
		if (values == null) {
			trc.values = null;
		}
		else {
			trc.values = new RatingValueContainer[values.length];
			for (int i = 0; i < values.length; ++i) {
				trc.values[i] = new RatingValueContainer();
				values[i].clone(trc.values[i]);
			}
		}
		if (extensionValues == null) {
			trc.extensionValues = null;
		}
		else {
			trc.extensionValues = new RatingValueContainer[extensionValues.length];
			for (int i = 0; i < extensionValues.length; ++i) {
				extensionValues[i].clone(trc.extensionValues[i]);
			}
		}
	}

	@Override
	public AbstractRatingContainer getInstance() {
		return new TableRatingContainer();
	}
	
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#newRating()
	 */
	@Override
	public AbstractRating newRating() throws RatingException {
		TableRating rating = new TableRating(this);
		return rating;
	}

	public static TableRatingContainer fromXml(Element ratingElement) throws RatingException {
		TableRatingContainer trc = new TableRatingContainer();
		try {
			AbstractRatingContainer.fromXml(ratingElement, trc);
		}
		catch (VerticalDatumException e) {
			throw new RatingException(e);
		}
		if (ratingElement.getChildren("rating-points").size() > 0) {
			trc.values = RatingValueContainer.makeContainers(ratingElement, "rating-points");
		}
		if (ratingElement.getChildren("extension-points").size() > 0) {
			trc.extensionValues = RatingValueContainer.makeContainers(ratingElement, "extension-points");
		}
		return trc;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#toXml(java.lang.CharSequence)
	 */
	@Override
	public String toXml(CharSequence indent) {
		return toXml(indent, 0);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#toXml(java.lang.CharSequence, int)
	 */
	@Override
	public String toXml(CharSequence indent, int level) {
		try {
			if (vdc != null && vdc.getCurrentOffset() != 0.) {
				TableRatingContainer _clone = new TableRatingContainer();
				clone(_clone);
				_clone.toNativeVerticalDatum();
				return _clone.toXml(indent, level);
			}
		}
		catch (VerticalDatumException e) {
			throw new RuntimeException(e);
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level; ++i) sb.append(indent);
		String prefix = sb.toString();
		sb.delete(0, sb.length());
		if (level == 0) {
			sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			sb.append("<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n");
			prefix += indent;
		}
		sb.append(super.toXml(prefix, indent, "simple-rating"));
		String pointsPrefix = prefix + indent;
		if (values != null) {
			boolean multiParam = values[0].depTable != null;
			if (!multiParam) sb.append(prefix).append(indent).append("<rating-points>\n");
			for (int i = 0; i < values.length; ++i) {
				values[i].toXml(pointsPrefix, indent, sb);
			}
			if (!multiParam) sb.append(prefix).append(indent).append("</rating-points>\n");
		}
		if (extensionValues != null) {
			boolean multiParam = extensionValues[0].depTable != null;
			if (multiParam) {
				AbstractRating.getLogger().severe("Multiple independent parameter ratings cannot use extension values, ignoring");
			}
			else {
				sb.append(prefix).append(indent).append("<extension-points>\n");
				for (int i = 0; i < extensionValues.length; ++i) {
					extensionValues[i].toXml(pointsPrefix, indent, sb);
				}
				sb.append(prefix).append(indent).append("</extension-points>\n");
			}
		}
		sb.append(prefix).append("</simple-rating>\n");
		if (level == 0) {
			sb.append("</ratings>\n");
		}
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#toNativeVerticalDatum()
	 */
	@Override
	public boolean toNativeVerticalDatum() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		boolean change = false;
		double offset = vdc.getCurrentOffset();
		if (offset != 0.) {
			change = true;
			String[][] paramsAndUnits = getParamsAndUnits();
			String[] params = paramsAndUnits[0];
			String[] units  = paramsAndUnits[1];
			for (int i = 0; i < params.length; ++i) {
				if (params[i].startsWith("Elev")) {
					int paramNum = i == params.length-1 ? -1 : i;
					offset = vdc.getCurrentOffset(units[i]);
					try {
						addOffset(paramNum, -offset);
					}
					catch (RatingException e) {
						throw new VerticalDatumException(e);
					}
				}
			}
			vdc.toNativeVerticalDatum();
		}
		return change;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#toNGVD29()
	 */
	@Override
	public boolean toNGVD29() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		boolean change = false;
		if (!vdc.getCurrentVerticalDatum().equals("NGVD29")) {
			change = true;
			String[][] paramsAndUnits = getParamsAndUnits();
			String[] params = paramsAndUnits[0];
			String[] units  = paramsAndUnits[1];
			for (int i = 0; i < params.length; ++i) {
				if (params[i].startsWith("Elev")) {
					int paramNum = i == params.length-1 ? -1 : i;
					double offset1 = vdc.getNGVD29Offset(units[i]);
					double offset2 = vdc.getCurrentOffset(units[i]);
					try {
						addOffset(paramNum, offset1 - offset2);
					}
					catch (RatingException e) {
						throw new VerticalDatumException(e);
					}
				}
			}
			vdc.toNGVD29();
		}
		return change;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#toNAVD88()
	 */
	@Override
	public boolean toNAVD88() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		boolean change = false;
		if (!vdc.getCurrentVerticalDatum().equals("NAVD88")) {
			change = true;
			String[][] paramsAndUnits = getParamsAndUnits();
			String[] params = paramsAndUnits[0];
			String[] units  = paramsAndUnits[1];
			for (int i = 0; i < params.length; ++i) {
				if (params[i].startsWith("Elev")) {
					int paramNum = i == params.length-1 ? -1 : i;
					double offset1 = vdc.getNAVD88Offset(units[i]);
					double offset2 = vdc.getCurrentOffset(units[i]);
					try {
						addOffset(paramNum, offset1 - offset2);
					}
					catch (RatingException e) {
						throw new VerticalDatumException(e);
					}
				}
			}
			vdc.toNAVD88();
		}
		return change;
	}


	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#addOffset(int, double)
	 */
	@Override
	public void addOffset(int paramNum, double offset) throws RatingException {
		if (values != null) {
			for (int i = 0; i < values.length; ++i) {
				if (paramNum == 0) {
					values[i].indValue += offset;
				}
				else if (paramNum == -1) {
					if (values[i].depTable == null) {
						values[i].depValue += offset;
					}
					else {
						values[i].depTable.addOffset(paramNum, offset);
					}
				}
				else {
					if (values[i].depTable == null) {
						throw new RatingException("Invalid parameter number");
					}
					values[i].depTable.addOffset(paramNum-1, offset);
				}
			}
		}
		if (extensionValues != null) {
			for (int i = 0; i < extensionValues.length; ++i) {
				if (paramNum == 0) {
					extensionValues[i].indValue += offset;
				}
				else if (paramNum == -1) {
					if (extensionValues[i].depTable == null) {
						extensionValues[i].depValue += offset;
					}
					else {
						extensionValues[i].depTable.addOffset(paramNum, offset);
					}
				}
				else {
					if (extensionValues[i].depTable == null) {
						throw new RatingException("Invalid parameter number");
					}
					extensionValues[i].depTable.addOffset(paramNum-1, offset);
				}
			}
		}
	}
}
