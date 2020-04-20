package gov.noaa.nws.ncep.viz.ui.display;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.measure.unit.Unit;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences.DataMappingEntry;
import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.style.image.ImagePreferences;
import com.raytheon.uf.common.style.image.NumericFormat;
import com.raytheon.uf.common.style.image.SampleFormat;

import gov.noaa.nws.ncep.gempak.parameters.colorbar.ColorBarAnchorLocation;
import gov.noaa.nws.ncep.gempak.parameters.colorbar.ColorBarAttributesBuilder;
import gov.noaa.nws.ncep.gempak.parameters.colorbar.ColorBarOrientation;
import gov.noaa.nws.ncep.viz.common.RGBColorAdapter;

/**
 * An ColorBar initialized from a ColorMap.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * 04/14/10      259      Greg Hull Initial Creation.
 * 10/25/11      463      qzhou     Added equals()
 * 12/06/11               qzhou     Modified equals and added hashCode
 * 06/07/12      717      Archana   Added imagePreferences to store the label
 *                                  information.
 *                                  Added the method scalePixelValues() to
 *                                  scale the pixel values whenever the
 *                                  pixel values exceeded the actual number
 *                                  of colors in the color map.
 *                                  Added numPixelsToReAlignLabel to position
 *                                  the label at the beginning or at the middle
 *                                  of the color interval.
 *
 * 06/07/12      794      Archana   Added a Boolean flag called reverseOrder to
 *                                  enable/disable reversing the order of colors
 *                                  in the color-bar.
 *
 * 06/18/12      743      Archana   Added attributes to implement GEMPAK's
 *                                  CLRBAR parameter:
 *                                     xPixelCoordFraction,
 *                                     yPixelCoordFraction,
 *                                     drawColorBar,
 *                                     isDrawBoxAroundColorBar.
 *                                  Added the corresponding setter/getter methods
 *                                  Added setAttributesFromColorBarAttributesBuilder()
 * 07/18/12      717      Archana   Refactored numPixelsToReAlignLabel to
 *                                  alignLabelInTheMiddleOfInterval and added
 *                                  the corresponding setter/getter methods
 * 11/18/13      1059     G. Hull   don't marshal displayUnitsStr
 * Mar 20, 2019  7569     tgurney   getLabelString() - allow arg greater
 *                                  than the number of intervals
 * Apr 20, 2020  8145     randerso  Replace SamplePreferences with SampleFormat
 *
 * </pre>
 *
 * @author ghull
 */

@XmlAccessorType(XmlAccessType.NONE)
public class ColorBarFromColormap implements IColorBar, ISerializableObject {

    @XmlElement
    private ColorBarOrientation orientation = ColorBarOrientation.Vertical;

    @XmlElement
    private ColorBarAnchorLocation anchorLoc = ColorBarAnchorLocation.LowerLeft;

    // @XmlElement
    // private String colorMapName = null;

    // @XmlElement
    private ColorMap colorMap = null;

    // if we let the user edit the colormap then the IntervalColors will
    // be used to create a new colormap instead of reading the colorMapName
    //
    // @XmlElement
    private Boolean colorMapModified = false;

    @XmlElement
    @XmlJavaTypeAdapter(RGBColorAdapter.class)
    private RGB labelColor = new RGB(255, 255, 255);

    @XmlElement(name = "pixelRGBs")
    @XmlJavaTypeAdapter(RGBColorAdapter.class)
    private final List<RGB> pixelRGBs = new ArrayList<>();

    private final List<Color> colors = new ArrayList<>();

    private Unit<?> dataUnits;

    @XmlElement
    private Boolean showLabels = true;

    @XmlElement
    private Boolean reverseOrder = true;

    // don't marshal since the value (currently) is set by either the Record or
    // the Style Rules.
    private String displayUnitStr = null;

    private static final Float defaultLength = .5f;

    // change to 0.01
    private static final int defaultWidth = 10;

    @XmlElement
    // as a ratio of the screen
    private Float lengthAsRatio = defaultLength;

    @XmlElement
    // in pixels ?
    private Integer widthInPixels = defaultWidth;

    /**
     * the Display used to create the Colors in the intervals
     */
    private Display display = null;

    private ImagePreferences imagePreferences = null;

