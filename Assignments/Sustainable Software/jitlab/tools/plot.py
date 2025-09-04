#!/usr/bin/env python3

import matplotlib
matplotlib.use("Agg")   # force non-GUI backend

import argparse, pandas as pd, matplotlib.pyplot as plt

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--load", required=True, help="load_timeseries.csv from load_py.py")
    ap.add_argument("--monitor", required=True, help="server_monitor.csv from monitor.py")
    ap.add_argument("--title", default="JITLab Run")
    args = ap.parse_args()

    load = pd.read_csv(args.load)
    mon = pd.read_csv(args.monitor)

    # Align times (optional)
    t0 = min(load["ts"].min(), mon["ts"].min())
    load["t"] = load["ts"] - t0
    mon["t"] = mon["ts"] - t0

    # Figure 1: Throughput (RPS) and Power (W)
    plt.figure()
    plt.plot(load["t"], load["rps"], label="RPS")
    plt.plot(mon["t"], mon["power_w"], label="Power (W)")
    plt.xlabel("Time (s)"); plt.ylabel("RPS / Watts"); plt.title(args.title + " — RPS & Power"); plt.legend()
    plt.tight_layout(); plt.savefig("plot_rps_power.png")

    # Figure 2: Latency p95 (ms)
    plt.figure()
    plt.plot(load["t"], load["p95_ms"])
    plt.xlabel("Time (s)"); plt.ylabel("Latency p95 (ms)"); plt.title(args.title + " — Latency p95")
    plt.tight_layout(); plt.savefig("plot_latency_p95.png")

    # Figure 3: CPU% and RSS
    plt.figure()
    plt.plot(mon["t"], mon["cpu_percent"], label="CPU % (proc)")
    plt.plot(mon["t"], mon["rss_mb"], label="RSS (MB)")
    plt.xlabel("Time (s)"); plt.ylabel("CPU% / MB"); plt.title(args.title + " — CPU & Memory"); plt.legend()
    plt.tight_layout(); plt.savefig("plot_cpu_mem.png")

    # Figure 4: Cumulative Energy (J)
    plt.figure()
    plt.plot(mon["t"], mon["energy_j_total"])
    plt.xlabel("Time (s)"); plt.ylabel("Energy (J)"); plt.title(args.title + " — Energy")
    plt.tight_layout(); plt.savefig("plot_energy.png")

    print("Saved: plot_rps_power.png, plot_latency_p95.png, plot_cpu_mem.png, plot_energy.png")

if __name__ == "__main__":
    main()
