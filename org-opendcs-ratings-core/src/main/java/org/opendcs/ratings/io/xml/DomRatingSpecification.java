/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */
package org.opendcs.ratings.io.xml;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import mil.army.usace.hec.metadata.DataSetException;
import mil.army.usace.hec.metadata.Version;
import mil.army.usace.hec.metadata.location.LocationTemplate;
import org.opendcs.ratings.IRatingSpecification;
import org.opendcs.ratings.IRatingTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Describes a rating specification. Has a location, rating template, and a
 * specification version. TULA.Stage;Flow.USGS-shifted.Production
 * ARCA.Count-Conduit_Gates
 * ,Opening-Conduit_Gates,Elev-Pool;Flow-Conduit_Gates.Normal.Production
 * Location.Template.SpecVersion
 *
 * Eventually this object will be able to read and write itself using the XML
 * formats.
 *
 * @author psmorris
 */
public class DomRatingSpecification implements IRatingSpecification,
        Comparable<IRatingSpecification>
{

    /**
     * holds the location and the office
     */
    private LocationTemplate _locationRef;
    /**
     * the template, parameters and version.
     */
    private DomRatingTemplate _template;
    /**
     * the spec version
     */
    private Version _specVersion;
    /**
     * pattern for parsing the specification.
     */
    private static final Pattern parsingPattern = Pattern.compile("(.*)\\.(.*;.*\\..*)\\.(.*)");

    /**
     * Default constructor. Fill in the object with the setters to become valid.
     */
    public DomRatingSpecification()
    {
    }

    /**
     * String parsing constructor to create a populating rating specification.
     *
     * @param officeId
     * @param specificationId
     */
    public DomRatingSpecification(String officeId, String specificationId) throws DataSetException
    {
        // TULA.Stage;Flow.USGS-shifted.Production
        // ARCA.Count-Conduit_Gates,Opening-Conduit_Gates,Elev-Pool;Flow-Conduit_Gates.Normal.Production
        // Cowlitz River at Randle, WA.Stage;Stage-Corrected.Linear.USGS-NWIS4
        if (specificationId == null)
        {
            throw new DataSetException("Null rating specification identifier");
        }
        Matcher matcher = parsingPattern.matcher(specificationId);
        if (!matcher.matches())
        {
            throw new DataSetException("Illegal rating specification identifier: "
                    + specificationId);
        }
        matcher = parsingPattern.matcher(specificationId);
        if (!matcher.matches() || matcher.groupCount() != 3)
        {
            throw new DataSetException("Illegal rating specification identifier: "
                    + specificationId);
        }
        String loc = matcher.group(1);
        String templ = matcher.group(2);
        String ver = matcher.group(3);
        this._locationRef = new LocationTemplate(officeId, loc);
        this._template = new DomRatingTemplate(officeId, templ);
        this._specVersion = new Version(ver);
    }

    /**
     * Parameterized constructor that has the args required to be valid.
     *
     * @param locationRef
     * @param template
     * @param specVersion
     */
    public DomRatingSpecification(LocationTemplate locationRef, IRatingTemplate template,
                                  Version specVersion)
    {
        this._locationRef = locationRef;
        setTemplate(template);
        this._specVersion = specVersion;
    }

    /**
     * Copy constructor that creates a copy of the arg rating specification.
     *
     * @param ratingSpecification
     */
    public DomRatingSpecification(IRatingSpecification ratingSpecification)
    {
        this._locationRef = new LocationTemplate(ratingSpecification.getLocationRef());
        this._template = new DomRatingTemplate(ratingSpecification.getTemplate());
        this._specVersion = new Version(ratingSpecification.getSpecVersion().getVersion());
    }

    /**
     * Performs a comparison of this spec to the arg spec and returns a number
     * that can be used for ordering.
     */
    @Override
    public int compareTo(IRatingSpecification o)
    {
        if (o == null)
        {
            return 1;
        }
        if (this.equals(o))
        {
            return 0;
        }
        StringBuilder thisSb = new StringBuilder();
        // office id is not in the to string for this.
        if (this.getLocationRef() != null && this.getLocationRef().getDbOfficeId() != null)
        {
            thisSb.append(this.getLocationRef().getDbOfficeId().toString());
            thisSb.append(";");
        }
        thisSb.append(this.toString());

        StringBuilder oSb = new StringBuilder();
        if (o.getLocationRef() != null && o.getLocationRef().getDbOfficeId() != null)
        {
            oSb.append(o.getLocationRef().getDbOfficeId().toString());
            oSb.append(";");
        }
        oSb.append(o.toString());
        return thisSb.toString().compareTo(oSb.toString());
    }

    /**
     * Returns the location of this spec.
     */
    @Override
    public LocationTemplate getLocationRef()
    {
        return _locationRef;
    }

    /**
     * Sets the location of this spec.
     */
    @Override
    public void setLocationRef(LocationTemplate locRef)
    {
        this._locationRef = locRef;
    }

    /**
     * Returns the version of this spec.
     */
    @Override
    public Version getSpecVersion()
    {
        return _specVersion;
    }

    /**
     * Sets the version of this spec.
     */
    @Override
    public void setSpecVersion(Version specVersion)
    {
        this._specVersion = specVersion;
    }

    /**
     * Returns the template for this spec.
     */
    @Override
    public IRatingTemplate getTemplate()
    {
        return _template;
    }

    /**
     * Sets the template for this spec.
     */
    @Override
    public void setTemplate(IRatingTemplate template)
    {
        if (template instanceof DomRatingTemplate)
        {
            this._template = (DomRatingTemplate) template;
        }
        else
        {
            // xml would be nice here....
            this._template = new DomRatingTemplate(template);
        }
    }

    /**
     * Returns a hashcode for this spec.
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_locationRef == null) ? 0 : _locationRef.hashCode());
        result = prime * result + ((_template == null) ? 0 : _template.hashCode());
        result = prime * result + ((_specVersion == null) ? 0 : _specVersion.hashCode());
        return result;
    }

    /**
     * Returns if this spec is equal to the arg object.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        DomRatingSpecification other = (DomRatingSpecification) obj;
        // locref
        if (_locationRef == null)
        {
            if (other._locationRef != null)
            {
                return false;
            }
        }
        else if (!_locationRef.equals(other._locationRef))
        {
            return false;
        }

        // specVersion
        if (_specVersion == null)
        {
            if (other._specVersion != null)
            {
                return false;
            }
        }
        else if (!_specVersion.equals(other._specVersion))
        {
            return false;
        }
        // template
        if (_template == null)
        {
            if (other._template != null)
            {
                return false;
            }
        }
        else if (!_template.equals(other._template))
        {
            return false;
        }

        return true;
    }

    /**
     * Returns the string representation of this spec.
     * TULA.Stage;Flow.USGS-shifted.Production
     * ARCA.Count-Conduit_Gates,Opening-Conduit_Gates
     * ,Elev-Pool;Flow-Conduit_Gates.Normal.Production
     *
     * @return
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if (_locationRef != null)
        {
            sb.append(_locationRef.getLocationId());
        }
        sb.append(IRatingSpecification.PART_DELIM);
        if (_template != null)
        {
            sb.append(_template.toString());
        }
        sb.append(PART_DELIM);
        if (_specVersion != null)
        {
            sb.append(_specVersion.toString());
        }
        return sb.toString();
    }

    /**
     * Returns if this spec can be used in a reverse rating.
     */
    @Override
    public boolean isSimple()
    {
        return _template.isSimple();
    }
}