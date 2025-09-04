package com.example.jitlab.api.dto;

public class FilesRequest {
  private int fileCount = 5;          // how many files to create
  private int fileSizeBytes = 1024*64; // size of each file
  private String prefix = "blob";     // filename prefix

  public FilesRequest() {}

  public int getFileCount() { return fileCount; }
  public void setFileCount(int fileCount) { this.fileCount = fileCount; }

  public int getFileSizeBytes() { return fileSizeBytes; }
  public void setFileSizeBytes(int fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

  public String getPrefix() { return prefix; }
  public void setPrefix(String prefix) { this.prefix = prefix; }
}