    private Boolean isScalingAttemptedForThisColorMap = false;

    private ColorBarAttributesBuilder colorBarAttributesBuilder;

    @XmlElement
    private Boolean drawColorBar = true;

    @XmlElement
    private Boolean drawBoxAroundColorBar = true;

    private double xPixelCoordFraction = 0.005;

    private double yPixelCoordFraction = 0.05;

    /**
     * @param isScalingAttemptedForThisColorMap
     *            the isScalingAttemptedForThisColorMap to set
     */
    public final void setIsScalingAttemptedForThisColorMap(
            Boolean isScalingAttemptedForThisColorMap) {
        this.isScalingAttemptedForThisColorMap = isScalingAttemptedForThisColorMap;
    }

    /**
     * @return the isScalingAttemptedForThisColorMap
     */
    public final boolean isScalingAttemptedForThisColorMap() {
        return isScalingAttemptedForThisColorMap.booleanValue();
    }

    private boolean alignLabelInTheMiddleOfInterval;

    @Override
    public void setAlignLabelInTheMiddleOfInterval(boolean b) {
        alignLabelInTheMiddleOfInterval = b;
    }

    public ColorBarFromColormap() {
    }

    public ColorBarFromColormap(ColorBarFromColormap cbar) {
        if (cbar == null) {
            return;
        }

        if (cbar.imagePreferences != null) {
            imagePreferences = cbar.imagePreferences;
        } else {
            imagePreferences = new ImagePreferences();
        }
        anchorLoc = cbar.anchorLoc;
        orientation = cbar.orientation;
        labelColor = cbar.labelColor;
        dataUnits = cbar.dataUnits;
        showLabels = cbar.showLabels;
        lengthAsRatio = cbar.lengthAsRatio;
        widthInPixels = cbar.widthInPixels;
        reverseOrder = cbar.reverseOrder;
        alignLabelInTheMiddleOfInterval = cbar.alignLabelInTheMiddleOfInterval;
        isScalingAttemptedForThisColorMap = cbar.isScalingAttemptedForThisColorMap;
        colorBarAttributesBuilder = cbar.colorBarAttributesBuilder;
        alignLabelInTheMiddleOfInterval = cbar.alignLabelInTheMiddleOfInterval;
        if (cbar.displayUnitStr != null) {
            displayUnitStr = new String(cbar.displayUnitStr);
        }
        colorMap = null;
        if (cbar.getColorMap() != null) {
            setColorMap(cbar.getColorMap());
        }

    }

    public ColorMap getColorMap() {
        return colorMap;
    }

    public boolean setColorMap(ColorMap cm) {
        if (colorMap != null) {
            // cleanup.....
            pixelRGBs.clear();

            for (Color clr : colors) {
                clr.dispose();
            }
            colors.clear();
        }
        colorMap = cm;
        if (colorMap == null || colorMap.getSize() == 0) {
            return false;
        }

        float[] reds = colorMap.getRed();
        float[] greens = colorMap.getGreen();
        float[] blues = colorMap.getBlue();

        // interval units are pixels
        dataUnits = null;
        if (display == null) {
            display = NcDisplayMngr.getActiveNatlCntrsEditor()
                    .getActiveDisplayPane().getDisplay();
        }

        for (int c = 0; c < reds.length; c++) {
            RGB rgb = new RGB(scaleCmapValue(reds[c]),
                    scaleCmapValue(greens[c]), scaleCmapValue(blues[c]));
            pixelRGBs.add(rgb);
            colors.add(new Color(display, rgb));
        }

        return true;
    }

