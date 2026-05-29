variable "region" {
  description = "AWS region where all resources are created."
  type        = string
  default     = "us-east-1"
}

variable "cluster_name" {
  description = "Name of the EKS cluster and the prefix used for related resources (VPC, security groups, etc.)."
  type        = string
  default     = "fanloop"
}

variable "kubernetes_version" {
  description = "Kubernetes version for the EKS control plane and managed node group."
  type        = string
  default     = "1.32"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "node_instance_types" {
  description = "EC2 instance types for the EKS managed node group."
  type        = list(string)
  default     = ["t3.large"]
}

variable "node_desired_size" {
  description = "Desired number of worker nodes."
  type        = number
  default     = 2
}

variable "node_min_size" {
  description = "Minimum number of worker nodes."
  type        = number
  default     = 1
}

variable "node_max_size" {
  description = "Maximum number of worker nodes."
  type        = number
  default     = 4
}

variable "redis_node_type" {
  description = "ElastiCache node type for the Redis backplane."
  type        = string
  default     = "cache.t4g.micro"
}
