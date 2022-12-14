## Update email template
API: `PUT` `/api/templates/<template_key>/<lang_key>`
### Request body:
* `content` - email template content
* `templateSubject` - email template subject
### curl example
```
curl --location --request POST 'http://127.0.0.1:8701/api/templates/accountCreationEmail/en' 
--header 'Authorization: Bearer <TOKEN>
--header 'Content-Type: application/json' 
--data-raw '{
    "content": "<p> Hello ${user.firstName!} ${user.lastName!}, </p> <p> Create your account. </p> ",
    "templateSubject": "User account creation"
}
```
