/*
 * gov.noaa.nws.ncep.ui.pgen.rsc.PgenSinglePointDrawingTool
 *
 * 20 June 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.elements;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;

import java.awt.Color;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Abstract super class for DrawableElement and DECollection
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 06/09        #116        B. Yin      Initial Creation.
 * 06/09        #135        B. Yin      Added a parent field.
 * 04/11        #422        B. Yin      added startTime and endTime
 * 12/14/2015   R13161      S. Russell  renamed isLabeledSymbol to
 *                                      isLabeledSymbolType()
 *                                      added getSymbolLabel()
 *                                      added isASymbolAndHasALabel()
 * 
 * </pre>
 * 
 * 
 * @author B. Yin
 */

public abstract class AbstractDrawableComponent {

    protected String pgenCategory;

    protected String pgenType;

    protected AbstractDrawableComponent parent;

    protected Calendar startTime;

    protected Calendar endtime;

    public abstract void setColors(Color[] colors);

    public abstract List<Coordinate> getPoints();

    public abstract Iterator<DrawableElement> createDEIterator();

    public abstract AbstractDrawableComponent copy();

    public abstract DrawableElement getPrimaryDE();

    public abstract String getName();

    /**
     * @return the pgenCategory
     */
    public String getPgenCategory() {
        return pgenCategory;
    }

    /**
     * @param pgenCategory
     *            the pgenCategory to set
     */
    public void setPgenCategory(String pgenCategory) {
        this.pgenCategory = pgenCategory;
    }

    /**
     * @return the pgenType
     */
    public String getPgenType() {
        return pgenType;
    }

    /**
     * @param pgenType
     *            the pgenType to set
     */
    public void setPgenType(String pgenType) {
        this.pgenType = pgenType;
    }

    /**
     * 
     * @return AbstractDrawableComponent
     */
    public AbstractDrawableComponent getParent() {
        return parent;
    }

    /**
     * Is this object a DECollection with internal name "labeledSymbol". Such
     * collections are used for implementing labeled symbols. The parent
     * DrawableElement will have that name and the symbol information. The child
     * element, a Text object will contain the label
     * 
     * @return boolean
     */

    public boolean isLabeledSymbolType() {
        boolean isLabeledSymbol = false;

        if (parent == null) {
            return false;
        }

        if (parent.getName() == null) {
            return false;
        }

        if (parent.getName().equalsIgnoreCase(PgenConstant.LABELED_SYMBOL)) {
            isLabeledSymbol = true;
        }

        return isLabeledSymbol;
    }

    /**
     * If this object is DEColleciton uses as a labeled symbol, get the label
     * 
     * @return String[] the label of a labeled symbol
     */
    public String[] getSymbolLabel() {
        String[] label = null;

        if (!this.isLabeledSymbolType()) {
            return null;
        }

        Iterator<AbstractDrawableComponent> it = ((DECollection) this.parent)
                .getComponentIterator();

        while (it.hasNext()) {
            AbstractDrawableComponent item = it.next();
            if (item instanceof Text) {
                label = ((Text) item).getString();
                break;
            }
        }

        return label;

    }

    /**
     * Determine if this is a DEColleciton set up to house a labeled symbol and
     * if there is actually a symbol in this DECollection
     * 
     * @return boolean
     */
    public boolean isASymbolAndHasALabel() {
        boolean hasALabel = false;
        String[] label = null;

        if (!this.isLabeledSymbolType()) {
            return hasALabel;
        }

        label = this.getSymbolLabel();

        if (label == null) {
            return hasALabel;
        }

        if (label.length > 0) {
            hasALabel = true;
        }

        return hasALabel;
    }

    /**
     * 
     * @param parent
     */
    public void setParent(AbstractDrawableComponent parent) {
        this.parent = parent;
    }

    /**
     * 
     * @return boolean
     */
    public boolean isEmpty() {
        return false;
    }

    /**
     * 
     * @return String
     */
    public String getForecastHours() {
        return "";
    }

    /**
     * 
     * @return Calendar
     */
    public Calendar getStartTime() {
        return startTime;
    }

    /**
     * 
     * @param startTime
     */
    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    /**
     * 
     * @return Calendar
     */
    public Calendar getEndtime() {
        return endtime;
    }

    /**
     * 
     * @param endtime
     */
    public void setEndtime(Calendar endtime) {
        this.endtime = endtime;
    }
}
