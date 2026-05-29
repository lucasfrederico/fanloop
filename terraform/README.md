# fanloop Terraform Module

This module provisions the AWS platform that fanloop runs on. Terraform owns
the **platform layer** (VPC, EKS, ElastiCache). ArgoCD owns the **application
layer** (fanloop itself, kube-prometheus-stack). The two layers are connected
by a single `kubectl apply` handoff step after `terraform apply`.

## What it provisions

| Resource | Details |
|---|---|
| VPC | 3 AZs, private + public subnets, single NAT gateway |
| EKS | Managed node group, public endpoint, Kubernetes 1.32 |
| ElastiCache Redis | Single-node replication group, private subnets |
| ArgoCD | Installed via Helm into the `argocd` namespace |

## Prerequisites

- AWS CLI configured with credentials that can create VPCs, EKS clusters,
  ElastiCache, and IAM resources.
- An S3 bucket and a DynamoDB table for remote state (see `backend.tf` for
  setup instructions). Skip for local experimentation.
- Terraform >= 1.5.0 (`terraform -version`).
- `kubectl` and `helm` installed locally.

## COST WARNING

Running this module creates billable AWS resources:

- **EKS control plane**: ~$0.10/hour.
- **EC2 nodes** (t3.large x2 default): ~$0.17/hour per node.
- **NAT Gateway**: ~$0.045/hour + data transfer.
- **ElastiCache** (cache.t4g.micro): ~$0.017/hour.

Estimated minimum: **~$0.40/hour (~$290/month)**. Run `terraform destroy` when
done to avoid ongoing charges.

## Quickstart

```bash
cd terraform

# 1. Initialize providers and modules (no real AWS calls, no backend).
terraform init -backend=false

# For real apply, configure backend.tf first, then:
# terraform init

# 2. Review the plan.
terraform plan

# 3. Apply (creates all AWS resources).
terraform apply

# 4. Configure kubectl.
aws eks update-kubeconfig --region us-east-1 --name fanloop

# 5. Hand off app delivery to ArgoCD GitOps.
kubectl apply -f gitops/bootstrap/root-app.yaml
```

After step 5, ArgoCD takes over. It reads `gitops/` from the `master` branch
and deploys fanloop and kube-prometheus-stack into the cluster. Track sync
status with:

```bash
kubectl -n argocd get app --all-namespaces
```

## GitOps handoff

Terraform deliberately does NOT apply `gitops/bootstrap/root-app.yaml`. The
ArgoCD `Application` CRD is only available after `helm_release.argocd` is
complete, so applying the root-app at plan time would fail. The manual `kubectl
apply` step is the clean boundary between platform engineering (Terraform) and
application delivery (ArgoCD).

## Get the ArgoCD admin password

```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath='{.data.password}' | base64 -d && echo
```

## Tear down

```bash
# Remove the ArgoCD root-app and let ArgoCD delete managed apps first.
kubectl delete -f gitops/bootstrap/root-app.yaml

# Then destroy all Terraform-managed AWS resources.
terraform destroy
```

## Variables

| Name | Default | Description |
|---|---|---|
| `region` | `us-east-1` | AWS region |
| `cluster_name` | `fanloop` | EKS cluster name and resource prefix |
| `kubernetes_version` | `1.32` | EKS Kubernetes version |
| `vpc_cidr` | `10.0.0.0/16` | VPC CIDR block |
| `node_instance_types` | `["t3.large"]` | EC2 instance types for the node group |
| `node_desired_size` | `2` | Desired node count |
| `node_min_size` | `1` | Minimum node count |
| `node_max_size` | `4` | Maximum node count |
| `redis_node_type` | `cache.t4g.micro` | ElastiCache node type |

## Outputs

| Name | Description |
|---|---|
| `cluster_name` | EKS cluster name |
| `region` | AWS region |
| `configure_kubectl` | `aws eks update-kubeconfig` command |
| `redis_primary_endpoint` | Redis endpoint address (set as `REDIS_HOST`) |
| `argocd_admin_password_cmd` | Command to read the initial ArgoCD admin secret |
| `gitops_handoff` | The `kubectl apply` command to activate GitOps |
