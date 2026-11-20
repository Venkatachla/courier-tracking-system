# Courier Tracking System

Spring Boot microservices and React monorepo. The services use separate MySQL schemas and communicate through the gateway, REST, and RabbitMQ.

## Repository layout

```text
api-gateway/       Spring Cloud Gateway (8080)
user-service/      Identity and user profiles (5001)
shipment-service/  Shipment lifecycle (5002)
tracking-service/  Tracking timeline and RabbitMQ consumer (5003)
frontend/          React + Vite client (3000)
infra/mysql/       MySQL initialization
```

## User Service (step 2)

The User Service is a standalone Maven module under `user-service/` (no parent POM or shared library). Its Docker build uses the repository root as context. It owns `user_db`, applies its schema with Flyway, and seeds the three demo accounts through the BCrypt `PasswordEncoder` only when the users table is empty.

Build and run MySQL plus this service from the repository root:

```sh
docker compose up -d mysql
docker compose build user-service
docker compose up -d user-service
docker compose logs -f user-service
```

On Windows PowerShell, `.\mvnw.cmd test` runs Maven inside the configured Docker Maven image. The equivalent Unix entry point is `./mvnw test`.

### User Service endpoints

Base URL: `http://localhost:5001` with the debug Compose overlay. Registration and login are public. For now, `/api/users/me` and `/api/users` expect identity headers supplied by the API Gateway. `GET /api/users` additionally requires `X-User-Role: ADMIN`. `/internal/users/{id}` is intended for service-to-service use and must not be routed through the gateway.

The main Compose file no longer publishes this service's port. To use these direct-port examples, start it with `docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d mysql user-service`.

```sh
curl -i -X POST http://localhost:5001/api/auth/register -H 'Content-Type: application/json' -d '{"name":"Pat Customer","email":"pat@example.com","password":"Password@123","phone":"555-0100","role":"ADMIN"}'
curl -i -X POST http://localhost:5001/api/auth/login -H 'Content-Type: application/json' -d '{"email":"admin@courier.com","password":"Admin@123"}'
curl -i http://localhost:5001/api/users/me -H 'X-User-Id: 1' -H 'X-User-Role: ADMIN'
curl -i http://localhost:5001/api/users -H 'X-User-Id: 1' -H 'X-User-Role: ADMIN'
```

Swagger UI: `http://localhost:5001/swagger-ui.html`; health: `http://localhost:5001/actuator/health`.

## Shipment Service (step 3)

The standalone `shipment-service/` module owns `shipment_db`, validates and versions shipment updates, checks courier identity through the User Service, and publishes JSON events after successful database commits. Its port is internal to Compose by default; use the debug overlay for direct local access. The service uses a local `ShipmentEvent` contract; the Tracking Service copies it without a shared library.

Build and run the dependencies and both services:

```sh
docker compose --profile docker-broker up -d mysql rabbitmq
docker compose build user-service shipment-service
docker compose up -d user-service shipment-service
docker compose ps
docker compose logs -f shipment-service
```

Run only the Shipment Service tests through the Docker-backed Maven entry point with `.\mvnw.cmd shipment-service test` (or `./mvnw shipment-service test` on Unix).

The API trusts identity headers supplied by the gateway (for this step, pass them manually):

```sh
# Login response includes userId and role; demo accounts are seeded by User Service.
curl -i -X POST http://localhost:5001/api/auth/login -H 'Content-Type: application/json' -d '{"email":"customer@courier.com","password":"Customer@123"}'

# Create as customer (replace X-User-Id with the userId from login).
curl -i -X POST http://localhost:5002/api/shipments -H 'Content-Type: application/json' -H 'X-User-Id: 3' -H 'X-User-Role: CUSTOMER' -d '{"receiverName":"Taylor Receiver","receiverAddress":"10 Main Street","receiverPhone":"555-0101","weight":2.5}'
curl -i 'http://localhost:5002/api/shipments/my?page=0&size=20' -H 'X-User-Id: 3' -H 'X-User-Role: CUSTOMER'

# Assign as admin (courier demo user ID is 2 with a fresh user_db).
curl -i -X PUT http://localhost:5002/api/shipments/1/assign -H 'Content-Type: application/json' -H 'X-User-Id: 1' -H 'X-User-Role: ADMIN' -d '{"courierId":2}'

# Update as assigned courier.
curl -i -X PUT http://localhost:5002/api/shipments/1/status -H 'Content-Type: application/json' -H 'X-User-Id: 2' -H 'X-User-Role: COURIER' -d '{"status":"PICKED_UP","location":"Depot","description":"Parcel collected"}'
```

