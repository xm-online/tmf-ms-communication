<!DOCTYPE html>
<html>
<head>
    <title >XM^online account activation</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <link rel="shortcut icon" href="${baseUrl}/favicon.ico" />
</head>
    <body>
        <p>
            Dear ${user.firstName + ' ' + user.lastName}
        </p>
        <p>
            Your XM^online account has been created, please click on the URL below to access it:
        </p>
        <p>
            <a href="${baseUrl}/activate?key=${activationKey}"
            >Activation Link</a>
        </p>
        <p>
            <span>Regards, </span>
            <br/>
            <em>XM^online Team.</em>
        </p>
    </body>
</html>
