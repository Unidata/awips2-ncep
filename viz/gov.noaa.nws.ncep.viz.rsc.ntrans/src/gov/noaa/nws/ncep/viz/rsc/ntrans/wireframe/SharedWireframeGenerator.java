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
package gov.noaa.nws.ncep.viz.rsc.ntrans.wireframe;

import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;

import gov.noaa.nws.ncep.common.dataplugin.ntrans.NtransRecord;

/**
 * This class can be used when rendering multiple similar {@link NtransRecord}s
 * to detect shapes that are identical in multiple records and share a single
 * {@link IWireframeShape}. Specifically When multiple NtransRecords from a
 * single file are used they tend to share an identical map background shape.
 * This shape is often the largest shape in the entire metafile so reusing the
 * shape across multiple frames significantly reduces the use of graphics
 * memory.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Oct 24, 2016  R22550   bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class SharedWireframeGenerator {

    private final IDescriptor descriptor;

    private final IGraphicsTarget target;

    private final Map<WireframeShapeBuilder, SharedWireframe> cache = new HashMap<>();

    public SharedWireframeGenerator(IDescriptor descriptor,
            IGraphicsTarget target) {
        this.descriptor = descriptor;
        this.target = target;
    }

    public synchronized SharedWireframe getWireframeShape(
            WireframeShapeBuilder key) {
        SharedWireframe result = cache.get(key);
        if (result == null) {
            IWireframeShape wireframe = target.createWireframeShape(false,
                    descriptor);
            key.compile(wireframe);
            result = new SharedWireframe(wireframe, key);
            cache.put(key, result);
        } else {
            result.use();
        }
        return result;
    }

    /**
     * Clear the memory used to track redundant {@link WireframeShapeBuilder}.
     * This will not dispose of the {@link SharedWireframe}s.
     */
    public synchronized void dispose() {
        for (SharedWireframe s : cache.values()) {
            s.key = null;
        }
        cache.clear();
    }

    public class SharedWireframe {

        private int refCount;

        private IWireframeShape wireframe;

        private WireframeShapeBuilder key;

        private SharedWireframe(IWireframeShape wireframe,
                WireframeShapeBuilder key) {
            this.refCount = 1;
            this.wireframe = wireframe;
            this.key = key;
        }

        private synchronized void use() {
            refCount += 1;
        }

        public synchronized void dispose() {
            refCount -= 1;
            if (refCount == 0) {
                if (key != null) {
                    cache.remove(key);
                }
                wireframe.dispose();
                wireframe = null;
            }
        }

        public IWireframeShape getWireframe() {
            return wireframe;
        }

    }
}
