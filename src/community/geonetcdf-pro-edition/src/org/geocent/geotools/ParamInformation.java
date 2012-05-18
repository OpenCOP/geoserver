package org.geocent.geotools;

import java.util.Date;

import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.geometry.GeneralEnvelope;
import org.opengis.coverage.grid.GridEnvelope;

public class ParamInformation {
	private GeneralEnvelope requestedEnvelope = null;
	private Double elevation = null;
	private Date time = null;
	private String parameter = "water_temp";
	private GridEnvelope dim = null;
	private OverviewPolicy overviewPolicy = null;

	public GeneralEnvelope getRequestedEnvelope() {
		return requestedEnvelope;
	}

	public void setRequestedEnvelope(GeneralEnvelope requestedEnvelope) {
		this.requestedEnvelope = requestedEnvelope;
	}

	public Double getElevation() {
		return elevation;
	}

	public void setElevation(Double elevation) {
		this.elevation = elevation;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	public GridEnvelope getDim() {
		return dim;
	}

	public void setDim(GridEnvelope dim) {
		this.dim = dim;
	}

	public OverviewPolicy getOverviewPolicy() {
		return overviewPolicy;
	}

	public void setOverviewPolicy(OverviewPolicy overviewPolicy) {
		this.overviewPolicy = overviewPolicy;
	}
}
