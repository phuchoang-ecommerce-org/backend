# Local session and catalog demo

This guide starts the real ECP API and Next.js application with a deliberately local-only demo dataset. The seed runner is active only with the `demo` Spring profile; ordinary boot runs and every production profile are unchanged.

## Demo accounts

| Role | Email | Password |
| --- | --- | --- |
| Administrator | `admin@demo.local` | `DemoPass!2026` |
| Customer | `customer@demo.local` | `DemoPass!2026` |

Never reuse these credentials outside a local machine.

## Start the demo

1. Start Colima, then the local dependencies:

   ```sh
   colima start --memory 4
   cd backend-extract
   docker compose up -d postgres kafka redis-cache redis-state
   ```

2. Configure revalidation in `backend-extract/.env` (or export these values before booting):

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
   cd backend-extract
   SPRING_PROFILES_ACTIVE=demo ./gradlew :app:bootRun
   ```

   ```sh
   cd frontend-extract
   npm run dev
   ```

   Wait for `http://localhost:8080/healthz` and open `http://localhost:3000`.

The first demo-profile startup seeds two accounts, two categories, three published products with local image assets, variants, availability states, and one admin-only draft product. Later starts are idempotent and preserve edits made in the UI.

## Reset the demo records

This restores only records with the demo's fixed IDs and `DEMO-` SKUs. It deliberately does not clear unrelated local data or immutable audit entries.

```sh
cd backend-extract
SPRING_PROFILES_ACTIVE=demo ./gradlew :app:bootRun --args='--ecp.demo.reset=true'
```

Stop the resulting backend process after it reports that the application has started, then use the regular demo command to continue. If unrelated data has introduced a database dependency on a demo record, the transaction fails rather than deleting that data.

## Browser checklist

1. Visit `/sign-in`; try a wrong password and verify the generic failure message.
2. Sign in as the customer. Confirm `/account/security` loads, use **End all other sessions** from two separate browser profiles, then sign out and verify protected account pages are no longer available.
3. Sign in as the administrator and open `/admin/products`. The seeded draft is visible here but never in the public catalog.
4. Browse `/`, `/c/demo-coffee`, and the kettle, grinder, and mug product details. Confirm local images, pricing, variants, and the mug's out-of-stock advisory state.
5. In admin, edit a product price or publication state and confirm the storefront changes after the catalog event callback completes. Create a product, variant, or image to exercise the remaining catalog controls.

To exercise refresh in a short session, start the backend with `ECP_JWT_ACCESS_TTL_SECONDS=5`, wait past five seconds after sign-in, then navigate to an authenticated account page. The frontend should renew the server-held token once and keep the session active.
