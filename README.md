Snippr
******

Before deploying this project you need to create an empty database. Default database is postgreSQL.

Create database
---------------

<pre>
> CREATE USER snippr WITH PASSWORD 'snippr';
> CREATE DATABASE snipprdev;
> GRANT ALL PRIVILEGES ON DATABASE snipprdev TO snippr;
</pre>

Create default user to login
----------------------------

<pre>
> INSERT INTO users (id, user_name, password, enabled, account_non_expired, credentials_non_expired, account_non_locked) VALUES (1, 'test', 'test', TRUE, TRUE, TRUE, TRUE);
</pre>


Compile & deploy
----------------

<pre>
$ mvn clean install
$ mvn jetty:stop jetty:run
</pre>

Open website at localhost:8080
