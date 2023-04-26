package com.namsung.xgpsmanager.data;

/**
 * Created by cnapman on 2018. 2. 2..
 */

public class SatellitesInfo {
    public int elevation;
    public int azimuth;
    public int SNR;
    public boolean inUse;

    public SatellitesInfo() {}

    public SatellitesInfo(int elevation, int azimuth, int SNR, boolean inUse) {
        this.elevation = elevation;
        this.azimuth = azimuth;
        this.SNR = SNR;
        this.inUse = inUse;
    }

    public SatellitesInfo create(String elevation, String azimuth, String SNR, boolean inUse) {
        try {
            this.elevation = Integer.parseInt(elevation);
            this.azimuth = Integer.parseInt(azimuth);
            this.SNR = Integer.parseInt(SNR);
            this.inUse = inUse;
            return this;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
