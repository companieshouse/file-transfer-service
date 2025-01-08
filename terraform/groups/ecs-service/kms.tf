resource "aws_kms_key" "file_transfer_encryption_key" {
  description             = "Encrypt user uploaded data for the extensions project."
  deletion_window_in_days = 30
}

resource "aws_kms_key" "file_transfer_encryption_key_secure" {
  description             = "Encrypt user uploaded data for the extensions project."
  deletion_window_in_days = 30
}

resource "aws_kms_grant" "file_transfer_encryption_key_grant" {
  name              = "${var.environment}-file-transfer-service-key-grant"
  key_id            = aws_kms_key.file_transfer_encryption_key.key_id
  grantee_principal = aws_iam_role.file_transfer_service_execution.arn
  operations        = ["Encrypt", "Decrypt", "GenerateDataKey"]
}

resource "aws_kms_grant" "file_transfer_encryption_key_grant_secure" {
  name              = "${var.environment}-file-transfer-service-secure-key-grant"
  key_id            = aws_kms_key.file_transfer_encryption_key_secure.key_id
  grantee_principal = aws_iam_role.file_transfer_service_execution_secure.arn
  operations        = ["Encrypt", "Decrypt", "GenerateDataKey"]
}