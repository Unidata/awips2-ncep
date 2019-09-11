/*
 * Symbol
 *
 * Date created: 15 January 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.elements;

import java.awt.Color;

import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.annotation.ElementOperations;
import gov.noaa.nws.ncep.ui.pgen.annotation.Operation;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.display.ISymbol;

/**
 * Class to represent a symbol element.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date           Ticket#     Engineer    Description
 * ------------   ----------  ----------- --------------------------
 * 01/09                      J. Wu       Initial Creation.
 * 05/09          #42         S. Gilbert  Added pgenType and pgenCategory to constructors and copy()
 * 04/11          #?          B. Yin      Re-factor IAttribute
 * Sep 9, 2019    7596        tgurney     Change SinglePointElement field access
 * </pre>
 *
 * @author J. Wu
 */
@ElementOperations({ Operation.COPY_MOVE, Operation.EXTRAPOLATE })
public class Symbol extends SinglePointElement implements ISymbol {

    /**
     * Default constructor
     */
    public Symbol() {
    }

    /**
     * @param deleted
     * @param range
     * @param colors
     * @param lineWidth
     * @param sizeScale
     * @param clear
     * @param location
     * @param type
     */
    public Symbol(Coordinate[] range, Color[] colors, float lineWidth,
            double sizeScale, Boolean clear, Coordinate location,
            String pgenCategory, String pgenType) {
        super(range, colors, lineWidth, sizeScale, clear, location,
                pgenCategory, pgenType);
    }

    /**
     * Sets the
     */
    @Override
    public void update(IAttribute attr) {
        super.update(attr);
    }

    /**
     * @return the string
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(getClass().getSimpleName());

        result.append("Category:\t" + pgenCategory + "\n");
        result.append("Type:\t" + pgenType + "\n");
        result.append("Location:\t" + getLocation().x + "\t" + getLocation().y
                + "\n");
        result.append("Color:\t" + getColors()[0] + "\n");
        result.append("LineWidth:\t" + getLineWidth() + "\n");
        result.append("SizeScale:\t" + getSizeScale() + "\n");
        result.append("Clear:\t" + isClear() + "\n");

        return result.toString();
    }

    /**
     * Creates a copy of this object. This is a deep copy and new objects are
     * created so that we are not just copying references of objects
     */
    @Override
    public DrawableElement copy() {

        /*
         * create a new Symbol object and initially set its attributes to this
         * one's
         */
        Symbol newSymbol = new Symbol();
        newSymbol.update(this);

        /*
         * Set new Color, Strings and Coordinate so that we don't just set
         * references to this object's attributes.
         */
        newSymbol
                .setColors(new Color[] { new Color(this.getColors()[0].getRed(),
                        this.getColors()[0].getGreen(),
                        this.getColors()[0].getBlue()) });
        newSymbol.setLocation(new Coordinate(this.getLocation()));
        newSymbol.setPgenCategory(new String(this.getPgenCategory()));
        newSymbol.setPgenType(new String(this.getPgenType()));
        newSymbol.setParent(this.getParent());

        return newSymbol;

    }

    @Override
    public String getPatternName() {
        return getPgenType();
    }

}
