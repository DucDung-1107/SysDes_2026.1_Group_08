package vn.medora.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Test-only route evidence; stores no request bodies, credentials or actual IDs. */
final class ApiCoverageRecorder {
    private static final Map<String, String> CONTROLLERS = new ConcurrentHashMap<>();
    private static final Map<String, Map<Integer, AtomicInteger>> HITS = new ConcurrentHashMap<>();
    private ApiCoverageRecorder() { }

    static String normalize(String path) {
        return path.replaceAll("[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}", "{}")
            .replaceAll("\\{[^/]*}", "{}");
    }
    static void register(WebApplicationContext context) {
        var mappings = context.getBean(RequestMappingHandlerMapping.class);
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            for (String path : mapping.getPatternValues()) {
                if (!path.startsWith("/api/") && !path.equals("/health") && !path.equals("/ready")) { continue; }
                for (var method : mapping.getMethodsCondition().getMethods()) {
                    CONTROLLERS.put(method + " " + normalize(path), handler.getBeanType().getSimpleName());
                }
            }
        });
        CONTROLLERS.put("POST /api/auth/login", "SpringSecurity");
        CONTROLLERS.put("POST /api/auth/logout", "SpringSecurity");
    }
    static void record(MvcResult result) {
        String key = result.getRequest().getMethod() + " " + normalize(result.getRequest().getRequestURI());
        HITS.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>())
            .computeIfAbsent(result.getResponse().getStatus(), ignored -> new AtomicInteger()).incrementAndGet();
    }
    static synchronized void write() throws IOException {
        StringBuilder report = new StringBuilder("controller\tmethod\tpath\tresponses_2xx\tresponses_4xx\tresponses_5xx\tstatuses\n");
        for (var route : new TreeMap<>(CONTROLLERS).entrySet()) {
            Map<Integer, AtomicInteger> counts = HITS.getOrDefault(route.getKey(), Map.of());
            int success = 0, rejected = 0, serverError = 0;
            var statuses = new TreeMap<Integer, Integer>();
            for (var hit : counts.entrySet()) {
                int status = hit.getKey(), count = hit.getValue().get(); statuses.put(status, count);
                if (status >= 200 && status < 300) { success += count; }
                if (status >= 400 && status < 500) { rejected += count; }
                if (status >= 500) { serverError += count; }
            }
            int split = route.getKey().indexOf(' ');
            report.append(route.getValue()).append('\t').append(route.getKey(), 0, split).append('\t')
                .append(route.getKey().substring(split + 1)).append('\t').append(success).append('\t')
                .append(rejected).append('\t').append(serverError).append('\t').append(statuses).append('\n');
        }
        Path target = Path.of("target", "api-route-coverage.tsv");
        Files.createDirectories(target.getParent());
        Files.writeString(target, report, StandardCharsets.UTF_8);
    }
}
