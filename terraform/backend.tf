# Remote state backend (S3 + DynamoDB).
#
# Before running `terraform init` against a real AWS account:
#   1. Create an S3 bucket (e.g. fanloop-tfstate-<account-id>) with versioning enabled.
#   2. Create a DynamoDB table (e.g. fanloop-tfstate-lock) with a partition key named
#      LockID (String) for state locking.
#   3. Uncomment the block below and fill in the values.
#
# For local validation the -backend=false flag is used instead:
#   terraform init -backend=false
#
# terraform {
#   backend "s3" {
#     bucket         = "fanloop-tfstate-<account-id>"
#     key            = "fanloop/terraform.tfstate"
#     region         = "us-east-1"
#     dynamodb_table = "fanloop-tfstate-lock"
#     encrypt        = true
#   }
# }
