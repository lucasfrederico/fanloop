# fanloop — Design Spec

**Date:** 2026-05-28
**Status:** Approved, pending implementation plan
**Author:** Lucas Frederico
**Repo target:** github.com/lucasfrederico/fanloop (MIT)

---

## Summary

fanloop is a self-hostable real-time fan-out gateway. Clients subscribe to named channels over WebSocket. Publishers send events over an HTTP API. The gateway delivers each event to every subscriber of that channel, across all server replicas, using a Redis pub/sub backplane. It is horizontally scalable, observable by default, and deployed to Kubernetes through an ArgoCD GitOps workflow.

The project doubles as a portfolio piece. It productizes the cross-shard pub/sub pattern the author ran in production for 5 years (a live JVM platform at 1,500 concurrent users, sub-50ms p95) and demonstrates Kubernetes and GitOps platform engineering end to end.

## Goals

- A genuinely useful product: anyone can self-host real-time fan-out without paying for a SaaS (Pusher, Ably, PubNub).
- Prove horizontal scale via a Redis backplane, with no sticky sessions.
- Observability as a first-class feature: connections, throughput, fan-out latency percentiles, backplane lag.
- A complete Kubernetes + GitOps story: containerized, Helm-packaged, Kustomize-composed, ArgoCD-deployed, Prometheus/Grafana-monitored.
- CI that proves the system actually runs (kind cluster + ArgoCD sync + smoke test), not just that manifests lint.
- Match the author's existing OSS quality bar: MIT, green CI, strong README, tagged releases, multi-arch image on GHCR.

## Non-goals (for now)

- Not a message broker with durability or replay in v0.1 (events are fire-and-forget fan-out).
- Not a managed service or multi-tenant control plane.
- No client SDKs beyond documented raw WebSocket and HTTP usage in v0.1.

## Architecture

### The scaling model (core idea)

Each server replica holds a subset of live WebSocket connections in memory, with a per-replica registry mapping channel name to the set of local sessions subscribed to it. When a publish arrives at any replica:

1. The replica validates the publisher API key.
2. It runs `PUBLISH fanloop:{channel} {payload}` on Redis.
3. Every replica is subscribed to the Redis channel pattern and receives the payload.
4. Each replica looks up its local sessions for that channel and writes the event to each WebSocket.

No sticky sessions are required. Any replica can accept any connection and any publish. This is the standard backplane pattern used by Socket.IO, SignalR, and Pusher, and it is the exact pattern the author ran cross-shard at LoverCraft.

**Rejected alternatives:** database polling (not real-time, high latency), or a replica-to-replica mesh (does not scale, N-squared connections). Redis pub/sub is the industry-standard choice and aligns with the author's production experience.

### Components

**fanloop-server** (Spring Boot 4.0.x on Java 25):

- WebSocket endpoint `/ws`. Clients send JSON control frames: `{"action":"subscribe","channel":"..."}` and `{"action":"unsubscribe","channel":"..."}`. Server pushes event frames `{"channel":"...","data":...}`.
- HTTP publish API `POST /publish/{channel}`, authenticated with an `X-API-Key` header, body is the event payload (arbitrary JSON), returns `202 Accepted`.
- Redis backplane: publish path issues `PUBLISH`; each instance holds a `SUBSCRIBE` listener that forwards to local subscribers.
- In-memory connection registry per replica: channel -> set of sessions.
- Auth (v0.1): API key required for publish. Subscribe is open to any channel. Signed subscriber tokens and private channels are deferred to v0.2.
- Observability: Micrometer with `/actuator/prometheus`. Metrics: active connections (gauge), subscriptions per channel, messages published per second, fan-out latency timer (p50/p95/p99), Redis backplane round-trip lag.
- Health: `/actuator/health` with liveness and readiness probes. Readiness fails if Redis is unreachable.
- Graceful shutdown on SIGTERM: stop accepting new connections, close existing WebSockets with a going-away code, deregister the Redis listener. Reflects the author's "recovery under 60s" production discipline.

**Redis**: the pub/sub backplane. Deployed in-cluster as a Helm dependency for the demo, or pointed at an external instance via configuration.

### Platform layer (the Kubernetes + GitOps story)

