<!DOCTYPE html>
<html>
<head>
    <title>${title}</title>
</head>
    <body>
        <p>
            Hello ${user.firstName!} ${user.lastName!},
        </p>
        <p>
            This is a custom test email. Reset password by link:
        </p>
        <p>
            <a href="${baseUrl}/reset/finish?key=${user.resetKey}">Reset password</a>
        </p>
    </body>
</html>
