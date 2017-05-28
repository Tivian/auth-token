# AuthToken

A little Java library for handling authorization tokens.
It have functionality equivalent to [Google Authenticator App](https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2).

## How to build

In order to compile java files execute:
'''bash
make
'''

## How to use

If you want to get currently valid key for given secret by authentication provider.
Secret phrases are stored by Google Authenticator in file /data/data/com.google.android.apps.authenticator2/databases/databases
'''java
new AuthToken("secret phrase").getKey();
'''

## API

See the [Javadoc](https://github.com/tivian/auth-token/doc/index.html).

## Deployment

* Add support for other hash algorithms used in OTP.
* Migrate project from make to Maven/Gradle

## Versioning

I use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/tivian/auth-token/tags).

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details