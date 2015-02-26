package it.matsca09.xposedmod.directapkinstall;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class DirectInstallMod implements IXposedHookLoadPackage{
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if(!lpparam.packageName.equals("com.android.packageinstaller")){
			return;
		}
		
		
		XposedHelpers.findAndHookMethod("com.android.packageinstaller.PackageInstallerActivity", lpparam.classLoader, "onCreate", "android.os.Bundle", new XC_MethodHook() {
            @Override
			protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            	if(!(Boolean)XposedHelpers.callMethod(param.thisObject, "isInstallingUnknownAppsAllowed")){
            		AlertDialog.Builder builder = new AlertDialog.Builder((Context) param.thisObject);
            	    builder.setTitle("Package installation");
            	    //Alert taken from Android settings
            	    builder.setMessage("You are trying to install an apk from an unknown source.\nYour phone/tablet and personal data are more vulnerable to attack by apps from unknown sources. You agree that you are solely responsible for any damage to your phone or loss of data that may result from using these apps.\n\nDo you want to continue the installation?");
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
				 */
				int DLG_UNKNOWN_APPS = Build.VERSION.SDK_INT >= 17 ? 1 : 2;
				if((Integer)param.args[0] != DLG_UNKNOWN_APPS){
					XposedHelpers.callMethod(param.thisObject, "removeDialog", (Integer)param.args[0]);
					XposedHelpers.callMethod(param.thisObject, "showDialog", (Integer)param.args[0]);
				}
				return null;
			}
		});
	}
	

}