    public void scalePixelValues() {
        if (colorMap != null && imagePreferences != null) {
            SampleFormat sampleFormat = imagePreferences.getSampleFormat();
            if (sampleFormat instanceof NumericFormat) {
                Double maxValue = ((NumericFormat) sampleFormat).getMaxValue();
                if (maxValue != null && maxValue != 0 && !maxValue.isNaN()
                        && maxValue > colorMap.getSize()) {
                    double scalingFactor = (int) Math
                            .round(maxValue / colorMap.getSize());
                    DataMappingPreferences dmPref = imagePreferences
                            .getDataMapping();
                    if (dmPref != null) {
                        List<DataMappingEntry> dmEntriesList = dmPref
                                .getEntries();
                        if (dmEntriesList != null && !dmEntriesList.isEmpty()) {
                            isScalingAttemptedForThisColorMap = true;
                            for (DataMappingEntry dmEntry : dmEntriesList) {
                                double thisPixVal = dmEntry.getPixelValue()
                                        .doubleValue();

                                double scaledPixVal = Math.round(thisPixVal);

                                if (thisPixVal >= scalingFactor) {
                                    scaledPixVal = Math
                                            .round(thisPixVal / scalingFactor);

                                    if (scaledPixVal > colorMap.getSize() - 1) {
                                        scaledPixVal = colorMap.getSize() - 1;
                                    }

                                    dmEntry.setPixelValue(scaledPixVal);

                                }

                            }

                            DataMappingEntry[] dmEntryArray = new DataMappingEntry[dmEntriesList
                                    .size()];
                            dmEntriesList.toArray(dmEntryArray);
                            dmPref.setSerializableEntries(dmEntryArray);
                            imagePreferences.setDataMapping(dmPref);
                        }
                    }
                }

            }
        }
    }

    public int scaleCmapValue(float cmapVal) {
        int rgbVal = (int) (cmapVal * 256);
        return rgbVal < 0 ? 0 : rgbVal > 255 ? 255 : rgbVal;
    }

    @Override
    public int getNumIntervals() {
        return colorMap != null ? colorMap.getSize() : 0;
    }

    @Override
    public RGB getLabelColor() {
        return labelColor;
    }

    @Override
    public void setLabelColor(RGB labelColor) {
        this.labelColor = labelColor;
    }

    @Override
    public Boolean getShowLabels() {
        return showLabels;
    }

    @Override
    public void setShowLabels(Boolean showLabels) {
        this.showLabels = showLabels;
    }

    @Override
    public Boolean getDrawToScale() {
        return false;
    }

    @Override
    public void setDrawToScale(Boolean drawToScale) {
    }

    @Override
    public ColorBarOrientation getOrientation() {
        return orientation;
    }

    @Override
    public ColorBarAnchorLocation getAnchorLoc() {
        return anchorLoc;
    }

    @Override
    public int getNumDecimals() {
        return 0;
    }

    @Override
    public void setNumDecimals(int numDecimals) {
    }

    // This doesn't really have a meaning for image colorbars
    @Override
    public void addColorBarInterval(Float min, Float max, RGB rgb) {
    }

    // Methods to get/set values for the colorBar pixels/intervals and colors

    @Override
    public RGB getRGB(int p) {
        return p < pixelRGBs.size() ? pixelRGBs.get(p) : null;
    }

    @Override
    public void setRGB(int c, RGB rgb) {
        if (c < 0 || c >= getNumIntervals()) {
            return;
        }
        colorMapModified = true;

        pixelRGBs.set(c, rgb);

        if (display != null) {
            colors.get(c).dispose();
            colors.set(c, new Color(display, rgb));
        }
        colorMap = null;
    }

    @Override
    public Float getIntervalMin(int c) {
        return (float) c;
    }

    @Override
    public Float getIntervalMax(int c) {
        return (float) (c + 1);
    }

    @Override
    public boolean isValueInInterval(int c, Float value, Unit<?> units) {
        // no-op for images
        return false;
    }

    @Override
    public void setIntervalMin(int c, Float min) {
    }

    @Override
    public void setIntervalMax(int c, Float max) {
    }

    // if numIntervals is passed in we will return the max value of the last
    // interval
    @Override
    public String getLabelString(int i) {
        if (!showLabels || imagePreferences == null
                || imagePreferences.getDataMapping() == null) {
            return null;
        }

        return imagePreferences.getDataMapping().getLabelValueForDataValue(i);

    }

    @Override
    public void createNewInterval(int c) {
    }

    @Override
    public void removeInterval(int c) {
    }

    // all the Colors in the intervals list will have this same device
    @Override
    public void setColorDevice(Display disp) {
        if (display != disp) {
            display = disp;

            for (Color c : colors) {
                c.dispose();
            }
            colors.clear();

            for (int i = 0; i < getNumIntervals(); i++) {
                colors.add(new Color(display, pixelRGBs.get(i)));
            }
        }
    }

