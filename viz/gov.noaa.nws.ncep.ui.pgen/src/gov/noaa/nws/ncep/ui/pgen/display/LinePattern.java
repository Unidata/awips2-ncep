/*
 * LinePattern
 * 
 * Date created: 25 NOVEMBER 2008
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.display;

import gov.noaa.nws.ncep.ui.pgen.display.ArrowHead.ArrowHeadType;
import gov.noaa.nws.ncep.ui.pgen.display.PatternSegment.PatternType;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.raytheon.uf.common.serialization.ISerializableObject;

/**
 * Defines a specific pattern that can applied to a line path. The Line Pattern
 * is made up a series of PatternSegment objects that can be repeatedly applied
 * to a line path to create the line pattern. Line patterns can have an optional
 * arrow head displayed at the end.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * ????                     J. Wu       Initial Creation.
 * 02/05/2016   R13558      P. Moyer    Dotted line dots not uniform in size.
 * </pre>
 * 
 * @author sgilbert
 * 
 */
@SuppressWarnings("deprecation")
@XmlAccessorOrder(XmlAccessOrder.UNDEFINED)
@XmlAccessorType(XmlAccessType.NONE)
public class LinePattern implements ISerializableObject {

    /**
     * Name given to the line pattern.
     */
    @XmlAttribute(name = "name")
    private String name;

    /**
     * List of segments that, when repeated, define the line pattern.
     */
    @XmlElement(name = "patternSegment")
    private ArrayList<PatternSegment> segments;

    /**
     * Indicates whether the line should have an arrow head at the end
     */
    @XmlAttribute(name = "hasArrowHead")
    private boolean arrowHead;

    /**
     * Type of arrow head. Open or closed.
     */
    @XmlAttribute
    private ArrowHeadType arrowHeadType;

    /**
     * Indicates whether the line pattern has a BLANK or LINE segment
     */
    @XmlAttribute(name = "hasBlankOrLine")
    private boolean blankOrLine;

    /**
     * Basic constructor with an empty PatternSegment arrayList.
     */
    public LinePattern() {
        segments = new ArrayList<PatternSegment>();
        blankOrLine = false;
    }

    /**
     * Constructor to create a LinePattern when all PatternSegments are known.
     * 
     * @param name
     *            Name of the Line Pattern.
     * @param segments
     *            List of Pattern Segments
     * @param arrowHead
     *            True, if an arrow head should be applied to line pattern.
     * @param arrowHeadType
     *            Open or Closed.
     * @param blankOrLine
     *            True, if the line pattern contains a BLANK or LINE Pattern
     *            Segment
     */
    public LinePattern(String name, ArrayList<PatternSegment> segments,
            boolean arrowHead, ArrowHeadType arrowHeadType, boolean blankOrLine) {
        this.name = name;
        this.segments = segments;
        this.arrowHead = arrowHead;
        this.arrowHeadType = arrowHeadType;
        this.blankOrLine = blankOrLine;

        if (this.arrowHeadType == null && this.arrowHead == true)
            this.arrowHeadType = ArrowHeadType.OPEN;
    }

    /**
     * Constructor used create a LinePattern when PatternSegments are not known.
     * Segments can be added in order using the addSegment method.
     * 
     * @param name
     *            Name of the Line Pattern.
     * @param arrowHead
     *            True, if an arrow head should be applied to line pattern.
     * @param arrowHeadType
     *            Open or closed.
     */
    public LinePattern(String name, boolean arrowHead,
            ArrowHeadType arrowHeadType) {

        this(name, new ArrayList<PatternSegment>(), arrowHead, arrowHeadType,
                false);

    }

    /**
     * Gets name of line pattern
     * 
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name of Line Pattern
     * 
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets list of PatternSegments
     * 
     * @return pattern segments used to define this line pattern
     */
    public ArrayList<PatternSegment> getSegments() {
        return segments;
    }

    /**
     * Sets the list of PatternSegments that make up this Line Pattern. Change
     * the state of blankOrLine depending on the new pattern.
     * 
     * @param segments
     */
    public void setSegments(ArrayList<PatternSegment> segments) {
        this.segments = segments;

        this.blankOrLine = false;
        /*
         * Determine if the new Pattern has any blanks or lines.
         */
        for (PatternSegment seg : this.segments) {

            if (seg.getPatternType() == PatternType.BLANK
                    || seg.getPatternType() == PatternType.LINE) {
                this.blankOrLine = true;
            }
        }

    }

    /**
     * Returns whether the stored Line Pattern has any BLANK or LINE Pattern
     * Segments.
     * 
     * @return true or false
     */
    public boolean getBlankOrLine() {
        return blankOrLine;
    }

    /**
     * Sets the value of blank or line. Should not normally be invoked -
     * creation of the object, copying the object, or adding new segments will
     * do the setting of this value instead. Included for completion's sake.
     * 
     * @param blankOrLine
     *            whether the LinePattern has a BLANK or LINE Pattern Segment.
     */
    public void setBlankOrLine(boolean blankOrLine) {
        this.blankOrLine = blankOrLine;
    }

    /**
     * Determines if this line pattern has an arrow head at the end
     * 
     * @return True, if line pattern contains an arrow head.
     */
    public boolean hasArrowHead() {
        return arrowHead;
    }

    /**
     * Sets whether this line pattern includes an arrow head.
     * 
     * @param arrowHead
     *            True, if line pattern should include an arrow head.
     */
    public void setArrowHead(boolean arrowHead) {
        this.arrowHead = arrowHead;
    }

