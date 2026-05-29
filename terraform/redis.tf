# Security group allowing Redis traffic from EKS worker nodes.
resource "aws_security_group" "redis" {
  name        = "${var.cluster_name}-redis"
  description = "Allow Redis access from EKS worker nodes."
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "Redis from EKS nodes"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name      = "${var.cluster_name}-redis"
    Project   = var.cluster_name
    ManagedBy = "terraform"
  }
}

# Subnet group for ElastiCache placed in the private subnets.
resource "aws_elasticache_subnet_group" "redis" {
  name       = "${var.cluster_name}-redis"
  subnet_ids = module.vpc.private_subnets

  tags = {
    Project   = var.cluster_name
    ManagedBy = "terraform"
  }
}

# Managed Redis replication group (single node, production-ready backplane).
# To add read replicas, increase num_cache_clusters. For Multi-AZ, set
# automatic_failover_enabled = true and num_cache_clusters >= 2.
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${var.cluster_name}-redis"
  description          = "fanloop Redis backplane"

  engine               = "redis"
  engine_version       = "7.1"
  node_type            = var.redis_node_type
  port                 = 6379
  num_cache_clusters   = 1
  parameter_group_name = "default.redis7"

  subnet_group_name  = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = false # Set true + update app TLS config before prod.

  tags = {
    Project   = var.cluster_name
    ManagedBy = "terraform"
  }
}
