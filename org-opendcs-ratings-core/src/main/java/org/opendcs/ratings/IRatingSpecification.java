package org.opendcs.ratings;
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

import mil.army.usace.hec.metadata.Version;
import mil.army.usace.hec.metadata.location.LocationTemplate;

/**
 * An interface for a rating specification. Has a location, rating template, and
 * spec version.
 *
 * @author psmorris
 */
public interface IRatingSpecification
{

    /**
     * The delimiter used for the indiv parts.
     */
    public static final String PART_DELIM = ".";

    /**
     * Returns the template for this spec.
     *
     * @return
     */
    IRatingTemplate getTemplate();

    /**
     * Sets the template for this spec.
     *
     * @param template
     */
    void setTemplate(IRatingTemplate template);

    /**
     * Returns the version of this spec. Note that the template also has a
     * version separate from the spec version.
     *
     * @return
     */
    Version getSpecVersion();

    /**
     * Sets the version of this spec. Note that the template also has a version
     * separate from the spec version.
     *
     * @param specVersion
     */
    void setSpecVersion(Version specVersion);

    /**
     * Returns a boolean indicating if this spec is simple. I.e. can be used to
     * perform a reverse rating. 1 independent parameter : 1 dependent
     * parameter.
     *
     * @return
     */
    public boolean isSimple();

    /**
     * Returns the ref location template
     *
     * @return
     */
    public LocationTemplate getLocationRef();

    /**
     * Sets the ref location template.
     *
     * @param locationRef
     */
    public void setLocationRef(LocationTemplate locationRef);
}