package de.tk.opensource.privacyproxy.config;

public class RetrievalEndpoint {

    private String remoteUrl;
    private String filename;

    public String getRemoteUrl() {
        return remoteUrl;
    }

    /**
     * @return url with cache buster added as querystring parameter
     */
    public String getRemoteUrlWithCacheBuster() {
        StringBuilder result = new StringBuilder(remoteUrl);
        if (remoteUrl.contains("?")) {
            result.append("&_=");
        } else {
            result.append("?_=");
        }
        result.append(System.currentTimeMillis());
        return result.toString();
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
