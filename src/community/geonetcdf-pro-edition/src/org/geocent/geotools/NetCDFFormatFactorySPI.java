package org.geocent.geotools;

import java.awt.RenderingHints.Key;
import java.util.Collections;
import java.util.Map;

import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;

public class NetCDFFormatFactorySPI implements GridFormatFactorySpi {

    @Override
    public boolean isAvailable() {
        return true; /* TODO: maybe check to see if it IS avaliable? Idk */
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Key, ?> getImplementationHints() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public AbstractGridFormat createFormat() {
        return new NetCDFFormat();
    }
}
