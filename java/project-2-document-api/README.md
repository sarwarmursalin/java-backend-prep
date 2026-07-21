# Project 2 — Document Metadata REST API

## Why I built this

After two Core Java projects, I wanted to build the thing closest to actual day-one backend work: a Spring Boot REST service backed by a real database, with a real test suite — not a toy CRUD demo, but one where I deliberately created and then proved I'd fixed a real problem (an N+1 query), and then deployed it to AWS myself rather than just running it locally.

---

## What I actually built

A CRUD REST API over "documents" (`title`, `owner`, `fileType`, `fileSizeBytes`, `uploadedAt`), with documents having a many-to-many relationship to `tags`.

**Endpoints** (`controller/DocumentController.java`):
- `GET /api/documents` — paginated list (`Pageable`, so `?page=0&size=20&sort=...` all work out of the box).
- `GET /api/documents/{id}` — single document, 404 if missing.
- `POST /api/documents` — create, validated with `@Valid` on a `DocumentRequest` DTO, returns 201.
- `PUT /api/documents/{id}` — update.
- `DELETE /api/documents/{id}` — returns 204.

**DTOs, not entities, on the wire.** `DocumentRequest`/`DocumentResponse` are separate from the `Document` JPA entity, so the API shape doesn't leak persistence details or accidentally expose/accept fields I don't want it to.

**Centralized error handling** (`exception/GlobalExceptionHandler.java`, `@RestControllerAdvice`) — a validation failure (`MethodArgumentNotValidException`) returns a clean 400 with a field→message map; a missing document (`DocumentNotFoundException`) returns a clean 404. Neither leaks a stack trace to the client.

**Persistence** — PostgreSQL via Spring Data JPA, schema managed with Flyway migrations (`V1__create_documents_table.sql`, `V2__create_tags_table.sql`) rather than Hibernate auto-DDL, so schema changes are versioned and reviewable like any other code change.

### The N+1 fix — proved, not assumed

This was the actual point of the project. `DocumentRepositoryTest` uses a real Testcontainers PostgreSQL instance and Hibernate's `Statistics` API to count queries, not just check the data came back right:

- `findAll_causesOneQueryPerDocument_whenTagsAccessedLazily` — loads 3 documents, then touches each one's lazily-loaded tags. Result: **4 queries** (1 for the documents, 1 more per document to fetch its tags) — the N+1 problem, caught with a number, not a guess.
- `findAllFetchTags_runsOneQuery_regardlessOfDocumentCount` — the fix: `DocumentRepository.findAllFetchTags` uses a join-fetch query instead. Same 3 documents, same tag access pattern. Result: **2 queries** total (1 count query for pagination, 1 join-fetch query), regardless of how many documents come back.

That before/after query count, from a real database via Testcontainers, is the artifact I actually care about keeping — it's proof, not a claim.

**Tests overall:** unit tests (Mockito) on the service layer, a slice test (`@WebMvcTest` + MockMvc) on the controller, and the Testcontainers-backed repository tests above.

## How to run it locally

```bash
cd java-aws-projects/java/project-2-document-api
# requires a local Postgres reachable at the URL in application.properties
mvn spring-boot:run
```

Run the tests (spins up a real Postgres container via Testcontainers, needs Docker running):
```bash
mvn test
```

## How I deployed it to AWS

I didn't just run this locally — I deployed it to a real, resilient AWS setup and load-tested it:

1. **Networking**: a custom VPC, with the app running behind an **Application Load Balancer** and an **Auto Scaling Group** — stateless, so any instance can be replaced without losing anything.
2. **Database**: **RDS PostgreSQL**, with credentials never baked into the AMI or launch config — the EC2 launch-template user data script (`deploy/launch-template-userdata.md`) pulls the DB username/password from **AWS Secrets Manager** at boot time via the AWS CLI, then creates the database if it doesn't exist yet.
3. **Running the app**: the jar is pulled from S3 and run as a `systemd` service (`deploy/document-api.service`) so it restarts automatically on failure (`Restart=always`) and logs to `/var/log/document-api.log`.
4. **Monitoring**: a CloudWatch agent config (`deploy/amazon-cloudwatch-agent.json`) ships that log file to a CloudWatch log group and reports memory usage every 60 seconds.
5. **Proving it actually self-heals**: I terminated a running instance manually and confirmed the Auto Scaling Group launched a replacement that came up healthy behind the load balancer with zero manual intervention.

## What this taught me

- **DTOs vs entities** — keeping them separate meant I could change the database schema (e.g. the N+1 fix) without touching the API contract at all.
- **Proving a fix, not just applying one** — the Hibernate `Statistics` API turned "I fixed the N+1 problem" from a claim into a number I can show.
- **Testcontainers over mocks for the repository layer** — mocking the repository would have proven nothing about the actual SQL Hibernate generates; only a real Postgres instance could show the query count difference.
- **Secrets never touching the AMI or the launch config** — pulling them from Secrets Manager at boot time, not baking them into anything that gets stored or logged.
