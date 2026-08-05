resource "aws_kms_grant" "file_transfer_encryption_key_grant" {
  count = var.file_transfer_create_ecs ? 1 : 0

  name              = "${var.environment}-file-transfer-service-key-grant"
  key_id            = data.aws_kms_alias.file_transfer_encryption_key_alias[0].target_key_id
  grantee_principal = aws_iam_role.task_role[0].arn
  operations        = ["Encrypt", "Decrypt", "GenerateDataKey"]
}
