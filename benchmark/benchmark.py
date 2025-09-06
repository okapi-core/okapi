#!/usr/bin/env python3
import time
import numpy as np
import multiprocessing as mp
import random
from typing import NamedTuple, List, Dict, Optional
from argparse import ArgumentParser
import requests
import csv
import math

TENANT_ID = "random_tenant"
DEFAULT_WRITE_TIMEOUT = 3.0  # seconds
DEFAULT_READ_TIMEOUT = 1.0   # seconds

def fmt_us(ms):
    return f"{ms*1000:.0f} µs" if ms == ms else "nan"
# -------------------------
# Request models
# -------------------------
class SubmitMetricsRequestInternal:
    def __init__(self, tenantId: str, metricName: str, tags: Dict[str, str],
                 vals: List[float], times: List[int]):
        self.tenantId = tenantId
        self.metricName = metricName
        self.tags = tags
        self.values = vals
        self.ts = times


class ReadBackRequest:
    def __init__(self, r: SubmitMetricsRequestInternal, res: str, agg: str):
        self.tenantId = r.tenantId
        self.metricName = r.metricName
        self.tags = r.tags
        self.resolution = res
        self.aggregation = agg


# -------------------------
# Helpers
# -------------------------
def generate_random(start_epoch: int, end_epoch: int, interval: int,
                    drop_fraction: float, min_val: float, max_val: float) -> (List[int], List[float]):
    timestamps = np.arange(start_epoch, end_epoch, interval, dtype=np.int64)
    if timestamps.size == 0:
        return [], []
    values = np.random.uniform(min_val, max_val, timestamps.size)
    if 0.0 < drop_fraction < 1.0:
        drop_mask = np.random.rand(timestamps.size) < drop_fraction
        timestamps = timestamps[~drop_mask]
        values = values[~drop_mask]
    return timestamps.tolist(), values.astype(float).tolist()


def time_millis() -> int:
    return int(round(time.time() * 1000))


def batch_list(values, batch_size: int):
    for i in range(0, len(values), batch_size):
        yield values[i:i + batch_size]


def random_string(length=10) -> str:
    import string
    letters = string.ascii_lowercase
    return ''.join(random.choice(letters) for _ in range(length))


def _normalize_endpoint(ep: str) -> str:
    if not ep.startswith("http://") and not ep.startswith("https://"):
        ep = "http://" + ep
    return ep.rstrip("/")


