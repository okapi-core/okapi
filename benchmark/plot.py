#!/usr/bin/env python3
import csv
import sys
from datetime import datetime
from collections import deque
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter, MaxNLocator

# ---------- helpers ----------

def format_ts(sec: int) -> str:
    # CSV has epoch seconds; render with .000 ms for consistency
    dt = datetime.fromtimestamp(sec)
    return dt.strftime("%H:%M:%S") + ".000"

def human_format(num, _pos=None):
    # 12k, 1.2M, 3.4B
    absn = abs(num)
    for unit in ["", "k", "M", "B", "T"]:
        if absn < 1000:
            return f"{num:.0f}{unit}"
        num /= 1000.0
        absn /= 1000.0
    return f"{num:.1f}P"

def moving_avg(seq, window=5):
    if window <= 1:
        return list(seq)
    out, q, s = [], deque(), 0.0
    for x in seq:
        q.append(x); s += x
        if len(q) > window:
            s -= q.popleft()
        out.append(s / len(q))
    return out


if __name__ == "__main__":
# ---------- load CSV ----------

    if len(sys.argv) < 2:
        print("Usage: python plot_okapi_rates.py <okapi-bench.csv>")
        sys.exit(1)

    csv_path = sys.argv[1]
    secs, reqs, samples = [], [], []
    with open(csv_path, newline="") as f:
        r = csv.DictReader(f)
        for row in r:
            secs.append(int(row["second"]))
            reqs.append(int(row["requests"]))
            samples.append(int(row["samples"]))

    labels = [format_ts(s) for s in secs]
    samples_ma = moving_avg(samples, window=5)
    reqs_ma = moving_avg(reqs, window=5)

    # ---------- common styling ----------
    def style_axes(ax, y_label):
        ax.set_xlabel("Time (HH:MM:SS.SSS)")
        ax.set_ylabel(y_label)
        ax.yaxis.set_major_formatter(FuncFormatter(human_format))
        ax.xaxis.set_major_locator(MaxNLocator(nbins=10, prune="both"))
        plt.setp(ax.get_xticklabels(), rotation=45, ha="right")
        ax.grid(True, linestyle="--", alpha=0.6)

    # ---------- Chart 1: Samples/sec ----------
    fig = plt.figure(figsize=(11, 5.5))
    ax = fig.add_subplot(111)
    ax.plot(labels, samples, linewidth=1.2, marker="o", markersize=3, alpha=0.6, label="samples/sec")
    ax.plot(labels, samples_ma, linewidth=2.0, label="samples/sec (MA, w=5)")
    style_axes(ax, "Samples/sec")
    ax.set_title("Okapi Ingest — Samples per Second")
    ax.legend()
    fig.tight_layout()
    fig.savefig("okapi_samples_per_sec.png", dpi=160)
    # plt.show()

    # ---------- Chart 2: Requests/sec ----------
    fig = plt.figure(figsize=(11, 5.5))
    ax = fig.add_subplot(111)
    ax.plot(labels, reqs, linewidth=1.2, marker="o", markersize=3, alpha=0.6, label="requests/sec")
    ax.plot(labels, reqs_ma, linewidth=2.0, label="requests/sec (MA, w=5)")
    style_axes(ax, "Requests/sec")
    ax.set_title("Okapi Ingest — Requests per Second")
    ax.legend()
    fig.tight_layout()
    fig.savefig("okapi_requests_per_sec.png", dpi=160)
    # plt.show()

    # ---------- Chart 3: Combined (dual-axis) ----------
    fig = plt.figure(figsize=(12, 6))
    ax1 = fig.add_subplot(111)
    ax1.plot(labels, samples_ma, linewidth=2.0, label="samples/sec (MA)")
    style_axes(ax1, "Samples/sec")

    ax2 = ax1.twinx()
    ax2.plot(labels, reqs_ma, linewidth=2.0, linestyle="--", label="requests/sec (MA)")
    ax2.yaxis.set_major_formatter(FuncFormatter(human_format))
    ax2.set_ylabel("Requests/sec")

    # Build a combined legend
    lines = ax1.get_lines() + ax2.get_lines()
    labels_legend = [l.get_label() for l in lines]
    ax1.legend(lines, labels_legend, loc="upper left")

    ax1.set_title("Okapi Ingest — Samples & Requests per Second (Moving Average)")
    fig.tight_layout()
    fig.savefig("okapi_ingest_combined.png", dpi=160)
    # plt.show()

    print("Saved charts:")
    print("  okapi_samples_per_sec.png")
    print("  okapi_requests_per_sec.png")
    print("  okapi_ingest_combined.png")
