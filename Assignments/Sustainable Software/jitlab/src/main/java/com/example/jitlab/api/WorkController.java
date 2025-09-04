package com.example.jitlab.api;

import com.example.jitlab.api.dto.WorkRequest;
import com.example.jitlab.api.dto.WorkResponse;
import com.example.jitlab.api.dto.FilesRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping
public class WorkController {

  @GetMapping("/ping")
  public String ping() {
    return "pong";
  }

  @PostMapping("/work/cpu")
  public WorkResponse cpu(@RequestBody WorkRequest req) {
    long start = System.nanoTime();

    int size = Math.max(1, Math.min(req.getPayloadSize(), 1_000_000));
    int iterations = Math.max(1, Math.min(req.getIterations(), 50_000_000));

    Random r = new Random(42L);
    int[] data = new int[size];
    for (int i = 0; i < size; i++) data[i] = r.nextInt();

    double acc = 0;
    for (int it = 0; it < iterations; it++) {
      long sum = 0;
      for (int v : data) {
        sum += (v ^ (v >>> 13)) * 17L;
      }
      acc += Math.log1p(Math.abs(sum % 1000));
      data[it % size] ^= it;
    }

    long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
    return new WorkResponse("cpu", iterations, size, elapsedMs, acc);
  }

  @PostMapping(value = "/work/files", produces = "application/zip")
  public ResponseEntity<StreamingResponseBody> files(@RequestBody FilesRequest req) throws IOException {
    final int count = Math.max(1, Math.min(req.getFileCount(), 500));
    final int bytesPerFile = Math.max(1, Math.min(req.getFileSizeBytes(), 10_000_000)); // cap at 10MB
    final String prefix = (req.getPrefix() == null || req.getPrefix().isBlank()) ? "blob" : req.getPrefix();

    final Path tmpDir = Files.createTempDirectory("jitlab_files_");
    long startNs = System.nanoTime();

    // Create files with pseudo-random content
    byte[] buf = new byte[Math.min(bytesPerFile, 1 << 20)]; // up to 1MiB buffer
    for (int i = 0; i < count; i++) {
      Path p = tmpDir.resolve(prefix + "_" + i + ".bin");
      try (OutputStream os = Files.newOutputStream(p, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
        int remaining = bytesPerFile;
        while (remaining > 0) {
          int chunk = Math.min(remaining, buf.length);
          ThreadLocalRandom.current().nextBytes(buf);
          os.write(buf, 0, chunk);
          remaining -= chunk;
        }
      }
    }

    long createdNs = System.nanoTime();
    long elapsedCreateMs = (createdNs - startNs) / 1_000_000L;

    StreamingResponseBody body = out -> {
      try (ZipOutputStream zos = new ZipOutputStream(out);
           DirectoryStream<Path> stream = Files.newDirectoryStream(tmpDir)) {
        for (Path p : stream) {
          ZipEntry entry = new ZipEntry(p.getFileName().toString());
          zos.putNextEntry(entry);
          try (InputStream is = Files.newInputStream(p, StandardOpenOption.READ)) {
            int r;
            while ((r = is.read(buf)) != -1) {
              zos.write(buf, 0, r);
            }
          }
          zos.closeEntry();
        }
        zos.finish();
      } finally {
        // Best-effort cleanup
        deleteRecursively(tmpDir);
      }
    };

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("application/zip"));
    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"files.zip\"");
    headers.set("X-Files-Count", Integer.toString(count));
    headers.set("X-File-Size-Bytes", Integer.toString(bytesPerFile));
    headers.set("X-Create-Elapsed-Ms", Long.toString(elapsedCreateMs));

    return ResponseEntity.ok().headers(headers).body(body);
  }

  private static void deleteRecursively(Path root) {
    if (root == null || !Files.exists(root)) return;
    try {
      // Delete files before directory (deepest first)
      try (var walk = Files.walk(root)) {
        walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
            .forEach(p -> {
              try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
      }
    } catch (IOException ignored) {
      // ignore best-effort cleanup errors
    }
  }
}
