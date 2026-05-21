# File Transfer Service

This API using Java 21 provides a generic interface to upload and download files via CHS. Each file is stored in S3 and a Virus Scan will be performed against it.

See the [a3-av-scanner](https://github.com/companieshouse/s3-av-scanner) repo for details and implementation of the workflow. A service design can also be found [here](https://companieshouse.atlassian.net/wiki/spaces/Arch/pages/878215317/File+Transfer+Service).

## Dependencies
The [file-transfer-api](https://github.com/companieshouse/file-transfer-api) infrastructure is creating S3 bucket and KMS keys. Those S3 buckets and KMS keys are referred in `file-transfer-service` running in Mesos & ECS.

## Authentication
This service uses Basic Authentication to perform operations. Each endpoint needs to receive an Authorization header pre-configured for each environment.


## Terraform deployment
This is an ECS Service so terraform is used to make all the necessary changes for Infrastructure

These are provisioned by Terraform and deployed from the concourse [pipeline](https://ci-platform.companieshouse.gov.uk/teams/team-development/pipelines/file-transfer-service).
The pipeline is capable of deploying everything so manual deployment should not be necessary.

## IAM Policies
The lambda function requires a role with several policies attached.
- S3 - upload/download from the file transfer bucket
- Cloudwatch - publish logs to cloudwatch

## Endpoints

| Endpoint | Method | Notes |
|---|---:|---|
| `/file-transfer-service/` | POST | Preferred multipart upload. Consumes `multipart/form-data`. Form parameter: `file`. Returns `IdApi` (JSON with `id`). Also accepts legacy JSON (`application/json`) for backwards compatibility, but that variant is deprecated. |
| `/file-transfer-service/upload` | POST | Legacy JSON upload **(deprecated)**. Consumes `application/json` and accepts the legacy `FileApi` JSON payload — do NOT send `multipart/form-data` here. |
| `/file-transfer-service/{fileId}` | GET | Retrieve file metadata. Returns `FileDetailsApi` (JSON). Throws 404 if not found. |
| `/file-transfer-service/{fileId}/download` | GET | Download file stream. Query param: `bypassAv`. Returns file `Resource` with appropriate `Content-Type` and `Content-Disposition: attachment; filename=...`. There is also a deprecated variant that produces `application/json` and returns the legacy `FileApi` JSON when requested. |
| `/file-transfer-service/{fileId}/downloadbinary` | GET | Deprecated. |
| `/file-transfer-service/{fileId}` | DELETE | Delete the file. Returns `204 No Content` on success. |
| `/file-transfer-service/healthcheck` | GET | Returns service health status. |


