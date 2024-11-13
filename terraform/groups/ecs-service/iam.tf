resource "aws_iam_role" "task_role_secure" {
  name               = "${var.environment}-${local.service_name_secure}-task-role"
  path               = "/"
  assume_role_policy = data.aws_iam_policy_document.task_assume_secure.json
}

resource "aws_iam_role" "task_role" {
  name               = "${var.environment}-${local.service_name}-task-role"
  path               = "/"
  assume_role_policy = data.aws_iam_policy_document.task_assume.json
}

data "aws_iam_policy_document" "task_assume_secure" {
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

data "aws_iam_policy_document" "task_assume" {
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
    sid       = "AllowFullAccessToS3File"
    effect    = "Allow"
    actions   = [
      "s3:ListBucket",
      "s3:PutObject",
      "s3:PutObjectAcl",
      "s3:GetObject",
      "s3:GetObjectAcl",
      "s3:DeleteObject",
      "s3:ListBucket",
      "s3:ListAllMyBuckets",
      "s3:GetBucketTagging",
      "s3:GetBucketLocation",
      "s3:CreateBucket",
      "s3:GetBucketPolicyStatus",
      "s3:GetBucketPublicAccessBlock",
      "s3:GetBucketAcl",
      "s3:GetBucketPolicy"
    ]
    resources = [
      "arn:aws:s3:::${var.file_transfer_bucket}/*"
    ]
  }

  statement {
    sid = "AllowAccessForKeyFile"
    effect    = "Allow"
    actions   = ["kms:*"]
    resources = ["*"]
  }
}

data "aws_iam_policy_document" "task_policy_secure" {

  statement {
    sid       = "AllowFullAccessToS3Secure"
    effect    = "Allow"
    actions   = [
      "s3:ListBucket",
      "s3:PutObject",
      "s3:PutObjectAcl",
      "s3:GetObject",
      "s3:GetObjectAcl",
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:ListBucket",
      "s3:ListAllMyBuckets",
      "s3:GetBucketTagging",
      "s3:GetBucketLocation",
      "s3:CreateBucket",
      "s3:GetBucketPolicyStatus",
      "s3:GetBucketPublicAccessBlock",
      "s3:GetBucketAcl",
      "s3:GetBucketPolicy"
    ]
    resources = [
      "arn:aws:s3:::${var.file_transfer_bucket_secure}/*"
    ]
  }

  statement {
    sid = "AllowAccessForKeySecure"
    effect    = "Allow"
    actions   = ["kms:*"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy_attachment" "task_role_attachment" {
  role       = aws_iam_role.task_role.name
  policy_arn = aws_iam_policy.task_policy.arn
}

resource "aws_iam_role_policy_attachment" "task_role_secure_attachment" {
  role       = aws_iam_role.task_role_secure.name
  policy_arn = aws_iam_policy.task_policy_secure.arn
}

