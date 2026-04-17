# WhatsApp template creation requests

These `curl` requests create all WhatsApp templates currently used by the app.

- **WABA ID:** `27738895879034319`
- **Token:** replace `YOUR_ACCESS_TOKEN`
- **Endpoint base:** `https://graph.facebook.com/v25.0/27738895879034319/message_templates`

## Notes

- These requests use language codes `en_US` (English) and `es` (Spanish). If you use a different language code (e.g. plain `en`) set `WHATSAPP_TEMPLATE_LANGUAGE_CODE_EN` to match.
- Every template with variables includes `example.body_text`, because Meta rejects templates without sample values.
- Utility templates send **body variables only**. Authentication and login-link templates also send a **button parameter** (the OTP code or the token URL suffix), which Meta requires for those template categories.
- Meta auth templates use a **special schema**: the auth template body is predefined, the footer is configured with `code_expiration_minutes`, and the copy-code button uses lowercase `otp` / `copy_code`.
- In practice, some WABAs reject the optional `add_security_recommendation` field even though Meta documents it. The requests below omit it and use the smallest accepted payload.
- The app now supports the extra OTP button parameter Meta requires when sending authentication templates.

## Suggested runtime mapping

After approval, set these env vars to the template names below:

```bash
WHATSAPP_TEMPLATE_OTP_AUTHENTICATION_EN=reporter_otp_en
WHATSAPP_TEMPLATE_OTP_AUTHENTICATION_ES=reporter_otp_es
WHATSAPP_TEMPLATE_REPORTER_WELCOME_ES=reporter_welcome_es
WHATSAPP_TEMPLATE_REPORTER_LINK_EN=reporter_link_en
WHATSAPP_TEMPLATE_REPORTER_LINK_ES=reporter_link_es
WHATSAPP_TEMPLATE_REPORT_SUBMITTED_EN=report_submitted_en
WHATSAPP_TEMPLATE_REPORT_SUBMITTED_ES=report_submitted_es
WHATSAPP_TEMPLATE_REPORT_UPDATED_EN=report_updated_en
WHATSAPP_TEMPLATE_REPORT_UPDATED_ES=report_updated_es
WHATSAPP_TEMPLATE_REPORT_DELETED_EN=report_deleted_en
WHATSAPP_TEMPLATE_REPORT_DELETED_ES=report_deleted_es
WHATSAPP_TEMPLATE_REPORTER_LOGIN_LINK_EN=reporter_login_link_en
WHATSAPP_TEMPLATE_REPORTER_LOGIN_LINK_ES=reporter_login_link_es
```

## 1. OTP authentication (English)

Category: `authentication`  
Language: `en_US`

```bash
curl -X POST \
  'https://graph.facebook.com/v25.0/27738895879034319/message_templates' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "reporter_otp_en",
    "category": "authentication",
    "language": "en_US",
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
            "text": "Copy code"
          }
        ]
      }
    ]
  }'
```

## 2. OTP authentication (Spanish)

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

If you want both languages from one request, use Meta's auth-specific bulk endpoint like this:

```bash
curl -X POST \
  'https://graph.facebook.com/v25.0/27738895879034319/upsert_message_templates' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "reporter_otp",
    "languages": ["en_US", "es"],
    "category": "AUTHENTICATION",
    "components": [
      {
        "type": "BODY"
      },
      {
        "type": "FOOTER",
        "code_expiration_minutes": 10
      },
      {
        "type": "BUTTONS",
        "buttons": [
          {
            "type": "OTP",
            "otp_type": "COPY_CODE"
          }
        ]
      }
    ]
  }'
```

## 3. Reporter welcome (English)

Category: `MARKETING`  
Language: `en`  
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
    "name": "reporter_welcome_en",
    "category": "MARKETING",
    "language": "en",
    "components": [
      {
        "type": "BODY",
        "text": "Hello {{1}}, your Diocese Dashboard reporter account is ready. Username: {{2}}. You can sign in at {{3}} and request your login code there.",
        "example": {
          "body_text": [
            ["Maria Perez", "maria.perez", "https://dashboard.example.org"]
          ]
        }
      }
    ]
  }'
```

## 4. Reporter welcome (Spanish)

Category: `MARKETING`  
Language: `es`

```bash
curl -X POST \
  'https://graph.facebook.com/v25.0/27738895879034319/message_templates' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "reporter_welcome_es",
    "category": "MARKETING",
    "language": "es",
    "components": [
      {
        "type": "BODY",
        "text": "Hola {{1}}, su cuenta de reportero de Diocese Dashboard ya está lista. Usuario: {{2}}. Puede iniciar sesión en {{3}} y solicitar ahí su código de acceso.",
        "example": {
          "body_text": [
            ["Maria Perez", "maria.perez", "https://dashboard.example.org"]
          ]
        }
      }
    ]
  }'
