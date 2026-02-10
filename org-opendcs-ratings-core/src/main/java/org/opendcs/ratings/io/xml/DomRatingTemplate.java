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
package org.opendcs.ratings.io.xml;

import mil.army.usace.hec.metadata.*;
import org.opendcs.ratings.IRatingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Describes a Rating Template that has a set of independent parameters, a
 * dependent parameter and a version. Eventually this stuff will support XML,
 * but not right now hence the JDom prefix. Stage;Flow.USGS-shifted
 * Count-Conduit_Gates,Opening-Conduit_Gates,Elev-Pool;Flow-Conduit_Gates.Normal
 * IndepParameter1,IndepParameter2..n;DepParameter.Version
 *
 * @author psmorris
 */
public class DomRatingTemplate implements IRatingTemplate, Comparable<IRatingTemplate>
{

    /**
     * The office id for this template.
     */
    private OfficeId _officeId;
    /**
     * An ordered list of independent parameters for this template.
     */
    private List<Parameter> _independentParameterList;
    /**
     * The dependent parameter for this template.
     */
    private Parameter _dependentParameter;
    /**
     * The version for this template.
     */
    private Version _templateVersion;
    /**
     * Regexp to parse the string version of the template.
     */
    public final String parsingRegexp = "(.*);(.*)\\.(.*)";
    /**
     * Regexp to validate the string version of the template.
     */
    public final String validationRegexp = "[^;.,-]{1,16}(-[^;.,]{1,32})?(,[^;.,-]{1,16}(-[^;.,]{1,32})?)*;[^;.,-]{1,16}(-[^;,.]{1,32})?\\.[^.]{1,32}";
    /**
     * Pattern used to parse the string version of the template.
     */
    public final Pattern parsingPattern = Pattern.compile(parsingRegexp);
    /**
     * Pattern used to validate the string version of the template.
     */
    public final Pattern validationPattern = Pattern.compile(validationRegexp);

    /**
     * Constructs an empty invalid template object. Use the setters to populate
     * this object.
     */
    public DomRatingTemplate()
    {
    }

    /**
     * Constructs a new template with the args required to be valid.
     *
     * @param officeId
     * @param independentParameterList
     * @param dependentParameter
     * @param version
     */
    public DomRatingTemplate(OfficeId officeId, List<Parameter> independentParameterList,
                             Parameter dependentParameter, Version version)
    {
        this._officeId = officeId;
        this._independentParameterList = independentParameterList;
        this._dependentParameter = dependentParameter;
        this._templateVersion = version;
    }

    /**
     * Copy constructs this template from the arg template.
     *
     * @param template
     */
    public DomRatingTemplate(IRatingTemplate template)
    {
        OfficeId otherOffice = template.getOfficeId();
        if (otherOffice != null)
        {
            this._officeId = new OfficeId(otherOffice);
        }
        List<Parameter> otherIndepList = template.getIndependentParameterList();
        if (otherIndepList != null)
        {
            this._independentParameterList = new ArrayList<Parameter>();
            for (Parameter op : otherIndepList)
            {
                this._independentParameterList.add(new Parameter(op));
            }
        }
        Parameter otherDep = template.getDependentParameter();
        if (otherDep != null)
        {
            this._dependentParameter = new Parameter(otherDep);
        }

        Version otherVersion = template.getTemplateVersion();
        if (otherVersion != null)
        {
            _templateVersion = new Version(otherVersion.getVersion());
        }

    }

    /**
     * Creates a new template from the Strings. Will throw an exception if the
     * arg Strings are not valid.
     *
     * @param officeId
     * @param templateId
     * @throws DataSetException
     */
    public DomRatingTemplate(String officeId, String templateId) throws DataSetException
    {
        // Stage;Flow.USGS-shifted
        // Count-Conduit_Gates,Opening-Conduit_Gates,Elev-Pool;Flow-Conduit_Gates.Normal
        if (templateId == null)
        {
            throw new DataSetException("Null rating template identifier.");
        }
        // split off version.
        Matcher m = validationPattern.matcher(templateId);
        if (!m.matches())
        {
            throw new DataSetException("Invalid rating template identifier: " + templateId);
        }
        m = parsingPattern.matcher(templateId);
        if (!m.matches() || m.groupCount() != 3)
        {
            throw new DataSetException("Invalid rating template identifier: " + templateId);
        }
        String indep = m.group(1);
        String dep = m.group(2);
        String ver = m.group(3);
        String[] indepSplit = indep.split(",");
        try
        {
            this._officeId = new OfficeId(officeId);
            this._independentParameterList = new ArrayList<Parameter>();
            for (String p : indepSplit)
            {
                _independentParameterList.add(new Parameter(p));
            }
            this._dependentParameter = new Parameter(dep);
            this._templateVersion = new Version(ver);
        }
        catch (DataSetIllegalArgumentException e)
        {
            throw new DataSetException(e);
        }
    }

