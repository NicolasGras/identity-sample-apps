# Deploying the Resource Server sample application

This sample application implements basic TODO functionality. It fulfills the
role of a Resource Server in this suite of sample applications, one of the
[four roles](https://tools.ietf.org/html/rfc6749#section-1.1) described in the
OAuth2 specification. In practical terms, any application that has protected
API endpoints requiring an OAuth2 token to access is fulfilling the role of a
Resource Server.

In this sample application the protected endpoints are:
 * `GET /todo` to list TODO items. Requires the access token to contain
 `todo.read` scope.
 * `POST /todo` to create a TODO item. Requires the access token to contain
 `todo.write` scope. Example response: `{"todo":"<content>"}`
 * `DELETE /todo/{id}` to delete a TODO item. Requires the access token to
 contain `todo.write` scope.

A client application such as the [authcode sample app](../authcode) may try to
access the resource server by passing an access token along with its requests
in the `Authorization: bearer <token-here>` header.  The access token
represents a user's delegated authorization and contains a list of scopes, or
permissions, that user has. The Resource Server performs access control by
checking for required scopes in the access token before returning a response.
If the token is valid and contains the correct scopes, the resource server will
return a
response to the request.

# ////////////// TODO >>>>>>>>>>>>>>>>>>>>
# Deploying the Resource Server with manual authorization server configuration

# Deploying the Resource Server sample application using the Pivotal Single
# Sign-On Service for authorization server discovery

## Prerequisites
1. You must have developer login with the Space Developer role for the org and
space where you'd like to deploy this sample app
1. An operator must have installed the [Pivotal Single Sign-On Service](https://docs.pivotal.io/p-identity/1-4/index.html)
1. An operator must have [configured at least one
plan](https://docs.pivotal.io/p-identity/1-4/manage-service-plans.html) for the
SSO Service that is visible to your Org.
1. The person using this sample app must know login credentials for a user in
this plan. For new plans, an operator may need to [create a
user](http://docs.pivotal.io/p-identity/1-4/manage-users.html).

### Step 0: Create an identity service instance

Using the CF CLI, login as a user with the [Space
Developer](https://docs.cloudfoundry.org/concepts/roles.html#roles) role and
target the space where you'd like the sample app to reside.

    cf api api.<your-domain>
    cf login
    cf target -o <your-org> -s <your-space>

All of the sample apps in this repository need to be bound to an identity
service instance. If you have previously deployed one of the other sample apps,
you can reuse the service instance you created at that time.  You can
check if you have any existing service instances in your 
space by running

    cf services 

If you don't see any service instances that have already been created for your
space you will need to create a new one. To create a service instance, first
list the available identity plans using

    cf marketplace -s p-identity
    
If you do not see any plans listed, you will need help from an administrator to
create and configure a plan. Assuming you have at least one plan available in
the marketplace, create a service instance in your space by running

    cf create-service p-identity <plan-name> p-identity-sample-instance
    
The name of your service instance can be whatever you like, but we have chosen
`p-identity-sample-instance` to match the name we have pre-specified in the
[resource server sample application manifest](./manifest.yml).

### Step 1: Update resource server manifest.yml with the name of your identity service instance

The [`manifest.yml`](./manifest.yml) includes [a configuration
block](https://docs.cloudfoundry.org/devguide/deploy-apps/manifest.html#services-block)
called `services`. Your app will be bound to any service instances you list in
this section when it is pushed.

If you used the exact commands provided in Step 2, you should have a created an
instance of the identity service called `p-identity-sample-instance`. Ensure
this value appears in the `services` section of the `manifest.yml`.

### Step 2: Build and push the application

The resource server needs to know the Auth Server (or UAA) location in order to
retrieve the token key to validate the tokens.  Set the Auth Server location as
the value of the auth_domain environment variable for the authcode sample app.

`cf set-env <RESOURCE_SERVER_APP_NAME> AUTH_SERVER <AUTH_SERVER_LOCATION>`

NOTE: Beginning with our Spring Boot 1.5 version of the identity sample
applications, bind the Resource Server to the Singl†e Sign-On Service instead
of providing the AUTH_SERVER value.

For example, for a given SSO service plan/UAA identity zone, the location would
be `https://subdomain.login.my-domain.org`


## Setting up Authcode Sample App to use Resource Server

Currently, only the authcode sample app uses the resource server, but the other grant types should be similar.
The authcode sample app needs to know the resource server location in order to manage TODO resources.

`cf set-env <AUTHCODE_APP_NAME> RESOURCE_URL <RESOURCE_SERVER_URL>`

NOTE: You must remove the trailing slash ('/') from the URL.

For the sample app to work you need to go to the Resource dashboard and create a Resource with name `todo` and `todo.read` and `todo.write` permissions.
After creating the resource, you need to update the authcode-sample app with the previously created scopes on the App dashboard.
Follow the steps [here](http://docs.pivotal.io/p-identity/manage-resources.html) to create the resource and permissions.

The authenticated user should also have the scopes `todo.read` and `todo.write`.

NOTE: If a user doesn't have these scopes, contact your local admin to grant these scopes to that user.

# Bootstrap Application Client Configurations for the Pivotal Single Sign-On Service Instance
Beginning in SSO 1.4.0, you can use the following values your application's manifest to bootstrap client configurations for your applications automatically when binding or rebinding your application to the service instance. These values will be automatically populated to the client configurations for your application through CF environment variables.

When you specify your own scopes and authorities, consider including openid for scopes on auth code, implicit, and password grant type applications, and uaa.resource for client credentials grant type applications, as these will not be provided if they are not specified.

The table below provides a description and the default values. Further details and examples are provided in the sample application manifests.

| Property Name | Description | Default |
| ------------- | ------------- | ------------- |
| name | Name of the application | (N/A - Required Value) |
| GRANT_TYPE | Allowed grant type for the application through the SSO service - only one grant type per application is supported by SSO | authorization_code |
| SSO_IDENTITY_PROVIDERS | Allowed identity providers for the application through the SSO service plan | uaa |
| SSO_REDIRECT_URIS | Comma separated whitelist of redirection URIs allowed for the application - Each value must start with http:// or https:// |  (Will always include the application route) |
| SSO_SCOPES | Comma separated list of scopes that belong to the application and are registered as client scopes with the SSO service. This value is ignored for client credential grant type applications. |  openid |
| SSO_AUTO_APPROVED_SCOPES | Comma separated list of scopes that the application is automatically authorized when acting on behalf of users through SSO service | <Defaults to existing scopes/authorities> |
| SSO_AUTHORITIES | Comma separated list of authorities that belong to the application and are registered as client authorities with the SSO service. Authorities are restricted to the space they were originally created. Privileged identity zone/plan administrator scopes (e.g. scim.read, idps.write) cannot be bootstrapped and must be assigned by zone/plan administrators. This value is ignored for any grant type other than client credentials. | uaa.resource |
| SSO_REQUIRED_USER_GROUPS | Comma separated list of groups a user must have in order to authenticate successfully for the application | (No value) |
| SSO_ACCESS_TOKEN_LIFETIME | Lifetime in seconds for the access token issued to the application by the SSO service | 43200 |
| SSO_REFRESH_TOKEN_LIFETIME | Lifetime in seconds for the refresh token issued to the application by the SSO service | 2592000 (not used for client credentials) |
| SSO_RESOURCES |  Resources that the application will use as scopes/authorities for the SSO service to be created during bootstrapping if they do not already exist - The input format can be referenced in the provided sample manifest. Note that currently all permissions within the same top level permission (e.g. todo.read, todo.write) must be specified in the same application manifest. Currently you cannot specify additional permissions in the same top level permission (e.g. todo.admin) in additional application manifests.| (No value) |
| SSO_ICON |  Application icon that will be displayed next to the application name on the Pivotal Account dashboard if show on home page is enabled - do not exceed 64kb | (No value) |
| SSO_LAUNCH_URL |  Application launch URL that will be used for the application on the Pivotal Account dashboard if show on home page is enabled | (Application route) |
| SSO_SHOW_ON_HOME_PAGE |  If set to true, the application will appear on the Pivotal Account dashboard with the corresponding icon and launch URL| True |

To remove any variables set through bootstrapping, you must use `cf unset-env <APP_NAME> <PROPERTY_NAME>` and rebind the application.
