package com.thistestuser.debugger;

import javax.swing.*;

public class DebuggerHook
{	
	/**
	 * The singleton instance of Debugger.
	 */
	public static Debugger debugger;
	public static boolean enableBreakpoints = true;
	
	/**
	 * Add a class for analysis in Debugger. The class loaded is the previous method's owner.
	 * @param instance The instance of the class (can be null)
	 * @param loader The loader to load the class through (if null, uses current loader)
	 */
	public static void injectDebugger(Object instance, ClassLoader loader)
	{
		try
		{
			if(debugger == null)
			{
				try
				{
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				}catch(ClassNotFoundException | InstantiationException
					| IllegalAccessException | UnsupportedLookAndFeelException e)
				{
					e.printStackTrace();
				}
				debugger = new Debugger();
			}
			debugger.addClass((new Throwable()).getStackTrace()[1].getClassName(), instance, loader);
		}catch(Exception e)
		{
			Debugger.showErrorDialog("Exception while loading Debugger.", e);
		}
	}
	
	/**
	 * Add a class for analysis in Debugger.
	 * @param instance The instance of the class (can be null)
	 * @param loader The loader to load the class through (if null, uses current loader)
	 */
	public static void injectDebugger(String clazzName, Object instance, ClassLoader loader)
	{
		try
		{
			if(debugger == null)
			{
				try
				{
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				}catch(ClassNotFoundException | InstantiationException
					| IllegalAccessException | UnsupportedLookAndFeelException e)
				{
					e.printStackTrace();
				}
				debugger = new Debugger();
			}
			debugger.addClass(clazzName, instance, loader);
		}catch(Exception e)
		{
			Debugger.showErrorDialog("Exception while loading Debugger.", e);
		}
	}
}
