resource "aws_iam_role" "task_role_secure" {
  name               = "${var.environment}-${local.service_name_secure}-task-role"
  path               = "/"
  assume_role_policy = data.aws_iam_policy_document.task_assume.json
}

resource "aws_iam_role" "task_role" {
  name               = "${var.environment}-${local.service_name}-task-role"
  path               = "/"
  assume_role_policy = data.aws_iam_policy_document.task_assume.json
}


data "aws_iam_policy_document" "task_assume" {
  statement {
    sid     = "AllowTaskAssumeRole"
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_policy" "task_policy" {
  name        = "${var.environment}-${local.service_name}-task-policy"
  policy      = data.aws_iam_policy_document.task_policy.json
}


resource "aws_iam_policy" "task_policy_secure" {
  name        = "${var.environment}-${local.service_name_secure}-task-policy"
  policy      = data.aws_iam_policy_document.task_policy_secure.json
}

data "aws_iam_policy_document" "task_policy" {

  statement {
    sid       = "AllowFullAccessToS3"
    effect    = "Allow"
    actions   = [
      "s3:*"
    ]
    resources = [
      "arn:aws:s3:::${var.file_transfer_bucket}/*"
    ]
  }

  statement {
    sid = "AllowAccessForKey"
    effect    = "Allow"
    actions   = ["kms:*"]
    resources = ["*"]
  }
}

data "aws_iam_policy_document" "task_policy_secure" {

  statement {
    sid       = "AllowFullAccessToS3"
    effect    = "Allow"
    actions   = [
      "s3:*"
    ]
    resources = [
      "arn:aws:s3:::${var.file_transfer_bucket_secure}/*"
    ]
  }

  statement {
    sid = "AllowAccessForKey"
    effect    = "Allow"
    actions   = ["kms:*"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy_attachment" "task_role_attachment" {
  role       = aws_iam_role.task_role.name
  policy_arn = aws_iam_policy.task_policy.arn
}
