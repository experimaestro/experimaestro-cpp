## Tests

To avoid SSH tests that require `docker` to be installed, use

```sh
./test/experimaestro-tests --gtest_filter="-SshTest.*"
```