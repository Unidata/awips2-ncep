/*
 * ComboSymbol
 *
 * Date created: 02 JUNE 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.elements;

import java.awt.Color;

import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.annotation.ElementOperations;
import gov.noaa.nws.ncep.ui.pgen.annotation.Operation;
import gov.noaa.nws.ncep.ui.pgen.display.ICombo;

/**
 * Class to represent a combo symbol element.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * -----------  -------     ----------  ------------------------------
 * 06/09                    S. Gilbert  Initial Creation.
 * Sep 9, 2019  7596        tgurney     Change SinglePointElement field access
 * </pre>
 *
 * @author J. Wu
 */
@ElementOperations({ Operation.COPY_MOVE, Operation.EXTRAPOLATE })
public class ComboSymbol extends SinglePointElement implements ICombo {

    /**
     * Default constructor
     */
    public ComboSymbol() {
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
    public ComboSymbol(Coordinate[] range, Color[] colors, float lineWidth,
            double sizeScale, Boolean clear, Coordinate location,
            String pgenCategory, String pgenType) {
        super(range, colors, lineWidth, sizeScale, clear, location,
                pgenCategory, pgenType);
    }

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
        ComboSymbol newSymbol = new ComboSymbol();
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

        return newSymbol;

    }

    /**
     * Returns an array of Symbol patterns names. The pattern names are in the
     * pgenType attribute with a vertical bar "|" as a delimiter.
     */
    @Override
    public String[] getPatternNames() {

        // new String[] names =
        String type = getPgenType();
        return type.split("\\|");

    }

}