RabbitMQ management UI is `http://localhost:15672` (credentials from `.env`). The service declares the durable topic exchange `shipment.exchange`. To inspect an event, open **Queues and Streams**, add a temporary queue, bind it to `shipment.exchange` with routing key `shipment.#`, then use **Get messages**. Events are transiently routed; the exchange does not retain messages without a bound queue.

Swagger UI: `http://localhost:5002/swagger-ui.html`; health: `http://localhost:5002/actuator/health`.

## Tracking Service (step 4)

The standalone `tracking-service/` module owns `tracking_db`, consumes the Shipment Service event contract, and keeps public timeline responses free of user IDs. It validates courier updates by calling Shipment Service's internal tracking-number lookup at `/internal/shipments/by-tracking/{trackingNumber}`. Keep that internal path out of API Gateway routes. Its port is internal to Compose unless the debug overlay is used.

Build and run all required services:

```sh
docker compose --profile docker-broker up -d mysql rabbitmq
docker compose build user-service shipment-service tracking-service
docker compose up -d user-service shipment-service tracking-service
docker compose ps
docker compose logs -f tracking-service
```

Run Tracking Service tests through the Docker-backed Maven entry point with `.\mvnw.cmd tracking-service test` (or `./mvnw tracking-service test` on Unix).

### Shipment to public timeline flow

With fresh seeded users, admin is ID 1, courier is ID 2, and customer is ID 3. Use the `id` and `trackingNumber` returned by create if they differ from the example below. Each shipment event is published after Shipment Service commits; Tracking Service consumes it asynchronously.

```sh
curl -i -X POST http://localhost:5002/api/shipments -H 'Content-Type: application/json' -H 'X-User-Id: 3' -H 'X-User-Role: CUSTOMER' -d '{"receiverName":"Taylor Receiver","receiverAddress":"10 Main Street","receiverPhone":"555-0101","weight":2.5}'
curl -i -X PUT http://localhost:5002/api/shipments/1/assign -H 'Content-Type: application/json' -H 'X-User-Id: 1' -H 'X-User-Role: ADMIN' -d '{"courierId":2}'
curl -i -X PUT http://localhost:5002/api/shipments/1/status -H 'Content-Type: application/json' -H 'X-User-Id: 2' -H 'X-User-Role: COURIER' -d '{"status":"PICKED_UP","location":"Depot","description":"Parcel collected"}'
curl -i http://localhost:5003/api/track/TRACKING_NUMBER_FROM_CREATE
```

Public timeline: `GET /api/track/{trackingNumber}`. Authenticated history: `GET /api/tracking/{trackingNumber}/history` with `X-User-Id` and `X-User-Role`. Manual updates: `POST /api/tracking/events` with the same identity headers; only ADMIN and the assigned COURIER may submit.

### RabbitMQ queues

RabbitMQ management is at `http://localhost:15672`. The consumer declares `tracking.shipment.events`, bound to `shipment.exchange` using `shipment.#`, and `tracking.shipment.events.dlq` for rejected or repeatedly failing messages. Open **Queues and Streams** and select either queue, then use **Get messages**. To inspect an event before consumption, pause the tracking service or use a temporary queue bound to `shipment.exchange`.

Swagger UI: `http://localhost:5003/swagger-ui.html`; health: `http://localhost:5003/actuator/health`.

## API Gateway (step 5)

The reactive Gateway is the public API entry point at `http://localhost:8080`. Its JWT validator uses the same UTF-8 `JWT_SECRET` and HS256 signature as User Service (`sub`, `userId`, `email`, `role`, `iat`, `exp`). It replaces client-supplied identity headers with verified claims and adds `X-Request-Id`. Backend ports are not published by the main Compose file; use `docker-compose.debug.yml` only when direct service access is needed.

