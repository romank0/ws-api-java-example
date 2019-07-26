# ISIGN.io API Java example
------
Request access token [here](https://www.isign.io/contacts#request-access).

Enter API Access token at Main:34 line.

Enter phone number and personal code if required at line 95-96, or leave with testing details.

To execute, run:

```shell
mvn clean compile exec:java
```

Or run with additinal parameters specified:

1. `file-to-sign`
2. phone number to use
3. personal code (kenitalla)
4. dokobit host to use (helpful to examine the request sent)

For example this issues a request to the test number which always emulates user
cancellation of the request to sign:

```shell
mvn compile exec:java -Dexec.args="test.pdf +37061100266 50001018854"
```
