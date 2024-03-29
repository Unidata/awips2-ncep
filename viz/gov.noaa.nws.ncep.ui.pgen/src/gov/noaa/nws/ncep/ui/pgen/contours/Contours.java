/*
 * gov.noaa.nws.ncep.ui.pgen.contours.Contours
 * 
 * october 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.contours;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.annotation.ElementOperations;
import gov.noaa.nws.ncep.ui.pgen.annotation.Operation;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;

/**
 * Class for Contours element.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer     Description
 * --------------------------------------------------------------------
 * 10/09        #167        J. Wu        Initial Creation.
 * 06/10        #215        J. Wu        Added min/max.
 * 07/10        #215        J. Wu        Create a new Contours from
 *                                       a set of lines.
 * 11/10        #345        J. Wu        Added support for Contours Circle
 * 07/13        TTR765      J. Wu        DEL_PART between vertexes.
 * 05/14        TTR1008     J. Wu        Added getKey() method.
 * 03/30/2016   R16622      J. Wu        Use current date/time as default.
 * 07/21/2016   R16077      J. Wu        Add copyWithExclusion().
 * 08/10/2016   R18805      J. Wu        Pad single-digit month/day with 0
 * 8/25/2019    67220       ksunil       added compareLevelParmForecastHour
 *
 * </pre>
 *
 * @author J. Wu
 */
@ElementOperations({ Operation.COPY_MOVE })
public class Contours extends DECollection implements IContours {

    private String parm;

    private String level;

    private String forecastHour;

    private String cint;

    private Calendar time1;

    private Calendar time2;

    /**
     * public constructor
     */
    public Contours() {

        super(PgenConstant.CONTOURS);
        setPgenCategory(PgenConstant.CATEGORY_MET);
        setPgenType(PgenConstant.CONTOURS);

        this.setParm("");
        this.setLevel("");
        this.setForecastHour("");
        this.setTime1((Calendar) Calendar.getInstance());
        this.setTime2((Calendar) Calendar.getInstance());
        this.setCint("");

    }

    public Contours(String name) {
        super(name);
    }

    public Contours(IAttribute attr, ArrayList<Coordinate> points) {

        super(PgenConstant.CONTOURS);
        setPgenCategory(PgenConstant.CATEGORY_MET);
        setPgenType(PgenConstant.CONTOURS);

    }

    /**
     * @param parm
     *            the parm to set
     */
    public void setParm(String parm) {
        this.parm = parm;
    }

    /**
     * @return the parm
     */
    public String getParm() {
        return parm;
    }

    /**
     * @return the level
     */
    public String getLevel() {
        return level;
    }

    /**
     * @param level
     *            the level to set
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * @return the forecastHour
     */
    public String getForecastHour() {
        return forecastHour;
    }

    /**
     * @param forecastHour
     *            the forecastHour to set
     */
    public void setForecastHour(String forecastHour) {
        this.forecastHour = forecastHour;
    }

    /**
     * @return the cint
     */
    public String getCint() {
        return cint;
    }

    /**
     * @param cint
     *            the cint to set
     */
    public void setCint(String cint) {
        this.cint = cint;
    }

    /**
     * @return the time1
     */
    public Calendar getTime1() {
        return time1;
    }

    /**
     * @param time
     *            the time to set
     */
    public void setTime1(Calendar time) {
        this.time1 = time;
    }

    /**
     * @return the time2
     */
    public Calendar getTime2() {
        return time2;
    }

    /**
     * @param time
     *            the time2 to set
     */
    public void setTime2(Calendar time) {
        this.time2 = time;
    }

    @Override
    /**
     * make a deep copy of the Contours
     */
    public Contours copy() {

        Contours newContours = new Contours();

        Iterator<AbstractDrawableComponent> iterator = this
                .getComponentIterator();

        while (iterator.hasNext()) {
            AbstractDrawableComponent adc = iterator.next().copy();
            adc.setParent(newContours);
            newContours.add(adc);
        }

        newContours.update(this);

        return newContours;

    }

