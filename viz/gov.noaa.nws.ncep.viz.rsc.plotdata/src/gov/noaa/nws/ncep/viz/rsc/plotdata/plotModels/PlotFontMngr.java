package gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels;

import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.rsc.plotdata.rsc.Tracer;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IFont.FontType;
import com.raytheon.uf.viz.core.drawables.IFont.Style;

/**
 * Plot Font Manager (PlotFontMngr)
 * 
 * Class which manages the fonts used to draw text objects around a station plot
 * by the Point Data Display (plotdata) resource. It is used only by the
 * NcPlotImageCreator class.
 * 
 * Caching: For each font family-size-style combination that is requested, we
 * create exactly one Font object, and only when first requested. We return the
 * same Font object for all subsequent requests for the same family-size-style
 * triplet.
 * 
 * Cleanup: All allocated Font objects are disposed when the PlotFontMngr is
 * disposed. (Callers should not dispose of the fonts themselves.)
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 11/17/2015    R9579     bhebbard    Initial creation
 * 06/30/2016    R16129    bsteffen    Make maps non-static
 * 
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */
public class PlotFontMngr {

    private final static String COURIER = "Courier";

    private final static String TIMES = "Times";

    private final static String HELVETICA = "Helvetica";

    private final static String NORMAL = "Normal";

    private final static String BOLD = "Bold";

    private final static String ITALIC = "Italic";

    private final static String BOLD_ITALIC = "Bold-Italic";

    private final static Style[] NORMAL_STYLE = null;

    private final static Style[] BOLD_STYLE = new Style[] { Style.BOLD };

    private final static Style[] ITALIC_STYLE = new Style[] { Style.ITALIC };

    private final static Style[] BOLD_ITALIC_STYLE = new Style[] { Style.BOLD,
            Style.ITALIC };

    private Map<String, IFont> fontNameToFontMap = null;

    private Map<String, FileFontEnum> fontNameToEnumMap = null;

    /**
     * Enumeration of the fonts that, if requested, we want to 'intercept' and
     * create from specific font files and related attributes. (Fonts that are
     * not in this set are system fonts, which will be passed through to the
     * runtime environment for creation by name.)
     */
    public static enum FileFontEnum {

        // @formatter:off
        
        COURIER_NORMAL_FONT             (COURIER,   NORMAL,      "cour.pfa",     FontType.TYPE1),
        COURIER_BOLD_FONT               (COURIER,   BOLD,        "cour.pfa",     FontType.TYPE1), 
        COURIER_ITALIC_FONT             (COURIER,   ITALIC,      "cour.pfa",     FontType.TYPE1), 
        COURIER_BOLD_ITALIC_FONT        (COURIER,   BOLD_ITALIC, "cour.pfa",     FontType.TYPE1), 

        TIMES_LIKE_NORMAL_FONT          (TIMES,     NORMAL,      "VeraSe.ttf",   FontType.TRUETYPE),
        TIMES_LIKE_BOLD_FONT            (TIMES,     BOLD,        "l049016t.pfa", FontType.TYPE1), 
        TIMES_LIKE_ITALIC_FONT          (TIMES,     ITALIC,      "l049033t.pfa", FontType.TYPE1),
        TIMES_LIKE_BOLD_ITALIC_FONT     (TIMES,     BOLD_ITALIC, "l049036t.pfa", FontType.TYPE1), 

        HELVETICA_LIKE_NORMAL_FONT      (HELVETICA, NORMAL,      "luxisr.ttf",   FontType.TRUETYPE),
        HELVETICA_LIKE_BOLD_FONT        (HELVETICA, BOLD,        "luxisb.ttf",   FontType.TRUETYPE), 
        HELVETICA_LIKE_ITALIC_FONT      (HELVETICA, ITALIC,      "luxisri.ttf",  FontType.TRUETYPE),
        HELVETICA_LIKE_BOLD_ITALIC_FONT (HELVETICA, BOLD_ITALIC, "luxisbi.ttf",  FontType.TRUETYPE);
        
        private String   fontName        = null;
        private Style[]  fontStyles      = null;
        private String   fontFileName    = null;
        private FontType fontType        = null;
        
