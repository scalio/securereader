## Dev Setup

Follow these steps to setup your dev environment:

1. Checkout securereader git repo
2. Init and update git submodules

    git submodule update --init --recursive

3. Fix support library mismatch

    ActionBarSherlock uses an outdated version of the support library. We must use the same version
    of the library in BigBuffalo and ABS.

    **Command Line**

        ./fix-support-library.sh
    Ignore the `rm: external/CacheWord/cachewordlib/libs/guava-r09.jar: No such file or directory` error

    **Manually**

    Copy `app/libs/android-support-v4.jar` to `external/HoloEverywhere/contrib/ActionBarSherlock/actionbarsherlock/libs/android-support-v4.jar` and to `external/CacheWord/cachewordlib/libs/android-support-v4.jar`

4. Build Project

   **Using Eclipse**

    I recommend using a new workspace in Eclipse. I recommend using the root of
    this repo.
    
    Run *Android SDK Manager* from [ADT-Eclipse](http://developer.android.com/sdk/index.html) and make sure that you have SDK Platform Api Level 16 installed. If not then install those and restart the eclipse environment.

    Import into Eclipse (using the *File -> Import -> Android -> "Existing Android Code Into Workspace"* option) the
    projects in the following order. Do not check "Copy projects into workspace".

    Note: The import order is crucial! (ps Order may not be crucial anymore)

        app/
        external/CacheWord/cachewordlib
        external/HoloEverywhere/contrib/ActionBarSherlock/actionbarsherlock
        external/HoloEverywhere/library
        external/OnionKit/libonionkit
        external/securereaderlibrary
        external/bho/TibetanTextLibrary

    When importing app/ double click on the value "MainActivity" and change it
    to "Secure Reader" under the New Project Name heading before finishing the
    import.


   **Using command line**

        ./setup-ant.sh
        cd app/
        ant clean debug

### Troubleshooting

**Eclipse complains about overlapping an existing project when importing**

1. Make sure the project isn't in your workspace, if it is delete it (right click -> delete)
2. Close eclipse completely
2. Open the directory you're importing and delete `.project`, `.settings/`, `.classpath`
3. Restart eclipse, and import the project as an existing Android project

(sometimes an additional open/restart cycle is required to clear Eclipse's project cache)

**Invalid Project Description**

This is another occurrence of the previous problem, see above.
