# AuthToken

A little Java library for handling authorization tokens.
It have functionality equivalent to [Google Authenticator App](https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2).

## How to build

```bash
make
```

## How to use

If you want to get currently valid key for given secret by authentication provider.
Secret phrases are stored by Google Authenticator in file /data/data/com.google.android.apps.authenticator2/databases/databases
```java
new AuthToken("secret phrase").getKey();
```

## How to run test suite

```bash
make test
```

## API

See the [Javadoc](https://tivian.github.io/auth-token/docs/).

## Deployment

* Add support for other hash algorithms used in OTP.
* Migrate project from make to Maven/Gradle

## Versioning

I use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/tivian/auth-token/tags).

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details