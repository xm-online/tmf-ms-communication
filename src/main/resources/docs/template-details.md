API. Get template details

## Preconditions
### Initialize mail templates
Mail templates could be:
* default
* custom
1. Default

The mail template name must follow the pattern: `{langKey}.ftl`. For example: `en.ftl`.
The mail template path may consist folders with more information about your template name.

Example:

Before you had email template: `config/tenants/{tenantName}/uaa/emails/en/activationEmail.ftl`.
After you will have the updated path and name: `config/tenants/{tenantName}/communication/emails/activation/en.ftl`.


## Get template details
When you have already initialized email templates you can retrieve template details by key.
You must be logged in before retrieving template details.

API: `GET` `/api/templates`

### curl example
```
curl --location --request GET 'http://localhost:8701/api/templates/<TEMPLATE_KEY>' 
--header 'Authorization: <TOKEN>
```
