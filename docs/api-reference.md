# API Reference

The Helios REST API is used to create, retrieve, list, and manage scheduled jobs.

**Base Path**

```text
/api/v1
```

If Swagger/OpenAPI is enabled, interactive API documentation is available through the application's Swagger UI.

---

## Endpoints

| Method   | Endpoint            | Description                              |
| -------- | ------------------- | ---------------------------------------- |
| `POST`   | `/jobs/create`      | Create a new scheduled job               |
| `GET`    | `/jobs/{id}`        | Retrieve a job by its ID                 |
| `GET`    | `/jobs`             | List all scheduled jobs                  |
| `PUT`    | `/jobs/{id}`        | Update an existing job                   |
| `DELETE` | `/jobs/{id}`        | Delete a scheduled job                   |
| `POST`   | `/jobs/create/bulk` | Create multiple jobs in a single request |

---

## Create Job

### Request

```json
{
  "name": "Ping job",
  "cronExpression": "*/30 * * * * *",
  "jobType": "HTTP",
  "payload": "{\"url\":\"https://httpbin.org/get\",\"method\":\"GET\",\"headers\":{}}",
  "maxRetries": 3
}
```

### Response

```json
{
  "id": "b1f33cb2-5807-4cf1-b0a4-4ad5c7afcc91",
  "name": "Ping job",
  "jobType": "HTTP"
}
```

---

## Update Job

### Request

```json
{
  "name": "Updated job name",
  "cronExpression": "0 */5 * * * *",
  "jobType": "HTTP",
  "payload": "{\"url\":\"https://httpbin.org/post\",\"method\":\"POST\",\"headers\":{\"Content-Type\":\"application/json\"}}",
  "maxRetries": 5
}
```

All fields are optional. Only provided fields will be updated. If `cronExpression` is changed, `nextFireAt` is recalculated automatically.

### Response

```json
{
  "id": "b1f33cb2-5807-4cf1-b0a4-4ad5c7afcc91",
  "name": "Updated job name",
  "jobType": "HTTP"
}
```

---

## Example Requests

### Create a Job

```bash
curl -X POST http://localhost:8080/api/v1/jobs/create \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Ping job\",\"cronExpression\":\"*/30 * * * * *\",\"jobType\":\"HTTP\",\"payload\":\"{\\\"url\\\":\\\"https://httpbin.org/get\\\",\\\"method\\\":\\\"GET\\\",\\\"headers\\\":{}}\",\"maxRetries\":3}"
```

### Retrieve a Job

```bash
curl http://localhost:8080/api/v1/jobs/{id}
```

### List All Jobs

```bash
curl http://localhost:8080/api/v1/jobs
```

### Update a Job

```bash
curl -X PUT http://localhost:8080/api/v1/jobs/{id} \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Updated job\",\"maxRetries\":5}"
```

### Delete a Job

```bash
curl -X DELETE http://localhost:8080/api/v1/jobs/{id}
```

### Bulk Create Jobs

```bash
curl -X POST http://localhost:8080/api/v1/jobs/create/bulk \
  -H "Content-Type: application/json" \
  -d "[{\"name\":\"A\",\"cronExpression\":\"*/15 * * * * *\",\"jobType\":\"HTTP\",\"payload\":\"{\\\"url\\\":\\\"https://httpbin.org/get\\\",\\\"method\\\":\\\"GET\\\",\\\"headers\\\":{}}\",\"maxRetries\":2}]"
```

---

## Job Status

Jobs transition through the following lifecycle states during scheduling and execution:

| Status       | Description                               |
| ------------ | ----------------------------------------- |
| `PENDING`    | Waiting for its scheduled execution time  |
| `DISPATCHED` | Published for worker execution            |
| `RUNNING`    | Currently executing                       |
| `COMPLETED`  | Successfully executed                     |
| `FAILED`     | Execution failed after exhausting retries |

---

## Error Handling

Typical API errors include:

| HTTP Status                 | Description                                |
| --------------------------- | ------------------------------------------ |
| `400 Bad Request`           | Invalid request payload or malformed input |
| `404 Not Found`             | Requested job does not exist               |
| `500 Internal Server Error` | Unexpected server-side error               |

---

## Notes

* Job schedules use standard six-field cron expressions.
* The `payload` field contains the execution details for the configured job type.
* Each job is assigned a unique UUID when created.

---

**Related docs:** [README](../README.md) · [Architecture](architecture.md) · [Setup](setup.md)