def generate_write_load(card: int, per_second: int, hrs: int,
                        min_val: float, max_val: float, batch_size: int) -> List[SubmitMetricsRequestInternal]:
    submit_requests: List[SubmitMetricsRequestInternal] = []
    now = time_millis()
    start_epoch = now - 3600 * 1000 * max(1, hrs)
    end_epoch = now
    interval = max(1, 1000 // max(1, per_second))
    for _ in range(card):
        times, values = generate_random(start_epoch, end_epoch, interval, 0.0, min_val, max_val)
        if not times:
            continue
        metric = "test-" + random_string(10)
        tags: Dict[str, str] = {}
        for t_batch, v_batch in zip(batch_list(times, batch_size), batch_list(values, batch_size)):
            submit_requests.append(
                SubmitMetricsRequestInternal(
                    tenantId=TENANT_ID, metricName=metric, times=t_batch, vals=v_batch, tags=tags
                )
            )
    return submit_requests


# -------------------------
# Worker HTTP plumbing
# -------------------------
_SESSION = None  # set by _init_worker


def _init_worker(pool_size: int = 1024):
    """Initializer for each worker process to set up a pooled HTTP session."""
    global _SESSION
    s = requests.Session()
    adapter = requests.adapters.HTTPAdapter(pool_connections=pool_size, pool_maxsize=pool_size, max_retries=0)
    s.mount("http://", adapter)
    s.mount("https://", adapter)
    _SESSION = s


def _post_json(url: str, payload: dict, headers: dict, timeout: float):
    r = _SESSION.post(url, json=payload, headers=headers, timeout=timeout)
    r.raise_for_status()
    return r


# -------------------------
# Summaries & aggregation
# -------------------------
class WriteSummary(NamedTuple):
    durations_ms: List[float]   # per-request wall time (ms)
    batch_sizes: List[int]      # samples per request
    end_ts_ms: List[int]        # wall-clock end time per request (ms)
    total_ok: int
    failed: int


class ReadSummary(NamedTuple):
    secondly_lat_ms: List[float]
    minutely_lat_ms: List[float]


class AggWriteSummary(NamedTuple):
    req_per_sec: float
    samples_per_sec: float
    p50_ms: float
    p90_ms: float
    p99_ms: float


class AggReadSummary(NamedTuple):
    secondly_p50: float
    secondly_p90: float
    secondly_p99: float
    minutely_p50: float
    minutely_p90: float
    minutely_p99: float


# -------------------------
# Phase A: Ingest-only
# -------------------------
def submit_writes(ep: str, batch: List[SubmitMetricsRequestInternal],
                  timeout_s: float = DEFAULT_WRITE_TIMEOUT) -> WriteSummary:
    header = {'Content-Type': 'application/json', "Accept": "application/json"}
    url = _normalize_endpoint(ep) + "/api/v1/metrics"
    durations: List[float] = []
    sizes: List[int] = []
    ends: List[int] = []
    failed = 0
    for b in batch:
        st_ns = time.perf_counter_ns()
        try:
            _post_json(url, b.__dict__, header, timeout_s)
            dur_ms = max(0.001, (time.perf_counter_ns() - st_ns) / 1e6)
            print(f"{dur_ms}")
            durations.append(dur_ms)
            sizes.append(len(b.ts))
            ends.append(time_millis())
        except Exception:
            failed += 1
    return WriteSummary(durations, sizes, ends, len(durations), failed)


def aggregate_writes(results: List[WriteSummary], start_ms: int, end_ms: int) -> AggWriteSummary:
    lat = np.array([d for r in results for d in r.durations_ms], dtype=float)
    sizes = np.array([s for r in results for s in r.batch_sizes], dtype=float)
    if lat.size == 0:
        return AggWriteSummary(0.0, 0.0, float('nan'), float('nan'), float('nan'))

    p50, p90, p99 = np.percentile(lat, [50, 90, 99]).tolist()

    # Per-second aggregation using captured end timestamps
    end_ts = np.array([t for r in results for t in r.end_ts_ms], dtype=np.int64)
    if end_ts.size and sizes.size and end_ts.size == sizes.size:
        sec_keys = (end_ts // 1000).astype(np.int64)
        # requests per second
        _, reqs_per_sec = np.unique(sec_keys, return_counts=True)
        # samples per second
        # group-by sum
        order = np.argsort(sec_keys)
        keys_sorted = sec_keys[order]
        sizes_sorted = sizes[order]
        uniq_keys, idx_start = np.unique(keys_sorted, return_index=True)
        sums = np.add.reduceat(sizes_sorted, idx_start)
        duration_s = max(1.0, (end_ms - start_ms) / 1000.0)
        req_per_sec = float(reqs_per_sec.sum() / len(reqs_per_sec))
        samples_per_sec = float(sums.sum() / len(sums))
    else:
        # fallback to totals over window
        duration_s = max(1.0, (end_ms - start_ms) / 1000.0)
        req_per_sec = float(lat.size / duration_s)
        samples_per_sec = float(sizes.sum() / duration_s)

    return AggWriteSummary(req_per_sec, samples_per_sec, p50, p90, p99)


def export_per_second_csv(path: str, results: List[WriteSummary], start_ms: int, end_ms: int):
    end_ts = [t for r in results for t in r.end_ts_ms]
    sizes = [s for r in results for s in r.batch_sizes]
    rows = {}
    for ts, sz in zip(end_ts, sizes):
        sec = ts // 1000
        if sec not in rows:
            rows[sec] = [0, 0]
        rows[sec][0] += 1       # reqs
        rows[sec][1] += sz      # samples
    secs = range(start_ms // 1000, math.ceil(end_ms / 1000))
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["second", "requests", "samples"])
        for sec in secs:
            r, s = rows.get(sec, [0, 0])
            w.writerow([sec, r, s])


# -------------------------
# Phase B: Reads-only
# -------------------------
def submit_reads(ep: str, batch: List[SubmitMetricsRequestInternal]) -> ReadSummary:
    header = {'Content-Type': 'application/json', "Accept": "application/json"}
    url = _normalize_endpoint(ep) + "/api/v1/metrics/q"
    sec_lat: List[float] = []
    min_lat: List[float] = []

    for b in batch:
        # SECONDLY
        st = time.perf_counter_ns()
        try:
            req = ReadBackRequest(b, "SECONDLY", "P95")
            _post_json(url, req.__dict__, header, DEFAULT_READ_TIMEOUT)
            sec_lat.append((time.perf_counter_ns() - st) / 1e6)  # ms
        except Exception:
            pass

        # MINUTELY
        st = time.perf_counter_ns()
        try:
            req = ReadBackRequest(b, "MINUTELY", "P95")
            _post_json(url, req.__dict__, header, DEFAULT_READ_TIMEOUT)
            min_lat.append((time.perf_counter_ns() - st) / 1e6)  # ms
        except Exception:
            pass

    return ReadSummary(sec_lat, min_lat)


def aggregate_reads(results: List[ReadSummary]) -> 'AggReadSummary':
    sec = np.array([d for r in results for d in r.secondly_lat_ms], dtype=float)
    minu = np.array([d for r in results for d in r.minutely_lat_ms], dtype=float)

    def pct(arr: np.ndarray, p: float) -> float:
        return float(np.percentile(arr, p)) if arr.size else float('nan')

    return AggReadSummary(
        secondly_p50=pct(sec, 50), secondly_p90=pct(sec, 90), secondly_p99=pct(sec, 99),
        minutely_p50=pct(minu, 50), minutely_p90=pct(minu, 90), minutely_p99=pct(minu, 99)
    )


# -------------------------
# Benchmark runner
# -------------------------
def run_benchmark(ep: str, card: int, per_second: int, hrs: int,
                  min_val: float, max_val: float, batch_size: int,
                  warmup_s: int, measure_s: int, workers: int,
                  reads: int, pool_size: int, csv_path: Optional[str]):
    # Prepare load once so write + read hit same shapes
    load = generate_write_load(card, per_second, hrs, min_val, max_val, batch_size)
    random.shuffle(load)

    # Partition for workers
    workers = max(1, workers)
    chunk = max(1, len(load) // workers)
    write_batches = list(batch_list(load, chunk))
    read_batches = write_batches

    # Warmup (one pass over batches)
    if warmup_s > 0:
        with mp.Pool(processes=workers, initializer=_init_worker, initargs=(pool_size,)) as pool:
            _ = list(pool.starmap(submit_writes, [(ep, b) for b in write_batches], chunksize=2))

    # Phase A: Ingest-only (duration-driven loop)
    with mp.Pool(processes=workers, initializer=_init_worker, initargs=(pool_size,)) as pool:
        phase_a_start = time.time()
        deadline = phase_a_start + measure_s
        write_results: List[WriteSummary] = []
        args = [(ep, b) for b in write_batches]
        while True:
            for arg in args:
                if time.time() >= deadline:
                    break
                write_results.extend(pool.starmap(submit_writes, [arg], chunksize=1))
            if time.time() >= deadline:
                break
        phase_a_end = time.time()

    start_ms = int(phase_a_start * 1000)
    end_ms = int(phase_a_end * 1000)
    write_agg = aggregate_writes(write_results, start_ms, end_ms)

    # Optional CSV export (per-second series)
    if csv_path:
        export_per_second_csv(csv_path, write_results, start_ms, end_ms)

    # Phase B: Reads-only
    if reads:
        with mp.Pool(processes=workers, initializer=_init_worker, initargs=(pool_size,)) as pool:
            read_results = list(pool.starmap(submit_reads, [(ep, b) for b in read_batches], chunksize=2))
        read_agg = aggregate_reads(read_results)
    else:
        read_agg = AggReadSummary(*(float('nan'),) * 6)

    # Report
    print("=== Okapi Benchmark ===")
    print(f"endpoint           : {_normalize_endpoint(ep)}")
    print(f"workers            : {workers}")
    print(f"cardinality        : {card}")
    print(f"batch_size         : {batch_size}")
    print(f"backfill_hours     : {hrs}")
    print(f"target_points/sec  : {per_second} (used to shape timestamp spacing)")
    print(f"warmup_seconds     : {warmup_s}")
    print(f"measure_seconds    : {measure_s}  (actual ingest window = {(phase_a_end - phase_a_start):.2f}s)")
    print("")
    print(f"Ingest: {write_agg.samples_per_sec:,.0f} samples/s ({write_agg.req_per_sec:,.0f} req/s, batch≈{batch_size})")
    print(f"Ingest latency (ms): p50={write_agg.p50_ms:.1f}  p90={write_agg.p90_ms:.1f}  p99={write_agg.p99_ms:.1f}")
    if csv_path:
        print(f"Per-second CSV     : {csv_path}")
    print("")
    if reads:
        print("Reads:")
        print(f"  SECONDLY  p50={fmt_us(read_agg.secondly_p50)}  "
              f"p90={fmt_us(read_agg.secondly_p90)}  p99={fmt_us(read_agg.secondly_p99)}")
        print(f"  MINUTELY  p50={fmt_us(read_agg.minutely_p50)}  "
              f"p90={fmt_us(read_agg.minutely_p90)}  p99={fmt_us(read_agg.minutely_p99)}")
    else:
        print("Reads phase        : skipped (use --reads 1 to enable)")


# -------------------------
# CLI
# -------------------------
if __name__ == "__main__":
    ap = ArgumentParser(description="Okapi benchmark: ingest-only then reads-only, with CSV export")
    ap.add_argument("--ep", default="http://localhost:9000", type=str, help="Okapi endpoint")
    ap.add_argument("--workers", default=1, type=int, help="number of worker processes")
    ap.add_argument("--card", default=10, type=int, help="distinct metric series")
    ap.add_argument("--ps", dest="per_second", default=1, type=int, help="points/second per series (shapes spacing)")
    ap.add_argument("--min", dest="min_val", default=0.0, type=float, help="min value")
    ap.add_argument("--max", dest="max_val", default=100.0, type=float, help="max value")
    ap.add_argument("--hrs", default=6, type=int, help="hours of backfilled data to generate")
    ap.add_argument("--batch_size", default=200, type=int, help="samples per write request")
    ap.add_argument("--warmup_seconds", default=3, type=int, help="warmup writes before measuring")
    ap.add_argument("--seconds", dest="measure_seconds", default=30, type=int, help="measurement window")
    ap.add_argument("--reads", default=1, type=int, help="1=run reads phase, 0=skip")
    ap.add_argument("--pool", default=1024, type=int, help="per-process HTTP connection pool size")
    ap.add_argument("--csv", dest="csv_path", default="./okapi-bench.csv", type=str, help="optional path to write per-second CSV")
    args = ap.parse_args()

    run_benchmark(
        ep=args.ep,
        card=args.card,
        per_second=args.per_second,
        hrs=args.hrs,
        min_val=args.min_val,
        max_val=args.max_val,
        batch_size=args.batch_size,
        warmup_s=args.warmup_seconds,
        measure_s=args.measure_seconds,
        workers=args.workers,
        reads=args.reads,
        pool_size=args.pool,
        csv_path=args.csv_path,
    )
