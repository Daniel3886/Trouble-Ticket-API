# Trouble Ticket API

Implementacja backendowa kontraktu `trouble-ticket-api.yaml`

Technologie: Java 21, Spring Boot 4, Spring Security (OAuth2 Resource Server, JWT), Spring Data JPA, Flyway, PostgreSQL 17, Gradle, Spotless (palantir-java-format).

Kontrakt API (źródło prawdy dla implementacji): [`src/main/resources/trouble-ticket-api.yaml`](src/main/resources/trouble-ticket-api.yaml). Plik jest oryginalną specyfikacją z zadania, bez zmian.

## Zakres

| Metoda | Ścieżka                           | Opis                                                             |
|--------|-----------------------------------|------------------------------------------------------------------|
| POST   | `/api/v1/troubleTicket`           | Utworzenie zgłoszenia (idempotentne po `(tenantId, externalId)`) |
| GET    | `/api/v1/troubleTicket`           | Lista zgłoszeń tenanta (wersja skrócona)                         |
| GET    | `/api/v1/troubleTicket/{id}`      | Pełna reprezentacja zgłoszenia                                   |
| PATCH  | `/api/v1/troubleTicket/{id}`      | Zamknięcie zgłoszenia (dozwolony wyłącznie status `closed`)      |
| POST   | `/api/v1/troubleTicket/{id}/note` | Dodanie notatki                                                  |

## Struktura projektu

Kod jest podzielony według funkcjonalności biznesowej (package-by-feature), a nie według warstw technicznych.
Klasa jest `public` tylko wtedy, gdy używa jej kod z innego pakietu; pozostałe są pakietowo-prywatne,
więc granice między funkcjonalnościami pilnuje kompilator.

```
src/main/java/org/springboot/troubleticketapi/
├── ticket/           # Zgłoszenia: kontroler, serwis (idempotencja, izolacja tenantów), encje zgłoszenia
│                     # i notatek, repozytorium, rekordy żądań/odpowiedzi, enum TroubleTicketStatus.
│                     # Notatka istnieje tylko w ramach zgłoszenia, więc nie ma osobnego pakietu ani repozytorium.
│                     # Publiczne są tylko wyjątki obsługiwane w common: TicketNotFoundException
│                     # i InvalidStatusException.
├── servicecatalog/   # Usługi tenantów (tabela services). Na zewnątrz widoczny tylko ServiceCatalog
│                     # (requireActiveService) i ServiceNotFoundException; encja i repozytorium są ukryte.
├── security/         # Konfiguracja JWT, CurrentTenant (tenant z claimu tenant_id), błędy 401/403 w formacie Error
└── common/           # Elementy wspólne dla wszystkich funkcjonalności: GlobalExceptionHandler,
                      # rekord Error, RequestIdFilter (nagłówek X-Request-ID)

src/main/resources/
├── application.yaml
├── trouble-ticket-api.yaml   # kontrakt OpenAPI
└── db/
    ├── migration/            # V1__init_schema.sql – schemat bazy (Flyway)
    └── seed/                 # R__demo_services.sql – usługi demonstracyjne (tylko lokalnie)

src/test/java/org/springboot/troubleticketapi/
├── TroubleTicketApiIntegrationTest.java   # end-to-end: HTTP + JWT + PostgreSQL (Testcontainers)
├── ticket/                                # TroubleTicketControllerTest (@WebMvcTest), TroubleTicketServiceTest,
│                                          # TicketFixtures (wspólne dane testowe)
└── security/                              # SecurityTestConfig: udostępnia testom HTTP pakietowo-prywatną
                                           # konfigurację bezpieczeństwa

http/                         # Kolekcja żądań dla IntelliJ HTTP Client + instrukcja
```

## Uruchomienie

### Całe środowisko w Docker Compose (zalecane)

Wymagania: Docker z Docker Compose.

```bash
docker compose up --build
```

`compose.yaml` uruchamia dwa serwisy:
- `postgres`: PostgreSQL 17
- `app`: obraz budowany z `Dockerfile`. Startuje, dopiero gdy baza jest gotowa.

API jest dostępne pod `http://localhost:8080`. 

