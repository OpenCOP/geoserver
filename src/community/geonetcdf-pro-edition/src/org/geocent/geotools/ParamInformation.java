package org.geocent.geotools;

import java.util.Date;

import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.geometry.GeneralEnvelope;
import org.opengis.coverage.grid.GridEnvelope;

public class ParamInformation {
    GeneralEnvelope requestedEnvelope = null;
    Double elevation = null;
    Date time = null;
    String parameter = "water_temp";
    GridEnvelope dim = null;
    OverviewPolicy overviewPolicy = null;
}
