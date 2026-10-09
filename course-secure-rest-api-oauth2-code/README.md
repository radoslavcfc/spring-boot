# README

This repo supports the Securing a REST API with OAuth 2.0 course.

Use the Auth server commands:
1. start the app
2. run the auth docker `docker run --rm --name sso -p 9000:9000 ghcr.io/spring-academy/course-secure-rest-api-oauth2-code/sso:latest`
3. get the token `http -a cashcard-client:secret --form :9000/oauth2/token grant_type=client_credentials scope=cashcard:read`
4. decode the token `jwt decode <token>`
5. export the token `export REQUESTED_TOKEN=<token>`
6. get the data `http -A bearer -a $REQUESTED_TOKEN :8080/cashcards`