| Gateway path | Downstream service | Access |
| --- | --- | --- |
| `/api/auth/**`, `/api/users/**` | User Service | Register/login public; other requests require a token |
| `/api/shipments/**` | Shipment Service | Token and role rules below |
| `/api/tracking/**`, `/api/track/**` | Tracking Service | Public track lookup; other requests require a token |
| `/internal/**`, `/actuator/**` (except Gateway health), Swagger/API docs | None | Blocked |

| Method and path | Allowed token roles |
| --- | --- |
| `GET /api/users` | ADMIN |
| `GET /api/users/me` | Any authenticated role |
| `POST /api/shipments` | CUSTOMER, ADMIN |
| `GET /api/shipments/my`, `GET /api/shipments/{id}` | Any authenticated role; services check ownership |
| `PUT /api/shipments/{id}/assign` | ADMIN |
| `PUT /api/shipments/{id}/status` | COURIER, ADMIN |
| `PUT /api/shipments/{id}/cancel` | CUSTOMER, ADMIN |
| `GET /api/tracking/{trackingNumber}/history` | Any authenticated role |
| `POST /api/tracking/events` | COURIER, ADMIN |
| Any other routed path | ADMIN |

The Gateway allows CORS origins from `CORS_ALLOWED_ORIGINS` and applies in-memory per-IP limits from `RATE_LIMIT_GENERAL`, `RATE_LIMIT_AUTH`, and `RATE_LIMIT_WINDOW_SECONDS`. Only `GET /actuator/health` is exposed on the Gateway itself.

### Start services through the Gateway

For the Docker broker, set `RABBITMQ_HOST=rabbitmq` in `.env`, stop the native broker if it is using ports 5672/15672, then run:

```powershell
docker compose --profile docker-broker up -d mysql rabbitmq
docker compose build user-service shipment-service tracking-service gateway
docker compose --profile docker-broker up -d user-service shipment-service tracking-service gateway
```

For the native broker, leave the Windows service running, do not enable `docker-broker`, set `RABBITMQ_HOST=host.docker.internal` plus the dedicated broker username/password in `.env`, then run:

```powershell
docker compose up -d mysql
docker compose build user-service shipment-service tracking-service gateway
docker compose up -d user-service shipment-service tracking-service gateway
```

Only port 8080 is published for the backend stack. For direct local debugging, use `docker compose -f docker-compose.yml -f docker-compose.debug.yml up` to publish ports 5001–5003 on loopback. The Docker-backed Maven entry point can run gateway tests with `.\mvnw.cmd api-gateway test` (Unix: `./mvnw api-gateway test`).

### End-to-end flow through port 8080

These requests register a fresh customer, log in as that customer plus the seeded admin and courier, and use the returned real JWTs. Identity headers are never supplied manually.

```powershell
$customerEmail = "gateway-customer-$([guid]::NewGuid().ToString('N'))@example.com"
$registerBody = @{ name = "Gateway Customer"; email = $customerEmail; password = "Password@123" } | ConvertTo-Json -Compress
curl.exe -i -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d $registerBody
$customerLogin = @{ email = $customerEmail; password = "Password@123" } | ConvertTo-Json -Compress
$customer = curl.exe -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d $customerLogin | ConvertFrom-Json
$admin = curl.exe -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"admin@courier.com","password":"Admin@123"}' | ConvertFrom-Json
$courier = curl.exe -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"courier@courier.com","password":"Courier@123"}' | ConvertFrom-Json

$body = '{"receiverName":"Taylor Receiver","receiverAddress":"10 Main Street","receiverPhone":"555-0101","weight":2.5}'
$shipment = curl.exe -s -X POST http://localhost:8080/api/shipments -H "Content-Type: application/json" -H "Authorization: Bearer $($customer.token)" -d $body | ConvertFrom-Json
curl.exe -i -X PUT "http://localhost:8080/api/shipments/$($shipment.id)/assign" -H "Content-Type: application/json" -H "Authorization: Bearer $($admin.token)" -d '{"courierId":2}'
curl.exe -i -X PUT "http://localhost:8080/api/shipments/$($shipment.id)/status" -H "Content-Type: application/json" -H "Authorization: Bearer $($courier.token)" -d '{"status":"PICKED_UP","location":"Depot","description":"Parcel collected"}'
Start-Sleep -Seconds 2
curl.exe -i "http://localhost:8080/api/track/$($shipment.trackingNumber)"
```