    @Override
    public void dispose() {
        if (display != null) {
            display = null;

            for (Color c : colors) {
                c.dispose();
            }
            colors.clear();
        }

        showLabels = false;
        imagePreferences = null;
        displayUnitStr = null;
    }

    public boolean unlabelPixel(int p) {

        if (imagePreferences == null) {
            return false;
        }

        DataMappingPreferences dmPref = imagePreferences.getDataMapping();
        if (dmPref != null) {
            List<DataMappingEntry> dmEntriesList = dmPref.getEntries();
            if (dmEntriesList != null && !dmEntriesList.isEmpty()) {
                Iterator<DataMappingEntry> itr = dmEntriesList.iterator();

                while (itr.hasNext()) {
                    if (itr.next().equals(new Double(p))) {
                        itr.remove();
                        break;
                    }
                }
            }
        }
        return false;
    }

    public boolean isPixelLabeled(int p) {
        if (imagePreferences == null) {
            return false;
        }

        DataMappingPreferences dmPref = imagePreferences.getDataMapping();

        if (dmPref == null) {
            return false;
        }

        return dmPref.getLabelValueForDataValue(p) != null;
    }

    @Override
    public float getDiscreteRange() {
        return getNumIntervals();
    }

    @Override
    public Color getColor(int intrvl) {
        if (display == null) {
            return null;
        }

        if (intrvl < getNumIntervals()) {
            // if for some reason there are fewer colors than intervals then
            // fill in the colors
            // array from the rgb array
            while (colors.size() < getNumIntervals()) {
                colors.add(new Color(display, getRGB(colors.size())));
            }
            return colors.get(intrvl);
        }
        return null;
    }

    @Override
    public Unit<?> getDataUnits() {
        return null;
    }

    @Override
    public Float getLengthAsRatio() {
        return lengthAsRatio;
    }

    @Override
    public Integer getWidthInPixels() {
        return widthInPixels;
    }

    /**
     * For a pixel p, applies the corresponding label lblStr
     */
    @Override
    public void labelInterval(int p, String lblStr) {
        if (lblStr != null) {
            labelPixel(p, lblStr);
        } else {
            unlabelPixel(p);
        }
    }

    public void labelPixel(int p, String lblStr) {

        if (lblStr == null || imagePreferences == null) {
            return;
        }

        DataMappingPreferences dmPref = imagePreferences.getDataMapping();
        if (dmPref != null) {
            List<DataMappingEntry> dmEntriesList = dmPref.getEntries();
            if (dmEntriesList != null && !dmEntriesList.isEmpty()) {
                if (!dmEntriesList.contains(new Double(p))) {
                    DataMappingEntry dmEntry = new DataMappingEntry();
                    dmEntry.setPixelValue((double) p);
                    dmEntry.setLabel(lblStr);
                    dmEntriesList.add(dmEntry);
                    DataMappingEntry[] entryArray = new DataMappingEntry[dmEntriesList
                            .size()];
                    dmEntriesList.toArray(entryArray);
                    dmPref.setSerializableEntries(entryArray);
                    imagePreferences.setDataMapping(dmPref);
                }
            }
        }

    }

    @Override
    public boolean isIntervalLabeled(int intrvl) {
        return isPixelLabeled(intrvl);
    }

    @Override
    public void setDataUnits(Unit<?> dataUnits) {

    }

    @Override
    public void setLengthAsRatio(Float l) {
        lengthAsRatio = l;
    }

