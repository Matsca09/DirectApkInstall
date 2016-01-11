/*
   Copyright 2016 Matsca09

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package it.matsca09.xposedmod.directapkinstall;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class DirectInstallMod implements IXposedHookLoadPackage{

	private String checkUnknownSourceMethod = ""; 
	private String packageInstallerID = "";

	// Default warning message
	private String instAlertTitle = "Package installation";
	private String instAlertBody = "You are trying to install an apk from an unknown source.\nYour phone/tablet and personal data are more vulnerable to attack by apps from unknown sources. You agree that you are solely responsible for any damage to your phone or loss of data that may result from using these apps.\n\nDo you want to continue the installation?";

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

		/*
		 * Yay! Google has changed the package installer package name in Marshmallow
		 */

		if(Build.VERSION.SDK_INT >= 23){
			packageInstallerID = "com.google.android.packageinstaller";
		}else{
			packageInstallerID = "com.android.packageinstaller";
		}

		if(!lpparam.packageName.equals(packageInstallerID)){
			return;
		}

		/*
		 * Search for the correct method name that checks if the unknown sources option is enabled
		 * "isUnknownSourcesEnabled" in Android 5.1.1 (API level 22), "isInstallingUnknownAppsAllowed" in older releases.
		 */

		if(Build.VERSION.SDK_INT >= 22){
			checkUnknownSourceMethod = "isUnknownSourcesEnabled";
		}else{
			checkUnknownSourceMethod = "isInstallingUnknownAppsAllowed";
		}

		XposedHelpers.findAndHookMethod("com.android.packageinstaller.PackageInstallerActivity", lpparam.classLoader, "onCreate", "android.os.Bundle", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(final MethodHookParam param) throws Throwable {

				// This should never happen.
				if((checkUnknownSourceMethod.isEmpty()) || (checkUnknownSourceMethod == null)){
					return;
				}

				/*
				 * Don't show the dialog if the device administrator has disabled the package installation for other accounts
				 * This is available at the moment only on Marshmallow
				 */

				if(Build.VERSION.SDK_INT >= 23){
					if(!(Boolean)XposedHelpers.callMethod(param.thisObject, "isUnknownSourcesAllowedByAdmin")){
						return;
					}
				}
				if(!(Boolean)XposedHelpers.callMethod(param.thisObject, checkUnknownSourceMethod)){
					AlertDialog.Builder builder = new AlertDialog.Builder((Context) param.thisObject);
					builder.setTitle(instAlertTitle);
					//Alert taken from Android settings
					builder.setMessage(instAlertBody);
					builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { 
							dialog.dismiss();
							XposedHelpers.callMethod(param.thisObject, "initiateInstall");
						}
					});
					builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { 
							dialog.dismiss();
							XposedHelpers.callMethod(param.thisObject, "finish");
						}
					});
					builder.show();
				}
			}

		});

		XposedHelpers.findAndHookMethod("com.android.packageinstaller.PackageInstallerActivity", lpparam.classLoader, "showDialogInner", Integer.TYPE, new XC_MethodReplacement() {

			@Override
			protected Object replaceHookedMethod(MethodHookParam param)	throws Throwable {

				/*
				 * Avoid showing "Install blocked" popup alert.
				 * DLG_UNKNOWN_APPS value is 1 on Android >= 4.3 (JELLY_BEAN_MR1), 2 in older releases.
				 * checkUnknownSourceMethod should never be empty or null.
				 */
				int DLG_UNKNOWN_APPS = Build.VERSION.SDK_INT >= 17 ? 1 : 2;
				if(((Integer)param.args[0] != DLG_UNKNOWN_APPS) || (checkUnknownSourceMethod.isEmpty()) || (checkUnknownSourceMethod == null)){
					XposedHelpers.callMethod(param.thisObject, "removeDialog", (Integer)param.args[0]);
					XposedHelpers.callMethod(param.thisObject, "showDialog", (Integer)param.args[0]);
				}

				return null;
			}
		});
	}


}
