# Unified Auth Design

## Goal

Provide a usable authentication flow for the admin web app and the miniapp:

- Admin users log in with account and password and receive a JWT.
- Miniapp users log in through the existing WeChat login entry and receive a JWT.
- Gateway validates JWTs for protected APIs and forwards user identity headers to downstream services.

## Architecture

`common-core` owns JWT signing and verification so `user-service` and `gateway` use the same token format. The implementation uses HMAC-SHA256 with only JDK APIs to avoid adding a new third-party dependency across modules.

`user-service` owns identity. It adds `/auth/admin/login` for admin login and changes `/user/wechat/login` to return an auth session instead of only a user profile. Admin credentials are configuration-backed for this stage because the current schema does not yet contain account/password/RBAC tables.

`gateway` remains the single edge guard. Public paths stay configurable. Protected paths require a valid unexpired Bearer token, and valid requests receive `X-User-Id`, `X-User-Type`, and `X-User-Name` headers.

## Data Flow

Admin:

1. `frontend-admin` posts account/password to `/auth/admin/login`.
2. `user-service` validates against configured admin credentials.
3. `user-service` returns `token`, `expiresAt`, `user`.
4. The admin app stores the token and attaches it to later requests.

Miniapp:

1. `miniapp` calls `uni.login`.
2. The miniapp posts the current payload to `/user/wechat/login`.
3. `user-service` creates or reuses the volunteer user and returns `token`, `expiresAt`, `user`.
4. The miniapp stores the token and attaches it to later requests.

## Error Handling

Missing credentials, invalid password, missing openid, invalid token, malformed token, and expired token all return business or HTTP unauthorized errors. Real WeChat `code` to `openid` exchange remains a future adapter because appid/appsecret values are not available in the project yet.

## Testing

Add focused unit tests for:

- JWT signing, parsing, tamper rejection, and expiry rejection.
- Admin login success/failure.
- WeChat login session token issuance.
- Gateway rejection of missing/invalid tokens and forwarding headers for valid tokens.
