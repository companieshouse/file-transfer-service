# File Transfer Service
resource "aws_iam_role" "task_role" {
  count = var.file_transfer_create_ecs ? 1 : 0

  name               = "${var.environment}-${local.service_name}-task-role"
  path               = "/"
  assume_role_policy = data.aws_iam_policy_document.task_assume[0].json
}

resource "aws_iam_policy" "task_policy" {
  count = var.file_transfer_create_ecs ? 1 : 0

  name   = "${var.environment}-${local.service_name}-task-policy"
  policy = data.aws_iam_policy_document.file_transfer_ecs_execution[0].json
}


resource "aws_iam_role_policy_attachment" "task_role_attachment" {
  count = var.file_transfer_create_ecs ? 1 : 0

  role       = aws_iam_role.task_role[0].name
  policy_arn = aws_iam_policy.task_policy[0].arn
}
