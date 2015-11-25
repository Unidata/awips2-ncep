/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.edex.plugin.geomag;

import gov.noaa.nws.ncep.common.dataplugin.geomag.calculation.CalcUtil;

import java.util.Calendar;
import java.util.Vector;

import com.raytheon.uf.common.time.DataTime;

/**
 * Class that represents a geomag data
 * 
 * <pre>
 * * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer        Description
 * ------------ ---------- -----------     --------------------------
 * 10/07/2015   R11429     sgurung,jtravis Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class GeoMagData {

    private String stationCode = null;

    private String comp1RefersTo = null;

    private String comp2RefersTo = null;

    private String comp3RefersTo = null;

    private String comp4RefersTo = null;

    private Double comp1Val = null;

    private Double comp2Val = null;

    private Double comp3Val = null;

    private Double comp4Val = null;

    private int sourceId = 101;

    private Calendar observationTime = null;

    private DataTime headerTime = null;

    /**
     * Default constructor that assigns the component values to missing value
     * code read from the properties file
     */
    public GeoMagData() {
        this.comp1Val = CalcUtil.missingVal;
        this.comp2Val = CalcUtil.missingVal;
        this.comp3Val = CalcUtil.missingVal;
        this.comp4Val = CalcUtil.missingVal;
    }

    /**
     * Constructor that assigns the component values to user defined missing
     * value code
     * 
     * @param missingVal
     */
    public GeoMagData(Double missingVal) {
        this.comp1Val = missingVal;
        this.comp2Val = missingVal;
        this.comp3Val = missingVal;
        this.comp4Val = missingVal;
    }

    /**
     * @return the stationCode
     */
    public String getStationCode() {
        return stationCode;
    }

    /**
     * @param stationCode
     *            the stationCode to set
     */
    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    /**
     * @return the comp1RefersTo
     */
    public String getComp1RefersTo() {
        return comp1RefersTo;
    }

    /**
     * @param comp1RefersTo
     *            the comp1RefersTo to set
     */
    public void setComp1RefersTo(String comp1RefersTo) {
        this.comp1RefersTo = comp1RefersTo;
    }

    /**
     * @return the comp2RefersTo
     */
    public String getComp2RefersTo() {
        return comp2RefersTo;
    }

    /**
     * @param comp2RefersTo
     *            the comp2RefersTo to set
     */
    public void setComp2RefersTo(String comp2RefersTo) {
        this.comp2RefersTo = comp2RefersTo;
    }

    /**
     * @return the comp3RefersTo
     */
    public String getComp3RefersTo() {
        return comp3RefersTo;
    }

    /**
     * @param comp3RefersTo
     *            the comp3RefersTo to set
     */
    public void setComp3RefersTo(String comp3RefersTo) {
        this.comp3RefersTo = comp3RefersTo;
    }

    /**
     * @return the comp4RefersTo
     */
    public String getComp4RefersTo() {
        return comp4RefersTo;
    }

    /**
     * @param comp4RefersTo
     *            the comp4RefersTo to set
     */
    public void setComp4RefersTo(String comp4RefersTo) {
        this.comp4RefersTo = comp4RefersTo;
    }

    /**
     * @return the comp1Val
     */
    public Double getComp1Val() {
        return comp1Val;
    }

    /**
     * @param comp1Val
     *            the comp1Val to set
     */
    public void setComp1Val(Double comp1Val) {
        this.comp1Val = comp1Val;
    }

    /**
     * @return the comp2Val
     */
    public Double getComp2Val() {
        return comp2Val;
    }

    /**
     * @param comp2Val
     *            the comp2Val to set
     */
    public void setComp2Val(Double comp2Val) {
        this.comp2Val = comp2Val;
    }

    /**
     * @return the comp3Val
     */
    public Double getComp3Val() {
        return comp3Val;
    }

    /**
     * @param comp3Val
     *            the comp3Val to set
     */
    public void setComp3Val(Double comp3Val) {
        this.comp3Val = comp3Val;
    }

    /**
     * @return the comp4Val
     */
    public Double getComp4Val() {
        return comp4Val;
    }

    /**
     * @param comp4Val
     *            the comp4Val to set
     */
    public void setComp4Val(Double comp4Val) {
        this.comp4Val = comp4Val;
    }

    /**
     * @return the sourceId
     */
    public int getSourceId() {
        return sourceId;
    }

    /**
     * @param sourceId
     *            the sourceId to set
     */
    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * @return the observationTime
     */
    public Calendar getObservationTime() {
        return observationTime;
    }

    /**
     * @param observationTime
     *            the observationTime to set
     */
    public void setObservationTime(Calendar observationTime) {
        this.observationTime = observationTime;
    }

    /**
     * @return the headerTime
     */
    public DataTime getHeaderTime() {
        return headerTime;
    }

    /**
     * @param headerTime
     *            the headerTime to set
     */
    public void setHeaderTime(DataTime headerTime) {
        this.headerTime = headerTime;
    }

    public Vector<Double> getComponentValues() {

        Vector<Double> componentValues = new Vector<Double>();

        componentValues.add(this.comp1Val);
        componentValues.add(this.comp2Val);
        componentValues.add(this.comp3Val);
        componentValues.add(this.comp4Val);

        return componentValues;

    }

}