    /**
     * Make a deep copy of the Contours, excluding the specified component.
     * 
     * @param exclude
     *            the AbstractDrawableComponent to be excluded
     * @return a new contour without the excluded ADC
     * 
     */
    public Contours copyWithExclusion(AbstractDrawableComponent exclude) {

        Contours newContours = new Contours();

        Iterator<AbstractDrawableComponent> iterator = this
                .getComponentIterator();

        while (iterator.hasNext()) {
            AbstractDrawableComponent nextAdc = iterator.next();
            if (nextAdc != exclude) {
                AbstractDrawableComponent adc = nextAdc.copy();
                adc.setParent(newContours);
                newContours.add(adc);
            }
        }

        newContours.update(this);

        return newContours;

    }

    /**
     * update the attributes for a Contours element
     */
    public void update(IContours attr) {
        this.setParm(attr.getParm());
        this.setLevel(attr.getLevel());
        this.setForecastHour(attr.getForecastHour());
        this.setTime1(attr.getTime1());
        this.setTime2(attr.getTime2());
        this.setCint(attr.getCint());

    }

    /**
     * Split a contour line in a Contours. - using index
     * 
     * Older version - use only the points on the original (un-smoothed) line
     */
    public Contours split(ContourLine cline, int start, int end) {

        Contours newContours = new Contours();

        Iterator<AbstractDrawableComponent> iterator = this
                .getComponentIterator();

        while (iterator.hasNext()) {

            AbstractDrawableComponent oldAdc = iterator.next();
            AbstractDrawableComponent newAdc = oldAdc.copy();

            if (oldAdc.equals(cline)) {
                ArrayList<ContourLine> newLines = ((ContourLine) newAdc)
                        .split(start, end);
                for (ContourLine cln : newLines) {
                    cln.setParent(newContours);
                    newContours.add(cln);
                }
            } else {
                newAdc.setParent(newContours);
                newContours.add(newAdc);
            }

        }

        newContours.update(this);

        return newContours;

    }

    /**
     * @return the string
     */
    public String toString() {
        StringBuilder result = new StringBuilder(getClass().getSimpleName());

        result.append("Category:\t" + pgenCategory + "\n");
        result.append("Type:\t" + pgenType + "\n");
        result.append("Parm:\t" + parm + "\n");
        result.append("Level:\t" + level + "\n");
        result.append("Cint:\t" + cint + "\n");
        result.append("Time1:\t" + time1 + "\n");
        result.append("Time2:\t" + time2 + "\n");

        return result.toString();
    }

    /**
     * @return a list of all ContourLines
     */
    public ArrayList<ContourLine> getContourLines() {

        ArrayList<ContourLine> lines = new ArrayList<ContourLine>();

        Iterator<AbstractDrawableComponent> iterator = this
                .getComponentIterator();

        while (iterator.hasNext()) {
            AbstractDrawableComponent adc = iterator.next();
            if (adc instanceof ContourLine) {
                lines.add((ContourLine) adc);
            }
        }

        return lines;

    }

    /**
     * @return a list of all ContourMinmaxs
     */
    public ArrayList<ContourMinmax> getContourMinmaxs() {

        ArrayList<ContourMinmax> cmms = new ArrayList<ContourMinmax>();

        Iterator<AbstractDrawableComponent> iterator = this
                .getComponentIterator();

        while (iterator.hasNext()) {
            AbstractDrawableComponent adc = iterator.next();
            if (adc instanceof ContourMinmax) {
                cmms.add((ContourMinmax) adc);
            }
        }

        return cmms;

    }

    /**
     * @return a list of all ContourCircles
     */
    public ArrayList<ContourCircle> getContourCircles() {

        ArrayList<ContourCircle> cmms = new ArrayList<ContourCircle>();

        Iterator<AbstractDrawableComponent> iterator = this
                .getComponentIterator();

        while (iterator.hasNext()) {
            AbstractDrawableComponent adc = iterator.next();
            if (adc instanceof ContourCircle) {
                cmms.add((ContourCircle) adc);
            }
        }

        return cmms;

    }

