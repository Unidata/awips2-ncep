package gov.noaa.nws.ncep.edex.common.metparameters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.DataTime.FLAG;
import com.raytheon.uf.common.units.UnitAdapter;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.NotDerivableException;

/**
 * An abstract class for all metParameters. This will hold the value of the
 * parameter and its units.
 * 
 * TODO : add support for the Level/Layer at which the value applies. The level
 * could be a PressureLevel, a Height or 'Surface' or other defined layer. This
 * would allow the derive methods to remove the PressureLevel derive() arguments
 * and do a compatibility check on the other arguments to make sure they are all
 * from the same level. Could also add support for the time or duration for
 * which the value applies.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 05/05/2011              Greg Hull    Initial creation
 * 06/05/2011              Greg Hull    Added check for infinite recursion in derive method.
 * 10/05/2011              Greg Hull    add dataTime 
 * 10/14/2011              Greg Hull    add setValueFromString
 * 11/14/2011              B. Hebbard   remove standardUnit
 * 06/17/2014              S. Russell   TTR 923: added member, get/set methods associatedMetParam
 * 12/04/2014    R5437     B. Hebbard   Add/enhance recursive getDeriveMethod(..) variants to return
 *                                      ('bubble up') bottom-level (non-derived) params from the
 *                                      given set that are actually needed in the derivation.
 * 08/24/2016    R18194    RReynolds    Added access to met data arrays
 * 01/25/2017    R27759    S.Russell    added overrideCallChildDeriveAnyway
 *                                      and used it in derive() to make
 *                                      R27759 work, call the child derive()
 * 04/15/2019    7596      lsingh       Updated units framework to JSR-363. Also updated
 *                                      AbstractMetParameter to be a generic class.
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public abstract class AbstractMetParameter<Q extends Quantity<Q>> extends Amount
        implements ISerializableObject {

    /*-
     * If you have only one derive() in the child class, and you want that
     * derive() to be called despite missing values in some of the arguments,
     * and your derive() is set up to handle that -- set this to true in the
     * constructor of the child class.
     */
    protected boolean overrideCallChildDeriveAnyway = false;

    @DynamicSerializeElement
    private static final long serialVersionUID = 7369542461296836406L;

    public AbstractMetParameter() {
        super();
        dbParamNamesForDerivingThisMetPrm = new HashSet<String>(0);
    }

    @DynamicSerializeElement
    private boolean useStringValue; // override to true for String parameters.

    @DynamicSerializeElement
    protected DataTime dataTime;

    // TTR 923, Holds the second MetParameter in a combination
    // AbstractMetParameter uch as the PTND "button"
    // (PressPressureChange3HrAndTendency MetParameter)
    @DynamicSerializeElement
    private AbstractMetParameter associatedMetParam = null;

    protected Set<String> dbParamNamesForDerivingThisMetPrm;

    public final Set<String> getDbParamNamesForDerivingThisMetParameter() {
        return dbParamNamesForDerivingThisMetPrm;
    }

    public void setDbParamNamesForDerivingThisMetPrm(
            Set<String> dbParamNamesForDerivingThisMetPrm) {
        this.dbParamNamesForDerivingThisMetPrm = dbParamNamesForDerivingThisMetPrm;
    }

    public String dbValsString[];

    public Number dbValsNumber[];

    public void setDbValsString(String[] dbv) {
        dbValsString = dbv;
    }

    public void setDbValsNumber(Number[] dbv) {
        dbValsNumber = dbv;
    }

    public Number[] getDbValsNumber() {

        return dbValsNumber;
    }

    public String[] getDbValsString() {

        return dbValsString;
    }

    // TTR 923
    public void setAssociatedMetParam(AbstractMetParameter amp) {
        // TODO remove following?
        if (this.associatedMetParam != null){
            this.associatedMetParam = null;
        }

        this.associatedMetParam = amp;
    }

    public AbstractMetParameter getAssociatedMetParam() {
        return this.associatedMetParam;
    }

    /**
     * @return the dataTime
     */
    public final DataTime getDataTime() {
        return dataTime;
    }

    /**
     * @param dataTime
     *            the dataTime to set
     */
    public final void setDataTime(DataTime dataTime) {
        this.dataTime = dataTime;
    }

    /**
     * @return the valueString
     */
    public final String getValueString() {
        return valueString;
    }

    /**
     * @param valueString
     *            the valueString to set
     */
    public final void setValueString(String valueString) {
        this.valueString = valueString;
    }

    /**
     * -
     * 
     * @param useStringValue
     *            the useStringValue to set
     */
    public final void setUseStringValue(boolean useStringValue) {
        this.useStringValue = useStringValue;
    }

    /**
     * @param standardUnit
     *            the standardUnit to set
     */

    // only one of these may be set at a time. In order to hold a string value
    // the Quantity of the parameter must be Dimensionless
    @DynamicSerializeElement
    protected String valueString; // "" is MISSING, null for non-string values

    // if this list is set then derive() will first look for a derive method
    // using just these parameters.
    @DynamicSerializeElement
    protected ArrayList<String> preferredDeriveParameters;

    /**
     * @return the useStringValue
     */
    public final boolean isUseStringValue() {
        return useStringValue;
    }

    protected AbstractMetParameter(Unit<?> u) {
        super(u);
        valueString = null;
        // standardUnit = u;
    }

    protected AbstractMetParameter(Unit<?> u, DataTime dt) {
        super(u);
        valueString = null;
        // standardUnit = u;
        dataTime = dt;
    }

    protected AbstractMetParameter(String unitStr) { // String ncPrmName ) {
        super(unitStr);
        try {
            valueString = null;
            Unit<?> newUnit = new UnitAdapter().unmarshal(unitStr);
            // standardUnit = newUnit;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    // Override for real Description information
    public String getParameterDescription() {
        return getMetParamName();
    }

    protected void setValueIsString() {
        useStringValue = true;
    }

    // To be removed
    public boolean isUnitCompatible(Unit<?> u) {
        // return standardUnit.isCompatible( u );
        return getUnit().isCompatible(u);
    }

    public boolean isUnitCompatible(String unitName) {
        try {
            // return getStandardUnit().isCompatible( new
            // UnitAdapter().unmarshal(unitName) );
            return getUnit()
                    .isCompatible(new UnitAdapter().unmarshal(unitName));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Boolean hasStringValue() {
        return useStringValue;
    }

    public String getStringValue() {
        return (valueString == null ? "" : valueString);
    }

    public Boolean hasValidTime() {
        return (dataTime != null);
    }

    public DataTime getValidTime() {
        return dataTime;
    }

    public Boolean isValidAtTime(DataTime dt) {
        if (dataTime == null) {
            return null;
        }

        if (dataTime.getUtilityFlags().contains(FLAG.PERIOD_USED)) {
            return dataTime.getValidPeriod()
                    .contains(dt.getValidTime().getTime());
        } else {
            return dataTime.compareTo(dt) == 0;
        }
    }

    public void setValidTime(DataTime dt) {
        dataTime = dt;
    }

    @Override
    public boolean hasValidValue() {
        if (useStringValue) {
            return (valueString != null);
        } else {
            return super.hasValidValue();
        }
    }

    // ?throw exceptions for invalid value or for incompatible units?
    // TODO remove this method
    public Number getValueAs(Unit<?> unitNeeded) {
        if (!hasValidValue()) {
            return null;
        } else if (!isUnitCompatible(unitNeeded)) {
            System.out.println("getValueAs() : asking for incompatible units. "
                    + getUnit().toString() + ", " + unitNeeded.toString());
            return null;
        }

        if (useStringValue) {
            return getMissingValueSentinel();
        } else {
            return super.getValueAs(unitNeeded);
        }
    }

    public Number getValueAs(String unitName) {
        if (!hasValidValue()) {
            return null;
        }
        try {

            if (!isUnitCompatible(unitName)) {
                System.out.println(
                        "getValueAs() : asking for incompatible units. "
                                + getUnit().toString() + " , " + unitName);
                return null;
            }

            if (useStringValue) {
                return getMissingValueSentinel();
            } else {
                return super.getValueAs(new UnitAdapter().unmarshal(unitName));
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    // This will work off of the current units so if we need to
    // format at string using different units the value must be
    // changed before calling this method.
    // The formatStr can be a 'printf' style string or a
    // parameter-specific tag for abbreviating, trimming or
    // converting the parameter value. In the latter case this
    // method must be overridden.
    //
    public String getFormattedString(String formatStr) {

        String formattedValueStr = null;

        if (hasStringValue()) {
            formattedValueStr = valueString;
        } else {
            formattedValueStr = getValue().toString();
        }

        if (formatStr == null) {
            return formattedValueStr;
        } else if (formatStr.startsWith("%")) {
            // double formattedValue = valueAs.doubleValue();
            StringBuilder sb = new StringBuilder();
            Formatter fmtr = new Formatter(sb);
            fmtr.format(formatStr, getValue()); // formattedValue

            formattedValueStr = sb.toString();

            // sValue = sValue.substring( trim );

            return formattedValueStr;
        } else {
            System.out.println("Sanity Check: Unrecognized format string ("
                    + formatStr + ") for metParameter " + getMetParamName());
            return formattedValueStr;
        }
    }

    // if this
    public void setStringValue(String sv) {
        setValueToMissing();

        valueString = sv;
    }

    // This is intended for numeric params that may need to parse their values
    // from a string
    //
    public void setValueFromString(String valStr, Unit<?> u) {
        if (hasStringValue()) {
            setStringValue(valStr);
        } else {
            NumberFormat numFmt = NumberFormat.getInstance();
            try {
                setValue(numFmt.parse(valStr), u);
            } catch (ParseException pe) {
                super.setValueToMissing();
            }
        }
    }

    @Override
    public void setValueToMissing() {
        if (useStringValue) {
            valueString = null;
        }
        super.setValueToMissing();
    }

    /**
     * Returns true iff "this" parameter can be derived from (some subset of)
     * the given availableParams minus the checkedParams. (The checkedParams are
     * used in recursive descent to narrow the search space.) If true, also
     * returns the base set of (non-derived) usedParams needed for the
     * derivation.
     * 
     * @param checkedParams
     *            members of availableParams to ignore (b/c already used at
     *            higher recursion level)
     * @param availableParams
     *            set of params (some possibly derived themselves) from which to
     *            derive "this" param
     * @param usedParams
     *            RETURN set of non-derived params used at this level or below
     * @return true if derivable from availableParams, and false otherwise
     */
    private Boolean derivable(ArrayList<String> checkedParams,
            Collection<AbstractMetParameter<Q>> availableParams,
            Collection<AbstractMetParameter<Q>> usedParams) {
        return (getDeriveMethod(checkedParams, availableParams,
                usedParams) != null);
    }

    /**
     * Returns a Method to derive "this" parameter from (some subset of) the
     * given availableParams, or returns null if not derivable from them.
     * 
     * @param availableParams
     *            set of params (some possibly derived themselves) from which to
     *            derive "this" param
     * @return Method to derive "this" parameter at the top level
     */
    public Method getDeriveMethod(
            Collection<AbstractMetParameter<Q>> availableParams) {
        Collection<AbstractMetParameter<Q>> usedParams = new HashSet<>();
        Method returnMethod = getDeriveMethod(availableParams, usedParams);
        // discard usedParams (as this variant doesn't care about returning
        // them)
        return returnMethod;
    }

    /**
     * Returns a Method to derive "this" parameter from (some subset of) the
     * given availableParams, or returns null if not derivable. If derivable,
     * also returns the base set of (non-derived) usedParams needed for the
     * derivation.
     * 
     * @param availableParams
     *            set of params (some possibly derived themselves) from which to
     *            derive "this" param
     * @param usedParams
     *            RETURN set of non-derived params used at this level or below
     * @return Method to derive "this" parameter at the top level
     */
    public Method getDeriveMethod(
            Collection<AbstractMetParameter<Q>> availableParams,
            Collection<AbstractMetParameter<Q>> usedParams) {

        ArrayList<String> checkedParams = new ArrayList<String>();

        // if the preferredDeriveParameters list is set then only use these
        // parameters.
        if (preferredDeriveParameters != null) {

            ArrayList<AbstractMetParameter<Q>> availableParamsList = new ArrayList<>();

            for (AbstractMetParameter<Q> prm : availableParams) {
                if (preferredDeriveParameters.contains(prm.getMetParamName())) {
                    availableParamsList.add(prm);
                }
            }
            return getDeriveMethod(checkedParams, availableParamsList,
                    usedParams);
        } else {
            return getDeriveMethod(checkedParams, availableParams, usedParams);
        }
    }

    /**
     * Returns a Method to derive "this" parameter from (some subset of) the
     * given availableParams minus the checkedParams, or returns null if not
     * derivable. (The checkedParams are used in recursive descent to narrow the
     * search space.) If derivable, also returns the base set of (non-derived)
     * usedParams needed for the derivation.
     * 
     * @param checkedParams
     *            members of availableParams to ignore (b/c already used at
     *            higher recursion level)
     * @param availableParams
     *            set of params (some possibly derived themselves) from which to
     *            derive "this" param
     * @param usedParams
     *            RETURN set of non-derived params used at this level or below
     * @return Method to derive "this" parameter at the top level
     */
    private Method getDeriveMethod(ArrayList<String> checkedParams,
            Collection<AbstractMetParameter<Q>> availableParams,
            Collection<AbstractMetParameter<Q>> usedParams) {

        // check each of the methods named 'derive' and check to see if the
        // given availableParams are sufficient to derive this ncParameter.

        Method[] deriveMthds = this.getClass().getDeclaredMethods();

        // map containing derive methods which have been found to be applicable
        // given the set of availableParams, and for each method, the associated
        // subset of non-derived availableParams actually used in the derivation
        Map<Method, Set<AbstractMetParameter<Q>>> foundDeriveMthds = new HashMap<Method, Set<AbstractMetParameter<Q>>>();

        // check each derive method to see if its arguments are in the
        // availableParams
        for (Method m : deriveMthds) {
            boolean derivable = true;
            Set<AbstractMetParameter<Q>> baseParamsForThisMethod = new HashSet<>();

            if (m.getAnnotation(DeriveMethod.class) != null) {

                Class<?> rtype = m.getReturnType();
                Class<?>[] deriveMthdArgs = m.getParameterTypes();

                // loop thru the list of args for this derive() method and check
                // if it is in the availableParams list.
                for (Class<?> argClass : deriveMthdArgs) {
                    boolean prmFound = false;
                    boolean prmIsDerivable = false;

                    for (AbstractMetParameter<Q> inputPrm : availableParams) {
                        // if we have this input parameter...
                        if (inputPrm.getClass() == argClass) {
                            prmFound = true;
                            baseParamsForThisMethod.add(inputPrm);
                            break;
                        }
                    }

                    // ...or if in the list and if we have not already checked
                    // this parameter (at higher level) then see if it is
                    // derivable
                    if (!prmFound && !checkedParams
                            .contains(this.getMetParamName())) {

                        AbstractMetParameter<Q> argParam;
                        try {
                            argParam = (AbstractMetParameter<Q>) argClass
                                    .getConstructor().newInstance();

                            checkedParams.add(argParam.getMetParamName());

                            prmIsDerivable = argParam.derivable(checkedParams,
                                    availableParams, usedParams);

                            checkedParams.remove(argParam.getMetParamName());

                        } catch (Exception e) {
                            System.out.println(
                                    "error getting newInstance for metParam "
                                            + argClass.getSimpleName());
                        }
                    }

                    if (!prmFound && !prmIsDerivable) {
                        derivable = false;
                        // break;
                    }
                } // end loop thru derive() args

                if (derivable) {
                    // remember this derive Method and its associated set of
                    // base (non-derived) parameters
                    foundDeriveMthds.put(m, baseParamsForThisMethod);
                }
            }
        }

        //
        if (foundDeriveMthds.isEmpty()) {
            return null;
        } else if (foundDeriveMthds.size() > 1) {
            // If this happens then the caller should set the
            // preferredDeriveParameters list to tell this method which
            // arguments to use.
            System.out.println("Sanity Check: metParameter " + getMetParamName()
                    + " has multiple derive() methods for "
                    + "the given input parameters.");
            return null;
        } else {
            if (usedParams == null) {
                usedParams = new HashSet<AbstractMetParameter<Q>>();
            }
            for (Set<AbstractMetParameter<Q>> ampSet : foundDeriveMthds.values()) {
                usedParams.addAll(ampSet);
            }
            return (Method) foundDeriveMthds.keySet().toArray()[0];
        }
    }

    /**
     * Returns an AbstractMetParameter derived from (some subset of) the given
     * availableParams.
     * 
     * @param availableParams
     *            set of params (some possibly derived themselves) from which to
     *            derive "this" param
     * @return AbstractMetParameter as derived
     * @throws NotDerivableException
     *             if "this" parameter cannot be derived from the
     *             availableParams
     */
    public AbstractMetParameter<Q> derive(
            Collection<AbstractMetParameter<Q>> availableParams)
            throws NotDerivableException {
        Collection<AbstractMetParameter<Q>> usedParams = null;
        // this variant (overload) doesn't return usedParams; we just discard
        // the ones passed up to us from below
        return derive(availableParams, usedParams);
    }

    /**
     * Returns an AbstractMetParameter derived from (some subset of) the given
     * availableParams. If derivable, also returns the base set of (non-derived)
     * usedParams needed for the derivation.
     * 
     * @param availableParams
     *            set of params (some possibly derived themselves) from which to
     *            derive "this" param
     * @param usedParams
     *            RETURN set of non-derived params used at this level or below
     * @return AbstractMetParameter as derived
     * @throws NotDerivableException
     *             if "this" parameter cannot be derived from the
     *             availableParams
     */
    public AbstractMetParameter<Q> derive(
            Collection<AbstractMetParameter<Q>> availableParams,
            Collection<AbstractMetParameter<Q>> usedParams)
            throws NotDerivableException {

        Method deriveMthd = getDeriveMethod(availableParams, usedParams);

        if (deriveMthd == null) {
            setValueToMissing();
            return this;
            // throw new
            // NotDerivableException("can't derive param from given
            // parameters.");
        }
        String errMsg = "";

        try {
            // check each derive method to see if its arguments are in the input
            Class<?>[] deriveMthdArgs = deriveMthd.getParameterTypes();

            // a list of the parameter args (actual values) that will be passed
            // to the derive() method.
            List<AbstractMetParameter<Q>> mthdArgs = new ArrayList<>(
                    0);

            for (Class<?> argClass : deriveMthdArgs) {
                boolean prmFound = false;

                for (AbstractMetParameter<Q> inputPrm : availableParams) {
                    // if we don't have this input parameter, derive it
                    if (inputPrm.getClass() == argClass) {
                        if (!inputPrm.hasValidValue()) {
                            setValueToMissing();

                            if (!this.overrideCallChildDeriveAnyway) {
                                return this;
                            }

                        } else {
                            mthdArgs.add(inputPrm);
                            prmFound = true;
                            break;
                        }
                    }
                }
                if (!prmFound) {
                    // create an object for this parameter and then set/derive
                    // the value
                    Constructor constr = argClass.getConstructor();
                    AbstractMetParameter<Q> derivedArgPrm = (AbstractMetParameter<Q>) constr
                            .newInstance();
                    derivedArgPrm = derivedArgPrm.derive(availableParams,
                            usedParams);
                    mthdArgs.add(derivedArgPrm);
                }
            }
            
            Object derivedParam = null;

            switch (mthdArgs.size()) {
            case 1:
                derivedParam = deriveMthd.invoke(this, mthdArgs.get(0));
                break;
            case 2:
                derivedParam = deriveMthd.invoke(this, mthdArgs.get(0),
                        mthdArgs.get(1));
                break;
            case 3:
                derivedParam = deriveMthd.invoke(this, mthdArgs.get(0),
                        mthdArgs.get(1), mthdArgs.get(2));
                break;
            case 4:
                derivedParam = deriveMthd.invoke(this, mthdArgs.get(0),
                        mthdArgs.get(1), mthdArgs.get(2), mthdArgs.get(3));
                break;
            }

            return (AbstractMetParameter<Q>) derivedParam;

        } catch (IllegalArgumentException e) {
            errMsg = e.getMessage();
        } catch (IllegalAccessException e) {
            errMsg = e.getMessage();
        } catch (InvocationTargetException e) {
            errMsg = e.getMessage();
        } catch (SecurityException e) {
            errMsg = e.getMessage();
        } catch (NoSuchMethodException e) {
            errMsg = e.getMessage();
        } catch (InstantiationException e) {
            errMsg = e.getMessage();
        }

        throw new NotDerivableException(errMsg == null ? "" : errMsg);
    }

    public String getMetParamName() {
        return this.getClass().getSimpleName();
    }

    public ArrayList<String> getPreferredDeriveParameters() {
        return preferredDeriveParameters;
    }

    // Assume that the caller has already called isValidMetParameterName() to
    // validate the names in the list.
    public void setPreferredDeriveParameters(
            ArrayList<String> preferredDeriveParameters) {
        this.preferredDeriveParameters = preferredDeriveParameters;
    }

    @Override
    public String toString() {
        // String
        if (hasStringValue()) {
            return getClass().getSimpleName() + " " + getStringValue();
        } else {
            return getClass().getSimpleName() + " " + getValue().toString()
                    + " " + getUnit().toString();
        }
    }
}
