# Manual API checks with the IntelliJ HTTP Client

`trouble-ticket-api.http` holds 28 requests that cover every endpoint: happy paths and the
error responses the contract requires. Each request has a test script (`> {% ... %}`) that checks
the status code, the `Error.code`, and the important body fields and headers.

## Setup

Start the stack: `docker compose up --build`. That's all: no environment has to be selected.

All variables are declared at the top of `trouble-ticket-api.http` as in-file variables
(`@baseUrl = ...`, `@tokenA = ...`), so they resolve however the file is run, including
right-click → **Run All** on the file, which always runs without an environment.

| Variable        | Value                                             |
|-----------------|---------------------------------------------------|
| `baseUrl`       | `http://localhost:8080/api/v1`                    |
| `tokenA`        | JWT for `tenant-a`                                |
| `tokenB`        | JWT for `tenant-b`                                |
| `tokenNoTenant` | JWT with an empty `tenant_id` (for the 403 check) |

The tokens are valid for 1 year and are signed with the local default `JWT_SECRET`. They are
committed on purpose: that secret is already public in `compose.yaml`, so anyone can mint the same
tokens, and they don't work against an app with a real secret. If you change `JWT_SECRET` in `.env`
(or requests return 401), regenerate them with the commands in the file's header and paste them in.

## Running the requests in IntelliJ IDEA

1. Open `http/trouble-ticket-api.http`. IntelliJ recognizes `.http` files natively
   (the bundled *HTTP Client* plugin).
2. Run:
   - **Everything:** right-click the file → **Run All**, or click ▶▶ at the top of the editor.
     Use this the first time: requests 1.2 onwards use the `ticketId` and `externalId`
     that request 1.1 stores in global variables.
   - **One request:** click the green ▶ in the gutter next to its `POST`/`GET` line.
3. Read the results in the **Services** tool window (opens automatically at the bottom):
   - the left side lists each request with its status code;
   - the **Tests** tab shows every `client.test(...)` check as passed (green) or failed (red),
     with the failure message (e.g. `got 500`);
   - the response body and headers are shown next to it. IntelliJ also saves each response under
     `.idea/httpRequests/`, and **History** in the HTTP Client menu lists earlier runs.

## How the file works (for writing your own requests)

```http
### Title shown in IntelliJ          <- "###" separates requests
POST {{baseUrl}}/troubleTicket        <- {{name}} = variable declared with @name = value
Authorization: Bearer {{tokenA}}
Content-Type: application/json
                                      <- empty line, then the body
{ "externalId": "OK-{{$uuid}}" }      <- {{$uuid}} = new random UUID every run

> {%                                  <- response handler, plain JavaScript
    client.test("201 Created", () => client.assert(response.status === 201, "got " + response.status));
    client.global.set("ticketId", response.body.id);   // usable later as {{ticketId}}
%}
```

- `response.status`, `response.body` (parsed JSON), `response.headers.valueOf("Location")`.
- `client.test(name, fn)` + `client.assert(condition, message)` produce the entries in the **Tests** tab.
- `client.global.set/get` passes values between requests.

## Expected responses (what the contract requires)

Demo services from `db/seed`: `987654321` tenant-a active, `987654323` tenant-a inactive,
`123456789` tenant-b active.

| # | Request | Expected |
|---|---|---|
| 1.1 | Create, valid body | `201`, `Location: /api/v1/troubleTicket/{id}`, full ticket, initial note = `notes[0]` |
| 1.2 | Create again with the same `externalId` | `200`, same `id`, original data unchanged |
| 1.3 | Create with `status` ≠ `new` | `400 VALIDATION_ERROR` |
| 1.4 | Create without a required field | `400 VALIDATION_ERROR` |
| 1.5 | Create with an unknown field | `400 VALIDATION_ERROR` |
| 1.6 | Create with a wrong type (`"serviceId": "abc"`) | `400 VALIDATION_ERROR` |
| 1.7 | Create with `serviceId: 0` | `400 VALIDATION_ERROR` |
| 1.8 | Create with malformed JSON | `400 VALIDATION_ERROR` |
| 1.9 | Create for an inactive service | `404 SERVICE_NOT_FOUND` |
| 1.10 | Create for another tenant's service | `404 SERVICE_NOT_FOUND` |
| 1.11 | Create for a non-existent service | `404 SERVICE_NOT_FOUND` |
| 2.1 | List | `200`, array of `{externalId, serviceId, description, status}` only |
| 2.2 | List as another tenant | `200`, without tenant-a's tickets |
| 3.1 | Get own ticket | `200`, full ticket with `notes` |
| 3.2 | Get unknown id | `404 TROUBLE_TICKET_NOT_FOUND` |
| 3.3 | Get another tenant's ticket | `404 TROUBLE_TICKET_NOT_FOUND` (existence not revealed) |
| 4.1 | Add note | `201`, exactly the created `Note` |
| 4.2 | Add note with empty text | `400 VALIDATION_ERROR` |
| 4.3 | Add note to unknown ticket | `404 TROUBLE_TICKET_NOT_FOUND` |
| 5.1 | Patch with `status` ≠ `closed` | `400 VALIDATION_ERROR` |
| 5.2 | Patch without `status` | `400 VALIDATION_ERROR` |
| 5.3 | Close another tenant's ticket | `404 TROUBLE_TICKET_NOT_FOUND` |
| 5.4 | Close | `200`, `status: closed`, notes kept in order |
| 5.5 | Close an already closed ticket | `200` (assumption: idempotent) |
| 5.6 | Close unknown ticket | `404 TROUBLE_TICKET_NOT_FOUND` |
| 6.1 | No token | `401 UNAUTHORIZED`, `requestId` = `X-Request-ID` header |
| 6.2 | Tampered token | `401 UNAUTHORIZED` |
| 6.3 | Valid token without `tenant_id` | `403` (app returns code `MISSING_TENANT`) |

Every error body has the `Error` shape: `code`, `message`, `requestId`.

Outside the contract (not in the file): a wrong HTTP method (e.g. `DELETE`) returns `405` in
Spring Boot's default error format, not `Error`; an unknown path with a valid token returns
`403 FORBIDDEN`.
