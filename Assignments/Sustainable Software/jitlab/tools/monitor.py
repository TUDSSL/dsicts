#!/usr/bin/env python3
import argparse, csv, time, psutil, glob, os, sys

def find_energy_file(path_hint=None):
    """Quick, shallow search for a readable energy_uj file."""
    if path_hint and os.path.isfile(path_hint) and os.access(path_hint, os.R_OK):
        return path_hint
    for pat in ("/sys/class/powercap/*/energy_uj",
                "/sys/class/powercap/*/*/energy_uj",
                "/sys/class/powercap/*/*/*/energy_uj"):
        for p in glob.glob(pat):
            if os.path.isfile(p) and os.access(p, os.R_OK):
                return p
    return None

def pick_proc_by_cmd(substr: str):
    me = os.getpid()
    for p in psutil.process_iter(attrs=["pid", "cmdline"]):
        try:
            if p.info["pid"] == me:
                continue
            cmd = " ".join(p.info.get("cmdline") or [])
            if substr in cmd:
                return psutil.Process(p.info["pid"])
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            continue
    return None

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--pid", type=int, help="Java server PID")
    ap.add_argument("--cmd", type=str, help="Substring to match process cmdline")
    ap.add_argument("--interval", type=float, default=1.0, help="sampling seconds")
    ap.add_argument("--duration", type=float, default=0.0, help="stop after N seconds (0 = until Ctrl+C)")
    ap.add_argument("--out", default="server_monitor.csv")
    ap.add_argument("--energy-path", default=None)
    args = ap.parse_args()

    # Resolve process
    if args.pid:
        try:
            proc = psutil.Process(args.pid)
        except psutil.NoSuchProcess:
            print(f"[monitor] PID {args.pid} not found", file=sys.stderr)
            sys.exit(1)
    elif args.cmd:
        proc = pick_proc_by_cmd(args.cmd)
        if not proc:
            print(f"[monitor] No process matching --cmd '{args.cmd}'", file=sys.stderr)
            sys.exit(1)
    else:
        print("[monitor] Provide --pid or --cmd", file=sys.stderr)
        sys.exit(1)

    # Prepare output immediately
    out_path = os.path.abspath(args.out)
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    print(f"[monitor] sampling PID {proc.pid} -> writing to {out_path}", file=sys.stderr)

    # Energy file (optional)
    energy_path = find_energy_file(args.energy_path)
    if energy_path:
        print(f"[monitor] using energy file: {energy_path}", file=sys.stderr)
    else:
        print("[monitor] no energy_uj found; power/energy will be 0", file=sys.stderr)

    # Prime CPU% (non-blocking)
    try:
        proc.cpu_percent(interval=None)
    except psutil.NoSuchProcess:
        print("[monitor] process ended before sampling started", file=sys.stderr)
        sys.exit(0)

    header = ["ts", "cpu_percent", "rss_mb", "power_w", "energy_j_total"]
    t0 = time.time()
    next_tick = t0 + args.interval
    last_e = None
    last_t = t0
    total_j = 0.0

    with open(out_path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(header)
        f.flush()
        try:
            while True:
                now = time.time()
                if args.duration and (now - t0) >= args.duration:
                    print("[monitor] duration reached; exiting", file=sys.stderr)
                    break

                # Sleep until next tick
                sleep_for = max(0.0, min(args.interval, next_tick - now))
                time.sleep(sleep_for)
                now = time.time()

                # Read CPU/RSS
                try:
                    cpu = proc.cpu_percent(interval=None)  # % since last call
                    rss_mb = proc.memory_info().rss / (1024 * 1024)
                except psutil.NoSuchProcess:
                    print("[monitor] process ended; exiting", file=sys.stderr)
                    break

                # Energy/power
                power_w = 0.0
                if energy_path and os.path.isfile(energy_path) and os.access(energy_path, os.R_OK):
                    try:
                        with open(energy_path, "r") as ef:
                            e = int(ef.read().strip())  # microjoules
                        if last_e is not None:
                            dt = max(1e-6, now - last_t)
                            de_uj = e - last_e if e >= last_e else (1 << 64) + e - last_e
                            de_j = de_uj / 1_000_000.0
                            power_w = de_j / dt
                            total_j += de_j
                        last_e = e
                        last_t = now
                    except Exception:
                        # If energy read fails, keep power_w at 0 and continue
                        pass

                # Write row
                w.writerow([now, f"{cpu:.2f}", f"{rss_mb:.2f}", f"{power_w:.3f}", f"{total_j:.6f}"])
                f.flush()
                next_tick += args.interval
        except KeyboardInterrupt:
            print("[monitor] Ctrl+C; exiting", file=sys.stderr)

if __name__ == "__main__":
    main()
