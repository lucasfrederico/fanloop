# Changelog

All notable changes to this project are documented here.

## v0.1.0 (2026-05-29)

Initial release.

### Added
- WebSocket endpoint `/ws` with subscribe and unsubscribe control frames.
- HTTP publish API `POST /publish/{channel}` with API-key auth.
- Redis pub/sub backplane for cross-replica fan-out (no sticky sessions).
- Prometheus metrics: active connections, publishes per second, fan-out
  latency p50/p95/p99.
- Liveness and readiness probes; graceful drain of WebSocket connections on
  shutdown.
- Multi-arch Docker image on GHCR.
- Helm chart with HPA, probes, ServiceMonitor, and a Redis dependency.
- Kustomize dev and prod overlays.
- ArgoCD app-of-apps deploying fanloop and kube-prometheus-stack.
- Committed Grafana dashboard.
- CI that builds, tests (Testcontainers), lints manifests, and runs an
  end-to-end smoke test on a kind cluster across two replicas.
