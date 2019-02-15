package gov.noaa.nws.ncep.viz.tools.cursor;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.FileUtil;

import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.tools.Activator;

/**
 * Store and handle attributes for National Centers Cursors.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer   Description
 * ----------    -------  --------   ----------------------------------
 * 06/22/09      #109     M. Li      Created
 * 07/28/11      #450     G. Hull    NcPathManager, remove cursorType tbl and
 *                                   determine cursors from available png files
 * Feb 14, 2019  7562     tgurney    Rewrite
 *
 * </pre>
 *
 * @author mli
 */
public class NCCursors {

    public static enum CursorRef {
        DEFAULT, POINT_SELECT, BUSY
    }

    public static enum Color {
        RED(0xFF0000), BLACK(0x000000), GREEN(0x00FF00), YELLOW(0xFFFF00), CYAN(
                0x00FFFF), MAGENTA(0xFF00FF), WHITE(0xFFFFFF);

        private final int rgb;

        private Color(int rgb) {
            this.rgb = rgb;
        }
    }

    public static enum CursorType {
        SMALL_ARROW(1, 1), LARGE_ARROW(1, 1), SMALL_CROSS(9, 8), LARGE_CROSS(16,
                15), SMALL_X(9, 8), LARGE_X(16, 15);

        private int hotspotX;

        private int hotspotY;

        private CursorType(int hotspotX, int hotspotY) {
            this.hotspotX = hotspotX;
            this.hotspotY = hotspotY;
        }

        public int getHotspotX() {
            return hotspotX;
        }

        public int getHotspotY() {
            return hotspotY;
        }
    }

    /** Stores all information associated with a cursor */
    public static class CursorInfo {
        public final CursorRef ref;

        public Color color;

        public CursorType type;

        public CursorInfo(CursorRef ref, Color color, CursorType type) {
            this.ref = ref;
            this.color = color;
            this.type = type;
        }

        public CursorInfo(CursorInfo other) {
            this.ref = other.ref;
            this.color = other.color;
            this.type = other.type;
        }
    }

    private static final NCCursors INSTANCE = new NCCursors();

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private Map<CursorRef, CursorInfo> cursorInfos = new EnumMap<>(
            CursorRef.class);

    private Map<CursorRef, Cursor> cachedCursors = new EnumMap<>(
            CursorRef.class);

    /** Cursors pending disposal */
    private Map<CursorRef, Cursor> toDispose = new EnumMap<>(CursorRef.class);

    private NCCursors() {
        try {
            cursorInfos = getDefaultCursorInfos();
        } catch (Exception e) {
            statusHandler.warn("Failed to load cursor table", e);
        }
    }

    public static NCCursors getInstance() {
        return INSTANCE;
    }

    /** Try to get cursor from cache, or create a new one */
    private Cursor getCursor(Display d, CursorRef cursorRef) {
        Cursor rval = null;
        rval = cachedCursors.get(cursorRef);
        if (rval == null) {
            CursorInfo cursorInfo = cursorInfos.get(cursorRef);
            if (cursorInfo != null) {
                ImageDescriptor id = AbstractUIPlugin.imageDescriptorFromPlugin(
                        Activator.PLUGIN_ID,
                        FileUtil.join("images", cursorInfo.type + ".png"));
                ImageData image = id.getImageData();
                // Set cursor color
                for (int i = 0; i < image.width; i++) {
                    for (int j = 0; j < image.height; j++) {
                        if (image.getPixel(i, j) != 0) {
                            image.setPixel(i, j, cursorInfo.color.rgb);
                        }
                    }
                }
                rval = new Cursor(d, image, cursorInfo.type.getHotspotX(),
                        cursorInfo.type.getHotspotY());
                cachedCursors.put(cursorRef, rval);
            }
        }
        return rval;

    }

    /**
     * Set cursor on the specified shell.
     *
     * @param shell
     * @param cursorRef
     *            Cursor to set. If null, use the system default cursor for that
     *            type of control
     */
    public void setCursor(Shell shell, CursorRef cursorRef) {
        Display display = shell.getDisplay();
        Cursor cursor = getCursor(display, cursorRef);
        shell.setCursor(cursor);
        toDispose.values().forEach(Cursor::dispose);
        toDispose.clear();
    }

    /** @return A copy of the cursor infos */
    public Map<CursorRef, CursorInfo> getCursorInfos() {
        Map<CursorRef, CursorInfo> rval = new EnumMap<>(CursorRef.class);
        for (Entry<CursorRef, CursorInfo> e : cursorInfos.entrySet()) {
            rval.put(e.getKey(), new CursorInfo(e.getValue()));
        }
        return rval;
    }

    /** @return A copy of the default cursor infos */
    public static Map<CursorRef, CursorInfo> getDefaultCursorInfos()
            throws JAXBException {
        Map<CursorRef, CursorInfo> rval = new EnumMap<>(CursorRef.class);
        CursorReferenceTableReader curRefTbl = new CursorReferenceTableReader(
                NcPathManager.getInstance()
                        .getStaticFile(NcPathConstants.CURSOR_REFS_TBL)
                        .getAbsolutePath());

        for (CursorReference r : curRefTbl.getTable()) {
            Color color = Color.valueOf(r.cursorColor.toUpperCase());
            CursorType type = CursorType.valueOf(r.cursorName.toUpperCase());
            CursorRef ref = CursorRef.values()[r.getReferenceIndex()];
            rval.put(ref, new CursorInfo(ref, color, type));
        }
        return rval;
    }

    public void updateCursorInfo(CursorInfo cursorInfo) {
        cursorInfos.put(cursorInfo.ref, cursorInfo);
        Cursor c = cachedCursors.remove(cursorInfo.ref);
        if (c != null) {
            toDispose.put(cursorInfo.ref, c);
        }
    }

    /** @return the cursor ref for this cursor, if it can be determined. */
    public Optional<CursorRef> getCursorRef(Cursor c) {
        for (Entry<CursorRef, Cursor> cursorEntry : cachedCursors.entrySet()) {
            if (cursorEntry.getValue().equals(c)) {
                return Optional.of(cursorEntry.getKey());
            }
        }
        for (Entry<CursorRef, Cursor> cursorEntry : toDispose.entrySet()) {
            if (cursorEntry.getValue().equals(c)) {
                return Optional.of(cursorEntry.getKey());
            }
        }
        return Optional.empty();
    }

}
