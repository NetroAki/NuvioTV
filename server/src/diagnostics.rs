use std::{
    sync::atomic::{AtomicU64, Ordering},
    time::Instant,
};

use serde::Serialize;

pub struct Diagnostics {
    started: Instant,
    requests: AtomicU64,
    in_flight: AtomicU64,
    failures: AtomicU64,
    total_latency_micros: AtomicU64,
}

impl Diagnostics {
    pub fn new() -> Self {
        Self {
            started: Instant::now(),
            requests: AtomicU64::new(0),
            in_flight: AtomicU64::new(0),
            failures: AtomicU64::new(0),
            total_latency_micros: AtomicU64::new(0),
        }
    }

    pub fn begin_request(&self) -> Instant {
        self.requests.fetch_add(1, Ordering::Relaxed);
        self.in_flight.fetch_add(1, Ordering::Relaxed);
        Instant::now()
    }

    pub fn finish_request(&self, started: Instant, successful: bool) {
        self.in_flight.fetch_sub(1, Ordering::Relaxed);
        if !successful {
            self.failures.fetch_add(1, Ordering::Relaxed);
        }
        let micros = started.elapsed().as_micros().min(u128::from(u64::MAX)) as u64;
        self.total_latency_micros
            .fetch_add(micros, Ordering::Relaxed);
    }

    pub fn snapshot(&self) -> DiagnosticsSnapshot {
        let requests = self.requests.load(Ordering::Relaxed);
        let latency = self.total_latency_micros.load(Ordering::Relaxed);
        DiagnosticsSnapshot {
            uptime_seconds: self.started.elapsed().as_secs(),
            requests,
            in_flight: self.in_flight.load(Ordering::Relaxed),
            failures: self.failures.load(Ordering::Relaxed),
            average_latency_micros: (requests > 0).then_some(latency / requests),
        }
    }
}

impl Default for Diagnostics {
    fn default() -> Self {
        Self::new()
    }
}

#[derive(Debug, Clone, Serialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticsSnapshot {
    pub uptime_seconds: u64,
    pub requests: u64,
    pub in_flight: u64,
    pub failures: u64,
    pub average_latency_micros: Option<u64>,
}