### Gateway rejection checks

```powershell
# Missing token: 401
curl.exe -i http://localhost:8080/api/users/me

# Customer token on admin-only assignment: 403
curl.exe -i -X PUT http://localhost:8080/api/shipments/1/assign -H "Authorization: Bearer $($customer.token)" -H "Content-Type: application/json" -d '{"courierId":2}'

# Forged role is discarded; the customer token still gets 403.
curl.exe -i http://localhost:8080/api/users -H "Authorization: Bearer $($customer.token)" -H "X-User-Role: ADMIN"

# Internal route is blocked and is never forwarded.
curl.exe -i http://localhost:8080/internal/shipments/by-tracking/x
```

## Local infrastructure

Copy `.env.example` to `.env`. Choose Docker or native RabbitMQ as described below, then build and start the desired services. Compose initializes MySQL with `user_db`, `shipment_db`, and `tracking_db`. MySQL is available on `localhost:3306`; RabbitMQ management is at `http://localhost:15672` (credentials from `.env`). The intended public app entry points are `http://localhost:3000` and `http://localhost:8080`.

## RabbitMQ: Docker vs native

Both Shipment Service and Tracking Service read `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, and `RABBITMQ_PASSWORD` from Compose environment values expanded from `.env`. `.env.example` defaults to `RABBITMQ_HOST=rabbitmq` for the container broker.

### Docker broker

The RabbitMQ container is opt-in under the `docker-broker` profile. Set `RABBITMQ_HOST=rabbitmq` in `.env`, stop the native Windows RabbitMQ service so ports 5672 and 15672 are free, then run:

```powershell
docker compose --profile docker-broker up -d rabbitmq
docker compose up -d mysql
docker compose build user-service shipment-service tracking-service
docker compose up -d user-service shipment-service tracking-service
```

The container reads its default user and password from `.env`. Its management UI is `http://localhost:15672`.

### Native Windows broker

Keep the native RabbitMQ service running and do not activate the `docker-broker` profile. In `.env`, set `RABBITMQ_HOST=host.docker.internal`, `RABBITMQ_PORT=5672`, `RABBITMQ_USER=courier`, and `RABBITMQ_PASSWORD` to the password you create for the dedicated `courier` user. Do not use `guest`; RabbitMQ restricts that account to localhost connections.

In an elevated PowerShell shell, create the dedicated broker user and allow it access to the default virtual host:

```powershell
rabbitmqctl add_user courier "replace-with-a-strong-password"
rabbitmqctl set_user_tags courier administrator
rabbitmqctl set_permissions -p / courier ".*" ".*" ".*"
netstat -ano | findstr :5672
```

Start or stop the Windows service from an Admin shell with `net start RabbitMQ` or `net stop RabbitMQ`. Then start the application services without the broker profile:

```powershell
docker compose up -d mysql
docker compose build user-service shipment-service tracking-service
docker compose up -d user-service shipment-service tracking-service
```

The services tolerate a broker that is not ready during application startup: listener containers retry queue recovery, and Shipment Service retries transient publishes. The tracking queue, dead-letter exchange, bindings, and shipment exchange are declared by the applications, so no manual topology setup is needed.

### Inspect queues in either broker

Open `http://localhost:15672` and sign in with the credentials for the selected broker. Under **Queues and Streams**, inspect `tracking.shipment.events` and `tracking.shipment.events.dlq`, then use **Get messages**. The main queue is consumed promptly; to inspect a pending message there, stop Tracking Service, publish a shipment event, then browse the queue. The DLQ retains rejected or exhausted messages for inspection.

#   c o u r i e r - t r a c k i n g - s y s t e m  
 