    @Override
    public void setWidthInPixels(Integer w) {
        widthInPixels = w;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (anchorLoc == null ? 0 : anchorLoc.hashCode());
        result = prime * result + (colorMap == null ? 0 : colorMap.hashCode());
        result = prime * result
                + (colorMapModified == null ? 0 : colorMapModified.hashCode());
        result = prime * result + (colors == null ? 0 : colors.hashCode());
        result = prime * result
                + (dataUnits == null ? 0 : dataUnits.hashCode());
        result = prime * result
                + (labelColor == null ? 0 : labelColor.hashCode());
        result = prime * result
                + (lengthAsRatio == null ? 0 : lengthAsRatio.hashCode());
        result = prime * result
                + (orientation == null ? 0 : orientation.hashCode());
        result = prime * result
                + (pixelRGBs == null ? 0 : pixelRGBs.hashCode());
        result = prime * result
                + (showLabels == null ? 0 : showLabels.hashCode());
        result = prime * result
                + (widthInPixels == null ? 0 : widthInPixels.hashCode());
        result = prime * result
                + (imagePreferences == null ? 0 : imagePreferences.hashCode());

        result = prime * result
                + (reverseOrder == null ? 0 : reverseOrder.hashCode());

        result = prime * result + (isScalingAttemptedForThisColorMap == null ? 0
                : isScalingAttemptedForThisColorMap.hashCode());

        result = prime * result + (colorBarAttributesBuilder == null ? 0
                : colorBarAttributesBuilder.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }

        ColorBarFromColormap other = (ColorBarFromColormap) obj;
        if (anchorLoc != other.anchorLoc) {
            return false;
        }
        if (colorMap == null) {
            if (other.colorMap != null) {
                return false;
            }
        } else if (!colorMap.equals(other.colorMap)) {
            return false;
        }
        if (colorMapModified == null) {
            if (other.colorMapModified != null) {
                return false;
            }
        } else if (!colorMapModified.equals(other.colorMapModified)) {
            return false;
        }
        if (colors == null) {
            if (other.colors != null) {
                return false;
            }
        } else if (!colors.equals(other.colors)) {
            return false;
        }
        if (dataUnits == null) {
            if (other.dataUnits != null) {
                return false;
            }
        } else if (!dataUnits.equals(other.dataUnits)) {
            return false;
        }
        if (labelColor == null) {
            if (other.labelColor != null) {
                return false;
            }
        } else if (!labelColor.equals(other.labelColor)) {
            return false;
        }
        if (lengthAsRatio == null) {
            if (other.lengthAsRatio != null) {
                return false;
            }
        } else if (!lengthAsRatio.equals(other.lengthAsRatio)) {
            return false;
        }
        if (orientation != other.orientation) {
            return false;
        }
        if (pixelRGBs == null) {
            if (other.pixelRGBs != null) {
                return false;
            }
        } else if (!pixelRGBs.equals(other.pixelRGBs)) {
            return false;
        }
        if (showLabels == null) {
            if (other.showLabels != null) {
                return false;
            }
        } else if (!showLabels.equals(other.showLabels)) {
            return false;
        }
        if (widthInPixels == null) {
            if (other.widthInPixels != null) {
                return false;
            }
        } else if (!widthInPixels.equals(other.widthInPixels)) {
            return false;
        }

        if (imagePreferences == null) {
            if (other.imagePreferences != null) {
                return false;
            }
        } else if (!imagePreferences.equals(other.imagePreferences)) {
            return false;
        }

        if (reverseOrder == null) {
            if (other.reverseOrder != null) {
                return false;
            }
        } else if (!reverseOrder.equals(other.reverseOrder)) {
            return false;
        }

        if (isScalingAttemptedForThisColorMap == null) {
            if (other.isScalingAttemptedForThisColorMap != null) {
                return false;
            }
        } else if (!isScalingAttemptedForThisColorMap
                .equals(other.isScalingAttemptedForThisColorMap)) {
            return false;
        }

        if (colorBarAttributesBuilder == null) {
            if (other.colorBarAttributesBuilder != null) {
                return false;
            }
        } else if (!colorBarAttributesBuilder
                .equals(other.colorBarAttributesBuilder)) {
            return false;
        }

        return true;
    }

    /**
     * @param imagePreferences
     *            the imagePreferences to set
     */
    public void setImagePreferences(ImagePreferences imgPref) {
        this.imagePreferences = imgPref;
    }

    /**
     * @return the imagePreferences
     */
    public ImagePreferences getImagePreferences() {
        return imagePreferences;
    }

    /**
     * @return the reverseOrder
     */
    @Override
    public Boolean getReverseOrder() {
        return reverseOrder;
    }

    /**
     * @param reverseOrder
     *            the reverseOrder to set
     */

    @Override
    public void setReverseOrder(Boolean reverseOrder) {
        this.reverseOrder = reverseOrder;
    }

    /**
     * @param displayUnitStr
     *            the displayUnitStr to set
     */
    public void setDisplayUnitStr(String displayUnitStr) {
        this.displayUnitStr = displayUnitStr;
    }