        // FileFontEnum Constructor:  Set up parameters to be used in subsequent construction of each Font
        private FileFontEnum (String fontFamily, String fontStyle, String fontFileName, FontType fontType) {
            this.fontName        = fontFamily + "-" + fontStyle;
            this.fontStyles      =
                    fontStyle.equals(NORMAL)      ? NORMAL_STYLE :
                    fontStyle.equals(BOLD)        ? BOLD_STYLE :
                    fontStyle.equals(ITALIC)      ? ITALIC_STYLE :
                    fontStyle.equals(BOLD_ITALIC) ? BOLD_ITALIC_STYLE : 
                    null;
            this.fontFileName    = fontFileName;
            this.fontType        = fontType;
        }
        // @formatter:on

        public String getFontName() {
            return fontName;
        }

        public Style[] getFontStyles() {
            return fontStyles;
        }

        public File getFontFile() {
            return NcPathManager.getInstance()
                    .getStaticFile(
                            NcPathManager.NcPathConstants.FONT_FILES_DIR
                                    + fontFileName);
        }

        public FontType getFontType() {
            return fontType;
        }
    } // end FileFontEnum

    /**
     * Constructs and initializes a plot font manager object
     */
    public PlotFontMngr() {
        fontNameToFontMap = new HashMap<String, IFont>();
        fontNameToEnumMap = new HashMap<String, FileFontEnum>(
                FileFontEnum.values().length);
        for (FileFontEnum f : FileFontEnum.values()) {
            fontNameToEnumMap.put(f.getFontName(), f);
        }
    }

    /**
     * Disposes of the plot font manager, and all Font objects it has cached
     */
    public void dispose() {
        for (IFont font : fontNameToFontMap.values()) {
            if (font != null) {
                font.dispose();
            }
        }
        fontNameToFontMap.clear();
        fontNameToEnumMap.clear();
    }

    /**
     * Requests an font of the specified family, style, and size (for example,
     * Helvetica-Italic-16). If such a font object has been requested before,
     * the cached object is returned; if not, a new font object is created,
     * returned, and cached for future use.
     * 
     * Callers should not dispose of the returned font object(s). Rather, the
     * PlotFontMngr should be disposed after all fonts are no longer needed,
     * which will in turn dispose of all cached fonts.
     */
    public IFont getFont(String fontFamily, String fontStyle, int fontSize) {
        Tracer.print("> Entry");

        // Get font based on font family name and style

        String fontName = fontFamily + "-" + fontStyle;
        String fontKey = fontName + "-" + fontSize;
        IFont font = fontNameToFontMap.get(fontKey);

        if (font == null) {
            // A font with this key (family-style-size) doesn't exist yet, so
            // need to initialize it. Need a target to do this...

            IDisplayPane displayPane = NcDisplayMngr.getActiveNatlCntrsEditor()
                    .getActiveDisplayPane();
            IGraphicsTarget target = displayPane.getTarget();

            // First, see if it's one of the enumerated known file-based fonts
            FileFontEnum fontEnum = fontNameToEnumMap.get(fontName);
            if (fontEnum != null) {
                // Yes -- initialize the font based on file and characteristics
                // stored in the enum:
                font = target.initializeFont(fontEnum.getFontFile(),
                        fontEnum.getFontType(), fontSize,
                        fontEnum.getFontStyles());
            } else {
                // No -- initialize a system (name-based) font, instead of a
                // file-based one
                // @formatter:off
                Style[] styles = 
                            fontStyle.equals(NORMAL)      ? NORMAL_STYLE :
                            fontStyle.equals(BOLD)        ? BOLD_STYLE :
                            fontStyle.equals(ITALIC)      ? ITALIC_STYLE :
                            fontStyle.equals(BOLD_ITALIC) ? BOLD_ITALIC_STYLE : 
                            null;
                // @formatter:on
                font = target.initializeFont(fontFamily, fontSize, styles);
            }

            // A few final preparations to get the font ready for use
            if (font != null) {
                font.setMagnification(1);
                // disable anti-aliasing, to make text less fuzzy
                font.setScaleFont(false);
                font.setSmoothing(false);
            }

            // Cache this font, for subsequent requests for the same one
            fontNameToFontMap.put(fontKey, font);
        }
        Tracer.print("< Exit");

        return font;
    }
}