### Tryb deweloperski (aplikacja z IDE / Gradle)

Wymagania: Java 21 oraz Docker.

```bash
cp .env.example .env            # m.in. FLYWAY_LOCATIONS z danymi demonstracyjnymi
docker compose up -d postgres   # tylko baza
./gradlew bootRun               # aplikacja na http://localhost:8080
```

Bez pliku `.env` aplikacja uruchomi same migracje schematu, bez usług demonstracyjnych (patrz "Dane startowe").

Swagger UI: `http://localhost:8080/swagger-ui/index.html`. Dokumentacja w Swagger UI jest generowana przez springdoc z kodu kontrolerów, a nie z pliku kontraktu. Kanonicznym opisem API pozostaje `trouble-ticket-api.yaml`.

### Konfiguracja

Zmienne środowiskowe (wartości domyślne w `application.yaml` i `compose.yaml` są przeznaczone wyłącznie do lokalnego uruchomienia):

| Zmienna                              | Domyślnie                                         | Znaczenie                                                                                         |
|--------------------------------------|---------------------------------------------------|---------------------------------------------------------------------------------------------------|
| `SPRING_DATASOURCE_URL`              | `jdbc:postgresql://localhost:5432/trouble_ticket` | URL połączenia z PostgreSQL                                                                       |
| `POSTGRES_DB`                        | `trouble_ticket`                                  | Nazwa bazy tworzonej przez kontener                                                               |
| `POSTGRES_USER`, `POSTGRES_PASSWORD` | `ticket_user` / `local_dev_password`              | Dane logowania używane przez aplikację i kontener                                                 |
| `JWT_SECRET`                         | `local-dev-secret-change-me-1234567890`           | Klucz HMAC (HS256) do weryfikacji tokenów, min. 32 B                                              |
| `FLYWAY_LOCATIONS`                   | `classpath:db/migration`                          | Migracje Flyway; `compose.yaml` i `.env.example` dodają `classpath:db/seed` (dane demonstracyjne) |

Zmienne można ustawić w pliku `.env`. Plik czyta zarówno `docker compose`, jak i sama aplikacja (`spring.config.import` w `application.yaml`, uruchomienie z katalogu projektu). Zmienne środowiskowe mają pierwszeństwo przed `.env`, a `.env` przed wartościami domyślnymi. W kontenerze `app` adres bazy jest ustawiany w `compose.yaml` na host `postgres`.

### Dane startowe

Schemat bazy tworzy Flyway przy starcie aplikacji (`src/main/resources/db/migration`). Hibernate działa w trybie `ddl-auto: validate`: tylko sprawdza zgodność encji ze schematem i nigdy go nie zmienia.

Tworzenie zgłoszenia wymaga istniejącej, aktywnej usługi w tabeli `services`,
należącej do tenanta z tokenu (patrz "Walidacja usługi"). Usługi demonstracyjne ładuje migracja `db/seed/R__demo_services.sql`,
włączana zmienną `FLYWAY_LOCATIONS` (ustawioną w `compose.yaml` i `.env.example`). Domyślnie, bez tej zmiennej, dane demonstracyjne nie są ładowane, więc nie trafią do innego środowiska.

| `serviceId` | Tenant     | Aktywna | Wynik `POST` tokenem `tenant-a`  |
|-------------|------------|---------|----------------------------------|
| `987654321` | `tenant-a` | tak     | `201` (przykład ze specyfikacji) |
| `987654322` | `tenant-a` | tak     | `201`                            |
| `987654323` | `tenant-a` | nie     | `404 SERVICE_NOT_FOUND`          |
| `123456789` | `tenant-b` | tak     | `404 SERVICE_NOT_FOUND`          |

`externalId` w żądaniu wybiera klient (dowolny niepusty tekst, np. `OK-123456`); ponowne wysłanie tego samego `externalId` zwraca `200` z istniejącym zgłoszeniem.

Najprościej sprawdzić wszystkie endpointy kolekcją `http/trouble-ticket-api.http` (patrz "Ręczna weryfikacja API"). Przykład z `curl`:

