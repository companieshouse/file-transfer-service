# File Transfer Service

This API using Java 21 provides a generic interface to upload and download files via CHS. Each file is stored in S3 and a Virus Scan will be performed against it.

See the [a3-av-scanner](https://github.com/companieshouse/s3-av-scanner) repo for details and implementation of the workflow. A service design can also be found [here](https://companieshouse.atlassian.net/wiki/spaces/Arch/pages/878215317/File+Transfer+Service).

## Terraform deployment
This is an ECS Service so terraform is used to make all the necessary changes for Infrastructure

These are provisioned by Terraform and deployed from the concourse [pipeline](https://ci-platform.companieshouse.gov.uk/teams/team-development/pipelines/file-transfer-service).
The pipeline is capable of deploying everything so manual deployment should not be necessary.

## IAM Policies
The lambda function requires a role with several policies attached.
- S3 - upload/download from the file transfer bucket
- Cloudwatch - publish logs to cloudwatch
