package net.vo;


public class DeviceInfo {
    private String manufacturerName;
    private String systemVersion;
    private String deviceName;
    private String appVersion;
    private String mccmnc;
    private String network;

    public DeviceInfo(String manufacturerName, String systemVersion, String deviceName,
                      String appVersion, String mccmnc, String network) {
        this.manufacturerName = manufacturerName;
        this.systemVersion = systemVersion;
        this.deviceName = deviceName;
        this.appVersion = appVersion;
        this.mccmnc = mccmnc;
        this.network = network;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    public void setSystemVersion(String systemVersion) {
        this.systemVersion = systemVersion;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public void setMccmnc(String mccmnc) {
        this.mccmnc = mccmnc;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public String getSystemVersion() {
        return systemVersion;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getMccmnc() {
        return mccmnc;
    }

    public String getNetwork() {
        return network;
    }
}
