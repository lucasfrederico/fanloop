# Changelog

All notable changes to this project are documented here.

## Unreleased

### Added

- Terraform module (`terraform/`) that provisions the AWS platform: VPC (3 AZs,
  private + public subnets, single NAT gateway), EKS managed node group,
  ElastiCache Redis replication group (managed backplane), and ArgoCD bootstrap
  via Helm. GitOps handles app delivery after a single `kubectl apply` handoff.
- CI job `terraform` that validates `fmt`, `init -backend=false`, and `validate`
  on every push and pull request.

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