```bash
TOKEN=$(./gradlew -q generateToken)
curl -i -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"externalId":"OK-123456","serviceId":987654321,"description":"Brak transmisji","status":"new","note":"Pierwsza notatka"}' \
  http://localhost:8080/api/v1/troubleTicket
```

### Uwierzytelnianie

Wszystkie endpointy `/api/v1/**` wymagają nagłówka `Authorization: Bearer <JWT>`. Token musi być podpisany algorytmem HS256 kluczem z `JWT_SECRET` i zawierać claim `tenant_id`.

Konfiguracja bezpieczeństwa (`SecurityConfig`):
- `/api/v1/**`: wymaga poprawnego tokenu JWT,
- `/swagger-ui/**`, `/v3/api-docs/**`: dostępne bez tokenu (Swagger UI i opis OpenAPI generowany przez springdoc),
- `/openapi/**`: dopuszczone bez tokenu, ale aplikacja nic pod tym adresem nie udostępnia,
- `/error`: dostępne bez tokenu (wewnętrzny adres Spring Boota, na który trafiają błędy),
- każda inna ścieżka jest zablokowana: bez tokenu `401 UNAUTHORIZED`, z poprawnym tokenem `403 FORBIDDEN`.

API jest bezstanowe (bez sesji, `SessionCreationPolicy.STATELESS`), a ochrona CSRF jest wyłączona,
ponieważ uwierzytelnianie odbywa się tokenem w nagłówku, a nie ciasteczkiem. Walidator tokenu sprawdza podpis oraz `exp` i `nbf` (z tolerancją 60 s na różnicę zegarów).

#### Tokeny testowe

Do ręcznej weryfikacji nie trzeba generować tokenów: kolekcja `http/trouble-ticket-api.http` zawiera gotowe tokeny (`tenant-a`, `tenant-b` oraz token bez `tenant_id`), ważne rok i podpisane domyślnym lokalnym sekretem.

Własny token JWT generuje zadanie Gradle:

```bash
./gradlew -q generateToken                                     # tenant-a, ważny 1 h
./gradlew -q generateToken -Ptenant=tenant-b -PttlSeconds=7200 # tenant-b
```

Zadanie podpisuje token HS256 tym samym sekretem co aplikacja i ustala go w tej samej kolejności: zmienna `JWT_SECRET`, potem plik `.env`, na końcu domyślna wartość lokalna. Token zawiera `sub`, `tenant_id`, `iat` i `exp`.

## Ręczna weryfikacja API

`http/trouble-ticket-api.http` to kolekcja 28 żądań dla IntelliJ HTTP Client: wszystkie endpointy,
ścieżki poprawne i błędy wymagane przez kontrakt (400, 401, 403, 404). 
Każde żądanie ma skrypt sprawdzający status, `Error.code` oraz najważniejsze pola i nagłówki (`Location`, `X-Request-ID`).
Uruchomienie: `docker compose up --build`, następnie w IntelliJ prawy przycisk na pliku → *Run All*.
Szczegóły i tabela oczekiwanych odpowiedzi: [`http/README.md`](http/README.md).

## Testy

```bash
./gradlew test   # wymaga uruchomionego Dockera (Testcontainers)
```

Testy są podzielone na trzy poziomy:

- **`TroubleTicketApiIntegrationTest`**: test end-to-end. Prawdziwy serwer HTTP na losowym porcie, prawdziwe bezpieczeństwo (token JWT podpisany tym samym sekretem) i PostgreSQL 17 w kontenerze (Testcontainers) z migracjami Flyway i danymi demonstracyjnymi. Obejmuje:
  - idempotencję tworzenia (`201`, potem `200` z tym samym `id`),
  - pełny przebieg: utworzenie, dodanie notatki, zamknięcie,
  - izolację tenantów (zgłoszenie `tenant-a` niewidoczne dla `tenant-b`),
  - nieaktywną usługę (`404 SERVICE_NOT_FOUND`) i token bez `tenant_id` (`403 MISSING_TENANT`),
  - wyścig: 8 równoległych żądań z tym samym `externalId` tworzy dokładnie jedno zgłoszenie.

  Test czyta JSON do własnych rekordów zamiast klas produkcyjnych, więc zmiana nazwy pola w odpowiedzi API go zepsuje, tak jak powinna.
