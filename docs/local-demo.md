# Local session and catalog demo

This guide starts the real ECP API and Next.js application with a local development dataset. The seed runner is active only with the `dev` Spring profile and is excluded whenever `prod` is active.

## Demo accounts

| Role | Email | Password |
| --- | --- | --- |
| Administrator | `admin@demo.local` | `DemoPass!2026` |
| Customer | `customer@demo.local` | `DemoPass!2026` |
| Staff | `staff@demo.local` | `DemoPass!2026` |
| Warehouse operator | `warehouse@demo.local` | `DemoPass!2026` |
| Customer support | `support@demo.local` | `DemoPass!2026` |

`GUEST` is the unauthenticated actor, so it has no account. Every listed account signs in through
the normal `POST /api/v1/sessions` endpoint and receives its own role in the JWT.

Never reuse these credentials outside a local machine.

## Start the demo

1. Start Docker Desktop, then the local dependencies:

   ```sh
   cd backend
   docker compose up -d postgres kafka redis-cache redis-state
   ```

2. Configure revalidation in `backend/.env` (or export these values before booting):

   ```dotenv
   ECP_REVALIDATION_URL=http://localhost:3000/api/internal/revalidate
   ECP_REVALIDATE_SECRET=demo-local-revalidation-secret
   ```

3. Configure `frontend-extract/.env.local`:

   ```dotenv
   ECP_API_BASE_URL=http://localhost:8080/api/v1
   CSRF_SECRET=demo-local-csrf-secret
   ECP_REVALIDATE_SECRET=demo-local-revalidation-secret
   ```

4. In separate terminals, run the backend with the demo profile and the frontend:

   ```sh
   cd backend
   SPRING_PROFILES_ACTIVE=dev ./gradlew :app:bootRun
   ```

   ```sh
   cd frontend-extract
   npm run dev
   ```

   Wait for `http://localhost:8080/healthz` and open `http://localhost:3000`.

The first dev-profile startup seeds five accounts, eight categories, 33 published products, 35 variants, availability states, and one admin-only draft product. Thirty catalog items are distributed across six collections so the storefront's 24-item cursor page has a stable second page. Later starts restore missing fixture rows and preserve edits to existing rows.

## Reset the demo records

This restores only records with the demo's fixed IDs and `DEMO-` SKUs. It deliberately does not clear unrelated local data or immutable audit entries.

```sh
cd backend
SPRING_PROFILES_ACTIVE=dev ./gradlew :app:bootRun --args='--ecp.dev.reset=true'
```

Stop the resulting backend process after it reports that the application has started, then use the regular demo command to continue. If unrelated data has introduced a database dependency on a demo record, the transaction fails rather than deleting that data.

## Browser checklist

1. Visit `/sign-in`; try a wrong password and verify the generic failure message.
2. Sign in as the customer. Confirm `/account/security` loads, use **End all other sessions** from two separate browser profiles, then sign out and verify protected account pages are no longer available.
3. Sign in as the administrator and open `/admin/products`. The seeded draft is visible here but never in the public catalog.
4. Browse `/`, `/c/demo-coffee`, and the kettle, grinder, and mug product details. Confirm local images, pricing, variants, and the mug's out-of-stock advisory state.
5. In admin, edit a product price or publication state and confirm the storefront changes after the catalog event callback completes. Create a product, variant, or image to exercise the remaining catalog controls.

To exercise refresh in a short session, start the backend with `ECP_JWT_ACCESS_TTL_SECONDS=5`, wait past five seconds after sign-in, then navigate to an authenticated account page. The frontend should renew the server-held token once and keep the session active.
