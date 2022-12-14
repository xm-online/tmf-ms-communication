
## Preconditions
### Initialize mail templates
Mail templates could be:
* Default

  Default email templates should be located in the `.ftl` format file by the following path:
  `config/tenants/{tenantName}/communication/emails/**`


* Custom

   Custom email templates should be located in the `.ftl` format file by the following path:
   `config/tenants/{tenantName}/communication/custom-emails/**`

The mail template name must follow the pattern: `{langKey}.ftl`. For example: `en.ftl`.
The mail template path may consist folders with more information about your template name. For example: `config/tenants/{tenantName}/communication/emails/activation/en.ftl`

**_Note: custom email templates have more value and will override default emails fields if exists_**

### Initialize email specification
Email specifications could be:
1. Default

  Default email specification should be located in the `email-spec.yml` by the following path:
  `config/tenants/{tenantName}/communication/email-spec.yml`

  The default email specification has the following structure:
  *  `langs` - array of supporting email languages
  *  `emails` 
        * `templateKey` - unique template name
        * `subjectTemplate` - subject for email
        * `name` - name of specification
        * `templatePath` - relative path under `emails` folder to the existing email template.
        * `contextSpec` - schema for the email context
        * `contextForm` - form for the email context
        * `contextExample` - example data for the email context
  
  #### Default email specification example: 
  ```
  ---
langs:
  - en
  - uk
emails:
  - templateKey: firstTemplateKey
    name: "First template"
    subjectTemplate: {en: "Default subject 1", uk: "Тема за замовчуванням 1"}
    templatePath: activation
    contextExample: |
      {
          "name": "Default name"
      }
    contextSpec: |
      {
          "type": "object",
          "properties": {
              "name": { "type": "string" }
          }
      }
    contextForm: |
      {
        "form": [
           { "key": "name", "title": { "en": "Name" }, "type": "string" }
        ]
      }
  ```  
    
2. Custom

  Custom email specification should be located in the `custom-email-spec.yml` by the following path:
  `config/tenants/{tenantName}/communication/custom-email-spec.yml`

  The default email specification has the following structure:
*  `emails`
    * `templateKey` - existing template key in the default email specification
    * `subjectTemplate` - subject for email that will override default email specification subject

#### Custom email specification example:

```
---
emails:
  - templateKey: firstTemplateKey
    subjectTemplate: {en: "New subject ", uk: "Нова тема"}
```


## Get email specifications
When you have already initialized email template specifications you can retrieve all specs.
You must be logged in before retrieving template details.

API: `GET` `/api/templates`
```
curl --location --request GET 'http://localhost:8701/api/templates/' 
--header 'Authorization: <TOKEN>
```

## Get template details
When you have already initialized email templates and specifications you can retrieve template details by key and specific language.
You must be logged in before retrieving template details.

API: `GET` `/api/templates`

### curl example
```
curl --location --request GET 'http://localhost:8701/api/templates/firstTemplateKey/en' 
--header 'Authorization: <TOKEN>
```

## Render content to html
API: `POST` `/api/templates/render`
### Request body:
* `content` - freemarker content to render
* `model` -  variables from freemarker content
### curl example
```
curl --location --request POST 'http://localhost:8701/api/templates/render' 
--header 'Authorization: Bearer <TOKEN>
--header 'Content-Type: application/json' 
--data-raw '{
    "content": "<p> Hello ${user.firstName!} ${user.lastName!}, </p> <p> Activate your account. </p> ",
    "model": {
        "user": {
            "firstName": "Name",
            "lastName": "Surname"
        }
    }
}
```