- **`TroubleTicketControllerTest`** (`@WebMvcTest`): warstwa HTTP z zamockowanym serwisem, bez bazy. Sprawdza kody odpowiedzi, nagłówek `Location`, kształt JSON, `401` w formacie `Error` oraz walidację: test parametryzowany (`@ParameterizedTest`) przechodzi przez listę niepoprawnych ciał żądania i dla każdego oczekuje `400 VALIDATION_ERROR` z odpowiednim komunikatem.
- **`TroubleTicketServiceTest`**: testy jednostkowe serwisu (JUnit 5 + Mockito), bez bazy danych. Repozytorium zgłoszeń, `ServiceCatalog` i `CurrentTenant` są zamockowane. Obejmują:
  - tworzenie zgłoszenia i przypisanie tenanta z kontekstu uwierzytelnienia,
  - idempotencję: dla istniejącego `(tenantId, externalId)` zwracane jest istniejące zgłoszenie, bez zapisu i bez nadpisania opisu ani notatek,
  - odrzucenie niedozwolonego statusu (innego niż `new` przy tworzeniu i `closed` przy zamykaniu),
  - wyścig przy tworzeniu: gdy zapis trafi na unikalny indeks, zwracane jest zgłoszenie utworzone przez równoległe żądanie (`created = false`),
  - walidację usługi (`ServiceNotFoundException`),
  - listowanie i pobieranie zgłoszeń z filtrem tenanta,
  - zamykanie zgłoszenia i błąd dla nieistniejącego zgłoszenia,
  - dodawanie notatki i zwracanie dokładnie dodanej notatki.

Wszystkie testy mają strukturę given/when/then (komentarze oddzielają przygotowanie danych, wywołanie i sprawdzenie). Wspólne dane testowe są w `TicketFixtures` i metodach pomocniczych testu integracyjnego, a sprawdzenia wielu pól naraz używają miękkich asercji (`SoftAssertions`, `andExpectAll`), które raportują wszystkie różnice jednocześnie zamiast zatrzymać się na pierwszej.

Docker jest potrzebny tylko dla `TroubleTicketApiIntegrationTest`; testy kontrolera i serwisu działają bez bazy.

Obraz Dockera buduje wyłącznie plik jar poleceniem `./gradlew bootJar`, które nie uruchamia testów. Testy uruchamia się osobno (`./gradlew test`), dzięki czemu `docker compose up --build` pozostaje szybkie.

## Styl kodu

Formatowanie wymusza Spotless z palantir-java-format (styl google-java-format, 120 znaków w linii, wcięcie 4 spacje), więc styl nie zależy od ustawień IDE ani od osoby piszącej kod.

```bash
./gradlew spotlessApply   # formatuje kod
./gradlew check           # testy + kontrola formatowania; niesformatowany kod kończy się błędem
```

W IntelliJ IDEA ten sam format daje wtyczka palantir-java-format (włączona w ustawieniach wtyczki), dzięki czemu skrót formatowania w IDE i Spotless dają identyczny wynik.

## Decyzje i założenia

### Idempotencja tworzenia
Klucz idempotencji to `(tenantId, externalId)`. Jeśli zgłoszenie już istnieje, API zwraca `200 OK` z aktualną reprezentacją zasobu, bez jego modyfikacji (opis, usługa i notatka z ponowionego żądania są ignorowane). Nowe zgłoszenie zwraca `201 Created`. Nagłówek `Location` jest zwracany w obu przypadkach, zgodnie ze specyfikacją. Serwis zwraca `CreateResult(ticket, created)`, dzięki czemu kontroler wybiera kod odpowiedzi bez dodatkowego zapytania.

Unikalność jest wymuszona także w bazie (`unique_tenant_external_id`).

### Izolacja tenantów
Tenant wynika wyłącznie z claimu `tenant_id` zweryfikowanego tokenu, nigdy z parametrów żądania. Każde zapytanie o zgłoszenia zawiera `tenantId`. Zgłoszenie innego tenanta jest traktowane jak nieistniejące i zwraca `404 TROUBLE_TICKET_NOT_FOUND`, żeby nie ujawniać istnienia zasobu.

