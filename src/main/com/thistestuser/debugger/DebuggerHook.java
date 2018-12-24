package com.thistestuser.debugger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;

public class DebuggerHook
{	
	/**
	 * The singleton instance of Debugger.
	 */
	public static Debugger debugger;
	public static boolean enableBreakpoints = true;
	private static ExecutorService executor = Executors.newSingleThreadExecutor();
	
	/**
	 * Add a class for analysis in Debugger. The class loaded is the previous method's owner.
	 * @param instance The instance of the class (can be null)
	 * @param loader The loader to load the class through (if null, uses current loader)
	 */
	public static void injectDebugger(Object instance, ClassLoader loader)
	{
		StackTraceElement[] stack = new Throwable().getStackTrace();
		Thread thread = new Thread()
		{
			@Override
			public void run()
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
					debugger.addClass(stack[1].getClassName(), instance, loader);
				}catch(Exception e)
				{
					Debugger.showErrorDialog("Exception while loading Debugger.", e);
				}
			}
		};
		executor.execute(thread);
	}

	/**
	 * Add a class for analysis in Debugger.
	 * @param instance The instance of the class (can be null)
	 * @param loader The loader to load the class through (if null, uses current loader)
	 */
	public static void injectDebugger(String clazzName, Object instance, ClassLoader loader)
	{
		Thread thread = new Thread()
		{
			@Override
			public void run()
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
		};
		executor.execute(thread);
	}
	
	/**
	 * Adds a breakpoint to the debugger. Breakpoints record information about the variables you choose, and
	 * also stop the execution if necessary. You can choose when to continue if the execution is stopped.
	 * @param clazzName The class name of the tab that the breakpoint will be associated with.
	 * @param instance The instance of the tab that the breakpoint will be associated with. If the tab doesn't exist, it will be created.
	 * @param loader The loader to load the class. Used only if the class isn't already loaded.
	 * @param id The unique ID of the breakpoint. Only 1 ID is allowed per class instance.
	 * @param input The variables to analyze. The results can be printed out with the "View" button.
	 * @param mode If the breakpoint should pause the execution of the program. 0 will depend on the pass checkbox, 
	 * 1 will always pass, and 2 will always pause.
	 * @param override If this is true and there is already a breakpoint with the same ID, 
	 * the breakpoint will automatically pass and be replaced with this one.
	 */
	public static void injectBreakpoint(String clazzName, Object instance, ClassLoader loader, 
		int id, Object[] input, int mode, boolean override)
	{
		AtomicBoolean isFinished = new AtomicBoolean();
		Thread thread = new Thread()
		{
			@Override
			public void run()
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
				isFinished.set(true);
			}
		};
		Future<?> future = executor.submit(thread);
		try
		{
			while(true)
			{
				if((isFinished.get() || future.isDone()) && debugger == null)
					throw new IllegalStateException("Debugger class could not load");
				if(debugger != null && debugger.isInstInit(clazzName, instance, loader))
					break;
			}
			debugger.injectBreakpoint(clazzName, instance, loader, id, input, mode, override);
		}catch(Exception e)
		{
			Debugger.showErrorDialog("Exception while adding breakpoint.", e);
		}
	}
}
