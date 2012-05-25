package hec.data.cwmsRating;

import static hec.lang.Const.UNDEFINED_DOUBLE;
import static javax.xml.xpath.XPathConstants.NUMBER;
import static javax.xml.xpath.XPathConstants.STRING;
import hec.data.RatingException;
import hec.data.cwmsRating.io.RatingValueContainer;
import hec.data.cwmsRating.io.TableRatingContainer;
import hec.lang.Observable;

import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implements rating values for TableRating objects
 *
 * @author Mike Perryman
 */
public class RatingValue implements Observer {
	/**
	 * Object that provides the Observable-by-composition functionality
	 */
	
	protected Observable observationTarget = null;
	/**
	 * The independent value
	 */
	protected double indValue;
	/**
	 * The dependent value - valid only if "depTable" is null
	 */
	protected double depValue;
	/**
	 * A note about the rating value - valid only if "depTable" is null
	 */
	protected String note = null;
	/**
	 * The dependent rating table if this is a non-leaf node of a multiple independent parameter rating
	 */
	protected TableRating depTable = null;
	/**
	 * Flag specifying whether this RatingValue has a dependent rating tabel
	 */
	protected boolean hasDepTable = false;

	static String format(double val) {
		boolean negative = val < 0;
		if (negative) val = Math.abs(val); 
		String s = String.format("%s", val);
		if (s.endsWith(".0")) {
			s = s.substring(0, s.length()-2);
		}
		else if (s.startsWith("0.")) {
			s = s.substring(1);
		}
		return negative ? "-" + s : s;
	}
	
