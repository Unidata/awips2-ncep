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
 */
package com.raytheon.uf.viz.gempak.common.adapter;

import java.nio.FloatBuffer;

import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.serialization.IDeserializationContext;
import com.raytheon.uf.common.serialization.ISerializationContext;
import com.raytheon.uf.common.serialization.ISerializationTypeAdapter;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.viz.gempak.common.data.GempakDbDataResponse;

/**
 * Serialization adapter for {@link GempakDbDataResponse}s.
 *
 * This only exists for performance reasons, as the responses could simply be
 * serialized by annotations. However, deserializing float[] over a socket is
 * slow for an unknown reason, so we wrap it in a {@link FloatBuffer} for
 * (de)serializing. However, this is still slow (again for an unknown reason) if
 * anything is deserialized after the float buffer, so we (de)serialize it last.
 *
 * The slowness for float[] occurs when serializing through a socket. If the
 * client deserializes a response from the socket while the server is waiting to
 * deserialize something from the socket as well, then it is slow (adds ~40 ms
 * for an array of 30k floats, which can add up).
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 24, 2018 54483      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakDbDataResponseAdapter
        implements ISerializationTypeAdapter<GempakDbDataResponse> {

    @Override
    public void serialize(ISerializationContext serializer,
            GempakDbDataResponse dataResponse) throws SerializationException {
        serializer.writeObject(dataResponse.getSubgSpatialObj());
        // For performance reasons, wrap floats in a buffer and write it last
        serializer.writeObject(FloatBuffer.wrap(dataResponse.getData()));
    }

    @Override
    public GempakDbDataResponse deserialize(
            IDeserializationContext deserializer)
            throws SerializationException {
        // Read spatial object
        ISpatialObject subgSpatialObject = (ISpatialObject) deserializer
                .readObject();

        // Read float data
        FloatBuffer fb = (FloatBuffer) deserializer.readObject();
        float[] floatArray;
        if (fb.hasArray()) {
            floatArray = fb.array();
        } else {
            floatArray = new float[fb.limit()];
            fb.get(floatArray);
        }

        return new GempakDbDataResponse(floatArray, subgSpatialObject);
    }
}
