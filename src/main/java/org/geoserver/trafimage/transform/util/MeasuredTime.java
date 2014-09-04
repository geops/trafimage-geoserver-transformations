package org.geoserver.trafimage.transform.util;

abstract public class MeasuredTime {
	
	private long timeSpend = 0;
	private long measureStart = 0;
	private boolean measuringEnabled = true;
	
	public void setMeasuringEnabled(boolean enabled) {
		this.measuringEnabled = enabled;
	}
	
	public void clearTimeSpend() {
		this.timeSpend = 0;
	}
	
	public long getTimeSpendInNanoSeconds() {
		return this.timeSpend;
	}

	public double getTimeSpendInSeconds() {
		return (double) this.getTimeSpendInNanoSeconds() / 1000000000.0;
	}
	
	protected void startMeasuring() {
		if (this.measuringEnabled) {
			this.measureStart = System.nanoTime();
		}
	}
	
	protected void stopMeasuring() {
		if (this.measuringEnabled) {
			this.timeSpend = this.timeSpend + (System.nanoTime() - this.measureStart);
		}
	}
}