    /**
     * @return a new Contours from a set of lines generated from a grid.
     */
    public Contours createContours(int nContours, int[] nContourPts,
            double[] latlons, float[] contourValue, Color clr) {

        /*
         * Create a new Contours element from the contour lines
         */
        Contours gridContours = this.copy();
        gridContours.clear();

        int tPts = 0;
        ArrayList<Coordinate> linePts = new ArrayList<Coordinate>();
        for (int mm = 0; mm < nContours; mm++) {

            for (int nn = 0; nn < nContourPts[mm]; nn++) {
                Coordinate point = new Coordinate();
                point.x = latlons[tPts + nn * 2];
                point.y = latlons[tPts + nn * 2 + 1];

                linePts.add(point);
            }

            tPts = tPts + nContourPts[mm] * 2;

            int nLabels = 2;
            ContourLine cline = new ContourLine(linePts, false,
                    new String[] { "" + contourValue[mm] }, nLabels);

            cline.setParent(gridContours);
            if (clr != null)
                cline.getLine().setColors(new Color[] { clr });
            cline.getLine().setLineWidth(2);
            gridContours.add(cline);

            linePts.clear();
        }

        return gridContours;

    }

    /**
     * @return a new Contours from a set of lines generated from a grid.
     */
    public Contours createContours(int nContours, int[] nContourPts,
            double[] latlons, String[] contourValue, Color clr) {

        /*
         * Create a new Contours element from the contour lines
         */
        Contours gridContours = this.copy();
        gridContours.clear();

        int tPts = 0;
        ArrayList<Coordinate> linePts = new ArrayList<Coordinate>();
        for (int mm = 0; mm < nContours; mm++) {

            for (int nn = 0; nn < nContourPts[mm]; nn++) {
                Coordinate point = new Coordinate();
                point.x = latlons[tPts + nn * 2];
                point.y = latlons[tPts + nn * 2 + 1];

                linePts.add(point);
            }

            tPts = tPts + nContourPts[mm] * 2;

            int nLabels = 2;
            ContourLine cline = new ContourLine(linePts, false,
                    new String[] { contourValue[mm] }, nLabels);

            cline.setParent(gridContours);
            if (clr != null)
                cline.getLine().setColors(new Color[] { clr });
            cline.getLine().setLineWidth(2);
            gridContours.add(cline);

            linePts.clear();
        }

        return gridContours;

    }

    /**
     * Split a contour line in a Contours - using coordinate
     */
    public Contours split(ContourLine cline, Coordinate start, Coordinate end) {

        Contours newContours = new Contours();

        Iterator<AbstractDrawableComponent> iterator = this
                .getComponentIterator();

        while (iterator.hasNext()) {

            AbstractDrawableComponent oldAdc = iterator.next();
            AbstractDrawableComponent newAdc = oldAdc.copy();

            if (oldAdc.equals(cline)) {
                ArrayList<ContourLine> newLines = ((ContourLine) newAdc)
                        .split(start, end);
                for (ContourLine cln : newLines) {
                    cln.setParent(newContours);
                    newContours.add(cln);
                }
            } else {
                newAdc.setParent(newContours);
                newContours.add(newAdc);
            }

        }

        newContours.update(this);

        return newContours;

    }

    /**
     * Form a key that could be used to ID this contour
     */
    public String getKey() {
        return getKey(this);
    }

    /**
     * Form a key that could be used to ID a given contour
     */
    public static String getKey(IContours ctr) {

        Calendar ctrTime = ctr.getTime1();
        String hr = String.format("%02d", ctrTime.get(Calendar.HOUR_OF_DAY));

        String mt = "";
        int minute = ctrTime.get(Calendar.MINUTE);
        if (minute > 0) {
            mt = String.format(":%02d", minute);
        }

        /*
         * Padding single-digit months and days string with a leading 0. Note
         * that in Calendar, for the month, 0 - January.
         */
        String month = String.format("%02d", ctrTime.get(Calendar.MONTH) + 1);
        String day = String.format("%02d", ctrTime.get(Calendar.DAY_OF_MONTH));

        String key = ctr.getParm() + "," + ctr.getLevel() + ","
                + ctr.getForecastHour() + "|" + ctrTime.get(Calendar.YEAR) + "-"
                + month + "-" + day + "," + hr + mt + "Z";

        return key;
    }

    /*
     * A light version of equals. means two Contours have same
     * "parm, level, forecasthour"
     */

    public boolean compareLevelParmForecastHour(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Contours other = (Contours) obj;
        if (forecastHour == null) {
            if (other.forecastHour != null)
                return false;
        } else if (!forecastHour.equals(other.forecastHour))
            return false;
        if (level == null) {
            if (other.level != null)
                return false;
        } else if (!level.equals(other.level))
            return false;
        if (parm == null) {
            if (other.parm != null)
                return false;
        } else if (!parm.equals(other.parm))
            return false;
        return true;
    }

}
