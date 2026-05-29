# tflint configuration for the fanloop Terraform module.
#
# The built-in terraform ruleset (bundled with tflint) runs without any
# network access. The aws plugin is commented out here because it requires
# a download step (tflint --init) and an internet connection. Enable it
# in environments with network access for additional AWS-specific checks.

config {
  format = "compact"
}

plugin "terraform" {
  enabled = true
  preset  = "recommended"
}

# Uncomment to enable the AWS ruleset (requires: tflint --init + internet access).
# plugin "aws" {
#   enabled = true
#   version = "0.38.0"
#   source  = "github.com/terraform-linters/tflint-ruleset-aws"
# }