    /**
     * Gets the arrow head type. open or closed.
     * 
     * @return Arrow Head type.
     */
    public ArrowHeadType getArrowHeadType() {
        return arrowHeadType;
    }

    /**
     * Sets type of arrow head for this line pattern
     * 
     * @param arrowHeadType
     */
    public void setArrowHeadType(ArrowHeadType arrowHeadType) {
        this.arrowHeadType = arrowHeadType;
    }

    /**
     * Gets the length of this line pattern
     * 
     * @return pattern length
     */
    public double getLength() {

        double length = 0.0;
        for (PatternSegment seg : segments) {
            length += seg.getLength();
        }
        return length;

    }

    /**
     * Gets the number of PatternSegments in this Line Pattern
     * 
     * @return Number of pattern segments
     */
    public int getNumSegments() {
        return segments.size();
    }

    /**
     * Adds a PatternSegment to the current list of pattern segments for this
     * Line Pattern.
     * 
     * @param ps
     *            Pattern Segment to be added.
     */
    public void addSegment(PatternSegment ps) {
        segments.add(ps);

        // if we're adding a BLANK or LINE segment, change the blankOrLine flag
        // to true. Since you can only add segments, not remove them, we don't
        // need to add other checks elsewhere.
        if (ps.getPatternType() == PatternType.BLANK
                || ps.getPatternType() == PatternType.LINE) {
            blankOrLine = true;
        }
    }

    public double getMaxExtent() {

        double extent = 1.0;
        double temp;

        for (PatternSegment seg : segments) {
            switch (seg.getPatternType()) {
            case CIRCLE:
            case CIRCLE_FILLED:
            case ARC_180_DEGREE:
            case ARC_180_DEGREE_FILLED:
            case ARC_180_DEGREE_CLOSED:
            case ARC_90_DEGREE:
            case ARC_270_DEGREE:
            case ARC_270_DEGREE_WITH_LINE:
            case ARROW_HEAD:
                // extent is the radius = half the segment length
                temp = seg.getLength() * 0.5;
                break;

            case BOX:
            case BOX_FILLED:
            case X_PATTERN:
            case Z_PATTERN:
            case DOUBLE_LINE:
            case TICK:
                // extent is the offset size
                temp = seg.getOffsetSize();
                break;
            default:
                temp = 1.0;
            }

            extent = Math.max(extent, temp);
        }

        return extent;
    }

    /**
     * 
     * @return
     */
    public boolean needsLengthUpdate() {

        if (segments.isEmpty())
            return false;

        for (PatternSegment seg : segments) {
            if (seg.getLength() == 0)
                return true;
        }

        return false;
    }

    /**
     * Sets all zero length segments to the given length
     * 
     * @param length
     * @return new Pattern with the changes
     */
    public LinePattern updateLength(double length) {

        LinePattern lp = new LinePattern(name, arrowHead, arrowHeadType);

        for (PatternSegment seg : segments) {
            PatternSegment newseg = seg.copy();
            if (newseg.getLength() == 0)
                newseg.setLength(length);
            lp.addSegment(newseg);
        }

        return lp;
    }

    /**
     * Toggles the reverseSide flag in each segment
     * 
     * @return new Pattern with changes
     */
    public LinePattern flipSide() {

        LinePattern lp = new LinePattern(name, arrowHead, arrowHeadType);

        for (PatternSegment seg : segments) {
            PatternSegment newseg = seg.copy();
            newseg.setReverseSide(!seg.isReverseSide());
            lp.addSegment(newseg);
        }

        return lp;
    }

    /**
     * Scales the length of each segment proportionally so that they sum to the
     * given length.
     * 
     * @param length
     *            length of the allocated space on the line.
     * @return new Pattern with the changes
     */
    public LinePattern scaleToLength(double length) {

        LinePattern lp = new LinePattern(name, arrowHead, arrowHeadType);

        double scale = length / getLength();

        // if the scale is greater than one (and change) and the pattern
        // contains BLANK or LINE segments that are scalable, then use the
        // Front's method of scaling instead (which leaves symbol parts alone)
        if ((scale > 1.1) && (getBlankOrLine() == true)) {
            return scaleBlankLineToLength(length);
        }

        // otherwise scale everything equally
        for (PatternSegment seg : segments) {
            PatternSegment newseg = seg.copy();
            newseg.setLength(seg.getLength() * scale);
            lp.addSegment(newseg);

        }

        return lp;
    }

    /**
     * Scales the length of each BLANK and LINE segment proportionally so that
     * the sum of all segments is equal to the given length. The length of
     * segments other than BLANK and LINE is not changed
     * 
     * @param length
     * @return new Pattern with the changes
     */
    public LinePattern scaleBlankLineToLength(double length) {

        LinePattern lp = new LinePattern(name, arrowHead, arrowHeadType);

        /*
         * get sum on all BLANK and LINE segments
         */
        double segsum = 0.0;
        for (PatternSegment seg : segments) {
            if (seg.getPatternType() == PatternType.BLANK
                    || seg.getPatternType() == PatternType.LINE) {
                segsum += seg.getLength();
            }
        }

        double scale = 1.0;
        if (segsum > 0.0) {
            scale = (length - getLength()) / segsum;
            scale += 1.0;
        }

        /*
         * scale each BLANK and LINE segment only
         */
        for (PatternSegment seg : segments) {
            PatternSegment newseg = seg.copy();
            if (seg.getPatternType() == PatternType.BLANK
                    || seg.getPatternType() == PatternType.LINE) {
                newseg.setLength(seg.getLength() * scale);
            }
            lp.addSegment(newseg);
        }

        return lp;
    }

}