package org.geocent.netcdf;

import java.io.Serializable;
import java.util.Date;

public class CacheKey implements Serializable {

    private static final long serialVersionUID = -4055981005030882263L;
    public String parameterName;
    public Double elevation;
    public Date time;

    public CacheKey(String parameterName, Double elevation, Date time) {
        this.time = time;
        this.parameterName = parameterName;
        this.elevation = elevation;
    }

    @Override
    public boolean equals(Object arg0) {
        if (!(arg0 instanceof CacheKey))
            return false;
        CacheKey c = (CacheKey) arg0;
        if (this.parameterName.equals(c.parameterName) && this.elevation.equals(c.elevation) && this.time.equals(c.time))
            return true;

        else
            return false;
    }

    @Override
    public int hashCode() {
        return this.parameterName.hashCode() ^ this.elevation.hashCode() ^ this.time.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Parameter: %s\nElevation: %f\nTime: %s\nHashCode: %d", this.parameterName, this.elevation, this.time, hashCode());
    }
}
