package de.tk.opensource.privacyproxy.config;

import org.springframework.util.StringUtils;

public class RetrievalEndpoint {

    private String remoteUrl;

    private String filename;

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {

        this.remoteUrl = remoteUrl;
        if (StringUtils.isEmpty(this.filename)) {
            setFilename(remoteUrl.substring(remoteUrl.lastIndexOf('/')));
        }

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

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

}
