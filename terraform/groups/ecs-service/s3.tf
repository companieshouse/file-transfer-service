resource "aws_kms_key" "file_transfer_encryption_key" {
  description             = "Encrypt user uploaded data for the extensions project."
  deletion_window_in_days = 30
}

data "aws_kms_alias" "file_transfer_encryption_key_alias_secure" {
  name          = var.file_transfer_kms_alias_secure
}

data "aws_kms_alias" "file_transfer_encryption_key_alias" {
  name          = var.file_transfer_kms_alias
}

data "aws_s3_bucket" "file_transfer_bucket" {
  bucket = var.file_transfer_bucket
}

data "aws_s3_bucket" "file_transfer_bucket_secure" {
  bucket = var.file_transfer_bucket_secure
}


resource "aws_kms_grant" "file_transfer_encryption_key_grant" {
  name              = "${var.stage}-file-transfer-api-key-grant"
  key_id            = aws_kms_key.file_transfer_encryption_key.key_id
  grantee_principal = aws_iam_role.file_transfer_service_execution.arn
  operations        = ["Encrypt", "Decrypt", "GenerateDataKey"]
}

resource "aws_iam_role" "file_transfer_service_execution" {
  name               = "${var.stage}-file-transfer-api-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.file_transfer_service_trust.json
}
