package com.example.jitlab.api.dto;

public class WorkResponse {
  private String op;
  private int iterations;
  private int payloadSize;
  private long elapsedMs;
  private double result;

  public WorkResponse(String op, int iterations, int payloadSize, long elapsedMs, double result) {
    this.op = op;
    this.iterations = iterations;
    this.payloadSize = payloadSize;
    this.elapsedMs = elapsedMs;
    this.result = result;
  }

  public String getOp() { return op; }
  public int getIterations() { return iterations; }
  public int getPayloadSize() { return payloadSize; }
  public long getElapsedMs() { return elapsedMs; }
  public double getResult() { return result; }
}

