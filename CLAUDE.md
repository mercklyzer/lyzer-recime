# CLAUDE.md

Recipe management REST API. See `requirements.md` for the product spec.

## Commands

```bash
./mvnw test                  # run tests
./mvnw spring-boot:run       # run the app
./mvnw clean verify          # full build
```

## Stack

- **Spring Boot 3.5.0 — strictly.** Do not upgrade the Boot line.
- Java 17, Maven (always `./mvnw`), PostgreSQL 14.17, Lombok.

## Package layout

Package **by feature**, not by layer:

```
com.lyzer.lyzerrecime
  recipe/          RecipeController, RecipeService, RecipeRepository, Recipe
    dto/           CreateRecipeRequest, RecipeResponse, RecipeSearchCriteria
  common/
    error/         GlobalExceptionHandler, exceptions
    config/
```

- Keep repositories package-private where possible — nothing outside the feature
  package reaches past the service.
- DTOs live in a `dto/` subpackage under their feature.

## Naming

- DTOs are named by role: `CreateXRequest`, `UpdateXRequest`, `XResponse`,
  `XSummaryResponse`. **Never `XDto`.**
- Query-parameter bindings are `XSearchCriteria`.
- No `I` prefix on interfaces, no `Impl` suffix. With a single implementation,
  skip the interface entirely.
- Booleans read as predicates (`vegetarian`, not `isVegetarian`).
- Test methods state behaviour: `returns404WhenRecipeDoesNotExist`.

## Java conventions

- Records for anything immutable and data-shaped. Classes only where mutability
  or JPA requires it.
- Prefer immutable fields and `final` wherever the framework and object lifecycle
  allow. Injected dependencies are **always** `final`. JPA entity state is mutable
  by design — that is the accepted exception.
- `Optional` is a return type only — never a parameter, never a field. Repositories
  return `Optional<T>`; the service unwraps it into a domain exception; nothing
  past the service sees one.
- Never return `null` from a public method: empty collection, `Optional`, or throw.
- No magic values — page-size defaults and similar live in constants or config.
- `var` is allowed for obvious right-hand sides. Don't overuse it, and never where
  it hides the type.
- Comments are minimal and why-not-what. No Javadoc on self-explanatory code.
  Assumptions belong in the README.

## Lombok

- **Never `@Data` or `@EqualsAndHashCode` on a JPA entity** — generated
  `equals`/`hashCode` span every field, trigger lazy-loading on `hashCode()`, and
  break `Set` membership once the DB assigns an `id`.
- **No `@Builder` on entities.** Entities are annotated
  `@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)`.
  Map request → entity in one place via a static factory, `Recipe.from(request)`.
- **No `@Setter` on an entity that enforces its own invariants.** Hibernate uses
  field access when `@Id` is on a field, so it never needs one, and a generated
  `setIngredients(List)` detaches the managed collection — *"a collection with
  cascade=all-delete-orphan was no longer referenced"*. Mutate through named
  methods (`recipe.applyUpdate(request)`) that keep the invariants intact.
  `@Setter` is acceptable only on an entity with no invariants to protect.
- **DTOs use no Lombok** — they are records.
- `@RequiredArgsConstructor` on `final` fields is the injection mechanism.
  Constructor injection only: no field `@Autowired`, no setter injection.
- `@Slf4j` for logging. **Never `@SneakyThrows`.**

## Web layer

- Controllers are thin: bind, delegate, map. No business logic, no repository
  access, no `@Transactional`.
- Entities are never used as request or response types.
- Errors: one `@RestControllerAdvice` extending `ResponseEntityExceptionHandler`,
  responses as `ProblemDetail` (RFC 9457). Controllers and services never build
  error responses — they throw.
- Not-found is an exception (`RecipeNotFoundException`), never an `Optional`
  returned to a controller.
- Validation lives on request records (`@NotBlank`, `@Positive`, …) with `@Valid`
  on the body; `MethodArgumentNotValidException` → 400 with per-field errors.

| Operation | Endpoint | Success |
|---|---|---|
| Create | `POST /api/recipes` | 201 + `Location` |
| Read | `GET /api/recipes/{id}` | 200 |
| Search | `GET /api/recipes` | 200, `Page<RecipeSummaryResponse>` |
| Update | `PUT /api/recipes/{id}` | 200 |
| Delete | `DELETE /api/recipes/{id}` | 204, empty body |

- Update is `PUT` only, full replacement. No `PATCH`.
- Search filters are query params on the collection resource — **no `/search`
  path**, no verbs in URLs.

## Persistence

- `spring.jpa.open-in-view=false`, set explicitly.
- All associations `FetchType.LAZY` (override the EAGER default on `@ManyToOne`).
  Use `@EntityGraph` where the detail view needs children.
- `@Transactional` on the service only — never controller or repository. Reads are
  `@Transactional(readOnly = true)`.
- Dynamic search uses JPA `Specification`s: one composable predicate per filter,
  combined with `and()`. Not derived query methods per filter combination.
- Search is always `Pageable`; repositories return `Page<T>`, never an unbounded
  `List`.
- Timestamps via JPA auditing (`@CreatedDate` / `@LastModifiedDate`), not hand-set.
- **Schema is managed by Flyway** — versioned SQL in `db/migration`, with
  `spring.jpa.hibernate.ddl-auto=validate`. Never `update` or `create-drop`.

## Config & logging

- `@ConfigurationProperties` with a typed record for app-specific settings.
  `@Value` only for genuine one-offs.
- Never log-and-rethrow — the exception handler logs once, at the boundary.
- 5xx logged at `error` with stack trace; 4xx are client mistakes, not incidents.
  Never log request bodies.

## Testing

Tests run against **Testcontainers PostgreSQL 14.17** (`@ServiceConnection`),
never H2. Flyway migrations run in tests.

1. **Repository / `Specification` tests** — `@DataJpaTest`. The highest-value
   layer: every search filter, pagination and count correctness. Pay particular
   attention to `excludeIngredients`, which needs a `NOT EXISTS` subquery — a
   naive `notEqual` on a join matches almost everything.
2. **Controller slice tests** — `@WebMvcTest` with a mocked service. Status codes,
   `Location` on create, validation 400s, 404 `ProblemDetail` shape, malformed
   JSON → 400, query-param binding to `RecipeSearchCriteria`.
3. **One `@SpringBootTest` integration test** — create → fetch → search → update →
   delete. The only place the controller, real service and real repository meet.

- No standalone service unit tests: the service is delegation, and those cases are
  covered through the controller slice.
- AssertJ (`assertThat`), not JUnit assertions.
- Do not test getters/setters, Lombok output, Spring wiring, or `Recipe.from(...)`
  in isolation.
