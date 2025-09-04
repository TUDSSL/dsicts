package com.example.jitlab.bench;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.nio.charset.StandardCharsets;

public class Bench {
  record Result(long nanos, int code) {}

  public static void main(String[] args) throws Exception {
    String base = arg(args, "--base", "http://localhost:8080");
    int concurrency = Integer.parseInt(arg(args, "--concurrency", "8"));
    int iterations = Integer.parseInt(arg(args, "--iterations", "2000")); // per request payload
    int payloadSize = Integer.parseInt(arg(args, "--payloadSize", "20000"));
    int warmupSec = Integer.parseInt(arg(args, "--warmupSec", "10"));
    int runSec = Integer.parseInt(arg(args, "--runSec", "30"));
    String label = arg(args, "--label", "default");

    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .version(HttpClient.Version.HTTP_1_1)
        .build();

    String body = "{\"iterations\":" + iterations + ",\"payloadSize\":" + payloadSize + "}";
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(base + "/work/cpu"))
        .timeout(Duration.ofSeconds(60))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
        .build();

    ExecutorService pool = Executors.newFixedThreadPool(concurrency);
    AtomicBoolean stop = new AtomicBoolean(false);
    AtomicBoolean record = new AtomicBoolean(false);
    List<Result> results = Collections.synchronizedList(new ArrayList<>(100_000));
    AtomicLong ok = new AtomicLong();
    AtomicLong err = new AtomicLong();

    Runnable worker = () -> {
      while (!stop.get()) {
        long t0 = System.nanoTime();
        int code = 0;
        try {
          HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
          code = resp.statusCode();
          if (code >= 200 && code < 300) ok.incrementAndGet();
          else err.incrementAndGet();
        } catch (Exception e) {
          err.incrementAndGet();
          code = -1;
        } finally {
          long dt = System.nanoTime() - t0;
          if (record.get()) results.add(new Result(dt, code));
        }
      }
    };

    for (int i = 0; i < concurrency; i++) pool.submit(worker);

    // Warmup to trigger JIT
    Thread.sleep(warmupSec * 1000L);
    record.set(true);

    long tStart = System.nanoTime();
    Thread.sleep(runSec * 1000L);
    stop.set(true);
    pool.shutdown();
    pool.awaitTermination(30, TimeUnit.SECONDS);
    long tEnd = System.nanoTime();

    // Aggregate
    int n = results.size();
    long[] lat = new long[n];
    int idx = 0;
    for (Result r : results) lat[idx++] = r.nanos;
    java.util.Arrays.sort(lat);

    double wallSec = (tEnd - tStart) / 1e9;
    double rps = ok.get() / wallSec;

    double p50 = toMs(percentile(lat, 0.50));
    double p95 = toMs(percentile(lat, 0.95));
    double p99 = toMs(percentile(lat, 0.99));
    double avg = toMs(mean(lat));

    System.out.printf(
        "label,concurrency,iterations,payloadSize,warmupSec,runSec,ok,err,throughput_rps,avg_ms,p50_ms,p95_ms,p99_ms%n");
    System.out.printf(
        "%s,%d,%d,%d,%d,%d,%d,%d,%.2f,%.3f,%.3f,%.3f,%.3f%n",
        label, concurrency, iterations, payloadSize, warmupSec, runSec,
        ok.get(), err.get(), rps, avg, p50, p95, p99);
  }

  static String arg(String[] args, String key, String def) {
    for (int i = 0; i < args.length - 1; i++) if (args[i].equals(key)) return args[i+1];
    return def;
  }
  static long percentile(long[] sorted, double q) {
    if (sorted.length == 0) return 0;
    int i = (int)Math.min(sorted.length - 1, Math.max(0, Math.round((sorted.length - 1) * q)));
    return sorted[i];
    }
  static double mean(long[] sorted) {
    if (sorted.length == 0) return 0;
    long s = 0; for (long v : sorted) s += v; return ((double)s)/sorted.length;
  }
  static double toMs(double nanos) { return nanos / 1_000_000.0; }
}
