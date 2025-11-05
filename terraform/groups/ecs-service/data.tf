data "vault_generic_secret" "stack_secrets" {
  path = local.vault_stack_path
}

data "aws_kms_key" "kms_key" {
  key_id = local.kms_alias
}

data "vault_generic_secret" "service_secrets" {
  path = "${local.vault_stack_path}/${local.service_name}"
}

data "aws_vpc" "vpc" {
  filter {
    name   = "tag:Name"
    values = [local.vpc_name]
  }
}

#Get application subnet IDs
data "aws_subnets" "application" {
  filter {
    name   = "tag:Name"
    values = [local.application_subnet_pattern]
  }
}

data "aws_ecs_cluster" "ecs_cluster" {
  cluster_name = "${local.name_prefix}-cluster"
}

data "aws_iam_role" "ecs_cluster_iam_role" {
  name = "${local.name_prefix}-ecs-task-execution-role"
}

data "aws_lb" "service_lb" {
  count = var.file_transfer_create_ecs ? 1 : 0

  name = "alb-${var.environment}-file-transfer"
}

data "aws_lb_listener" "service_lb_listener" {
  count = var.file_transfer_create_ecs ? 1 : 0

  load_balancer_arn = data.aws_lb.service_lb[0].arn
  port              = 443
}


data "aws_lb" "service_lb_secure" {
  count = var.secure_file_transfer_create_ecs ? 1 : 0

  name = "alb-${var.environment}-secure-file-transfer"
}

data "aws_lb_listener" "service_lb_listener_secure" {
  count = var.secure_file_transfer_create_ecs ? 1 : 0

  load_balancer_arn = data.aws_lb.service_lb_secure[0].arn
  port              = 443
}

# retrieve all secrets for this stack using the stack path
data "aws_ssm_parameters_by_path" "secrets" {
  path = "/${local.name_prefix}"
}

# create a list of secrets names to retrieve them in a nicer format and lookup each secret by name
data "aws_ssm_parameter" "secret" {
  for_each = toset(data.aws_ssm_parameters_by_path.secrets.names)
  name     = each.key
}

# retrieve all global secrets for this env using global path
data "aws_ssm_parameters_by_path" "global_secrets" {
  path = "/${local.global_prefix}"
}
# create a list of secrets names to retrieve them in a nicer format and lookup each secret by name
data "aws_ssm_parameter" "global_secret" {
  for_each = toset(data.aws_ssm_parameters_by_path.global_secrets.names)
  name     = each.key
}

// --- s3 bucket for shared services config ---
data "vault_generic_secret" "shared_s3" {
  path = "aws-accounts/shared-services/s3"
}

data "aws_kms_alias" "file_transfer_encryption_key_alias_secure" {
  count = var.secure_file_transfer_create_ecs ? 1 : 0

  name = var.file_transfer_kms_alias_secure
}

data "aws_kms_alias" "file_transfer_encryption_key_alias" {
  count = var.file_transfer_create_ecs ? 1 : 0

  name = var.file_transfer_kms_alias
}

data "aws_s3_bucket" "file_transfer_bucket" {
  count = var.file_transfer_create_ecs ? 1 : 0

  bucket = var.file_transfer_bucket
}

data "aws_s3_bucket" "file_transfer_bucket_secure" {
  count = var.secure_file_transfer_create_ecs ? 1 : 0

  bucket = var.file_transfer_bucket_secure
}

# IAM policy documents for ECS task roles
data "aws_iam_policy_document" "task_assume" {
  count = var.file_transfer_create_ecs ? 1 : 0

  statement {
    sid     = "AllowTaskAssumeRoleFile"
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "task_assume_secure" {
  count = var.secure_file_transfer_create_ecs ? 1 : 0

  statement {
    sid     = "AllowTaskAssumeRoleSecure"
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "file_transfer_ecs_execution" {
  count = var.file_transfer_create_ecs ? 1 : 0

  statement {
    sid    = "S3RootBucketAllow"
    effect = "Allow"

    actions = [
      "s3:ListBucket"
    ]

    resources = [
      data.aws_s3_bucket.file_transfer_bucket[0].arn
    ]
  }

  statement {
    sid    = "AllowS3ObjectActions"
    effect = "Allow"

    actions = [
      "s3:PutObject",
      "s3:GetObject",
      "s3:DeleteObject",
      "s3:GetObjectAcl",
      "s3:GetObjectTagging",
      "s3:PutObjectTagging"
    ]

    resources = [
      "${data.aws_s3_bucket.file_transfer_bucket[0].arn}/*"
    ]
  }

  statement {
    sid    = "AllowAccessForKeyFile"
    effect = "Allow"

    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:GenerateDataKey"
    ]

    resources = [data.aws_kms_alias.file_transfer_encryption_key_alias[0].target_key_arn]
  }
}

data "aws_iam_policy_document" "file_transfer_secure_ecs_execution" {
  count = var.secure_file_transfer_create_ecs ? 1 : 0

  statement {
    sid    = "S3RootBucketAllow"
    effect = "Allow"

    actions = [
      "s3:ListBucket"
    ]

    resources = [
      data.aws_s3_bucket.file_transfer_bucket_secure[0].arn
    ]
  }

  statement {
    sid    = "AllowS3ObjectActions"
    effect = "Allow"

    actions = [
      "s3:PutObject",
      "s3:GetObject",
      "s3:DeleteObject",
      "s3:GetObjectAcl",
      "s3:GetObjectTagging",
      "s3:PutObjectTagging"
    ]

    resources = [
      "${data.aws_s3_bucket.file_transfer_bucket_secure[0].arn}/*"
    ]
  }

  statement {
    sid    = "AllowAccessForKeyFile"
    effect = "Allow"

    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:GenerateDataKey"
    ]

    resources = [data.aws_kms_alias.file_transfer_encryption_key_alias_secure[0].target_key_arn]
  }
}
