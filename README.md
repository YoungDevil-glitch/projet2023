# Project 2023

Cloud Computing project
## Project presentation 

## Interacting with the sample

After starting the sample with `sbt run` the following requests can be made:

List all users:

    curl http://localhost:8080/users

Create a user:

    curl -XPOST http://localhost:8080/users -d '{"name": "Liselott", "age": 32, "countryOfResidence": "Norway"}' -H "Content-Type:application/json"

Get the details of one user:

    curl http://localhost:8080/users/Liselott

Delete a user:

    curl -XDELETE http://localhost:8080/users/Liselott