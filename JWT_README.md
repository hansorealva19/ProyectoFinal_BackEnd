 # JWT secret setup

This project uses JWT tokens issued by `user-service`. To ensure all microservices validate the tokens
correctly, set the environment variable `JWT_SECRET` to the same value across services before starting them.

Quick steps (PowerShell)

1) set JWT_SECRET for the current session (example strong secret used in this repo defaults)

	$env:JWT_SECRET = "Ue7d9Kj4QpLmZ8sR2xvTgH5yVbNc1fR0wYz3AaPqS6tHjK9L"

2) then start services (example for frontend)

	cd "C:\Users\HANS\OneDrive\Documents\Java_Backend\ProyectoFinal_BackEnd"
	.\mvnw.cmd -DskipTests -pl frontend-service spring-boot:run

Also ensure the webhook secret used by `payment-service` and `order-service` match. The repo sets a secure default,
but you should override in env vars for production:

	$env:PAYMENT_WEBHOOK_SECRET = "<your-secret>"
	$env:ORDER_WEBHOOK_SECRET = "<your-secret>"

Only the merchant bank account id must be provided manually in `payment-service` config:

	payment.merchant.accountId=<MERCHANT_ACCOUNT_ID>

Security note: Do not commit real production secrets. Use a secret manager or environment variables in production.
