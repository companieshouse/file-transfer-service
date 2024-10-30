data "vault_generic_secret" "stack_secrets" {
  path = "applications/${var.aws_profile}/${var.environment}/${local.stack_name}-stack"
}

data "aws_kms_key" "kms_key" {
  key_id = local.kms_alias
}

data "vault_generic_secret" "service_secrets" {
  path = "applications/${var.aws_profile}/${var.environment}/${local.stack_name}-stack/${local.service_name}"
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
  name = "${var.environment}-chs-internalapi"
}

data "aws_lb_listener" "service_lb_listener" {
  load_balancer_arn = data.aws_lb.service_lb.arn
  port = 443

}


data "aws_lb" "service_lb_secure" {
  name = "${var.environment}-chs-secure-data-app"
}

data "aws_lb_listener" "service_lb_listener_secure" {
  load_balancer_arn = data.aws_lb.service_lb_secure.arn
  port = 443

}

resource "aws_lb_listener_rule" "redirect_rule" {
  listener_arn = data.aws_lb_listener.service_lb_listener_secure.arn
  priority     = 143
  action {
    type = "redirect"

    redirect {
      port        = "3000"
      protocol    = "HTTP"
      host        = data.aws_lb_target_group.secure_target_group.name
      path        = "/files"
      status_code = "HTTP_301"
    }
  }

  condition {
    path_pattern {
      values = ["*/secure/files"]
    }
  }
}

data "aws_lb_target_group" "secure_target_group" {
  name = "${var.environment}-${local.service_name}-secu-far"
}


# retrieve all secrets for this stack using the stack path
data "aws_ssm_parameters_by_path" "secrets" {
  path = "/${local.name_prefix}"
}

# create a list of secrets names to retrieve them in a nicer format and lookup each secret by name
data "aws_ssm_parameter" "secret" {
  for_each = toset(data.aws_ssm_parameters_by_path.secrets.names)
  name = each.key
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

# ------------------------------------------------------------------------------
# Policy Documents
# ------------------------------------------------------------------------------
data "aws_iam_policy_document" "file_transfer_api_trust" {
  statement {
    sid       = "FileTransferAPITrust"
    effect = "Allow"
    actions = [
      "sts:AssumeRole",
    ]

    principals {
      type = "Service"

      identifiers = [
        "ecs.amazonaws.com",
        "ecs-tasks.amazonaws.com",
      ]
    }
  }
}

output "execution_role" {
  value = aws_iam_role.file_transfer_api_execution.arn
}

