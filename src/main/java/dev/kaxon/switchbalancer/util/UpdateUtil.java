package dev.kaxon.switchbalancer.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateUtil {
    private UpdateUtil() {}

    public static final String GITHUB_RELEASES_URL = "https://github.com/kaxlabs/DingKeySwitchBalancer/releases";

    public static boolean isGreater(String latestVersion, String currentVersion) {
        int[] version1Segments = parseVersion(latestVersion);
        int[] version2Segments = parseVersion(currentVersion);

        for (int i = 0; i < Math.min(version1Segments.length, version2Segments.length); i++) {
            if (version1Segments[i] > version2Segments[i]) {
                return true;
            } else if (version1Segments[i] < version2Segments[i]) {
                return false;
            }
        }

        return version1Segments.length > version2Segments.length;
    }

    private static int[] parseVersion(String version) {
        return Arrays.stream(version.split("\\."))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    public static CompletableFuture<String> getLatestVersionAsync() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_RELEASES_URL))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        String responseBody = response.body();
                        Pattern versionPattern = Pattern.compile("releases/tag/v([\\d.]+)");
                        Matcher matcher = versionPattern.matcher(responseBody);

                        if (matcher.find()) {
                            return matcher.group(1);
                        } else {
                            throw new RuntimeException("Version not found in the response");
                        }
                    } else {
                        throw new RuntimeException("Failed to fetch releases page, status code: " + response.statusCode());
                    }
                });
    }
}
