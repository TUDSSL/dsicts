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

public class BenchFiles {

  static class Result {
    final long nanos;
    final int code;
    final long createElapsedMs;
    Result(long nanos, int code, long createElapsedMs) {
      this.nanos = nanos; this.code = code; this.createElapsedMs = createElapsedMs;
    }
  }

  public static void main(String[] args) throws Exception {
    String base = arg(args, "--base", "http://localhost:8080/work/files");
    int concurrency = Integer.parseInt(arg(args, "--concurrency", "4"));
    int fileCount = Integer.parseInt(arg(args, "--fileCount", "5"));
    int fileSizeBytes = Integer.parseInt(arg(args, "--fileSizeBytes", "65536")); // 64 KiB
    String prefix = arg(args, "--prefix", "blob");
    int warmupSec = Integer.parseInt(arg(args, "--warmupSec", "5"));
    int runSec = Integer.parseInt(arg(args, "--runSec", "20"));
    String label = arg(args, "--label", "files");

    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .version(HttpClient.Version.HTTP_1_1)
        .build();

    String body = "{\"fileCount\":" + fileCount + ",\"fileSizeBytes\":" + fileSizeBytes + ",\"prefix\":\"" + prefix + "\"}";

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(base))
        .timeout(Duration.ofSeconds(300))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
        // We want to read the response fully (the ZIP) then discard it, to measure end-to-end time.
        .build();

    ExecutorService pool = Executors.newFixedThreadPool(concurrency);
    AtomicBoolean stop = new AtomicBoolean(false);
    AtomicBoolean record = new AtomicBoolean(false);
    List<Result> results = Collections.synchronizedList(new ArrayList<>(50_000));
    AtomicLong ok = new AtomicLong();
    AtomicLong err = new AtomicLong();

    Runnable worker = () -> {
      while (!stop.get()) {
        long t0 = System.nanoTime();
        int code = 0;
        long createElapsedMs = -1L;
        try {
          HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
          code = resp.statusCode();
          // custom headers from server (best-effort)
          String h = resp.headers().firstValue("X-Create-Elapsed-Ms").orElse("-1");
          try { createElapsedMs = Long.parseLong(h); } catch (NumberFormatException ignore) {}
          if (code >= 200 && code < 300) ok.incrementAndGet(); else err.incrementAndGet();
        } catch (Exception e) {
          err.incrementAndGet();
          code = -1;
        } finally {
          long dt = System.nanoTime() - t0;
          if (record.get()) results.add(new Result(dt, code, createElapsedMs));
        }
      }
    };

    for (int i = 0; i < concurrency; i++) pool.submit(worker);

    // Warmup so the server's file path + compression get hot
    Thread.sleep(warmupSec * 1000L);
    record.set(true);

    long tStart = System.nanoTime();
    Thread.sleep(runSec * 1000L);
    stop.set(true);
    pool.shutdown();
    pool.awaitTermination(60, TimeUnit.SECONDS);
    long tEnd = System.nanoTime();

    int n = results.size();
    long[] lat = new long[n];
    long[] createMs = new long[n];
    for (int i=0;i<n;i++) { lat[i]=results.get(i).nanos; createMs[i]=results.get(i).createElapsedMs; }
    java.util.Arrays.sort(lat);

    double wallSec = (tEnd - tStart) / 1e9;
    double rps = ok.get() / wallSec;

    double p50 = toMs(percentile(lat, 0.50));
    double p95 = toMs(percentile(lat, 0.95));
    double p99 = toMs(percentile(lat, 0.99));
    double avg = toMs(mean(lat));
    long createP50 = percentileLong(createMs, 0.50);
    long createP95 = percentileLong(createMs, 0.95);

    System.out.printf("label,concurrency,fileCount,fileSizeBytes,warmupSec,runSec,ok,err,throughput_rps,avg_ms,p50_ms,p95_ms,p99_ms,create_p50_ms,create_p95_ms%n");
    System.out.printf("%s,%d,%d,%d,%d,%d,%d,%d,%.2f,%.3f,%.3f,%.3f,%.3f,%d,%d%n",
        label, concurrency, fileCount, fileSizeBytes, warmupSec, runSec,
        ok.get(), err.get(), rps, avg, p50, p95, p99, createP50, createP95);
  }

  // utils
  static String arg(String[] args, String key, String def) {
    for (int i = 0; i < args.length - 1; i++) if (args[i].equals(key)) return args[i+1];
    return def;
  }
  static long percentile(long[] sorted, double q) {
    if (sorted.length == 0) return 0;
    int i = (int)Math.min(sorted.length - 1, Math.max(0, Math.round((sorted.length - 1) * q)));
    return sorted[i];
  }
  static long percentileLong(long[] vals, double q) {
    if (vals.length == 0) return -1;
    long[] copy = java.util.Arrays.copyOf(vals, vals.length);
    java.util.Arrays.sort(copy);
    int i = (int)Math.min(copy.length - 1, Math.max(0, Math.round((copy.length - 1) * q)));
    return copy[i];
  }
  static double mean(long[] sorted) {
    if (sorted.length == 0) return 0;
    long s = 0; for (long v : sorted) s += v; return ((double)s)/sorted.length;
  }
  static double toMs(double nanos) { return nanos / 1_000_000.0; }
}
