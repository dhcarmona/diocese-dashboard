# WhatsApp template creation requests

These `curl` requests create all WhatsApp templates currently used by the app.

- **WABA ID:** `27738895879034319`
- **Token:** replace `YOUR_ACCESS_TOKEN`
- **Endpoint base:** `https://graph.facebook.com/v25.0/27738895879034319/message_templates`

## Notes

- All templates use language code `es` (Spanish). Templates are sent in Spanish regardless of the reporter's preferred language.
- Every template with variables includes `example.body_text`, because Meta rejects templates without sample values.
- Utility templates send **body variables only**. Authentication and login-link templates also send a **button parameter** (the OTP code or the token URL suffix), which Meta requires for those template categories.
- Meta auth templates use a **special schema**: the auth template body is predefined, the footer is configured with `code_expiration_minutes`, and the copy-code button uses lowercase `otp` / `copy_code`.
- In practice, some WABAs reject the optional `add_security_recommendation` field even though Meta documents it. The requests below omit it and use the smallest accepted payload.
- The app supports the extra OTP button parameter Meta requires when sending authentication templates.

## Suggested runtime mapping

After approval, set these env vars to the template names below:

```bash
WHATSAPP_TEMPLATE_OTP_AUTHENTICATION_ES=reporter_otp_es
WHATSAPP_TEMPLATE_REPORTER_WELCOME_ES=reporter_welcome_es_v2
WHATSAPP_TEMPLATE_REPORTER_LINK_ES=reporter_link_es
WHATSAPP_TEMPLATE_REPORT_SUBMITTED_ES=report_submitted_es
WHATSAPP_TEMPLATE_REPORT_UPDATED_ES=report_updated_es
WHATSAPP_TEMPLATE_REPORT_DELETED_ES=report_deleted_es
WHATSAPP_TEMPLATE_REPORTER_LOGIN_LINK_ES=reporter_login_link_es
```

## 1. OTP authentication

Category: `authentication`
Language: `es`

```bash
curl -X POST \
  'https://graph.facebook.com/v25.0/27738895879034319/message_templates' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "reporter_otp_es",
    "category": "authentication",
    "language": "es",
    "message_send_ttl_seconds": 600,
    "components": [
      {
        "type": "body"
      },
      {
        "type": "footer",
        "code_expiration_minutes": 10
      },
      {
        "type": "buttons",
        "buttons": [
          {
            "type": "otp",
            "otp_type": "copy_code",
            "text": "Copiar código"
          }
        ]
      }
    ]
  }'
```

## 2. Reporter welcome

Category: `MARKETING`
Language: `es`
Variables:

- `{{1}}` = reporter full name
- `{{2}}` = username
- `{{3}}` = app URL

```bash
curl -X POST \
  'https://graph.facebook.com/v25.0/27738895879034319/message_templates' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "reporter_welcome_es_v2",
    "category": "MARKETING",
    "language": "es",
    "components": [
      {
        "type": "BODY",
        "text": "Hola {{1}}, su cuenta de reportero de Diocese Dashboard ya esta lista. Usuario: {{2}}. Puede iniciar sesion en {{3}} y solicitar ahi su codigo de acceso.",
        "example": {
          "body_text": [
            ["Maria Perez", "maria.perez", "https://dashboard.example.org"]
          ]
        }
      }
    ]
  }'
```

## 3. Reporter link

Category: `UTILITY`
Language: `es`
Variables:

- `{{1}}` = service template name
- `{{2}}` = church name
- `{{3}}` = active date
- `{{4}}` = reporter link URL

```bash
curl -X POST \
  'https://graph.facebook.com/v25.0/27738895879034319/message_templates' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "reporter_link_es",
    "category": "UTILITY",
    "language": "es",
    "components": [
      {
        "type": "BODY",
        "text": "Tiene un nuevo enlace para completar el reporte de servicio \"{{1}}\" en {{2}} (fecha: {{3}}). Use este enlace para acceder: {{4}} Por favor, abrala en su navegador.",
        "example": {
          "body_text": [
            ["Eucaristia Dominical", "Iglesia San Marcos", "2026-04-20", "https://dashboard.example.org/r/abc123"]
          ]
        }
      }
    ]
  }'
```

## 4. Report submitted

Category: `UTILITY`
Language: `es`
Variables:

- `{{1}}` = service template name
- `{{2}}` = church name
- `{{3}}` = service date

```bash
curl -X POST \
  'https://graph.facebook.com/v25.0/27738895879034319/message_templates' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "report_submitted_es",
    "category": "UTILITY",
    "language": "es",
    "components": [
      {
        "type": "BODY",
        "text": "Su reporte de servicio ha sido enviado exitosamente.\nPlantilla: {{1}}\nIglesia: {{2}}\nFecha: {{3}}\nEstado: registrado.",
        "example": {
          "body_text": [
            ["Eucaristia Dominical", "Iglesia San Marcos", "2026-04-20"]
          ]
        }
      }
    ]
  }'
```

## 5. Report updated

Category: `UTILITY`
Language: `es`
Variables:

- `{{1}}` = service template name
- `{{2}}` = church name
- `{{3}}` = service date
- `{{4}}` = change list

```bash
curl -X POST \
  'https://graph.facebook.com/v25.0/27738895879034319/message_templates' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "report_updated_es",
    "category": "UTILITY",
    "language": "es",
    "components": [
      {
        "type": "BODY",
        "text": "Un administrador ha actualizado su reporte de servicio.\nPlantilla: {{1}}\nIglesia: {{2}}\nFecha: {{3}}\nCambios:\n{{4}}\nPor favor, revise la actualizacion.",
        "example": {
          "body_text": [
            ["Eucaristia Dominical", "Iglesia San Marcos", "2026-04-20", "Asistencia actualizada a 54"]
          ]
        }
      }
    ]
  }'
```

## 6. Report deleted

Category: `UTILITY`
Language: `es`
Variables:

- `{{1}}` = service template name
- `{{2}}` = church name
- `{{3}}` = service date

```bash
curl -X POST \
  'https://graph.facebook.com/v25.0/27738895879034319/message_templates' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "report_deleted_es",
    "category": "UTILITY",
    "language": "es",
    "components": [
      {
        "type": "BODY",
        "text": "Un administrador ha eliminado su reporte de servicio.\nPlantilla: {{1}}\nIglesia: {{2}}\nFecha: {{3}}\nEstado: eliminado.",
        "example": {
          "body_text": [
            ["Eucaristia Dominical", "Iglesia San Marcos", "2026-04-20"]
          ]
        }
      }
    ]
  }'
```

## 7. Reporter login link

Category: `UTILITY`
Language: `es`
Variables:

- Button URL suffix `{{1}}` = login token (UUID)

```bash
curl -X POST \
  'https://graph.facebook.com/v25.0/27738895879034319/message_templates' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "reporter_login_link_es",
    "category": "UTILITY",
    "language": "es",
    "components": [
      {
        "type": "BODY",
        "text": "Accede al Diocese Dashboard con este botón. Caduca en 10 minutos."
      },
      {
        "type": "BUTTONS",
        "buttons": [
          {
            "type": "URL",
            "text": "Iniciar sesión",
            "url": "https://dashboard.episcopalcr.org/login?token={{1}}",
            "example": ["abc12345-0000-0000-0000-000000000000"]
          }
        ]
      }
    ]
  }'
```

## Optional: list all templates after creation

```bash
curl -X GET \
  'https://graph.facebook.com/v25.0/27738895879034319/message_templates?fields=name,language,category,status' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