    /**
     * @return the displayUnitStr
     */
    @Override
    public String getDisplayUnitStr() {
        return displayUnitStr;
    }

    @Override
    public boolean isAlignLabelInTheMiddleOfInterval() {
        return alignLabelInTheMiddleOfInterval;
    }

    public void setNumPixelsToReAlignLabel(boolean n) {
        alignLabelInTheMiddleOfInterval = n;
    }

    /**
     * @return the colorBarAttributesBuilder
     */
    @Override
    public final ColorBarAttributesBuilder getColorBarAttributesBuilder() {
        return colorBarAttributesBuilder;
    }

    /**
     * @param colorBarAttributesBuilder
     *            the colorBarAttributesBuilder to set
     */
    @Override
    public final void setColorBarAttributesBuilder(
            ColorBarAttributesBuilder colorBarAttributesBuilder) {
        this.colorBarAttributesBuilder = colorBarAttributesBuilder;
    }

    @Override
    public void setOrientation(ColorBarOrientation orientation) {
        this.orientation = orientation;

    }

    @Override
    public void setAnchorLoc(ColorBarAnchorLocation anchorLoc) {
        this.anchorLoc = anchorLoc;
    }

    @Override
    public void setAttributesFromColorBarAttributesBuilder(
            ColorBarAttributesBuilder colorBarAttributesBuilder) {
        /*
         * To be used when the GEMPAK parameter IMCBAR is implemented
         */
        if (colorBarAttributesBuilder != null) {

            setLengthAsRatio((float) colorBarAttributesBuilder.getLength());
            setWidthInPixels(new Integer(
                    (int) colorBarAttributesBuilder.getWidth() * 1000));
            setAnchorLoc(colorBarAttributesBuilder.getAnchorLocation());
            setOrientation(colorBarAttributesBuilder.getColorBarOrientation());

            setXPixelCoordFraction(colorBarAttributesBuilder.getX());
            setYPixelCoordFraction(colorBarAttributesBuilder.getY());
            setLabelColor(colorBarAttributesBuilder.getColor());
            setDrawBoxAroundColorBar(
                    colorBarAttributesBuilder.isDrawBoxAroundColorBar());
            setDrawColorBar(colorBarAttributesBuilder.isDrawColorBar());
        }
    }

    /**
     * @return the drawColorBar
     */
    @Override
    public final Boolean isDrawColorBar() {
        return drawColorBar;
    }

    /**
     * @param drawColorBar
     *            the drawColorBar to set
     */
    @Override
    public final void setDrawColorBar(Boolean drawColorBar) {
        this.drawColorBar = drawColorBar;
    }

    /**
     * @return the drawBoxAroundColorBar
     */
    @Override
    public final Boolean isDrawBoxAroundColorBar() {
        return drawBoxAroundColorBar;
    }

    /**
     * @param drawBoxAroundColorBar
     *            the drawBoxAroundColorBar to set
     */
    @Override
    public final void setDrawBoxAroundColorBar(Boolean drawBoxAroundColorBar) {
        this.drawBoxAroundColorBar = drawBoxAroundColorBar;
    }

    /**
     * @return the xPixelCoordFraction
     */
    @Override
    public final double getXPixelCoordFraction() {
        return xPixelCoordFraction;
    }

    /**
     * @param xPixelCoordFraction
     *            the xPixelCoordFraction to set
     */
    @Override
    public final void setXPixelCoordFraction(double xPixelCoordFraction) {
        this.xPixelCoordFraction = xPixelCoordFraction;
    }

    /**
     * @return the yPixelCoordFraction
     */
    @Override
    public final double getYPixelCoordFraction() {
        return yPixelCoordFraction;
    }

    /**
     * @param yPixelCoordFraction
     *            the yPixelCoordFraction to set
     */
    @Override
    public final void setYPixelCoordFraction(double yPixelCoordFraction) {
        this.yPixelCoordFraction = yPixelCoordFraction;
    }

    @Override
    public void removeAllLabels() {
        if (imagePreferences == null) {
            return;
        }

        DataMappingPreferences dmPref = imagePreferences.getDataMapping();

        if (dmPref == null) {
            return;
        }

        dmPref.getEntries().clear();

    }

}
