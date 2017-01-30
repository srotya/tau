# About

This section shows how to run an instance of Tau using Docker. Install uses [Docker Compose](https://docs.docker.com/compose/)
to deploy all required components:

* Tau Nucleus
* Tau API
* MySQL
* Mail

# How to run?
1. Download docker [here](https://docs.docker.com/engine/installation/)
2. open Terminal or Bash
3. ```export MYSQL_ROOT_PASSWORD=root```
4. ```git clone https://github.com/srotya/tau.git```
5. ```cd tau/install/configs/docker```
6. ```docker-compose up```