- **Dockerfile**: multi-stage build of the Spring Boot jar, running on a slim `eclipse-temurin:25` JRE base. Multi-arch (amd64, arm64) image pushed to GHCR, matching the pgcraft release pattern.
- **Helm chart** (`charts/fanloop`): packages the application. Templates Deployment (or Rollout in v0.2), Service, ConfigMap, HPA, ServiceMonitor, and a Redis sub-chart dependency. Values cover replica count, image tag, resources, and autoscaling. This is the artifact a user installs.
- **Kustomize** (`gitops/`): composes the cluster via an app-of-apps layout with `dev` and `prod` overlays. Division of labor: Helm packages the app, Kustomize composes the GitOps configuration. Both are used authentically for different purposes, as real platforms do.
- **ArgoCD app-of-apps**: a root `Application` points to child Applications: fanloop (the Helm release), kube-prometheus-stack (monitoring), Redis, and in v0.2 argo-rollouts and external-secrets. Cluster state equals what is in git.
- **kube-prometheus-stack**: Prometheus scrapes the fanloop ServiceMonitor. A Grafana dashboard is committed to the repo (as JSON in a ConfigMap) showing active connections, messages per second, fan-out p50/p95/p99, and backplane lag.
- **Argo Rollouts** (v0.2): replaces the Deployment with a Rollout performing a canary release (20% -> 50% -> 100%) with an analysis pause gated on the fan-out latency metric.
- **external-secrets** (v0.2): sources the publisher API key and Redis credentials from a secret store.

## Data flow (publish to fan-out)

1. Publisher: `POST /publish/orders` with `X-API-Key` and a JSON body.
2. The receiving replica validates the key, runs `PUBLISH fanloop:orders {body}`, and returns `202`.
3. All replicas receive the message from Redis.
4. Each replica looks up local sessions subscribed to `orders` and writes the event to each WebSocket.
5. Subscribers receive the event in real time, regardless of which replica they are connected to.

## Testing strategy

- **Unit:** channel registry, API-key auth, control-frame parsing.
- **Integration (Testcontainers):** a real Redis plus two in-process server instances. Assert cross-instance fan-out, message ordering within a channel, and correct unsubscribe behavior.
- **End-to-end (CI on kind):** a real cluster with a real ArgoCD sync. A WebSocket client receives an event published over HTTP, with at least two replicas to prove the backplane.
- **Load (optional, on-brand):** a small harness measuring fan-out p95 under concurrent connections, echoing the author's JMH/latency discipline from tickloop.

## CI / CD

- **`ci.yml`** (on push and PR):
  1. Build and run unit and integration tests (Testcontainers Redis).
  2. `helm lint` and `helm template`, `kustomize build` on overlays, `kubeconform` on rendered manifests.
  3. Spin up a **kind** cluster, install ArgoCD, apply the app-of-apps, wait for sync.
  4. Run a **smoke test**: connect a WebSocket client, publish via HTTP, assert the client receives the event, exercising 2+ replicas to prove the backplane.
- **`release.yml`** (on `v*` tag): build and push the multi-arch image to GHCR, package the Helm chart, and generate release notes. Matches the author's existing release workflow pattern.

## Stack versions (latest, per decision)

- Java 25 (current LTS)
- Spring Boot 4.0.x
- Micrometer (latest) with Prometheus registry
- Redis (latest stable) via Bitnami Helm chart
- Kubernetes 1.3x (kind for CI and local)
- ArgoCD (latest), Argo Rollouts (latest, v0.2)
- kube-prometheus-stack (latest)
- Build: Maven (multi-module if needed), Java Operator SDK not used (this is a service, not a controller)

## Repo conventions (match existing OSS)

- MIT LICENSE.
- README with: title, one-line tagline blockquote, badges (CI, License MIT, Java 25, Spring Boot 4), "Why this exists", "Status" (current version), "Quickstart" (docker run + helm install), "How it scales" (the backplane explanation), "Deploy with ArgoCD", "Observability" (the Grafana dashboard), "Configuration".
- CHANGELOG.md following the tickloop style.
- `.github/workflows/ci.yml` and `release.yml`.
- Conventional, clear commit messages.

## Phasing (each phase is a clean release; contains scope risk)

- **v0.1.0** (target ~1 to 1.5 weeks, already a complete and credible repo):
  fanloop-server (WebSocket subscribe, HTTP publish, Redis backplane, API-key auth, actuator/prometheus, graceful drain), multi-arch Dockerfile on GHCR, Helm chart, Kustomize overlays, ArgoCD app-of-apps (fanloop + Redis + kube-prometheus-stack), committed Grafana dashboard, HPA, CI with kind + ArgoCD + smoke test, README + LICENSE + CHANGELOG.
- **v0.2.0:** Argo Rollouts canary with metric analysis, external-secrets, SSE transport, signed subscriber tokens with private channels.
- **v0.3.0:** presence channels (who is online), per-key rate limiting, short message history and replay.

## Recruiter-signal framing

The repo demonstrates, in one coherent product: real-time distributed systems (the backplane), Kubernetes platform engineering (Helm, Kustomize, HPA, probes, graceful drain), GitOps (ArgoCD app-of-apps, and canary via Argo Rollouts in v0.2), and observability (Prometheus metrics plus a committed Grafana dashboard). It closes the Kubernetes and ArgoCD gap flagged on the Bitso Platform Engineer evaluation and strengthens any platform or SRE application, while the underlying problem (real-time fan-out at scale) is one the author has actually operated in production.
