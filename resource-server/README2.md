# Deploying the Resource Server sample application

The purpose of this sample application is to demonstrate how simple it is to
implement an OAuth2 Resource Server using [Spring Boot](https://projects.spring.io/spring-boot/) and the the [Spring Cloud SSO Connector](https://github.com/pivotal-cf/spring-cloud-sso-connector) when deploying to Cloud Foundry with the [Pivotal Single-Sign On Service](http://docs.pivotal.io/p-identity/1-4/getting-started.html).

This sample application implements basic TODO functionality. It fulfills the
role of a **Resource Server** in this suite of sample applications, one of the
[four roles](https://tools.ietf.org/html/rfc6749#section-1.1) described in the
OAuth2 specification. In practical terms, any application that has protected
API endpoints secured by access tokens is fulfilling the role of a
Resource Server.

In this sample application the protected endpoints are:
 * `GET /todo` to list TODO items. Requires the access token to contain
 `todo.read` scope.
 * `POST /todo` to create a TODO item. Requires the access token to contain
 `todo.write` scope. Example response: `{"todo":"<content>"}`
 * `DELETE /todo/{id}` to delete a TODO item. Requires the access token to
 contain `todo.write` scope.

A **Client** application such as the [authcode sample app](../authcode) may try to
call the resource server by passing an access token along with its requests
in the `Authorization: bearer <token-here>` header.  The access token
represents a user's delegated authorization and contains a list of scopes, or
permissions, that user has.

The Resource Server performs access control by
checking for required scopes in the access token before returning a response.
If the token is valid and contains the correct scopes, the resource server will
return a response to the request.

## Prerequisites for this deployment walkthrough
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

If you do not see any plans listed, you have not satisfied the prerequisites for this walkthrough and will need help from an administrator to create and configure an SSO plan. Assuming you have at least one plan available in
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

If you used the exact commands provided above, you should have created an
instance of the identity service called `p-identity-sample-instance`. Ensure
this value appears in the `services` section of the `manifest.yml`.

### Step 2: Build and push the application

    ./gradlew clean build
    cf push

When you push the sample resource server app, the following happens:
- The `resource-server.jar` artifact gets uploaded to the cloud controller
- A route gets bound to your application,
  `https://resource-server-sample.<your-domain>`
- The sample resource server app gets bound to the SSO service instance,
  resulting in the creation of an oauth client configuration for the application
  in the UAA.
- The cloud controller provides this oauth client configuration to the pushed
  resource server application through the `VCAP_SERVICES` environment variable.
  You can view these values yourself by running `cf env resource-server-sample`.
- The sample resource server app is started.
- During startup, the autoconfiguration performed by spring-cloud-sso-connector (a dependency of the sample app) reads the `VCAP_SERVICES` environment variable and translates the oauth client configuration values under `p-identity` into the properties used by Spring's OAuth implementation `org.springframework.cloud:spring-cloud-starter-oauth2`.

The resource server needs to know the Auth Server (or UAA) location in order to
retrieve the token key to validate the tokens.  Set the Auth Server location as
the value of the auth_domain environment variable for the authcode sample app.

`cf set-env <RESOURCE_SERVER_APP_NAME> AUTH_SERVER <AUTH_SERVER_LOCATION>`

NOTE: Beginning with our Spring Boot 1.5 version of the identity sample
applications, bind the Resource Server to the Single Sign-On Service instead
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
