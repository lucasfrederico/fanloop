# fanloop

> Real-time fan-out gateway. Clients subscribe to channels over WebSocket,
> publishers send events over HTTP, and the gateway delivers each event to
> every subscriber across all replicas using a Redis backplane. Self-hostable,
> observable, deployed with ArgoCD GitOps.

[![CI](https://github.com/lucasfrederico/fanloop/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/lucasfrederico/fanloop/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 25](https://img.shields.io/badge/Java-25-orange)](https://openjdk.org/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)](https://spring.io/projects/spring-boot)

## Why this exists

Real-time fan-out (push an event, deliver it to every connected subscriber) is
a recurring need: live dashboards, notifications, presence, collaborative apps,
game lobbies. The usual answer is a paid SaaS (Pusher, Ably, PubNub) or a
single-process WebSocket server that cannot scale past one box.

fanloop is the self-hostable middle ground. It scales horizontally with no
sticky sessions, because every replica shares a Redis pub/sub backplane: a
publish on any replica reaches subscribers on all replicas. This is the same
cross-shard pattern used to run a live JVM platform at 1,500 concurrent users.

## Status

**v0.1.0.** WebSocket subscribe, HTTP publish, Redis backplane, API-key auth,
Prometheus metrics, graceful drain. Helm chart, Kustomize overlays, ArgoCD
app-of-apps, and a committed Grafana dashboard. CI runs the whole thing on a
kind cluster with a cross-replica smoke test. See [CHANGELOG.md](CHANGELOG.md).

## Quickstart (Docker)

```bash
docker run -d --name redis redis:7-alpine
docker run -d --name fanloop -p 8080:8080 \
  -e REDIS_HOST=host.docker.internal \
  -e FANLOOP_API_KEY=my-secret \
  ghcr.io/lucasfrederico/fanloop:latest
```

Subscribe (any WebSocket client):

```
ws://localhost:8080/ws
> {"action":"subscribe","channel":"orders"}
```

Publish:

```bash
curl -X POST http://localhost:8080/publish/orders \
  -H "X-API-Key: my-secret" \
  -H "Content-Type: application/json" \
  -d '{"id":42,"status":"shipped"}'
```

The subscriber receives `{"channel":"orders","data":{"id":42,"status":"shipped"}}`.

## How it scales

Each replica keeps its WebSocket connections in memory and a map of channel to
local sessions. On publish, the receiving replica calls `PUBLISH fanloop:{channel}`
on Redis. Every replica subscribes to `fanloop:*` and forwards the event to its
own local subscribers. No replica needs to know about connections on other
replicas, and no load balancer affinity is required.

## Provision on AWS with Terraform

The `terraform/` module stands up the full AWS platform: VPC (3 AZs, private +
public subnets, single NAT gateway), EKS cluster with a managed node group,
and an ElastiCache Redis replication group as the managed backplane. It also
bootstraps ArgoCD via Helm. Once `terraform apply` completes, run one
`kubectl apply` to hand app delivery over to GitOps:

```bash
cd terraform
terraform init        # configure the S3 backend first (see terraform/README.md)
terraform plan
terraform apply
aws eks update-kubeconfig --region us-east-1 --name fanloop
kubectl apply -f gitops/bootstrap/root-app.yaml   # GitOps takes over here
```

See [terraform/README.md](terraform/README.md) for prerequisites, cost
warnings, variables, and teardown instructions.

## Deploy with ArgoCD

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl apply -f gitops/bootstrap/root-app.yaml
```

The app-of-apps deploys fanloop (with its Redis dependency) and
kube-prometheus-stack. Cluster state tracks `master`.

## Observability

fanloop exposes Prometheus metrics at `/actuator/prometheus`: active
connections, publishes per second, and fan-out latency (p50, p95, p99). A
Grafana dashboard is committed at `observability/grafana-dashboard.json` and is
auto-loaded by the Grafana sidecar when deployed via the app-of-apps.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `REDIS_HOST` | `localhost` | Redis backplane host |
| `REDIS_PORT` | `6379` | Redis backplane port |
| `FANLOOP_API_KEY` | `dev-key` | API key required to publish |

## Tech stack

Java 25, Spring Boot 3.5 (servlet stack with virtual threads),
spring-boot-starter-websocket, Spring Data Redis (Lettuce), Micrometer with
Prometheus. Packaged as a multi-arch image on GHCR, a Helm chart, and Kustomize
overlays. Deployed via an ArgoCD app-of-apps with kube-prometheus-stack.

## Roadmap

- v0.2: Argo Rollouts canary with metric analysis, external-secrets, SSE
  transport, signed subscriber tokens with private channels.
- v0.3: presence channels, per-key rate limiting, short message history.

## License

MIT. See [LICENSE](LICENSE).
