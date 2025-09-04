package com.example.jitlab.api.dto;

public class WorkRequest {
  private int iterations = 1000;
  private int payloadSize = 10_000;

  public WorkRequest() {}

  public int getIterations() { return iterations; }
  public void setIterations(int iterations) { this.iterations = iterations; }

  public int getPayloadSize() { return payloadSize; }
  public void setPayloadSize(int payloadSize) { this.payloadSize = payloadSize; }
}
