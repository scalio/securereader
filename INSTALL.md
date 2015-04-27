## Dev Setup

Follow these steps to setup your dev environment:

1. Checkout securereader git repo
2. Init and update git submodules

    git submodule update --init --recursive

3. Fix support library mismatch

    All subprojects must use the same version of the support library.

    **Command Line**

        ./fix-support-library.sh

    **Manually**

    Copy `app/libs/android-support-v4.jar` to `external/supportlibrary/v7/appcompat/libs/android-support-v4.jar` and to `external/OnionKit/libnetcipher/libs/android-support-v4.jar`. 

4. Build Project

   **Using Eclipse**

    I recommend using a new workspace in Eclipse. I recommend using the root of
    this repo.
    
    Run *Android SDK Manager* from [ADT-Eclipse](http://developer.android.com/sdk/index.html) and make sure that you have SDK Platform Api Level 16 installed. If not then install those and restart the eclipse environment.

    Import into Eclipse (using the *File -> Import -> Android -> "Existing Android Code Into Workspace"* option) the
    following projects. Do not check "Copy projects into workspace".

        app/
        external/OnionKit/libnetcipher
        external/securereaderlibrary
        external/bho/TibetanTextLibrary
	external/supportlibrary/v7/appcompat

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