    /**
     * Returns a hashcode for this template.
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        // office id has a hashcode.
        result = prime * result + ((_officeId == null) ? 0 : _officeId.hashCode());
        if (_independentParameterList != null)
        {
            for (Parameter p : _independentParameterList)
            {
                // parameter has a braindead equals already that just checks the
                // base parameter.
                // do our own test of parameter that uses both base and sub
                // parameter.
                result = prime * result + ((p == null) ? 0 : p.hashCode());
            }
        }
        // parameter has a braindead equals already that just checks the base
        // parameter.
        // do our own test of parameter that uses both base and sub parameter.
        result = prime * result
                + ((_dependentParameter == null) ? 0 : _dependentParameter.hashCode());
        // version has a hashcode.
        result = prime * result + ((_templateVersion == null) ? 0 : _templateVersion.hashCode());
        return result;
    }

    /**
     * Returns if this template is equal to the arg object.
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
        DomRatingTemplate other = (DomRatingTemplate) obj;
        if (_officeId == null)
        {
            if (other._officeId != null)
            {
                return false;
            }
        }
        else if (!_officeId.equals(other._officeId))
        {
            return false;
        }

        // indep params order matters.
        if (_independentParameterList == null)
        {
            if (other._independentParameterList != null)
            {
                return false;
            }
        }
        else if (!_independentParameterList.equals(other._independentParameterList))
        {
            return false;
        }

        // dep param
        if (_dependentParameter == null)
        {
            if (other._dependentParameter != null)
            {
                return false;
            }
        }
        // the parameter.equals(parameter) method only compares base parameters,
        // hence the cast to Object.
        else if (!_dependentParameter.equals((Object) other._dependentParameter))
        {
            return false;
        }

        // version
        if (_templateVersion == null)
        {
            if (other._templateVersion != null)
            {
                return false;
            }
        }
        else if (!_templateVersion.equals(other._templateVersion))
        {
            return false;
        }
        return true;
    }

    /**
     * Returns the string version of this template. Stage;Flow.USGS-shifted
     * Count-Conduit_Gates,Opening-Conduit_Gates,Elev-Pool
     * ;Flow-Conduit_Gates.Normal
     *
     * @return
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        // indep params ordered.
        if (_independentParameterList != null)
        {
            for (Parameter p : _independentParameterList)
            {
                sb.append(p);
                sb.append(INDEP_PARAM_DELIM);
            }
            sb.setLength(sb.length() - INDEP_PARAM_DELIM.length());
        }
        // ';'
        sb.append(INDEP_DEP_DELIM);
        // dep param
        if (_dependentParameter != null)
        {
            sb.append(_dependentParameter.toString());
        }
        // '.'
        sb.append(PART_DELIM);
        // version
        if (_templateVersion != null)
        {
            sb.append(_templateVersion.toString());
        }
        return sb.toString();
    }

    /**
     * Returns the office id of this template.
     */
    @Override
    public OfficeId getOfficeId()
    {
        return _officeId;
    }

    /**
     * Sets the office id of this template.
     */
    @Override
    public void setOfficeId(OfficeId officeId)
    {
        this._officeId = officeId;
    }

    /**
     * Returns the dependent parameter of this template.
     */
    @Override
    public Parameter getDependentParameter()
    {
        return _dependentParameter;
    }

    /**
     * Sets the dependent parameter of this template.
     */
    @Override
    public void setDependentParameter(Parameter dependentParameter)
    {
        this._dependentParameter = dependentParameter;
    }

    /**
     * Returns an ordered listing of the independent parameters for this
     * template.
     */
    @Override
    public List<Parameter> getIndependentParameterList()
    {
        return _independentParameterList;
    }

    /**
     * Sets the independent parmeters of this template to the ordered list.
     */
    @Override
    public void setIndependentParameterList(List<Parameter> independentParameterList)
    {
        this._independentParameterList = independentParameterList;
    }

    /**
     * Returns the version of this template.
     */
    @Override
    public Version getTemplateVersion()
    {
        return _templateVersion;
    }

    /**
     * Sets the version of this template.
     */
    @Override
    public void setTemplateVersion(Version version)
    {
        this._templateVersion = version;
    }

    /**
     * Compares this template to the arg template and returns an int describing
     * order.
     */
    @Override
    public int compareTo(IRatingTemplate o)
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
        if (this.getOfficeId() != null)
        {
            thisSb.append(this.getOfficeId());
            thisSb.append(";");
        }
        thisSb.append(this.toString());

        StringBuilder oSb = new StringBuilder();
        if (o.getOfficeId() != null)
        {
            oSb.append(o.getOfficeId());
            oSb.append(";");
        }
        oSb.append(o.toString());
        return thisSb.toString().compareTo(oSb.toString());
    }

    /**
     * Returns true if there is only one independent parameter in this template.
     * I.e. The template could be used for a reverse rating.
     */
    @Override
    public boolean isSimple()
    {
        return _independentParameterList.size() == 1;
    }

    /**
     * Returns a count of the independent parameters for this template.
     */
    @Override
    public int getIndependentParameterCount()
    {
        return _independentParameterList.size();
    }
}