/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opendcs.ratings;

import mil.army.usace.hec.metadata.OfficeId;
import mil.army.usace.hec.metadata.Parameter;
import mil.army.usace.hec.metadata.Version;

import java.util.List;

/**
 * An interface on to a rating template. Has an office id, parameters, and
 * version.
 *
 * @author psmorris
 */
public interface IRatingTemplate
{

    /**
     * The delimeter for the independent parameters.
     */
    public static final String INDEP_PARAM_DELIM = ",";
    /**
     * The delimeter between the independent and dependent parameters.
     */
    public static final String INDEP_DEP_DELIM = ";";
    /**
     * The delimeter between the parameters and version.
     */
    public static final String PART_DELIM = ".";

    /**
     * Returns the office id for this template.
     *
     * @return
     */
    OfficeId getOfficeId();

    /**
     * Sets the office id for this template.
     *
     * @param officeId
     */
    void setOfficeId(OfficeId officeId);

    /**
     * Returns an ordered listing of the independent parameters for this
     * template.
     *
     * @return
     */
    List<Parameter> getIndependentParameterList();

    /**
     * Sets the independent parameters for this template to the ordered arg
     * list.
     *
     * @param independentParameterList
     */
    void setIndependentParameterList(List<Parameter> independentParameterList);

    /**
     * Returns the dependent parameter for this template.
     *
     * @return
     */
    Parameter getDependentParameter();

    /**
     * Sets the dependent parameter for this template.
     *
     * @param dependentParameter
     */
    void setDependentParameter(Parameter dependentParameter);

    /**
     * Returns the version for this template. Note that a spec also has a
     * version separate from the template version.
     *
     * @return
     */
    Version getTemplateVersion();

    /**
     * Sets the version for this template. Note that a spec also has a version
     * separate from the template version.
     *
     * @param version
     */
    void setTemplateVersion(Version version);

    /**
     * If the rating has 1 independent parameter : 1 dependent parameter. I.e.
     * it can be used for reverse ratings.
     *
     * @return
     */
    boolean isSimple();

    /**
     * Returns a count of the independent parameters.
     *
     * @return
     */
    public int getIndependentParameterCount();
}