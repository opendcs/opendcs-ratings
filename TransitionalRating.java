/**
 * 
 */
package hec.data.cwmsRating;

import static hec.util.TextUtil.replaceAll;

import java.util.ArrayList;
import java.util.Arrays;

import hec.data.RatingException;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.SourceRatingContainer;
import hec.data.cwmsRating.io.TransitionalRatingContainer;
import hec.hecmath.computation.ComputationException;
import hec.hecmath.computation.Condition;
import hec.hecmath.computation.MathExpression;
import hec.hecmath.computation.VariableSet;
import hec.lang.Observable;

/**
 * Rating that selects among multiple possible ratings depending on input parameter values.
 *
 * @author Mike Perryman
 */
public class TransitionalRating extends AbstractRating {
	/**
	 * The selection conditions to evaluate
	 */
	protected Condition[] conditions = null;
	/**
	 * The evaluations - one for each condition plus one for the default condition 
	 */
	protected MathExpression[] evaluations = null;
	/**
	 * Source ratings referenced in conditions and/or evaluations
	 */
	protected SourceRating[] sourceRatings = null;
	/**
	 * Default constructor
	 */
	public TransitionalRating() {
		init();
	}
	/**
	 * Constructor from TransitionalRatingContainer
	 * @param trrc The TransitionalRatingContainer to construct from 
	 * @throws RatingException
	 */
	public TransitionalRating(TransitionalRatingContainer trrc) throws RatingException {
		init();
		setData(trrc);
	}
	/**
	 * Public constructor from XML text
	 * @param xmlText The XML text to initialize from
	 * @throws RatingException
	 */
	public TransitionalRating(String xmlText) throws RatingException {
		setData(new TransitionalRatingContainer(xmlText));
	}
	/**
	 * performs common initialization tasks
	 */
	protected void init() {
		synchronized(this) {
			if (observationTarget == null) observationTarget = new Observable();
		}
	}
	/**
	 * @return the conditions array
	 */
	public Condition[] getConditions() {
		synchronized(this) {
			return conditions == null ? null : Arrays.copyOf(conditions, conditions.length);
		}
	}
	/**
	 * @return the text representation of the conditions suitable for use in ratings XML
	 */
	public String[] getConditionStrings() {
		synchronized(this) {
			String[] conditionStrings = null;
			if (conditions != null) {
				conditionStrings = new String[conditions.length];
				for (int i = 0; i < conditions.length; ++i) {
					conditionStrings[i] = conditions[i].toString().replaceAll("\\$", "");
				}
			}
			return conditionStrings;
		}
	}
	/**
	 * Sets the conditions array
	 * @param conditions
	 */
	public void setConditions(Condition[] conditions) {
		synchronized(this) {
			this.conditions = conditions == null ? null : Arrays.copyOf(conditions, conditions.length);
		}
	}
	/**
	 * Sets the conditions array from text representations (as in ratings XML)
	 * @param conditionStrings
	 * @throws ComputationException 
	 */
	public void setConditions(String[] conditionStrings) throws ComputationException {
		synchronized(this) {
			Condition[] conditions = null;
			if (conditionStrings != null) {
				conditions = new Condition[conditionStrings.length];
				for (int i = 0; i < conditionStrings.length; ++i) {
					conditions[i] = new Condition(replaceAll(conditionStrings[i], "(^|\\W)(i|r)([1-9])", "$1\\$$2$3", "i"));
				}
			}
			this.conditions = conditions;
		}
	}
	/**
	 * @return the evaluations array
	 */
	public MathExpression[] getEvaluations() {
		synchronized(this) {
			return evaluations == null ? null : Arrays.copyOf(evaluations, evaluations.length);
		}
	}
	/**
	 * @return the text representations of the evaluations suitable for use in ratings XML
	 */
	public String[] getEvaluationStrings() {
		synchronized(this) {
			String[] evaluationStrings = null;
			if (evaluations != null) {
				evaluationStrings = new String[evaluations.length];
				for (int i = 0; i < evaluations.length; ++i) {
					evaluationStrings[i] = evaluations[i].toString().replaceAll("\\$", "");
				}
			}
			return evaluationStrings;
		}
	}
	/**
	 * Sets the evaluations array
	 * @param evaluations
	 */
	public void setEvaluations(MathExpression[] evaluations) {
		synchronized(this) {
			this.evaluations = evaluations == null ? null : Arrays.copyOf(evaluations, evaluations.length);
		}
	}
	/**
	 * Sets the evaluations array  from text representations (as in ratings XML)
	 * @param evaluationStrings
	 * @throws ComputationException 
	 */
	public void setEvaluations(String[] evaluationStrings) throws ComputationException {
		synchronized(this) {
			MathExpression[] evaluations = null;
			if (evaluationStrings != null) {
				evaluations = new MathExpression[evaluationStrings.length];
				for (int i = 0; i < evaluationStrings.length; ++i) {
					evaluations[i] = new MathExpression(replaceAll(evaluationStrings[i], "(^|\\W)(i|r)([1-9])", "$1\\$$2$3", "i"));
				}
			}
			this.evaluations = evaluations;
		}
	}
	/**
	 * @return a copy of the source ratings array 
	 */
	public SourceRating[] getSourceRatings() {
		synchronized(this) {
			return sourceRatings == null ? null : Arrays.copyOf(sourceRatings, sourceRatings.length);
		}
	}
	/**
	 * Set the source ratings array
	 * @param sources
	 * @throws RatingException 
	 */
	public void setSourceRatings(SourceRating[] sources) throws RatingException {
		synchronized(this) {
			sourceRatings = Arrays.copyOf(sources, sources.length);
			for (SourceRating sr : sources) {
				sr.addObserver(this);
			}
			findCycles();
		}
	}
	/**
	 * Finds cyclical rating references in source ratings
	 * @throws RatingException if cyclic reference is found
	 */
	public void findCycles() throws RatingException {
		ArrayList<String> specIds = new ArrayList<String>();
		findCycles(specIds);
	}
	/**
	 * Finds cyclical rating references in source ratings
	 * @throws RatingException if cyclic reference is found
	 */
	protected void findCycles(ArrayList<String> specIds) throws RatingException {
		String specId = getOfficeId()+"/"+getRatingSpecId();
		if (specIds.contains(specId)) {
			StringBuilder sb = new StringBuilder("Cycle detected in source ratings. Cycle path is:");
			for (String pathSpecId : specIds) {
				sb.append(pathSpecId.equals(specId) ? "\n -->" : "\n    ").append(pathSpecId);
			}
			sb.append("\n -->").append(specId);
			throw new RatingException(sb.toString());
		}
		specIds.add(specId);
		if (sourceRatings != null) {
			for (SourceRating sr : sourceRatings) {
				if (sr != null) {
					RatingSet rs = sr.getRatingSet();
					if (rs != null) {
						AbstractRating[] ratings = rs.getRatings();
						if (ratings != null) {
							for (AbstractRating ar : ratings) {
								if (ar instanceof VirtualRating) {
									((VirtualRating)ar).findCycles((ArrayList<String>)specIds.clone());
								}
								if (ar instanceof TransitionalRating) {
									((TransitionalRating)ar).findCycles((ArrayList<String>)specIds.clone());
								}
							}
						}
					}
				}
			}
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#toXmlString(java.lang.CharSequence, int)
	 */
	@Override
	public String toXmlString(CharSequence indent, int indentLevel) throws RatingException {
		return getData().toXml(indent, indentLevel);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingExtents(long)
	 */
	@Override
	public double[][] getRatingExtents(long ratingTime) throws RatingException {
		throw new RatingException("getRatingExtents is not supported for transitional ratings");
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(double)
	 */
	@Override
	public double rate(double indVal) throws RatingException {
		return rate(defaultValueTime, indVal);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rateOne(double[])
	 */
	@Override
	public double rateOne(double... indVals) throws RatingException {
		long[] valTimes = new long[] {defaultValueTime};
		double[][] _indVals = new double[indVals.length][];
		for (int i = 0; i < indVals.length; ++i) {
			_indVals[i] = new double[] {indVals[i]};
		}
		return rate(valTimes, _indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rateOne2(double[])
	 */
	@Override
	public double rateOne2(double[] indVals) throws RatingException {
		long[] valTimes = new long[] {defaultValueTime};
		double[][] _indVals = new double[indVals.length][];
		for (int i = 0; i < indVals.length; ++i) {
			_indVals[i] = new double[] {indVals[i]};
		}
		return rate(valTimes, _indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(double[])
	 */
	@Override
	public double[] rate(double[] indVals) throws RatingException {
		return rate(defaultValueTime, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(double[][])
	 */
	@Override
	public double[] rate(double[][] indVals) throws RatingException {
		return rate(defaultValueTime, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(long, double)
	 */
	@Override
	public double rate(long valTime, double indVal) throws RatingException {
		long[] valTimes = new long[] {valTime};
		double[][] indVals = new double[][] {{indVal}};
		return rate(valTimes, indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rateOne(long, double[])
	 */
	@Override
	public double rateOne(long valTime, double... indVals) throws RatingException {
		return rateOne2(valTime, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rateOne2(long, double[])
	 */
	@Override
	public double rateOne2(long valTime, double[] indVals) throws RatingException {
		long[] valTimes = new long[] {valTime};
		double[][] _indVals = new double[indVals.length][];
		for (int i = 0; i < indVals.length; ++i) {
			_indVals[i] = new double[] {indVals[i]};
		}
		return rate(valTimes, _indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(long, double[])
	 */
	@Override
	public double[] rate(long valTime, double[] indVals) throws RatingException {
		long[] valTimes = new long[indVals.length];
		Arrays.fill(valTimes, valTime);
		return rate(valTimes, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(long[], double[])
	 */
	@Override
	public double[] rate(long[] valTimes, double[] indVals) throws RatingException {
		return rate(valTimes, new double[][] {indVals});
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(long, double[][])
	 */
	@Override
	public double[] rate(long valTime, double[][] indVals) throws RatingException {
		long[] valTimes = new long[indVals.length];
		Arrays.fill(valTimes, valTime);
		return rate(valTimes, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(long[], double[][])
	 */
	@Override
	public double[] rate(long[] valTimes, double[][] indVals) throws RatingException {
		synchronized(this) {
			if (indVals.length != getIndParamCount()) {
				throw new RatingException(String.format("Expected %d value sets, got %d", getIndParamCount(), indVals.length));
			}
			if (valTimes == null) {
				throw new RatingException("No value times supplied");
			}
			for (int i = 0; i < indVals.length; ++i) {
				if (indVals[i].length != valTimes.length) {
					throw new RatingException("Inconsistent times and values arrays");
				}
			}
			double[] depVals = new double[valTimes.length];
			int indParamCount = this.getIndParamCount();
			double[] _indVals = null;
			int inputNumber = -1;
			int ratingNumber = -1;
			int conditionNumber = -1;
			try {
				//-----------------------------------//
				// for each independent variable set //
				//-----------------------------------//
				for (int i = 0; i < valTimes.length; ++i) {
					int evaluationNumber = -1;
					//----------------------------//
					// for each condition to test //
					//----------------------------//
					for (conditionNumber = 0; conditionNumber < conditions.length; ++conditionNumber) {
						//-----------------------------//
						// set the condition variables //
						//-----------------------------//
						VariableSet cvs = conditions[conditionNumber].getVariables();
						for (String cvname : cvs.getVariableNames()) {
							switch (cvname.charAt(1)) {
							case 'I' :
								inputNumber = Integer.parseInt(cvname.substring(2)) - 1;
								if (inputNumber < 0 || inputNumber >= indParamCount) {
									throw new RatingException(String.format("Variable \"%s\" specifies invalid independent parameter number", cvname));
								}
								break;
							case 'R' :
								inputNumber = -1;
								ratingNumber = Integer.parseInt(cvname.substring(2)) - 1;
								if (sourceRatings == null || ratingNumber < 0 || ratingNumber >= sourceRatings.length) {
									throw new RatingException(String.format("Variable \"%s\" specifies invalid rating number", cvname));
								}
								break;
							default :
								throw new RatingException("Unexpected variable name in condition: " + cvname);
							}
							if (inputNumber == -1) {
								if (_indVals == null) _indVals = new double[indParamCount];
								for (int ip = 0; ip < indParamCount; ++ip) {
									_indVals[ip] = indVals[ip][i];
									cvs.setValue(cvname, sourceRatings[ratingNumber].rateOne2(valTimes[i], _indVals));
								}
							}
							else {
								cvs.setValue(cvname, indVals[inputNumber][i]);
							}
						}
						//--------------------//
						// test the condition //
						//--------------------//
						if (conditions[conditionNumber].test()) {
							break;
						}
					}
					//---------------------------------------------------//
					// set the matched (or default) evaluation variables //
					//---------------------------------------------------//
					evaluationNumber = conditionNumber;
					VariableSet evs = evaluations[evaluationNumber].getVariables();
					for (String evname : evs.getVariableNames()) {
						switch (evname.charAt(1)) {
						case 'I' :
							inputNumber = Integer.parseInt(evname.substring(2)) - 1;
							if (inputNumber < 0 || inputNumber >= indParamCount) {
								throw new RatingException(String.format("Variable \"%s\" specifies invalid independent parameter number", evname));
							}
							break;
						case 'R' :
							inputNumber = -1;
							ratingNumber = Integer.parseInt(evname.substring(2)) - 1;
							if (sourceRatings == null || ratingNumber < 0 || ratingNumber >= sourceRatings.length) {
								throw new RatingException(String.format("Variable \"%s\" specifies invalid rating number", evname));
							}
							break;
						default :
							throw new RatingException("Unexpected variable name in condition: " + evname);
						}
						if (inputNumber == -1) {
							if (_indVals == null) _indVals = new double[indParamCount];
							for (int ip = 0; ip < indParamCount; ++ip) {
								_indVals[ip] = indVals[ip][i];
								evs.setValue(evname, sourceRatings[ratingNumber].rateOne2(valTimes[i], _indVals));
							}
						}
						else {
							evs.setValue(evname, indVals[inputNumber][i]);
						}
					}
					//----------------------------------//
					// finally, evaluate the expression //
					//----------------------------------//
					depVals[i] = evaluations[evaluationNumber].evaluate();
				}
			}
			catch (Throwable t) {
				if (t instanceof RatingException) throw (RatingException)t;
				throw new RatingException(t);
			}
			return depVals;
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getData()
	 */
	@Override
	public TransitionalRatingContainer getData() {
		synchronized(this) {
			TransitionalRatingContainer trrc = new TransitionalRatingContainer();
			super.getData(trrc);
			if (conditions != null) {
				trrc.conditions = new String[conditions.length];
				for (int i = 0; i < conditions.length; ++i) {
					trrc.conditions[i] = conditions[i].toString().replaceAll("\\$((I|R)\\d+)", "$1");
				}
			}
			if (evaluations != null) {
				trrc.evaluations = new String[evaluations.length];
				for (int i = 0; i < evaluations.length; ++i) {
					trrc.evaluations[i] = evaluations[i].toString().replaceAll("\\$((I|R)\\d+)", "$1");
				}
			}
			if (sourceRatings != null) {
				trrc.sourceRatings = new SourceRatingContainer[sourceRatings.length];
				trrc.sourceRatingIds = new String[sourceRatings.length];
				for (int i = 0; i < sourceRatings.length; ++i) {
					trrc.sourceRatings[i] = sourceRatings[i].getData();
					trrc.sourceRatingIds[i] = trrc.sourceRatings[i].rsc.ratingSpecContainer.specId;  
				}
			}
			return trrc;
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#setData(hec.data.cwmsRating.io.AbstractRatingContainer)
	 */
	@Override
	public void setData(AbstractRatingContainer rc) throws RatingException {
		synchronized(this) {
			if (!(rc instanceof TransitionalRatingContainer)) {
				throw new RatingException("Expected TransitionalRatingContainer, got " + rc.getClass().getName());
			}
			TransitionalRatingContainer trrc = (TransitionalRatingContainer)rc;
			if (trrc.conditions != null && trrc.conditions.length > 0 && trrc.evaluations != null && trrc.evaluations.length > 0) {
				if (trrc.evaluations.length - trrc.conditions.length != 1) {
					throw new RatingException("Transitional rating container has inconsistent number of conditions and evaluations");
				}
			}
			super._setData(trrc);
			conditions = null;
			evaluations = null;
			sourceRatings = null;
			if (trrc.conditions != null && trrc.conditions.length > 0) {
				conditions = new Condition[trrc.conditions.length];
				try {
					for (int i = 0; i < trrc.conditions.length; ++i) {
						conditions[i] = new Condition(trrc.conditions[i].toUpperCase().replaceAll("((I|R)\\d+)", "\\$$1"));
					}
				}
				catch (ComputationException e) {
					throw new RatingException(e);
				}
			}
			if (trrc.evaluations != null && trrc.evaluations.length > 0) {
				evaluations = new MathExpression[trrc.evaluations.length];
				try {
					for (int i = 0; i < trrc.evaluations.length; ++i) {
						evaluations[i] = new MathExpression(trrc.evaluations[i].toUpperCase().replaceAll("((I|R)\\d+)", "\\$$1"));
					}
				}
				catch (ComputationException e) {
					throw new RatingException(e);
				}
			}
			if (trrc.sourceRatings != null && trrc.sourceRatingIds.length > 0) {
				sourceRatings = new SourceRating[trrc.sourceRatings.length];
				for (int i = 0; i < trrc.sourceRatings.length; ++i) {
					sourceRatings[i] = new SourceRating(trrc.sourceRatings[i]);
				}
			}
			findCycles();
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#reverseRate(long[], double[])
	 */
	@Override
	public double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException {
		throw new RatingException("Cannot reverse through a transitional rating");
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getValues(java.lang.Integer)
	 */
	@Override
	public RatingValue[] getValues(Integer defaultInterval) {
		throw new UnsupportedOperationException("getValues is not supported for transitional ratings");
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getInstance(hec.data.cwmsRating.io.AbstractRatingContainer)
	 */
	@Override
	public AbstractRating getInstance(AbstractRatingContainer ratingContainer) throws RatingException {
		if (!(ratingContainer instanceof TransitionalRatingContainer)) {
			throw new UnsupportedOperationException("Transitional Ratings only support Transitional Rating Containers.");
		}
		return new TransitionalRating((TransitionalRatingContainer)ratingContainer);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj == this || (obj != null && obj.getClass() == getClass() && getData().equals(((TransitionalRating)obj).getData()));
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode() + getData().hashCode();
	}
}
