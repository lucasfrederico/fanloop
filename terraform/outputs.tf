output "cluster_name" {
  description = "EKS cluster name."
  value       = module.eks.cluster_name
}

output "region" {
  description = "AWS region where the cluster is deployed."
  value       = var.region
}

output "configure_kubectl" {
  description = "Run this command to configure kubectl for the new cluster."
  value       = "aws eks update-kubeconfig --region ${var.region} --name ${module.eks.cluster_name}"
}

output "redis_primary_endpoint" {
  description = "ElastiCache Redis primary endpoint address. Set as REDIS_HOST in fanloop config."
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "argocd_admin_password_cmd" {
  description = "Command to retrieve the initial ArgoCD admin password."
  value       = "kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d && echo"
}

output "gitops_handoff" {
  description = "Run this after terraform apply to hand app delivery over to ArgoCD GitOps."
  value       = "kubectl apply -f gitops/bootstrap/root-app.yaml"
}
