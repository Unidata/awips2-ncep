package gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels;

import org.eclipse.swt.graphics.RGB;

/**
 * SymbolKey
 * 
 * Class which simply collects those symbol parameters that must have equal
 * values across a single PGEN SymbolLocationSet (one symbol copied at many
 * geographic locations), to be used as a map key to a list of associated
 * locations.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 11/18/2015    R9579     bhebbard    Initial creation
 * 
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */
class SymbolKey {

    public SymbolKey(String symbolPatternName, boolean symbolIsAMarker,
            RGB rgb, float size, float width) {
        this.symbolPatternName = symbolPatternName;
        this.symbolIsAMarker = symbolIsAMarker;
        this.rgb = rgb;
        this.size = size;
        this.width = width;
    }

    String symbolPatternName = null;

    boolean symbolIsAMarker = false;

    RGB rgb = null;

    float size = 0.0f;

    float width = 0.0f;

    // Need hashCode()/equals() since this class serves as a map key
    // (otherwise equals() would use object identity, defeating the purpose
    // of the map)

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((rgb == null) ? 0 : rgb.hashCode());
        result = prime * result + Float.floatToIntBits(size);
        result = prime * result + (symbolIsAMarker ? 1231 : 1237);
        result = prime
                * result
                + ((symbolPatternName == null) ? 0 : symbolPatternName
                        .hashCode());
        result = prime * result + Float.floatToIntBits(width);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SymbolKey)) {
            return false;
        }
        SymbolKey other = (SymbolKey) obj;
        if (rgb == null) {
            if (other.rgb != null) {
                return false;
            }
        } else if (!rgb.equals(other.rgb)) {
            return false;
        }
        if (Float.floatToIntBits(size) != Float.floatToIntBits(other.size)) {
            return false;
        }
        if (symbolIsAMarker != other.symbolIsAMarker) {
            return false;
        }
        if (symbolPatternName == null) {
            if (other.symbolPatternName != null) {
                return false;
            }
        } else if (!symbolPatternName.equals(other.symbolPatternName)) {
            return false;
        }
        if (Float.floatToIntBits(width) != Float.floatToIntBits(other.width)) {
            return false;
        }
        return true;
    }
}