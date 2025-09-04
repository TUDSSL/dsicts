#!/usr/bin/env python3
import argparse, subprocess, os, shlex, sys, time
from datetime import datetime

def find_pid(cmd_substr):
    try:
        out = subprocess.check_output(["pgrep","-fa",cmd_substr], text=True)
        return int(out.strip().splitlines()[0].split()[0])
    except Exception:
        return None

def run():
    ap = argparse.ArgumentParser(description="Run monitor + Python loader + plots")
    ap.add_argument("--url", required=True)
    ap.add_argument("--body", default="{}")
    ap.add_argument("--concurrency", type=int, default=8)
    ap.add_argument("--warmupSec", type=int, default=10)
    ap.add_argument("--runSec", type=int, default=120)
    ap.add_argument("--title", default="JITLab Run")
    ap.add_argument("--server-cmd-substr", default="jitlab-0.0.1-SNAPSHOT.jar")
    ap.add_argument("--outdir", default="runs")
    args = ap.parse_args()

    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    os.makedirs(args.outdir, exist_ok=True)

    pid = find_pid(args.server_cmd_substr)
    if not pid:
        print(f"[one_run] Could not find server PID by '{args.server_cmd_substr}'. Start the server first.", file=sys.stderr)
        sys.exit(1)
    print(f"[one_run] Using server PID: {pid}")

    load_csv = os.path.join(args.outdir, f"load_{ts}.csv")
    mon_csv  = os.path.join(args.outdir, f"monitor_{ts}.csv")

    duration = args.warmupSec + args.runSec + 5
    mon_cmd = ["python3","tools/monitor.py","--pid",str(pid),
               "--interval","1","--duration",str(duration),
               "--out",mon_csv]
    print("[one_run] START monitor:", " ".join(shlex.quote(x) for x in mon_cmd))
    # Forward monitor's output so you see progress; don’t capture -> no deadlock
    mon_p = subprocess.Popen(mon_cmd, stdout=sys.stdout, stderr=sys.stderr)

    load_cmd = ["python3","tools/load_py.py",
                "--url",args.url,
                "--body",args.body,
                "--concurrency",str(args.concurrency),
                "--warmupSec",str(args.warmupSec),
                "--runSec",str(args.runSec),
                "--out",load_csv]
    print("[one_run] START loader:", " ".join(shlex.quote(x) for x in load_cmd))
    ret = subprocess.run(load_cmd).returncode
    print(f"[one_run] loader exited with code {ret}")

    # Poll the monitor instead of wait(); enforce a hard deadline
    print("[one_run] WAIT monitor to finish…")
    deadline = time.time() + duration + 15
    while mon_p.poll() is None and time.time() < deadline:
        time.sleep(0.2)
    if mon_p.poll() is None:
        print("[one_run] monitor did not exit in time; terminating…", file=sys.stderr)
        mon_p.terminate()
        try:
            mon_p.wait(timeout=5)
        except subprocess.TimeoutExpired:
            print("[one_run] monitor still alive; killing…", file=sys.stderr)
            mon_p.kill()

    # Plot (force headless backend)
    env = os.environ.copy()
    env["MPLBACKEND"] = "Agg"
    plot_cmd = ["python3","tools/plot.py","--load",load_csv,"--monitor",mon_csv,"--title",args.title]
    print("[one_run] START plot:", " ".join(shlex.quote(x) for x in plot_cmd))
    try:
        subprocess.check_call(plot_cmd, env=env)
    except subprocess.CalledProcessError as e:
        print(f"[one_run] plotting failed (exit {e.returncode}). "
              f"Check CSV paths:\n  load: {load_csv}\n  monitor: {mon_csv}", file=sys.stderr)
        sys.exit(e.returncode)

    print("\n✅ Done.")
    print(f"CSV (load):     {load_csv}")
    print(f"CSV (monitor):  {mon_csv}")
    print("PNGs:           plot_rps_power.png, plot_latency_p95.png, plot_cpu_mem.png, plot_energy.png")

if __name__ == "__main__":
    run()

