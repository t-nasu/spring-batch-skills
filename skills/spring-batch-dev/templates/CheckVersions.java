import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 新しいプロジェクトで使う版を、公開されている情報から割り出して表示する。
 * OS を問わず、JDK だけで動く: {@code java CheckVersions.java}
 * どれか1つでも割り出せなければ、終了コード 1 で終わる。
 */
public class CheckVersions {

    static final String M = "https://repo.maven.apache.org/maven2";
    static final String RELEASE = "[0-9]+\\.[0-9]+\\.[0-9]+";
    static final HttpClient HTTP = HttpClient.newBuilder()
            .proxy(ProxySelector.getDefault())
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    static boolean failed = false;

    public static void main(String[] args) {
        String boot = latest("org/springframework/boot/spring-boot-dependencies", RELEASE);
        show("spring-boot", boot);

        if (boot != null) {
            String bom = fetch(M + "/org/springframework/boot/spring-boot-dependencies/" + boot
                    + "/spring-boot-dependencies-" + boot + ".pom");
            show("spring-batch", prop(bom, "spring-batch.version"));
            String groovy = prop(bom, "groovy.version");
            show("groovy", groovy);
            show("mybatis-spring-boot-starter", mybatisFor(minor(boot)));
            show("spock-core", groovy == null ? null
                    : latest("org/spockframework/spock-core", "[0-9]+\\.[0-9]+-groovy-" + Pattern.quote(minor(groovy))));
        }

        show("archunit", latest("com/tngtech/archunit/archunit", RELEASE));
        show("aws-sdk-bom", latest("software/amazon/awssdk/bom", RELEASE));
        show("gradle", find(fetch("https://services.gradle.org/versions/current"), "\"version\"\\s*:\\s*\"([^\"]+)\""));
        show("foojay-resolver", prop(fetch("https://plugins.gradle.org/m2/org/gradle/toolchains/foojay-resolver-convention/"
                + "org.gradle.toolchains.foojay-resolver-convention.gradle.plugin/maven-metadata.xml"), "release"));
        show("java-lts", find(fetch("https://api.adoptium.net/v3/info/available_releases"), "\"most_recent_lts\"\\s*:\\s*([0-9]+)"));

        System.exit(failed ? 1 : 0);
    }

    /** MyBatis Starter のうち、Spring Boot と同じメジャー・マイナー版に対応する最新のもの。 */
    static String mybatisFor(String bootMinor) {
        for (String v : versions("org/mybatis/spring/boot/mybatis-spring-boot-starter", RELEASE).reversed()) {
            String pom = fetch(M + "/org/mybatis/spring/boot/mybatis-spring-boot/" + v + "/mybatis-spring-boot-" + v + ".pom");
            if (pom == null) {
                return null; // 古い版を誤って選ばないよう打ち切る
            }
            String sb = prop(pom, "spring-boot.version");
            if (sb == null) {
                return null;
            }
            int c = VERSION_ORDER.compare(minor(sb), bootMinor);
            if (c == 0) {
                return v;
            }
            if (c < 0) {
                return null; // 新しい順に見ているので、対応する Spring Boot がより古くなったら打ち切る
            }
        }
        return null;
    }

    static String latest(String path, String pattern) {
        List<String> vs = versions(path, pattern);
        return vs.isEmpty() ? null : vs.getLast();
    }

    /** maven-metadata.xml にある版のうち pattern に合うものを、古い順に並べる。 */
    static List<String> versions(String path, String pattern) {
        String xml = fetch(M + "/" + path + "/maven-metadata.xml");
        if (xml == null) {
            return List.of();
        }
        Pattern p = Pattern.compile(pattern);
        return Pattern.compile("<version>([^<]+)</version>").matcher(xml).results()
                .map(r -> r.group(1))
                .filter(v -> p.matcher(v).matches())
                .sorted(VERSION_ORDER)
                .toList();
    }

    /** 数字の部分を数として比べる（例: 4.10 は 4.9 より新しい）。 */
    static final Comparator<String> VERSION_ORDER = (a, b) -> {
        int[] x = numbers(a), y = numbers(b);
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int c = Integer.compare(i < x.length ? x[i] : 0, i < y.length ? y[i] : 0);
            if (c != 0) {
                return c;
            }
        }
        return a.compareTo(b);
    };

    static int[] numbers(String v) {
        return Arrays.stream(v.split("\\D+")).filter(s -> !s.isEmpty()).mapToInt(Integer::parseInt).toArray();
    }

    static String minor(String v) {
        String[] parts = v.split("\\.");
        return parts.length < 2 ? v : parts[0] + "." + parts[1];
    }

    static String prop(String xml, String name) {
        return find(xml, "<" + Pattern.quote(name) + ">([^<]+)</" + Pattern.quote(name) + ">");
    }

    static String find(String text, String regex) {
        if (text == null) {
            return null;
        }
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    /** 取得できなければ null。Maven Central が回数の制限（429）を返すことがあるので、間を空けて試し直す。 */
    static String fetch(String url) {
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                HttpResponse<String> res = HTTP.send(
                        HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).build(),
                        HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    return res.body();
                }
                if (res.statusCode() != 429 && res.statusCode() < 500) {
                    System.err.println(url + " -> " + res.statusCode());
                    return null;
                }
            } catch (Exception e) {
                System.err.println(url + " -> " + e);
            }
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    static void show(String name, String value) {
        if (value == null || value.isBlank()) {
            System.out.println(name + "=NOT_FOUND");
            failed = true;
        } else {
            System.out.println(name + "=" + value);
        }
    }
}
