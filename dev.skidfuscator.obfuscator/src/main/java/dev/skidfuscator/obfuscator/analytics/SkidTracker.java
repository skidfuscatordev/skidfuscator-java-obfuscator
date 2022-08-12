package dev.skidfuscator.obfuscator.analytics;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.piwik.java.tracking.PiwikTracker;

public class SkidTracker extends PiwikTracker {
    public SkidTracker(String hostUrl) {
        super(hostUrl);
    }

    public SkidTracker(String hostUrl, int timeout) {
        super(hostUrl, timeout);
    }

    public SkidTracker(String hostUrl, String proxyHost, int proxyPort) {
        super(hostUrl, proxyHost, proxyPort);
    }

    public SkidTracker(String hostUrl, String proxyHost, int proxyPort, int timeout) {
        super(hostUrl, proxyHost, proxyPort, timeout);
    }

    @Override
    public HttpClient getHttpClient() {
        return super.getHttpClient();
    }

    @Override
    public CloseableHttpAsyncClient getHttpAsyncClient() {
        return super.getHttpAsyncClient();
    }
}