```

## 5. Reporter link (English)

Category: `UTILITY`  
Language: `en`  
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
    "name": "reporter_link_en",
    "category": "UTILITY",
    "language": "en",
    "components": [
      {
        "type": "BODY",
        "text": "You have a new report link for \"{{1}}\" at {{2}} (date: {{3}}). Use this link to fill in the report: {{4}} Please open it in your browser.",
        "example": {
          "body_text": [
            ["Sunday Eucharist", "St Mark Church", "2026-04-20", "https://dashboard.example.org/r/abc123"]
          ]
        }
      }
    ]
  }'
```

## 6. Reporter link (Spanish)

Category: `UTILITY`  
Language: `es`

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
        "text": "Tiene un nuevo enlace para completar el reporte de servicio \"{{1}}\" en {{2}} (fecha: {{3}}). Use este enlace para acceder: {{4}} Por favor, ábrala en su navegador.",
        "example": {
          "body_text": [
            ["Eucaristia Dominical", "Iglesia San Marcos", "2026-04-20", "https://dashboard.example.org/r/abc123"]
          ]
        }
      }
    ]
  }'
```

## 7. Report submitted (English)

Category: `UTILITY`  
Language: `en`  
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
    "name": "report_submitted_en",
    "category": "UTILITY",
    "language": "en",
    "components": [
      {
        "type": "BODY",
        "text": "Your service report has been submitted successfully.\nTemplate: {{1}}\nChurch: {{2}}\nDate: {{3}}\nStatus: recorded.",
        "example": {
          "body_text": [
            ["Sunday Eucharist", "St Mark Church", "2026-04-20"]
          ]
        }
      }
    ]
  }'
```

## 8. Report submitted (Spanish)

Category: `UTILITY`  
Language: `es`

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

## 9. Report updated (English)

Category: `UTILITY`  
Language: `en`  
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
    "name": "report_updated_en",
    "category": "UTILITY",
    "language": "en",
    "components": [
      {
        "type": "BODY",
        "text": "An administrator has updated your service report.\nTemplate: {{1}}\nChurch: {{2}}\nDate: {{3}}\nChanges:\n{{4}}\nPlease review the update.",
        "example": {
          "body_text": [
            ["Sunday Eucharist", "St Mark Church", "2026-04-20", "Attendance updated to 54"]
          ]
        }
      }
    ]
  }'
```

## 10. Report updated (Spanish)

Category: `UTILITY`  
Language: `es`

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
        "text": "Un administrador ha actualizado su reporte de servicio.\nPlantilla: {{1}}\nIglesia: {{2}}\nFecha: {{3}}\nCambios:\n{{4}}\nPor favor, revise la actualización.",
        "example": {
          "body_text": [
            ["Eucaristia Dominical", "Iglesia San Marcos", "2026-04-20", "Asistencia actualizada a 54"]
          ]
        }
      }
    ]
  }'
```

## 11. Report deleted (English)

Category: `UTILITY`  
Language: `en`  
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
    "name": "report_deleted_en",
    "category": "UTILITY",
    "language": "en",
    "components": [
      {
        "type": "BODY",
        "text": "An administrator has deleted your service report.\nTemplate: {{1}}\nChurch: {{2}}\nDate: {{3}}\nStatus: deleted.",
        "example": {
          "body_text": [
            ["Sunday Eucharist", "St Mark Church", "2026-04-20"]
          ]
        }
      }
    ]
  }'
```

## 12. Report deleted (Spanish)

Category: `UTILITY`  
Language: `es`

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

## 13. Reporter login link (English)

Category: `UTILITY`  
Language: `en`  
Variables:

- Button URL suffix `{{1}}` = login token (UUID)

```bash
curl -X POST \
  'https://graph.facebook.com/v25.0/27738895879034319/message_templates' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "reporter_login_link_en",
    "category": "UTILITY",
    "language": "en",
    "components": [
      {
        "type": "BODY",
        "text": "Access Diocese Dashboard with this button. It expires in 10 minutes."
      },
      {
        "type": "BUTTONS",
        "buttons": [
          {
            "type": "URL",
            "text": "Sign In",
            "url": "https://YOUR_APP_DOMAIN/login?token={{1}}",
            "example": ["abc12345-0000-0000-0000-000000000000"]
          }
        ]
      }
    ]
  }'
```

## 14. Reporter login link (Spanish)

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
            "url": "https://YOUR_APP_DOMAIN/login?token={{1}}",
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
