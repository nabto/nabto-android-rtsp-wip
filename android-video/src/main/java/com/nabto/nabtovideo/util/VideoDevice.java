package com.nabto.nabtovideo.util;

public class VideoDevice {

    public static class VideoType {
        public static final int UNKNOWN = 0;
        public static final int WEB = 1;
        public static final int MPEG = 2;
    }

    public final static String DEVICETITLE = "com.nabto.api.DEVICETITLE";
    public final static String DEVICENAME = "com.nabto.api.DEVICENAME";
    public final static String DEVICEHOST = "com.nabto.api.DEVICEHOST";
    public final static String DEVICEPORT = "com.nabto.api.DEVICEPORT";
    public final static String DEVICEURL = "com.nabto.api.DEVICEURL";
    public final static String DEVICETYPE = "com.nabto.api.DEVICETYPE";

    public String title = "";
    public String name = "streamdemo.nabto.net";
	public String url = "";
	public int type = 1;
	public int port = 80;
    public int category = 0;
    public String host = "127.0.0.1";
	
	public VideoDevice(String _title, String _name, String _url, int _port, int _type, int _category, String _host) {

        if (_title != null && !_title.equals("")) {
            title = _title;
        }

        if (_name != null && !_name.equals("")) {
            name = _name;
        }

        if (_url != null && !_url.equals("")) {
            url = _url;
        }

        if (_port > 0) {
            port = _port;
        }

        if (_type > 0) {
            type = _type;
        }

        if (_category > 0) {
            category = _category;
        }

        if (!_host.isEmpty()) {
            host = _host;
        }
	}

    public String getTitle() {
        if (title.isEmpty()) {
            return name;
        }
        return title;
    }
	
	public String getName() {
		return name;
	}
	
	public String getUrl() {
		return url;
	}
	
	public int getType() {
		return type;
	}

    public String getTypeString() {
        switch (type) {
            case 1:
                return "Web";
            case 2:
                return "MPEG";
            default:
                return "Unknown";
        }
    }
	
	public int getPort() {
		return port;
	}

    public int getCategory() {
        return category;
    }

    public String getHost() { return host; }
	
	@Override
	public String toString() {
		return name;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VideoDevice that = (VideoDevice) o;

        if (port != that.port) return false;
        if (!name.equals(that.name)) return false;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + url.hashCode();
        result = 31 * result + port;
        return result;
    }
}