### Walidacja usługi (`SERVICE_NOT_FOUND`)
Specyfikacja wymaga `404 SERVICE_NOT_FOUND`, gdy usługa nie istnieje, nie jest aktywna albo nie należy do tenanta. Projekt nie ma dostępu do zewnętrznego katalogu usług, dlatego przyjąłem uproszczenie: tabela `services` (`id`, `tenant_id`, `active`) w tej samej bazie, sprawdzana przez `ServiceCatalog.requireActiveService` (pakiet `servicecatalog`).

Kolejność kroków przy tworzeniu: walidacja żądania (400), wyszukanie istniejącego zgłoszenia (200), walidacja usługi (404), zapis (201). Sprawdzenie usługi następuje celowo po wyszukaniu idempotentnym: ponowienie żądania dla istniejącego zgłoszenia powinno zwrócić `200`, nawet jeśli usługa została w międzyczasie dezaktywowana.

Uproszczenie: `services.id` jest samodzielnym kluczem głównym, więc ten sam identyfikator usługi nie może należeć do dwóch tenantów. W produkcji klucz powinien być złożony `(id, tenant_id)` albo walidacja powinna odpytywać system źródłowy usług.

### Statusy
- Tworzenie przyjmuje tylko `new`. Inna wartość lub jej brak to `400`.
- PATCH przyjmuje tylko `closed`. Inna wartość lub jej brak to `400`.
- W kodzie jest jeden enum `TroubleTicketStatus` z pełnym cyklem życia zgłoszenia. Kontrakt definiuje osobne schematy `TroubleTicketCreateStatus` i `TroubleTicketCloseStatus`, ale to ograniczenia wejścia poszczególnych endpointów, a nie osobne rodzaje statusu. Odwzorowanie ich trzema enumami dublowało kod (`@JsonCreator`/`@JsonValue`) i powodowało, że sprawdzenia w serwisie nigdy nie mogły się nie powieść.
- Wartość spoza enuma (np. `"foo"`) odrzuca deserializacja (`Field 'status' has an invalid value`). Poprawny status, który nie jest dozwolony w danym endpoincie (np. `"resolved"` przy tworzeniu), odrzuca serwis wyjątkiem `InvalidStatusException` (`Field 'status' must be 'new'` / `must be 'closed'`). Oba przypadki dają `400 VALIDATION_ERROR`.
- Specyfikacja dopuszcza, że nowe zgłoszenie od razu ma status `acknowledged` (przetwarzanie po stronie Systemu Obsługi Zgłoszeń). Ta implementacja nie symuluje SOZ, więc nowe zgłoszenie ma status `new`. Pozostałe statusy (`acknowledged`, `inProgress`, `resolved`, `rejected`) istnieją w modelu, ale w v1 ustawiałby je tylko proces wewnętrzny, poza publicznym API.
- Zamknięcie jest dozwolone z każdego statusu. Zamknięcie już zamkniętego zgłoszenia jest idempotentne i zwraca `200`. Kontrakt nie określa reguł przejść dla statusu źródłowego (np. czy można zamknąć `rejected`) przyjąłem, że ograniczenie dotyczy wyłącznie statusu docelowego.

### Identyfikatory
`id` zgłoszenia i notatki to UUID generowane przez aplikację. Przykłady w kontrakcie (`TT-2026-000001`, `NOTE-000001`) potraktowałem jako ilustracyjne, ponieważ schemat wymaga jedynie niepustego stringa. UUID nie ujawnia liczby zgłoszeń i nie wymaga sekwencji w bazie.

### Format błędów i request ID
Błędy obsługiwane przez aplikację mają format `Error` (`code`, `message`, `requestId`):

| Sytuacja                                                          | Status | `code`                     |
|-------------------------------------------------------------------|--------|----------------------------|
| Brakujące lub niepoprawne pole (walidacja `@Valid`)               | 400    | `VALIDATION_ERROR`         |
| Niedozwolona wartość statusu, nieznane pole, zły typ, błędny JSON | 400    | `VALIDATION_ERROR`         |
| Brak lub niepoprawny / wygasły token                              | 401    | `UNAUTHORIZED`             |
| Token bez claimu `tenant_id`                                      | 403    | `MISSING_TENANT`           |
| Poprawny token, ale ścieżka spoza `/api/v1` i dokumentacji        | 403    | `FORBIDDEN`                |
| Zgłoszenie nie istnieje lub należy do innego tenanta              | 404    | `TROUBLE_TICKET_NOT_FOUND` |
| Usługa nie istnieje, jest nieaktywna lub należy do innego tenanta | 404    | `SERVICE_NOT_FOUND`        |