	/**
	 * Generates a RatingValue object from the &lt;point&gt; XML element  
	 * @param pointNode The point XML element
	 * @return a new RatingValue object
	 * @throws RatingException
	 */
	static RatingValue fromXml(Node pointNode) throws RatingException {
		try {
			double indVal = (Double)RatingConst.indValXpath.evaluate(pointNode, NUMBER);
			double depVal = (Double)RatingConst.depValXpath.evaluate(pointNode, NUMBER);
			String note = (String)RatingConst.noteXpath.evaluate(pointNode, STRING);
			return new RatingValue(indVal, depVal, note); 
		} 
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
	/**
	 * Generates an array RatingValue objects from multiple &lt;point&gt; XML elements  
	 * @param pointNode The point XML elements
	 * @return a new RatingValue array
	 * @throws RatingException
	 */
	static RatingValue[] fromXml(NodeList pointNodes) throws RatingException {
		try {
			int pointCount = pointNodes.getLength();
			if (pointCount == 0) {
				throw new RatingException("No rating points.");
			}
			RatingValue[] rv = new RatingValue[pointCount];
			for (int i = 0; i < pointCount; ++i) {
				rv[i] = fromXml(pointNodes.item(i));
			}
			return rv;
		} 
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
	
	/**
	 * Public Constructor
	 * @param indValue The independent value
	 * @param depValue The dependent value
	 */
	public RatingValue(double indValue, double depValue) {
		this(indValue, depValue, null);
	}
	/**
	 * Public Constructor
	 * @param indValue The independent value
	 * @param depValue The dependent value
	 * @param note A note about the value pair
	 */
	public RatingValue(double indValue, double depValue, String note) {
		this.indValue = indValue;
		this.depValue = depValue;
		if (note != null && note.length() > 0) this.note = note;
		observationTarget = new Observable();
		this.hasDepTable = false;
	}
	/**
	 * Public Constructor
	 * @param indValue The independent value
	 * @param depValues The dependent rating table
	 */
	public RatingValue(double indValue, TableRating depValues) {
		this.indValue = indValue;
		this.depTable = depValues;
		observationTarget = new Observable();
		this.hasDepTable = true;
	}
	/**
	 * Public Constructor from RatingValueContainer object
	 * @param rvc The RatingValueContainer object to initialize from
	 * @throws RatingException
	 */
	public RatingValue(RatingValueContainer rvc) throws RatingException {
		indValue = rvc.indValue;
		if (rvc.depTable == null) {
			depValue = rvc.depValue;
			note = rvc.note;
			depTable = null;
			hasDepTable = false;
		}
		else {
			depValue = UNDEFINED_DOUBLE;
			note = null;
			depTable = new TableRating(rvc.depTable);
			hasDepTable = true;
		}
		observationTarget = new Observable();
	}
	/**
	 * Retrieves the independent value
	 * @return The independent value
	 */
	public double getIndValue() {
		return indValue;
	}
	/**
	 * Sets the independent value
	 * @param indValue The new independent value
	 */
	public void setIndValue(double indValue) {
		this.indValue = indValue;
		observationTarget.setChanged();
		observationTarget.notifyObservers();
	}
	/**
	 * Retrieves the dependent values
	 * @return The dependent value
	 * @throws RatingException if this object has a dependent rating table
	 */
	public double getDepValue() throws RatingException {
		if (hasDepTable) throw new RatingException("RatingPoint has a dependent value set and not a dependent value.");
		return depValue;
	}
	/**
	 * Sets the dependent value
	 * @param depValue The new dependent value
	 */
	public void setDepValue(double depValue) {
		if (depTable != null) depTable.deleteObserver(this);
		depTable = null;
		this.depValue = depValue;
		observationTarget.setChanged();
		observationTarget.notifyObservers();
	}
	/**
	 * Retrieves the note
	 * @return The note for the value pair
	 * @throws RatingException if this object has a dependent rating table
	 */
	public String getNote() throws RatingException {
		if (hasDepTable) throw new RatingException("No note is available because RatingPoint has a dependent table of values.");
		return note;
	}
	/**
	 * Sets the note
	 * @param note The new note
	 * @throws RatingException
	 */
	public void setNote(CharSequence note) throws RatingException {
		if (hasDepTable) throw new RatingException("Cannot set note because RatingPoint has a dependent table of values.");
		this.note = (String)note;
	}
	/**
	 * Retrieves whether the object has a note
	 * @return Whether the object has a note
	 */
	public boolean hasNote() {return note != null;}
	/**
	 * Retrieves whether the object has a dependent value
	 * @return Whether the object has a dependent value
	 */
	public boolean hasDepValue() {return !hasDepTable;}
	/**
	 * Retrieves whether the object has a dependent rating table
	 * @return Whether the object has a dependent rating table
	 */
	public boolean hasDepTable() {return hasDepTable;}
	/**
	 * Retrieves the dependent rating table
	 * @return The dependent rating table
	 * @throws RatingException if this object has a dependent value
	 */
	public TableRating getDepValues() throws RatingException {
		if (!hasDepTable) throw new RatingException("RatingPoint has a single dependent value.");
		return depTable;
	}
	/**
	 * Sets the dependent rating table
	 * @param depTable The new dependent rating table
	 * @throws RatingException
	 */
	public void setDepValues(TableRating depTable) throws RatingException {
		if (depTable == null) throw new RatingException("Cannot set dependent values to a null table.");
		this.depValue = UNDEFINED_DOUBLE;
		this.depTable = depTable;
		this.depTable.addObserver(this);
		observationTarget.setChanged();
		observationTarget.notifyObservers();
	}
	/**
	 * Retrieves a RatingValueContainer for the object
	 * @return The RatingValueContainer 
	 */
	public RatingValueContainer getData() {
		RatingValueContainer rvc = new RatingValueContainer();
		rvc.indValue = indValue;
		rvc.depValue = depValue;
		rvc.note = note;
		rvc.depTable = depTable == null ? null : (TableRatingContainer)depTable.getData();
		return rvc;
	}
	/**
	 * Sets the objects data from a RatingValueContainer
	 * @param rvc The RatingValueContainer to set the object from  
	 * @throws RatingException
	 */
	public void setData(RatingValueContainer rvc) throws RatingException {
		if (depTable != null) depTable.deleteObserver(this);
		indValue = rvc.indValue;
		if (rvc.depTable == null) {
			depValue = rvc.depValue;
			note = rvc.note;
			depTable = null;
			hasDepTable = false;
		}
		else {
			depValue = UNDEFINED_DOUBLE;
			note = null;
			depTable = new TableRating(rvc.depTable);
			hasDepTable = true;
		}
		observationTarget.setChanged();
		observationTarget.notifyObservers();
	}
	/**
	 * Returns an XML representation of this object
	 * @param indent The character(s) to use for each indentation level
	 * @param indentLevel The initial indentation level
	 * @param isExtension Flag specifying whether the value is part of a rating extension
	 * @param indValues A list of "upstream" independent values if this object is part of a dependent rating table
	 * @return
	 * @throws RatingException
	 */
	public String toXmlString(CharSequence indent, int indentLevel, boolean isExtension, List<Double>indValues) throws RatingException {
		StringBuilder sb = new StringBuilder();
		if (hasDepTable) {
			if (indValues == null) indValues = new ArrayList<Double>();
			indValues.add(indValue);
			sb.append(depTable.toXmlString(indent, indentLevel, depTable.values, isExtension, indValues));
		}
		return sb.toString();
	}
	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(java.util.Observable arg0, Object arg1) {
		observationTarget.setChanged();
		observationTarget.notifyObservers();
	}
	/**
	 * Registers an Observer to be notified when this object is modified
	 * @param o The Observer object to be notified
	 * @see java.util.Observer
	 */
	public void addObserver(Observer o) {
		observationTarget.addObserver(o);
	}
	/**
	 * De-registers an Observer from being notified when this object is modified
	 * @param o The Observer object to no longer be notified
	 * @see java.util.Observer
	 */
	public void deleteObserver(Observer o) {
		observationTarget.deleteObserver(o);
	}

}
