# Bootstrap ArgoCD via Helm. Terraform only installs the controller.
# The root Application (app-of-apps) is applied manually after terraform apply:
#
#   kubectl apply -f gitops/bootstrap/root-app.yaml
#
# This avoids the CRD chicken-and-egg problem: the Application CRD does not
# exist until ArgoCD is running, so a kubernetes_manifest for the root-app
# would fail at plan time. The manual step is documented in outputs.tf.

resource "helm_release" "argocd" {
  name             = "argocd"
  repository       = "https://argoproj.github.io/argo-helm"
  chart            = "argo-cd"
  version          = "~> 7.0"
  namespace        = "argocd"
  create_namespace = true

  # Wait for all ArgoCD pods to be ready before Terraform marks the release
  # as complete. This ensures the CRDs are installed before the handoff step.
  wait    = true
  timeout = 600

  set {
    name  = "server.service.type"
    value = "ClusterIP"
  }

  # Disable the built-in Dex (SSO) for a minimal bootstrap install.
  set {
    name  = "dex.enabled"
    value = "false"
  }
}