Kontrakt definiuje kody `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `SERVICE_NOT_FOUND` i `TROUBLE_TICKET_NOT_FOUND`. Kod `MISSING_TENANT` nie występuje w kontrakcie (najbliższy odpowiednik to `FORBIDDEN`).

Ciało żądania, którego nie da się odczytać (`HttpMessageNotReadableException`: nieznana wartość statusu, nieznane pole, zły typ, błędny JSON), zwraca `400 VALIDATION_ERROR`. Komunikat wskazuje wyłącznie nazwę pola (np. `Field 'status' has an invalid value`), a nie oryginalny komunikat Jacksona, który ujawniałby nazwy klas.

Niedozwolony status zgłasza serwis wyjątkiem `InvalidStatusException`, który `GlobalExceptionHandler` mapuje na `400 VALIDATION_ERROR`, tak samo jak pozostałe błędy walidacji.

`GlobalExceptionHandler` nadal zawiera obsługę `IllegalArgumentException` (`400 ILLEGAL_ARGUMENT`). Kod aplikacji już go nie rzuca, a handler zwraca komunikat wyjątku klientowi (patrz "Znane ograniczenia").

Pozostałe błędy (nieznana ścieżka, niedozwolona metoda HTTP, nieoczekiwany błąd serwera) nie są obsługiwane osobno i zwracają domyślną odpowiedź Spring Boota z odpowiednim kodem HTTP.

Błędy 401 i 403 powstają w łańcuchu filtrów Spring Security, przed kontrolerem, dlatego obsługują je `AuthenticationEntryPoint` i `AccessDeniedHandler` (`JsonSecurityErrorHandler`), a nie `@RestControllerAdvice`. Wyjątkiem jest `MISSING_TENANT`: brak `tenant_id` wykrywa dopiero `CurrentTenant` w serwisie, a `403` zwraca `GlobalExceptionHandler`.

Token poprawnie podpisany, ale bez `tenant_id`, traktuję jako brak uprawnień (`403`): tożsamość jest zweryfikowana, ale nie da się ustalić zakresu danych.

Każde żądanie otrzymuje UUID (`RequestIdFilter`, uruchamiany przed uwierzytelnieniem), zwracany jako `requestId` w obiekcie `Error` oraz w nagłówku `X-Request-ID` każdej odpowiedzi.

### `additionalProperties: false`
Specyfikacja zabrania nieznanych pól, a Spring Boot domyślnie je ignoruje. Włączono `spring.jackson.deserialization.fail-on-unknown-properties=true`, więc nieznane pole w żądaniu daje `400 VALIDATION_ERROR` w formacie `Error`.

### Limity długości
Specyfikacja nie podaje maksymalnej długości tekstów. Kolumny `description` i `text` mają 5000 znaków, a `external_id` 255. Te same limity są walidowane w DTO (`@Length`/`@Size`), więc zbyt długa wartość daje `400 VALIDATION_ERROR`, a nie błąd bazy (`500`).

### Notatki
Notatki są zwracane w kolejności utworzenia (`@OrderBy("date ASC")`), więc notatka z żądania utworzenia jest zawsze pierwszym elementem `notes`, zgodnie z kontraktem. Endpoint dodawania notatki zwraca dokładnie tę notatkę, którą utworzyło żądanie (obiekt zwrócony przez `addNote`), a nie ostatni element listy. Notatka nie zmienia statusu zgłoszenia. Notatki można dodawać również do zamkniętego zgłoszenia, ponieważ kontrakt tego nie zabrania.

### Współbieżność
Przy równoległym tworzeniu tego samego `(tenantId, externalId)` oba żądania mogą nie znaleźć zgłoszenia i oba spróbować je zapisać. Ostatecznym zabezpieczeniem jest unikalny indeks: drugi `INSERT` kończy się `DataIntegrityViolationException`, które serwis przechwytuje, a następnie odczytuje i zwraca zgłoszenie utworzone przez pierwsze żądanie (`200 OK`).

`createTroubleTicket` celowo nie jest oznaczony `@Transactional`. Każde wywołanie repozytorium działa we własnej krótkiej transakcji, więc nieudany zapis wycofuje tylko swoją transakcję, a ponowny odczyt startuje w nowej. Wewnątrz jednej transakcji PostgreSQL po błędzie odrzuca kolejne zapytania („current transaction is aborted”), więc odczyt w bloku `catch` zakończyłby się błędem `500`.
- Zapis używa `saveAndFlush`, żeby `INSERT` trafił do bazy od razu i ewentualny błąd unikalności pojawił się wewnątrz `try`.
- Odczytane zgłoszenie jest już odłączone od sesji, dlatego `findByTenantIdAndExternalId` ładuje notatki tym samym zapytaniem (`@EntityGraph(attributePaths = "notes")`). Bez tego odwołanie do leniwie ładowanej listy notatek kończyłoby się `LazyInitializationException`.
- Wcześniejsza wersja zapisywała zgłoszenie w osobnej transakcji (`REQUIRES_NEW`), co wymagało dodatkowej klasy i dwóch połączeń z puli na jedno żądanie.

Zachowanie sprawdza test integracyjny, który wysyła 8 równoległych żądań z tym samym `externalId` i oczekuje dokładnie jednego `201`.

Encja zgłoszenia ma pole `@Version` (blokowanie optymistyczne), które chroni przed nadpisaniem zmian przez równoległe transakcje. API nie udostępnia wersji klientowi (brak `ETag` / `If-Match`), a konflikt nie jest mapowany na `409`.

### Uwierzytelnianie
Tokeny są weryfikowane współdzielonym sekretem HS256. To uproszczenie na potrzeby zadania.

### Brak paginacji
Zgodnie ze specyfikacją lista zgłoszeń nie wspiera paginacji ani filtrowania. Przy dużej liczbie zgłoszeń na tenanta wymagałoby to rozszerzenia kontraktu.

## Znane ograniczenia i ryzyka

- **Ważność tokenu:** walidator JWT sprawdza `exp` i `nbf` tylko wtedy, gdy są obecne. Token bez `exp` jest akceptowany bezterminowo.
- **Schemat bazy:** migracje Flyway są uruchamiane przy starcie aplikacji. Przy wielu instancjach lub dużych migracjach lepiej uruchamiać je osobnym krokiem wdrożenia.
- **Katalog usług:** tabela `services` jest minimalnym substytutem zewnętrznego katalogu. Nie ma endpointu do zarządzania usługami.
- **Komunikaty wyjątków:** handler `IllegalArgumentException` zwraca klientowi treść komunikatu wyjątku, która może pochodzić z biblioteki i zawierać szczegóły wewnętrzne. Nieoczekiwane wyjątki nie mają osobnego handlera, który zalogowałby szczegóły i zwrócił ogólny komunikat z `requestId`.
- **Format błędów spoza kontraktu:** błędy, których kontrakt nie opisuje (`404` dla nieznanej ścieżki, `405`, `500`), zwracają domyślny format Spring Boota, a nie `Error`, i nie zawierają `requestId` w treści (jest tylko w nagłówku `X-Request-ID`).
- **Konflikty wersji:** równoległa modyfikacja tego samego zgłoszenia (`@Version`) kończy się `500`, kontrakt nie przewiduje `409 Conflict`.
- **Konfiguracja:** `application.yaml` ma domyślne wartości `JWT_SECRET` i `POSTGRES_PASSWORD` przeznaczone do lokalnego uruchomienia. Wdrożenie musi je nadpisać zmiennymi środowiskowymi.
- **Obserwowalność:** konfiguracja loguje wszystkie zapytania SQL (`show-sql`, `org.hibernate.SQL: DEBUG`). Brak metryk, endpointów health (liveness/readiness) i `requestId` w logach (MDC).
