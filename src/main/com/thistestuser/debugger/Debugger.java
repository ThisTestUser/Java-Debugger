package com.thistestuser.debugger;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.*;

@SuppressWarnings("serial")
public class Debugger extends JFrame
{
	private ImageIcon folder = new ImageIcon(Debugger.class.getResource("/assets/debugger/folder.png"));
	private ImageIcon defaultField = new ImageIcon(Debugger.class.getResource("/assets/debugger/field_default_obj.png"));
	private ImageIcon privateField = new ImageIcon(Debugger.class.getResource("/assets/debugger/field_private_obj.png"));
	private ImageIcon protectedField = new ImageIcon(Debugger.class.getResource("/assets/debugger/field_protected_obj.png"));
	private ImageIcon publicField = new ImageIcon(Debugger.class.getResource("/assets/debugger/field_public_obj.png"));
	private ImageIcon defaultMethod = new ImageIcon(Debugger.class.getResource("/assets/debugger/method_default_obj.png"));
	private ImageIcon privateMethod = new ImageIcon(Debugger.class.getResource("/assets/debugger/method_private_obj.png"));
	private ImageIcon protectedMethod = new ImageIcon(Debugger.class.getResource("/assets/debugger/method_protected_obj.png"));
	private ImageIcon publicMethod = new ImageIcon(Debugger.class.getResource("/assets/debugger/method_public_obj.png"));
	private ImageIcon finalCo = new ImageIcon(Debugger.class.getResource("/assets/debugger/final_co.png"));
	private ImageIcon staticCo = new ImageIcon(Debugger.class.getResource("/assets/debugger/static_co.png"));
	private ImageIcon transientCo = new ImageIcon(Debugger.class.getResource("/assets/debugger/transient_co.png"));
	private ImageIcon volatileCo = new ImageIcon(Debugger.class.getResource("/assets/debugger/volatile_co.png"));
	private ImageIcon abstractCo = new ImageIcon(Debugger.class.getResource("/assets/debugger/abstract_co.png"));
	private ImageIcon nativeCo = new ImageIcon(Debugger.class.getResource("/assets/debugger/native_co.png"));
	private ImageIcon synchronizedCo = new ImageIcon(Debugger.class.getResource("/assets/debugger/synchronized_co.png"));
	private ImageIcon defaultCo = new ImageIcon(Debugger.class.getResource("/assets/debugger/default_co.png"));
	private ImageIcon editable = new ImageIcon(Debugger.class.getResource("/assets/debugger/editable.png"));
	private ImageIcon runnable = new ImageIcon(Debugger.class.getResource("/assets/debugger/runnable.png"));
	private List<Instance> instances = new ArrayList<>();
	private JTabbedPane tabbedPane;
	private boolean showAllFields = false;
	private boolean showAllMethods = false;
	private static boolean isOnTop = false;
	private static final String VERSION = "2.0";
	
	public Debugger()
	{
		setTitle("Java Debugger");
		setBounds(100, 100, 1300, 900);
		setMinimumSize(new Dimension(750, 500));
		getContentPane().setLayout(new GridBagLayout());
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu fileMenu = new JMenu("File");
		JMenuItem forceEndItem = new JMenuItem("Force end");
		forceEndItem.setToolTipText("Closes the application and the debugger");
		forceEndItem.addActionListener(a -> System.exit(0));
		fileMenu.add(forceEndItem);
		menuBar.add(fileMenu);
		
		JMenu viewMenu = new JMenu("View");
		JCheckBoxMenuItem enableBreakpointsItem = new JCheckBoxMenuItem("Enable Breakpoints", DebuggerHook.enableBreakpoints);
		enableBreakpointsItem.setToolTipText("If false, new breakpoints will not be processed");
		enableBreakpointsItem.addActionListener(a -> DebuggerHook.enableBreakpoints = !DebuggerHook.enableBreakpoints);
		viewMenu.add(enableBreakpointsItem);
		viewMenu.addSeparator();
		JCheckBoxMenuItem onTopItem = new JCheckBoxMenuItem("Always on top");
		onTopItem.addActionListener(a -> {
			isOnTop = !isOnTop;
			setAlwaysOnTop(!isAlwaysOnTop());
		});
		viewMenu.add(onTopItem);
		menuBar.add(viewMenu);
		
		JMenu miscMenu = new JMenu("Misc");
		JCheckBoxMenuItem fieldsItem = new JCheckBoxMenuItem("Show all fields");
		fieldsItem.setToolTipText("If true, all dynamic fields will be loaded from superclasses");
		fieldsItem.addActionListener(a -> showAllFields = !showAllFields);
		miscMenu.add(fieldsItem);
		JCheckBoxMenuItem methodsItem = new JCheckBoxMenuItem("Show all methods");
		methodsItem.setToolTipText("If true, all dynamic methods will be loaded from superclasses");
		methodsItem.addActionListener(a -> showAllMethods = !showAllMethods);
		miscMenu.add(methodsItem);
		miscMenu.addSeparator();
		JMenuItem nullManagerItem = new JMenuItem("Null security manager");
		nullManagerItem.addActionListener(a -> {
			try
			{
				System.setSecurityManager(null);
			}catch(SecurityException e)
			{
				showErrorDialog("Could not null SecurityManager.", e);
			}
		});
		miscMenu.add(nullManagerItem);
		JMenuItem dumpClassItem = new JMenuItem("Dump Resource");
		dumpClassItem.addActionListener(a -> {
			JFrame newFrame = new JFrame();
			newFrame.setTitle("Dump Resource");
			newFrame.setBounds(100, 100, 500, 260);
			newFrame.setResizable(true);
			newFrame.getContentPane().setLayout(new GridBagLayout());
			
			JLabel label = new JLabel("<html>Dumps a resource file (first text box) to the specified location below (second box). "
				+ "This uses the current tab's classloader (or the system classloader if loader is null/no tabs are selected). "
				+ "This could be used to dump classes, but be sure to use slashes "
				+ "instead of dots and place a .class at the end. </html>");
			GridBagConstraints labelC = new GridBagConstraints();
			labelC.fill = GridBagConstraints.BOTH;
			labelC.insets = new Insets(15, 5, 5, 5);
			labelC.gridwidth = 2;
			newFrame.getContentPane().add(label, labelC);
			
			JTextField textField = new JTextField();
			GridBagConstraints textFieldC = new GridBagConstraints();
			textFieldC.insets = new Insets(2, 10, 5, 10);
			textFieldC.gridy = 1;
			textFieldC.gridwidth = 2;
			textFieldC.weightx = 1;
			textFieldC.fill = GridBagConstraints.BOTH;
			newFrame.getContentPane().add(textField, textFieldC);
			
			JTextField dumpLocField = new JTextField();
			GridBagConstraints dumpLocFieldC = new GridBagConstraints();
			dumpLocFieldC.insets = new Insets(5, 10, 0, 10);
			dumpLocFieldC.gridy = 2;
			dumpLocFieldC.weightx = 1;
			dumpLocFieldC.fill = GridBagConstraints.BOTH;
			newFrame.getContentPane().add(dumpLocField, dumpLocFieldC);
			JButton selectButton = new JButton("Select");
			selectButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					JFileChooser chooser = new JFileChooser();
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					int action = chooser.showOpenDialog(null);
					if(action == JFileChooser.APPROVE_OPTION)
					{
						String path = chooser.getSelectedFile().toString();
						dumpLocField.setText(path);
					}
				}
			});
			GridBagConstraints selectButtonC = new GridBagConstraints();
			selectButtonC.insets = new Insets(5, 0, 0, 10);
			selectButtonC.gridy = 2;
			selectButtonC.gridx = 1;
			newFrame.getContentPane().add(selectButton, selectButtonC);
			
			JCheckBox tabIsLoader = new JCheckBox("Tab Selected is ClassLoader");
			GridBagConstraints tabIsLoaderC = new GridBagConstraints();
			tabIsLoaderC.insets = new Insets(2, 10, 5, 10);
			tabIsLoaderC.gridy = 3;
			tabIsLoaderC.fill = GridBagConstraints.BOTH;
			newFrame.getContentPane().add(tabIsLoader, tabIsLoaderC);
			
			JButton submit = new JButton("Dump");
			GridBagConstraints submitC = new GridBagConstraints();
			submitC.insets = new Insets(0, 0, 10, 0);
			submitC.anchor = GridBagConstraints.SOUTH;
			submitC.gridy = 4;
			submitC.gridwidth = 2;
			submitC.weightx = 1;
			submitC.weighty = 1;
			newFrame.getContentPane().add(submit, submitC);
			submit.addActionListener(a2 -> {
				try
				{
					ClassLoader loader = null;
					if(tabbedPane.getSelectedIndex() > 0)
					{
						if(tabIsLoader.isSelected())
							loader = (ClassLoader)instances.get(tabbedPane.getSelectedIndex() - 1).instance;
						else
							loader = instances.get(tabbedPane.getSelectedIndex() - 1).clazz.getClassLoader();	
					}
					InputStream stream;
					if(loader == null)
						stream = getClass().getResourceAsStream(textField.getText());
					else
						stream = loader.getResourceAsStream(textField.getText());
					if(stream == null)
						throw new NullPointerException("Resource not found");
					File folder = new File(dumpLocField.getText());
					if(!folder.exists())
						throw new IllegalArgumentException("Dump directory does not exist");
					File dumpFile = new File(folder, textField.getText().replace('/', '.'));
					dumpFile.createNewFile();
					byte[] buffer = new byte[stream.available()];
					stream.read(buffer);
					FileOutputStream outStream = new FileOutputStream(dumpFile);
				    outStream.write(buffer);
				    outStream.close();
				}catch(Exception e)
				{
					showErrorDialog("Error dumping resource.", e);
				}
				newFrame.dispose();
			});
			
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Dimension screenSize = toolkit.getScreenSize();
			newFrame.setLocation((screenSize.width - newFrame.getWidth()) / 2, (screenSize.height - newFrame.getHeight()) / 2);
			newFrame.setAlwaysOnTop(isOnTop);
			newFrame.setVisible(true);
		});
		dumpClassItem.setToolTipText("Dump class using one of the selected ClassLoaders");
		miscMenu.add(dumpClassItem);
		miscMenu.addSeparator();
		JMenuItem loadClassItem = new JMenuItem("Load class");
		loadClassItem.addActionListener(a -> {
			JFrame newFrame = new JFrame();
			newFrame.setTitle("Load Class");
			newFrame.setBounds(100, 100, 500, 230);
			newFrame.setResizable(true);
			newFrame.getContentPane().setLayout(new GridBagLayout());
			
			JLabel label = new JLabel("<html>Input the class name (with dots) in the box below. This uses the current "
				+ "tab's classloader (or the system classloader if loader is null/no tabs are selected) to load the class here. "
				+ "Note that this does not allow to access instance fields/methods.</html>");
			GridBagConstraints labelC = new GridBagConstraints();
			labelC.fill = GridBagConstraints.BOTH;
			labelC.insets = new Insets(15, 5, 5, 5);
			newFrame.getContentPane().add(label, labelC);
			
			JTextField textField = new JTextField();
			GridBagConstraints textFieldC = new GridBagConstraints();
			textFieldC.insets = new Insets(2, 10, 5, 10);
			textFieldC.gridy = 1;
			textFieldC.weightx = 1;
			textFieldC.fill = GridBagConstraints.BOTH;
			newFrame.getContentPane().add(textField, textFieldC);
			
			JCheckBox tabIsLoader = new JCheckBox("Tab Selected is ClassLoader");
			GridBagConstraints tabIsLoaderC = new GridBagConstraints();
			tabIsLoaderC.insets = new Insets(2, 10, 5, 10);
			tabIsLoaderC.gridy = 2;
			tabIsLoaderC.fill = GridBagConstraints.BOTH;
			newFrame.getContentPane().add(tabIsLoader, tabIsLoaderC);
			
			JButton submit = new JButton("Submit");
			GridBagConstraints submitC = new GridBagConstraints();
			submitC.insets = new Insets(0, 0, 10, 0);
			submitC.anchor = GridBagConstraints.SOUTH;
			submitC.gridy = 3;
			submitC.weightx = 1;
			submitC.weighty = 1;
			newFrame.getContentPane().add(submit, submitC);
			submit.addActionListener(a2 -> {
				try
				{
					ClassLoader loader = null;
					if(tabbedPane.getSelectedIndex() > 0)
					{
						if(tabIsLoader.isSelected())
							loader = (ClassLoader)instances.get(tabbedPane.getSelectedIndex() - 1).instance;
						else
							loader = instances.get(tabbedPane.getSelectedIndex() - 1).clazz.getClassLoader();
					}
					DebuggerHook.injectDebugger(textField.getText(), null, loader);
				}catch(Exception e)
				{
					showErrorDialog("Error loading class.", e);
				}
				newFrame.dispose();
			});
			
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Dimension screenSize = toolkit.getScreenSize();
			newFrame.setLocation((screenSize.width - newFrame.getWidth()) / 2, (screenSize.height - newFrame.getHeight()) / 2);
			newFrame.setAlwaysOnTop(isOnTop);
			newFrame.setVisible(true);
		});
		loadClassItem.setToolTipText("Load class to Debugger. Only static fields will be editable");
		miscMenu.add(loadClassItem);
		menuBar.add(miscMenu);
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		GridBagConstraints tabbedPaneC = new GridBagConstraints();
		tabbedPaneC.anchor = GridBagConstraints.NORTHWEST;
		tabbedPaneC.fill = GridBagConstraints.BOTH;
		tabbedPaneC.weightx = 1.0;
		tabbedPaneC.weighty = 1.0;
		getContentPane().add(tabbedPane, tabbedPaneC);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent we)
			{
				int dialogResult = JOptionPane.showConfirmDialog(Debugger.this, "Do you want to close the main program?",
					"Debugger", JOptionPane.YES_NO_CANCEL_OPTION);
				if(dialogResult == JOptionPane.YES_OPTION)
					System.exit(0);
				else if(dialogResult == JOptionPane.NO_OPTION)
					dispose();
				else
					setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			}
		});
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBackground(Color.WHITE);
		
		JLabel debuggerLabel = new JLabel("Java Debugger " + VERSION + " By ThisTestUser");
		debuggerLabel.setFont(new Font("Tahoma", Font.ITALIC | Font.BOLD, 15));
		GridBagConstraints debuggerLabelC = new GridBagConstraints();
		debuggerLabelC.fill = GridBagConstraints.HORIZONTAL;
		debuggerLabelC.anchor = GridBagConstraints.NORTHWEST;
		debuggerLabelC.gridx = 0;
		debuggerLabelC.gridy = 0;
		debuggerLabelC.weightx = 1;
		debuggerLabelC.insets = new Insets(40, 35, 0, 0);
		panel.add(debuggerLabel, debuggerLabelC);
		JLabel text1 = new JLabel("Welcome to Java Debugger! You should be able to see tabs loading above based on your injection locations.");
		text1.setFont(new Font("Arial", Font.PLAIN, 15));
		GridBagConstraints text1C = new GridBagConstraints();
		text1C.fill = GridBagConstraints.HORIZONTAL;
		text1C.anchor = GridBagConstraints.NORTHWEST;
		text1C.gridx = 0;
		text1C.gridy = 1;
		text1C.weightx = 1;
		text1C.insets = new Insets(20, 35, 0, 0);
		panel.add(text1, text1C);
		JLabel text2 = new JLabel("To edit a field, press the wrench icon at the properties tab to edit it. An exclamation mark means that the field is null.");
		text2.setFont(new Font("Arial", Font.PLAIN, 15));
		GridBagConstraints text2C = new GridBagConstraints();
		text2C.fill = GridBagConstraints.HORIZONTAL;
		text2C.anchor = GridBagConstraints.NORTHWEST;
		text2C.gridx = 0;
		text2C.gridy = 2;
		text2C.weightx = 1;
		text2C.insets = new Insets(0, 35, 0, 0);
		panel.add(text2, text2C);
		JLabel text3 = new JLabel("To run methods, press the methods tab and double click the method. "
				+ "Note that only methods with primitive arguments are executable. Also note that the stacktrace will be visible.");
		text3.setFont(new Font("Arial", Font.PLAIN, 15));
		GridBagConstraints text3C = new GridBagConstraints();
		text3C.fill = GridBagConstraints.HORIZONTAL;
		text3C.anchor = GridBagConstraints.NORTHWEST;
		text3C.gridx = 0;
		text3C.gridy = 3;
		text3C.weightx = 1;
		text3C.insets = new Insets(0, 35, 0, 0);
		panel.add(text3, text3C);
		JLabel text4 = new JLabel("For additional information, go the the repository link below.");
		text4.setFont(new Font("Arial", Font.PLAIN, 15));
		GridBagConstraints text4C = new GridBagConstraints();
		text4C.fill = GridBagConstraints.HORIZONTAL;
		text4C.anchor = GridBagConstraints.NORTHWEST;
		text4C.gridx = 0;
		text4C.gridy = 4;
		text4C.weightx = 1;
		text4C.insets = new Insets(0, 35, 0, 0);
		panel.add(text4, text4C);
		JLabel text5 = new JLabel("https://github.com/ThisTestUser/Java-Debugger");
		Map<TextAttribute, Integer> fontAttributes = new HashMap<>();
		fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		text5.setFont(new Font("Arial", Font.PLAIN, 15).deriveFont(fontAttributes));
		text5.addMouseListener(new MouseAdapter()
		{
			@Override
	        public void mouseClicked(MouseEvent e) 
			{
				try 
				{
		            Desktop.getDesktop().browse(new URI("https://github.com/ThisTestUser/Java-Debugger"));
		        }catch(Exception ex) 
				{
		            ex.printStackTrace();
		        }
	        }

	        @Override
	        public void mouseEntered(MouseEvent e) 
	        {
	        	text5.setForeground(Color.GRAY);
	        }

	        @Override
	        public void mouseExited(MouseEvent e) 
	        {
	        	text5.setForeground(Color.BLACK);
	        }
		});
		GridBagConstraints text5C = new GridBagConstraints();
		text5C.anchor = GridBagConstraints.NORTHWEST;
		text5C.gridx = 0;
		text5C.gridy = 5;
		text5C.weightx = 1;
		text5C.weighty = 1;
		text5C.insets = new Insets(20, 35, 0, 0);
		panel.add(text5, text5C);
		
		tabbedPane.addTab("Home", panel);
		tabbedPane.setTabComponentAt(0, getTabComponent("Home", tabbedPane, null, false));
		getContentPane().setBackground(new Color(105, 105, 105));
		setVisible(true);
	}
	
	public boolean isInstInit(String clazz, Object instance, ClassLoader loader) throws Exception
	{
		//Check duplicates
		Instance inst = null;
		Class<?> cl;
		if(loader != null)
			cl = Class.forName(clazz, true, loader);
		else
			cl = Class.forName(clazz);
		List<Instance> instancesCopy = new ArrayList<>(instances);
		for(Instance in : instancesCopy)
			if(in != null && in.clazz == cl && in.instance == instance)
			{
				inst = in;
				break;
			}
		if(inst == null)
			return false;
		return inst.init;
	}
	
	public void addClass(String clazz, Object instance, ClassLoader loader) throws Exception
	{
		//Check duplicates
		Class<?> cl;
		if(loader != null)
			cl = Class.forName(clazz, true, loader);
		else
			cl = Class.forName(clazz);
		for(Instance inst : instances)
			if(inst.clazz == cl && inst.instance == instance)
				return;
		Instance inst = new Instance(cl, instance);
		instances.add(inst);
		JPanel panel = inst.setupJPanel();
		tabbedPane.addTab(clazz, panel);
		tabbedPane.setTabComponentAt(tabbedPane.indexOfComponent(panel), getTabComponent(clazz, tabbedPane, panel, true));
	}
	
	public void injectBreakpoint(String clazz, Object instance, ClassLoader loader, 
		int id, Object[] input, int mode, boolean override) throws Exception
	{
		Class<?> cl;
		if(loader != null)
			cl = Class.forName(clazz, true, loader);
		else
			cl = Class.forName(clazz);
		Instance inst = null;
		for(Instance in : instances)
			if(in.clazz == cl && in.instance == instance)
				inst = in;
		if(inst == null)
			inst = new Instance(cl, instance);
		if(!instances.contains(inst))
		{
			instances.add(inst);
			JPanel panel = inst.setupJPanel();
			tabbedPane.addTab(clazz, panel);
			tabbedPane.setTabComponentAt(tabbedPane.indexOfComponent(panel), getTabComponent(clazz, tabbedPane, panel, true));
		}
		DefaultTableModel model = (DefaultTableModel)inst.breakpointsTable.getModel();
		int index = -1;
		for(int i = 0; i < model.getRowCount(); i++)
			if((int)model.getValueAt(i, 0) == id)
			{
				index = i;
				break;
			}
		if(index != -1 && !override)
			return;
		AtomicBoolean bool = new AtomicBoolean((mode == 0 && inst.passBox.isSelected()) || mode == 1);
		if(index != -1)
		{
			long prevTime = (long)model.getValueAt(index, 1);
			((AtomicBoolean)model.getValueAt(index, 4)).set(true);
			model.removeRow(index);
			model.insertRow(index, new Object[] {id, System.currentTimeMillis(), System.currentTimeMillis() - prevTime,
				mode, bool, new Data(new Object[] {input, "View"})});
		}else
			model.addRow(new Object[] {id, System.currentTimeMillis(), 0L,
				mode, bool, new Data(new Object[] {input, "View"})});
		while(!bool.get())
		{
			Thread.sleep(100);
		}
	}
	
	public static void showErrorDialog(String message, Exception e)
	{
		JFrame newFrame = new JFrame();
		newFrame.setTitle("Error");
		newFrame.setBounds(100, 100, 500, 400);
		newFrame.setResizable(true);
		newFrame.getContentPane().setLayout(new GridBagLayout());
		
		JLabel yourConfiguration = new JLabel(message);
		GridBagConstraints gbc_yourConfiguration = new GridBagConstraints();
		gbc_yourConfiguration.anchor = GridBagConstraints.PAGE_START;
		gbc_yourConfiguration.insets = new Insets(15, 5, 5, 5);
		gbc_yourConfiguration.gridx = 0;
		gbc_yourConfiguration.gridy = 0;
		newFrame.getContentPane().add(yourConfiguration, gbc_yourConfiguration);
		
		JScrollPane scrollPane = new JScrollPane();
		JTextPane textPane = new JTextPane();
		textPane.setEditable(false);
		scrollPane.setViewportView(textPane);
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.insets = new Insets(2, 10, 5, 10);
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 1;
		gbc_scrollPane.weightx = 1;
		gbc_scrollPane.weighty = 1;
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		newFrame.getContentPane().add(scrollPane, gbc_scrollPane);
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		e.printStackTrace(writer);
		textPane.setText(stringWriter.toString());
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		newFrame.setLocation((screenSize.width - newFrame.getWidth()) / 2, (screenSize.height - newFrame.getHeight()) / 2);
		newFrame.setAlwaysOnTop(isOnTop);
		newFrame.setVisible(true);
	}
	
	private JPanel getTabComponent(String text, JTabbedPane pane, JPanel panel, boolean withButton)
	{
		JPanel component = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		if(withButton)
			component.setToolTipText(text);
		component.setOpaque(false);
		component.addMouseListener(new MouseAdapter()
		{
			@Override
		    public void mousePressed(MouseEvent e)
		    {
				tabbedPane.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, tabbedPane));
		    }
			
			@Override
			public void mouseEntered(MouseEvent e)
			{
				tabbedPane.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, tabbedPane));
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				tabbedPane.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, tabbedPane));
			}
		});
		JLabel label = new JLabel(text);
		label.setFont(new Font("Tahoma", Font.BOLD, 12));
		component.add(label);
		if(withButton)
		{
			label.setPreferredSize(new Dimension(200, label.getPreferredSize().height));
			JButton button = new JButton();
			button.setText("\u2716");
			button.setForeground(Color.GRAY);
			button.setContentAreaFilled(false);
			button.setFocusable(false);
			button.setBorder(new EmptyBorder(0, 0, 0, 0));
			button.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					pane.remove(panel);
					for(Instance inst : instances)
						if(inst.panel == panel)
						{
							instances.remove(inst);
							break;
						}
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
		            button.setForeground(Color.RED);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					button.setForeground(Color.GRAY);
				}
			});
			component.add(button);
		}
		return component;
	}
	
	public String getArrayName(Class<?> clazz)
	{
		if(clazz.isArray())
			return getArrayName(clazz.getComponentType()) + "[]";
		return clazz.getName();
	}
	
	public Class<?> getRealComponentType(Class<?> clazz)
	{
		if(clazz.isArray())
			return getRealComponentType(clazz.getComponentType());
		return clazz;
	}
	
	private class Instance
	{
		public final Class<?> clazz;
		public final Object instance;
		private JTree tree;
		private List<TreePath> expanded = new ArrayList<>();
		private DefaultMutableTreeNode top;
		public JPanel panel;
		private JPanel mainPanel;
		private JPanel propertiesPanel;
		private JPanel breakpointPanel;
		private MyTable fieldsTable;
		private MyTable methodsTable;
		private MyTable breakpointsTable;
		private JCheckBox passBox;
		private TreePath specificSearch = null;
		private Class<?> tableClazz;
		private Object tableValue;
		public boolean init;
		
		public Instance(Class<?> clazz, Object instance)
		{
			this.clazz = clazz;
			this.instance = instance;
		}
		
		public JPanel setupJPanel()
		{
			panel = new JPanel(new GridBagLayout());
			
			mainPanel = new JPanel(new BorderLayout());
			mainPanel.setPreferredSize(mainPanel.getPreferredSize());
			JLabel clazzLabel = new JLabel("  Class");
			clazzLabel.setFont(new Font("Tahoma", Font.BOLD, 15));
			clazzLabel.setPreferredSize(new Dimension(clazzLabel.getPreferredSize().width, clazzLabel.getPreferredSize().height + 6));
			clazzLabel.setOpaque(true);
			clazzLabel.setBackground(new Color(105, 105, 105));
			clazzLabel.setForeground(Color.WHITE);
			mainPanel.add(clazzLabel, BorderLayout.NORTH);
			top = new HideableTreeNode(clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1), true);
			tree = new JTree(top, true)
			{
				@Override
				public void paintComponent(Graphics g) 
				{
					g.setColor(getBackground());
					g.fillRect(0, 0, getWidth(), getHeight());
					if(getSelectionCount() > 0)
					{
						for(int i : getSelectionRows())
						{
							Rectangle r = getRowBounds(i);
							g.setColor(UIManager.getColor("List.selectionBackground"));
							g.fillRect(0, r.y, getWidth(), r.height);
						}
					}
					if(specificSearch != null && getRowForPath(specificSearch) != getLeadSelectionRow())
					{
						Rectangle r = getRowBounds(getRowForPath(specificSearch));
						g.setColor(Color.GREEN.brighter());
						g.fillRect(0, r.y, getWidth(), r.height);
					}
					super.paintComponent(g);
				}
			};
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			tree.setRowHeight(30);
			tree.setOpaque(false);
			tree.setFont(tree.getFont().deriveFont(15));
			tree.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
			tree.setShowsRootHandles(true);
			tree.setToggleClickCount(0);
			tree.setCellRenderer(new DefaultTreeCellRenderer()
			{
				private JLabel label = new JLabel();
				
				@Override
				public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
					boolean leaf, int row, boolean hasFocus)
				{
					label.setOpaque(true);
					if(value instanceof FieldTreeNode)
					{
						ImageIcon icon = null;
						Field field = ((FieldTreeNode)value).field;
						if(field != null)
						{
							if(Modifier.isPublic(field.getModifiers()))
								icon = publicField;
							else if (Modifier.isPrivate(field.getModifiers()))
								icon = privateField;
							else if (Modifier.isProtected(field.getModifiers()))
								icon = protectedField;
							else
								icon = defaultField;
							if(Modifier.isTransient(field.getModifiers()))
								icon = combineAccess(icon, transientCo, 2);
							boolean placedFinal = false;
							boolean placedVolatile = false;
							boolean placedStatic = false;
							if(Modifier.isFinal(field.getModifiers()))
							{
								icon = combineAccess(icon, finalCo, 0);
								placedFinal = true;
							}else if(Modifier.isVolatile(field.getModifiers()))
							{
								icon = combineAccess(icon, volatileCo, 0);
								placedVolatile = true;
							}else if(Modifier.isStatic(field.getModifiers()))
							{
								icon = combineAccess(icon, staticCo, 0);
								placedStatic = true;
							}
							if(Modifier.isFinal(field.getModifiers()) && !placedFinal)
								icon = combineAccess(icon, finalCo, 1);
							else if(Modifier.isVolatile(field.getModifiers()) && !placedVolatile)
								icon = combineAccess(icon, volatileCo, 1);
							else if(Modifier.isStatic(field.getModifiers()) && !placedStatic)
								icon = combineAccess(icon, staticCo, 1);
						}else
							icon = publicField;
						label.setIcon(icon);
					}else
						label.setIcon(folder);
					label.setText((String)((DefaultMutableTreeNode)value).getUserObject());
					if(selected)
						label.setBackground(backgroundSelectionColor);
					else if(specificSearch != null && specificSearch.getLastPathComponent() == value)
						label.setBackground(Color.GREEN.brighter());
					else
						label.setBackground(backgroundNonSelectionColor);
					return label;
				}
			});
			tree.setUI(new BasicTreeUI()
			{
				@Override 
				public Rectangle getPathBounds(JTree tree, TreePath path)
				{
					if(tree != null && treeState != null)
					{
						Rectangle bounds = new Rectangle();
						bounds = treeState.getBounds(path, bounds);
						if(bounds != null)
						{
							bounds.width = tree.getWidth();
							bounds.y += tree.getInsets().top;
						}
						return bounds;
					}
					return null;
				}
			});
			JScrollPane scrollPane = new JScrollPane(tree);
			scrollPane.setBorder(BorderFactory.createEmptyBorder());
			scrollPane.getViewport().setBackground(Color.WHITE);
			mainPanel.add(scrollPane, BorderLayout.CENTER);
			JPanel bottomClazzPanel = new JPanel(new GridBagLayout());
			JLabel filterP = new JLabel("   filter: ");
			filterP.setMinimumSize(filterP.getPreferredSize());
			bottomClazzPanel.add(filterP, new GridBagConstraints());
			JTextField textField = new JTextField(20);
			textField.setMinimumSize(textField.getPreferredSize());
			GridBagConstraints textFieldC = new GridBagConstraints();
			textFieldC.gridx = 1;
			bottomClazzPanel.add(textField, textFieldC);
			JButton clearSpecificSearch = new JButton("Clear Specific Search");
			clearSpecificSearch.setEnabled(false);
			clearSpecificSearch.addActionListener(a -> {
				specificSearch = null;
				clearSpecificSearch.setEnabled(false);
				List<TreePath> expandedCopy = new ArrayList<>(expanded);
				updateFilter(specificSearch != null ? (HideableTreeNode)specificSearch.getLastPathComponent() : 
					(HideableTreeNode)tree.getModel().getRoot(), textField.getText(), true);
				for(TreePath path : expandedCopy)
					tree.expandPath(path);
			});
			GridBagConstraints clearSpecificSearchC = new GridBagConstraints();
			clearSpecificSearchC.anchor = GridBagConstraints.WEST;
			clearSpecificSearchC.gridx = 2;
			bottomClazzPanel.add(clearSpecificSearch, clearSpecificSearchC);
			textField.getDocument().addDocumentListener(new DocumentListener()
			{
				@Override
				public void insertUpdate(DocumentEvent e)
				{
					List<TreePath> expandedCopy = new ArrayList<>(expanded);
					updateFilter(specificSearch != null ? (HideableTreeNode)specificSearch.getLastPathComponent() : 
						(HideableTreeNode)tree.getModel().getRoot(), textField.getText(), true);
					for(TreePath path : expandedCopy)
						tree.expandPath(path);
				}

				@Override
				public void removeUpdate(DocumentEvent e)
				{
					List<TreePath> expandedCopy = new ArrayList<>(expanded);
					updateFilter(specificSearch != null ? (HideableTreeNode)specificSearch.getLastPathComponent() : 
						(HideableTreeNode)tree.getModel().getRoot(), textField.getText(), true);
					for(TreePath path : expandedCopy)
						tree.expandPath(path);
				}

				@Override
				public void changedUpdate(DocumentEvent e)
				{
					List<TreePath> expandedCopy = new ArrayList<>(expanded);
					updateFilter(specificSearch != null ? (HideableTreeNode)specificSearch.getLastPathComponent() : 
						(HideableTreeNode)tree.getModel().getRoot(), textField.getText(), true);
					for(TreePath path : expandedCopy)
						tree.expandPath(path);
				}
			});
			JButton button = new JButton("Refresh");
			button.addActionListener(a -> {
				refreshTree(textField.getText());
				clearSpecificSearch.setEnabled(false);
			});
			GridBagConstraints buttonC = new GridBagConstraints();
			buttonC.anchor = GridBagConstraints.EAST;
			buttonC.weightx = 1;
			buttonC.gridx = 3;
			buttonC.insets = new Insets(0, 0, 0, 10);
			bottomClazzPanel.add(button, buttonC);
			tree.addTreeWillExpandListener(new TreeWillExpandListener() 
			{
	            @Override
	            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException 
	            {
	                TreePath path = event.getPath();
	                expanded.add(path);
	                if(path.getLastPathComponent() instanceof FieldTreeNode)
	                {
	                    FieldTreeNode node = (FieldTreeNode)path.getLastPathComponent();
	                    node.loadChildren(textField.getText());
	                }
	            }

	            @Override
	            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException 
	            {
	            	expanded.remove(event.getPath());
	            }
	        });
			tree.addMouseListener(new MouseAdapter()
		    {
				@Override
		        public void mousePressed(MouseEvent e)
		        {
		            if(SwingUtilities.isRightMouseButton(e))
		            {
		                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
		                Rectangle pathBounds = tree.getUI().getPathBounds(tree, path);
		                if(pathBounds != null && pathBounds.contains(e.getX(), e.getY()))
		                {
		                    JPopupMenu menu = new JPopupMenu();
		                    JMenuItem onlySearch = new JMenuItem("Only Search In Here");
		                    menu.add(onlySearch);
		                    onlySearch.addActionListener(a -> {
		                    	specificSearch = path;
								clearSpecificSearch.setEnabled(specificSearch != null);
								List<TreePath> expandedCopy = new ArrayList<>(expanded);
								updateFilter(specificSearch != null ? (HideableTreeNode)specificSearch.getLastPathComponent() : 
									(HideableTreeNode)tree.getModel().getRoot(), textField.getText(), true);
								for(TreePath pth : expandedCopy)
									tree.expandPath(pth);
		                    });
		                    JMenuItem loadAsTab = new JMenuItem("Load As Tab");
			            	menu.add(loadAsTab);
			            	loadAsTab.addActionListener(a -> {
			            		try
								{
									if(path.getLastPathComponent() instanceof FieldTreeNode)
									{
										FieldTreeNode treeNode = (FieldTreeNode)path.getLastPathComponent();
										if(treeNode.value != null && !treeNode.value.getClass().isArray())
										{
											boolean accessible = false;
											int modifier = 0;
											accessible = treeNode.field.isAccessible();
											modifier = treeNode.field.getModifiers();
											Field modifiersField = Field.class.getDeclaredField("modifiers");
											treeNode.field.setAccessible(true);
											modifiersField.setAccessible(true);
											modifiersField.setInt(treeNode.field,
												treeNode.field.getModifiers() & ~Modifier.FINAL);
											addClass(treeNode.value.getClass().getName(), treeNode.value,
												treeNode.value.getClass().getClassLoader());
											treeNode.field.setAccessible(accessible);
						    				modifiersField.setInt(treeNode.field, modifier);
						    				modifiersField.setAccessible(false);
										}
									}
								}catch(Exception ex)
								{
									showErrorDialog("Could not load result.", ex);
								}
			            	});
		                    menu.show(tree, e.getX(), e.getY());
		                }
		            }
		        }
		    });
			tree.addTreeSelectionListener(new TreeSelectionListener()
			{
				@Override
			    public void valueChanged(TreeSelectionEvent e) 
				{
					TreePath path = e.getNewLeadSelectionPath();
					if(path == null)
						return;
					if(!(path.getLastPathComponent() instanceof FieldTreeNode))
					{
						tableClazz = clazz;
						tableValue = instance;
						reloadFields(tableClazz, tableClazz);
						reloadMethods(tableClazz, tableClazz);
					}else
					{
						FieldTreeNode node = (FieldTreeNode)path.getLastPathComponent();
						tableClazz = node.field != null ? node.field.getType() : node.type;
						tableValue = node.value;
						reloadFields(tableClazz, node.owner);
						reloadMethods(tableClazz, node.owner);
					}
				}
			});
			bottomClazzPanel.setMinimumSize(new Dimension(0, 0));
			mainPanel.add(bottomClazzPanel, BorderLayout.SOUTH);
			mainPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
			
			propertiesPanel = new JPanel(new GridBagLayout());
			propertiesPanel.setPreferredSize(propertiesPanel.getPreferredSize());
			JTabbedPane fieldMethodsTab = new JTabbedPane();
			fieldMethodsTab.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
			JPanel jMethod = new JPanel(new BorderLayout());
			methodsTable = new MyTable(0, 3, 0);
			methodsTable.setAutoCreateRowSorter(true);
			methodsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			methodsTable.addMouseListener(new MouseAdapter()
			{
				private Object result;
				
				@Override
			    public void mousePressed(MouseEvent mouseEvent)
				{
			        JTable table = (JTable)mouseEvent.getSource();
			        if(mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1)
			        {
			        	JFrame newFrame = new JFrame();
			    		newFrame.setTitle("Java Debugger - Method");
			    		newFrame.setBounds(100, 100, 800, 500);
			    		newFrame.setMinimumSize(new Dimension(400, 300));
			    		newFrame.setResizable(true);
			    		newFrame.getContentPane().setLayout(new GridBagLayout());
			    		
			    		JPanel leftPanel = new JPanel(new GridBagLayout());
			    		
			    		Data data = (Data)table.getValueAt(table.getSelectedRow(), 2);
			    		Method method = (Method)data.data[1];
			    		
			    		GridBagConstraints ownerLabelC = new GridBagConstraints();
			    		ownerLabelC.insets = new Insets(10, 10, 0, 0);
			    		ownerLabelC.anchor = GridBagConstraints.NORTHWEST;
			    		ownerLabelC.fill = GridBagConstraints.HORIZONTAL;
			    		ownerLabelC.weightx = 1;
			    		leftPanel.add(new JLabel("Owner: " + method.getDeclaringClass().getName()), ownerLabelC);
			    		GridBagConstraints accessModC = new GridBagConstraints();
			    		accessModC.insets = new Insets(10, 10, 0, 0);
			    		accessModC.anchor = GridBagConstraints.NORTHWEST;
			    		accessModC.fill = GridBagConstraints.HORIZONTAL;
			    		accessModC.weightx = 1;
			    		accessModC.gridy = 1;
			    		String methodString = "<b><font color=7f0055>" + Modifier.toString(method.getModifiers()) + "</font></b>";
			    		if(method.isDefault())
			    			methodString += "<b><font color=7f0055> default</font></b>";
			    		if(method.isVarArgs())
			    			methodString += " varargs";
			    		if(method.isSynthetic())
			    			methodString += " synthetic";
			    		if(method.isBridge())
			    			methodString += " bridge";
			    		if(method.getReturnType().isPrimitive())
			    			methodString += " <b><font color=7f0055>" + getArrayName(method.getReturnType()) + "</font></b>";
			    		methodString += " " + method.getName() + "(";
			    		int argIndex = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
			    		for(Class<?> clazz : method.getParameterTypes())
			    			methodString += (clazz.isPrimitive() ? "<b><font color=7f0055>" : "") + 
			    			getArrayName(clazz) + (clazz.isPrimitive() ? "</font></b>" : "") + 
			    			" <font color=6a3e3e>var" + argIndex++ + "</font>, ";
			    		if(method.getParameterTypes().length > 0)
			    			methodString = methodString.substring(0, methodString.length() - 2);
			    		methodString += ")";
			    		leftPanel.add(new JLabel("<html>" + methodString + "</html>"), accessModC);
			    		JPanel panel = new JPanel(new GridBagLayout());
			    		GridBagConstraints panelC = new GridBagConstraints();
			    		panelC.insets = new Insets(10, 10, 0, 0);
			    		panelC.anchor = GridBagConstraints.NORTHWEST;
			    		panelC.fill = GridBagConstraints.BOTH;
			    		panelC.gridy = 2;
			    		panelC.weightx = 1;
			    		leftPanel.add(panel, panelC);
			    		
			    		int gridY = 0;
			    		for(Class<?> clazz : method.getParameterTypes())
			    		{
			    			if(clazz == boolean.class || Enum.class.isAssignableFrom(clazz))
				    		{
			    				JPanel innerPnl = new JPanel();
			    				innerPnl.setLayout(new BoxLayout(innerPnl, BoxLayout.X_AXIS));
				    			innerPnl.add(new JLabel(clazz == boolean.class ? "Boolean:   "  : "Enum:   "));
				    			JComboBox<String> comboBox = new JComboBox<>();
				    			List<Field> enumFields = new ArrayList<>();
				    			if(clazz == boolean.class)
				    			{
				    				comboBox.addItem("true");
				    				comboBox.addItem("false");
				    			}else
				    			{
				    				for(Field f : clazz.getDeclaredFields())
				    					if(f.getType() == clazz && Modifier.isStatic(f.getModifiers()))
				    						enumFields.add(f);
				    				for(Field f : enumFields)
				    					comboBox.addItem(f.getName());
				    			}
					    		innerPnl.add(comboBox);
					    		JCheckBox nullBox = new JCheckBox("Null");
					    		nullBox.addActionListener(a -> {
					    			if(nullBox.isSelected())
										comboBox.setEnabled(false);
									else
										comboBox.setEnabled(true);
					    		});
					    		if(clazz == boolean.class)
					    			nullBox.setEnabled(false);
			    				innerPnl.add(nullBox);
			    				GridBagConstraints innerPnlC = new GridBagConstraints();
					    		innerPnlC.anchor = GridBagConstraints.NORTHWEST;
			    				innerPnlC.fill = GridBagConstraints.HORIZONTAL;
			    				innerPnlC.gridy = gridY++;
			    				innerPnlC.weightx = 1;
			    				panel.add(innerPnl, innerPnlC);
				    		}else if(clazz.isArray())
				    		{
				    			JPanel innerPnl = new JPanel();
				    			innerPnl.setLayout(new BoxLayout(innerPnl, BoxLayout.X_AXIS));
				    			JLabel text = new JLabel("Value:   ");
				    			innerPnl.add(text);
				    			ListButton arrButton = new ListButton("Edit Array", Array.newInstance(clazz.getComponentType(),
				    				0), clazz.getComponentType(), panel);
					    		innerPnl.add(arrButton);
					    		JCheckBox nullBox = new JCheckBox("Null");
					    		nullBox.addActionListener(a -> {
					    			if(nullBox.isSelected())
										arrButton.setEnabled(false);
									else
										arrButton.setEnabled(true);
					    		});
					    		innerPnl.add(nullBox);
					    		GridBagConstraints innerPnlC = new GridBagConstraints();
					    		innerPnlC.anchor = GridBagConstraints.NORTHWEST;
			    				innerPnlC.fill = GridBagConstraints.HORIZONTAL;
			    				innerPnlC.gridy = gridY++;
			    				innerPnlC.weightx = 1;
			    				panel.add(innerPnl, innerPnlC);
				    		}else
				    		{
				    			JPanel innerPnl = new JPanel();
				    			innerPnl.setLayout(new BoxLayout(innerPnl, BoxLayout.X_AXIS));
				    			JLabel text = new JLabel("Value:   ");
				    			innerPnl.add(text);
				    			JTextField textField = new JTextField(20);
					    		innerPnl.add(textField);
					    		JCheckBox nullBox = new JCheckBox("Null");
					    		nullBox.addActionListener(a -> {
					    			if(nullBox.isSelected())
										textField.setEnabled(false);
									else
										textField.setEnabled(true);
					    		});
					    		if(clazz.isPrimitive())
					    			nullBox.setEnabled(false);
			    				innerPnl.add(nullBox);
					    		GridBagConstraints innerPnlC = new GridBagConstraints();
					    		innerPnlC.anchor = GridBagConstraints.NORTHWEST;
			    				innerPnlC.fill = GridBagConstraints.HORIZONTAL;
			    				innerPnlC.gridy = gridY++;
			    				innerPnlC.weightx = 1;
			    				panel.add(innerPnl, innerPnlC);
				    		}
			    		}
			    		
			    		JButton run = new JButton("Run");
			    		if(table.getValueAt(table.getSelectedRow(), 0) == null)
			    		{
			    			run.setEnabled(false);
			    			for(Component comp : panel.getComponents())
			    				for(Component comp2 : ((JPanel)comp).getComponents())
			    					comp2.setEnabled(false);
			    		}
			    		GridBagConstraints runC = new GridBagConstraints();
			    		runC.insets = new Insets(0, 0, 20, 0);
			    		runC.anchor = GridBagConstraints.SOUTH;
			    		runC.weighty = 1;
			    		runC.gridy = 3;
			    		leftPanel.add(run, runC);
			    		
			    		GridBagConstraints leftPanelC = new GridBagConstraints();
			    		leftPanelC.anchor = GridBagConstraints.NORTHWEST;
			    		leftPanelC.fill = GridBagConstraints.BOTH;
			    		leftPanelC.weighty = 1;
			    		newFrame.add(leftPanel, leftPanelC);
			    		
			    		HideableTreeNode root = new HideableTreeNode("Returns: " + getArrayName(method.getReturnType()), true);
			    		JTree tree = new JTree(root, true)
						{
							@Override
							public void paintComponent(Graphics g) 
							{
								g.setColor(getBackground());
								g.fillRect(0, 0, getWidth(), getHeight());
								if(getSelectionCount() > 0)
								{
									for(int i : getSelectionRows())
									{
										Rectangle r = getRowBounds(i);
										g.setColor(UIManager.getColor("List.selectionBackground"));
										g.fillRect(0, r.y, getWidth(), r.height);
									}
								}
								super.paintComponent(g);
							}
						};
			    		run.addActionListener(a -> {
			    			try
							{
								boolean access = method.isAccessible();
								method.setAccessible(true);
								List<Object> args = new ArrayList<>();
								for(int i = 0; i < panel.getComponents().length; i++)
								{
									JComponent comp = (JComponent)panel.getComponents()[i];
									JCheckBox box = (JCheckBox)((JPanel)comp).getComponent(2);
									if(box.isSelected())
										args.add(null);
									else if(((JPanel)comp).getComponent(1) instanceof JTextField)
									{
										JTextField field = (JTextField)((JPanel)comp).getComponent(1);
										if(method.getParameterTypes()[i] == byte.class)
											args.add(Byte.parseByte(field.getText()));
						    			else if(method.getParameterTypes()[i] == short.class)
						    				args.add(Short.parseShort(field.getText()));
						    			else if(method.getParameterTypes()[i] == int.class)
						    				args.add(Integer.parseInt(field.getText()));
						    			else if(method.getParameterTypes()[i] == long.class)
						    				args.add(Long.parseLong(field.getText()));
						    			else if(method.getParameterTypes()[i] == char.class)
						    			{
						    				if(field.getText().length() == 1)
						    					throw new IllegalArgumentException("Length of char must be 1");
						    				args.add(field.getText().charAt(0));
						    			}else if(method.getParameterTypes()[i] == float.class)
						    				args.add(Float.parseFloat(field.getText()));
						    			else if(method.getParameterTypes()[i] == double.class)
						    				args.add(Double.parseDouble(field.getText()));
						    			else if(String.class.isAssignableFrom(method.getParameterTypes()[i]))
						    				args.add(field.getText());
						    			else
						    				throw new IllegalStateException(method.getParameterTypes()[i].getName());
									}else if(((JPanel)comp).getComponent(1) instanceof JComboBox)
									{
										JComboBox<?> comboBox = (JComboBox<?>)((JPanel)comp).getComponent(1);
										if(method.getParameterTypes()[i] == boolean.class)
					    					args.add(comboBox.getSelectedIndex() == 0);
					    				else if(Enum.class.isAssignableFrom(method.getParameterTypes()[i]))
					    				{
					    					List<Field> enumFields = new ArrayList<>();
					    					for(Field f : method.getParameterTypes()[i].getDeclaredFields())
						    					if(f.getType() == method.getParameterTypes()[i] && Modifier.isStatic(f.getModifiers()))
						    						enumFields.add(f);
					    					if(comboBox.getSelectedIndex() < enumFields.size() && comboBox.getSelectedIndex() != -1)
					    					{
					    						try
					    						{
						    						Field modifiersField = Field.class.getDeclaredField("modifiers");
													modifiersField.setAccessible(true);
						    						boolean accessible = false;
													int modifier = 0;
													accessible = enumFields.get(comboBox.getSelectedIndex()).isAccessible();
													modifier = enumFields.get(comboBox.getSelectedIndex()).getModifiers();
													enumFields.get(comboBox.getSelectedIndex()).setAccessible(true);
													modifiersField.setInt(enumFields.get(comboBox.getSelectedIndex()),
														enumFields.get(comboBox.getSelectedIndex()).getModifiers() & ~Modifier.FINAL);
						    						args.add(enumFields.get(comboBox.getSelectedIndex()).get(null));
						    						enumFields.get(comboBox.getSelectedIndex()).setAccessible(accessible);
						    						modifiersField.setInt(enumFields.get(comboBox.getSelectedIndex()), modifier);
													modifiersField.setAccessible(false);
					    						}catch(Exception ex)
					    						{
					    							showErrorDialog("Exception occurred while adding Enum (arg " + i + "), using null instead", ex);
					    							args.add(null);
					    						}
					    					}else
					    						args.add(null);
					    				}else
					    					throw new IllegalStateException(method.getParameterTypes()[i].getName());
									}else if(((JPanel)comp).getComponent(1) instanceof ListButton)
										args.add(((ListButton)((JPanel)comp).getComponent(1)).array);
								}
								result = method.invoke(data.data[2], args.toArray(new Object[args.size()]));
								if(method.getReturnType() == void.class)
									root.setUserObject("void");
								else if(result == null)
									root.setUserObject("null");
								else if(method.getReturnType().isPrimitive())
									root.setUserObject(result + " (" + method.getReturnType().getName() + ")");
								else
								{
									if(String.class.isAssignableFrom(method.getReturnType()))
										root.setUserObject((String)result + " (" + result.getClass().getName() + ")");
									else if(Enum.class.isAssignableFrom(method.getReturnType()))
										root.setUserObject(((Enum<?>)result).name() + " (" + result.getClass().getName() + ")");
									else
										root.setUserObject("(" + getArrayName(result.getClass()) + ")");
									Class<?> type = result.getClass();
									deleteChildren((HideableTreeNode)tree.getModel().getRoot());
									if(result.getClass().isArray())
									{
										for(int i = 0; i < Array.getLength(result); i++)
										{
											Object o = Array.get(result, i);
											String name = "(" + getArrayName(result.getClass().getComponentType()) + ")";
											if((result.getClass().getComponentType().isPrimitive() 
												|| String.class.isAssignableFrom(result.getClass().getComponentType()) 
												|| Enum.class.isAssignableFrom(result.getClass().getComponentType()))
												&& o != null)
												name += " = " + o;
											FieldTreeNode node = new FieldTreeNode(name, result.getClass().getComponentType(), 
												result.getClass(), o, !result.getClass().getComponentType().isPrimitive());
											root.add(node);
										}
									}else
									{
										for(Field f : type.getDeclaredFields())
										{
											String name = f.getName()  + " (" + getArrayName(f.getType()) + ")";
											Object inst = null;
											if(Modifier.isStatic(f.getModifiers()) || result != null)
												try
												{
													boolean old = f.isAccessible();
													f.setAccessible(true);
													if((f.getType().isPrimitive() || String.class.isAssignableFrom(f.getType()) || Enum.class.isAssignableFrom(f.getType()))
														&& f.get(result) != null)
														name += " = " + f.get(result);
													inst = f.get(result);
													f.setAccessible(old);
												}catch(Exception ex)
												{
													System.out.println("[DEBUG] ERROR GETTING FIELD VALUE (" + f.getName() + " @ " + type.getName() + ")");
												}
											FieldTreeNode node = new FieldTreeNode(name, f, inst, !f.getType().isPrimitive());
											root.add(node);
										}
										if(showAllFields)
										{
											Class<?> clazz1 = type.getSuperclass();
											while(clazz1 != null)
											{
												for(Field f : clazz1.getDeclaredFields())
												{
													String name = f.getName()  + " (" + getArrayName(f.getType()) + ")";
													Object inst = null;
													if(!Modifier.isStatic(f.getModifiers()) || result != null)
														try
														{
															boolean old = f.isAccessible();
															f.setAccessible(true);
															if((f.getType().isPrimitive() || String.class.isAssignableFrom(f.getType()) 
																|| Enum.class.isAssignableFrom(f.getType()))
																&& f.get(result) != null)
																name += " = " + f.get(result);
															inst = f.get(result);
															f.setAccessible(old);
														}catch(Exception ex)
														{
															System.out.println("[DEBUG] ERROR GETTING FIELD VALUE (" + f.getName() + " @ " + clazz1.getName() + ")");
														}
													FieldTreeNode node = new FieldTreeNode(name, f, inst, !f.getType().isPrimitive());
													root.add(node);
												}
												clazz1 = clazz1.getSuperclass();
											}
										}
									}
									tree.collapsePath(tree.getPathForRow(0));
								}
								((DefaultTreeModel)tree.getModel()).reload();
								method.setAccessible(access);
							}catch(Exception ex)
							{
								showErrorDialog("Exception occurred while invoking method.", ex);
							}
			    		});
						tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
						tree.setRowHeight(30);
						tree.setOpaque(false);
						tree.setFont(tree.getFont().deriveFont(15));
						tree.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
						tree.setShowsRootHandles(true);
						tree.setToggleClickCount(0);
						tree.setCellRenderer(new DefaultTreeCellRenderer()
						{
							private JLabel label = new JLabel();
							
							@Override
							public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
								boolean leaf, int row, boolean hasFocus)
							{
								label.setOpaque(true);
								if(value instanceof FieldTreeNode)
								{
									ImageIcon icon = null;
									Field field = ((FieldTreeNode)value).field;
									if(field != null)
									{
										if(Modifier.isPublic(field.getModifiers()))
											icon = publicField;
										else if (Modifier.isPrivate(field.getModifiers()))
											icon = privateField;
										else if (Modifier.isProtected(field.getModifiers()))
											icon = protectedField;
										else
											icon = defaultField;
										if(Modifier.isTransient(field.getModifiers()))
											icon = combineAccess(icon, transientCo, 2);
										boolean placedFinal = false;
										boolean placedVolatile = false;
										boolean placedStatic = false;
										if(Modifier.isFinal(field.getModifiers()))
										{
											icon = combineAccess(icon, finalCo, 0);
											placedFinal = true;
										}else if(Modifier.isVolatile(field.getModifiers()))
										{
											icon = combineAccess(icon, volatileCo, 0);
											placedVolatile = true;
										}else if(Modifier.isStatic(field.getModifiers()))
										{
											icon = combineAccess(icon, staticCo, 0);
											placedStatic = true;
										}
										if(Modifier.isFinal(field.getModifiers()) && !placedFinal)
											icon = combineAccess(icon, finalCo, 1);
										else if(Modifier.isVolatile(field.getModifiers()) && !placedVolatile)
											icon = combineAccess(icon, volatileCo, 1);
										else if(Modifier.isStatic(field.getModifiers()) && !placedStatic)
											icon = combineAccess(icon, staticCo, 1);
									}else
										icon = publicField;
									label.setIcon(icon);
								}else
									label.setIcon(folder);
								label.setText((String)((DefaultMutableTreeNode)value).getUserObject());
								if(selected)
									label.setBackground(backgroundSelectionColor);
								else
									label.setBackground(backgroundNonSelectionColor);
								return label;
							}
						});
						tree.setUI(new BasicTreeUI()
						{
							@Override 
							public Rectangle getPathBounds(JTree tree, TreePath path)
							{
								if(tree != null && treeState != null)
								{
									Rectangle bounds = new Rectangle();
									bounds = treeState.getBounds(path, bounds);
									if(bounds != null)
									{
										bounds.width = tree.getWidth();
										bounds.y += tree.getInsets().top;
									}
									return bounds;
								}
								return null;
							}
						});
						tree.addTreeWillExpandListener(new TreeWillExpandListener() 
						{
				            @Override
				            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException 
				            {
				                TreePath path = event.getPath();
				                if(path.getLastPathComponent() instanceof FieldTreeNode)
				                {
				                    FieldTreeNode node = (FieldTreeNode)path.getLastPathComponent();
				                    node.loadChildren("");
				                }
				            }

				            @Override
				            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException 
				            {
				            }
				        });
						tree.addMouseListener(new MouseAdapter()
					    {
							@Override
					        public void mousePressed(MouseEvent e)
					        {
					            if(SwingUtilities.isRightMouseButton(e))
					            {
					            	TreePath path = tree.getPathForLocation(e.getX(), e.getY());
					                Rectangle pathBounds = tree.getUI().getPathBounds(tree, path);
					                if(pathBounds != null && pathBounds.contains(e.getX(), e.getY()))
					                {
						            	JPopupMenu menu = new JPopupMenu();
						            	JMenuItem loadAsTab = new JMenuItem("Load As Tab");
						            	menu.add(loadAsTab);
						            	loadAsTab.addActionListener(a -> {
						            		try
											{
												if(path.getLastPathComponent() instanceof FieldTreeNode)
												{
													FieldTreeNode treeNode = (FieldTreeNode)path.getLastPathComponent();
													if(treeNode.value != null && !treeNode.value.getClass().isArray())
													{
														boolean accessible = false;
														int modifier = 0;
														accessible = treeNode.field.isAccessible();
														modifier = treeNode.field.getModifiers();
														Field modifiersField = Field.class.getDeclaredField("modifiers");
														treeNode.field.setAccessible(true);
														modifiersField.setAccessible(true);
														modifiersField.setInt(treeNode.field,
															treeNode.field.getModifiers() & ~Modifier.FINAL);
														addClass(treeNode.value.getClass().getName(), treeNode.value,
															treeNode.value.getClass().getClassLoader());
														treeNode.field.setAccessible(accessible);
									    				modifiersField.setInt(treeNode.field, modifier);
									    				modifiersField.setAccessible(false);
													}
												}else if(result != null && !result.getClass().isArray())
													addClass(result.getClass().getName(), result,
														result.getClass().getClassLoader());
											}catch(Exception ex)
											{
												showErrorDialog("Could not load result.", ex);
											}
						            	});
						            	menu.show(tree, e.getX(), e.getY());
					                }
					            }
					        }
					    });
			    		GridBagConstraints treeC = new GridBagConstraints();
			    		treeC.insets = new Insets(0, 10, 0, 0);
			    		treeC.anchor = GridBagConstraints.SOUTH;
			    		treeC.fill = GridBagConstraints.BOTH;
			    		treeC.weightx = 1;
			    		treeC.weighty = 1;
			    		treeC.gridx = 1;
			    		JScrollPane scrollPane = new JScrollPane(tree);
						scrollPane.setBorder(BorderFactory.createEmptyBorder());
						scrollPane.getViewport().setBackground(Color.WHITE);
			    		newFrame.add(scrollPane, treeC);
			    		newFrame.setAlwaysOnTop(isOnTop);
			    		newFrame.setVisible(true);
			        }
				}
			});
			TableColumnModel colModM = methodsTable.getTableHeader().getColumnModel();
			TableColumn tabColM0 = colModM.getColumn(0);
			tabColM0.setCellRenderer(new DefaultTableCellRenderer()
			{
				JLabel lbl = new JLabel();
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
				{
					if(isSelected)
						lbl.setBackground(table.getSelectionBackground());
					else
						lbl.setBackground(table.getBackground());
					lbl.setIcon((ImageIcon)value);
					lbl.setOpaque(true);
					return lbl;
				}
			});
			tabColM0.setHeaderValue("");
			tabColM0.setMinWidth(25);
			tabColM0.setMaxWidth(25);
			TableColumn tabColM1 = colModM.getColumn(1);
			tabColM1.setCellRenderer(new DefaultTableCellRenderer()
			{
				JLabel lbl = new JLabel();
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
				{
					if(isSelected)
						lbl.setBackground(table.getSelectionBackground());
					else
						lbl.setBackground(table.getBackground());
					lbl.setIcon((ImageIcon)value);
					lbl.setOpaque(true);
					return lbl;
				}
			});
			tabColM1.setHeaderValue("");
			tabColM1.setMinWidth(25);
			tabColM1.setMaxWidth(25);
			TableColumn tabColM2 = colModM.getColumn(2);
			tabColM2.setHeaderValue("name");
			tabColM2.setMinWidth(35);
			methodsTable.setShowHorizontalLines(false);
			methodsTable.setGridColor(Color.LIGHT_GRAY);
			methodsTable.setRowHeight(25);
			jMethod.add(new JScrollPane(methodsTable), BorderLayout.CENTER);
			JPanel jField = new JPanel(new BorderLayout());
			fieldsTable = new MyTable(0, 4, 1);
			fieldsTable.setAutoCreateRowSorter(true);
			fieldsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			fieldsTable.addMouseListener(new MouseAdapter()
			{
				@Override
			    public void mousePressed(MouseEvent mouseEvent)
				{
			        JTable table = (JTable)mouseEvent.getSource();
			        if(mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1)
			        {
			        	JFrame newFrame = new JFrame();
			    		newFrame.setTitle("Java Debugger - Field");
			    		newFrame.setBounds(100, 100, 400, 300);
			    		newFrame.setMinimumSize(new Dimension(400, 300));
			    		newFrame.setResizable(true);
			    		newFrame.getContentPane().setLayout(new GridBagLayout());
			    		
			    		Data data = (Data)table.getValueAt(table.getSelectedRow(), 2);
			    		boolean isField = data.data[1] instanceof Field;
			    		Class<?> type = isField ? ((Field)data.data[1]).getType() 
			    			: ((Class<?>)data.data[4]).getComponentType(); 
			    		
			    		GridBagConstraints ownerLabelC = new GridBagConstraints();
			    		ownerLabelC.insets = new Insets(10, 10, 0, 0);
			    		ownerLabelC.anchor = GridBagConstraints.NORTHWEST;
			    		ownerLabelC.fill = GridBagConstraints.HORIZONTAL;
			    		ownerLabelC.weightx = 1;
			    		if(isField)
			    			newFrame.add(new JLabel("Owner: " + ((Field)data.data[1]).getDeclaringClass().getName()), ownerLabelC);
			    		else
			    			newFrame.add(new JLabel("Owner: " + ((Class<?>)((Data)table.
			    				getValueAt(table.getSelectedRow(), 2)).data[0]).getName()), ownerLabelC);
			    		if(isField)
			    		{
				    		GridBagConstraints accessModC = new GridBagConstraints();
				    		accessModC.insets = new Insets(10, 10, 0, 0);
				    		accessModC.anchor = GridBagConstraints.NORTHWEST;
				    		accessModC.fill = GridBagConstraints.HORIZONTAL;
				    		accessModC.gridy = 1;
				    		accessModC.weightx = 1;
				    		Field field = (Field)data.data[1];
				    		String fieldString = "<b><font color=7f0055>" + Modifier.toString(field.getModifiers()) + "</font></b>";
				    		if(field.isSynthetic())
				    			fieldString += " synthetic";
				    		if(field.getType().isPrimitive())
				    			fieldString += " <b><font color=7f0055>" + getArrayName(field.getType()) + "</font></b>";
				    		else
				    			fieldString += " " + getArrayName(field.getType());
				    		fieldString += " <font color=0000c0>" + field.getName()  + "</font>";
				    		newFrame.add(new JLabel("<html>" + fieldString + "</html>"), accessModC);
			    		}else
			    		{
			    			GridBagConstraints accessModC = new GridBagConstraints();
				    		accessModC.insets = new Insets(10, 10, 0, 0);
				    		accessModC.anchor = GridBagConstraints.NORTHWEST;
				    		accessModC.fill = GridBagConstraints.HORIZONTAL;
				    		accessModC.gridy = 1;
				    		accessModC.weightx = 1;
			    			newFrame.add(new JLabel(getArrayName(type)), accessModC);
			    		}
			    		Object value = null;
			    		try
			    		{
			    			boolean accessible = false;
			    			if(isField)
			    			{
			    				accessible = ((Field)data.data[1]).isAccessible();
			    				((Field)data.data[1]).setAccessible(true);
			    			}
			    			if(table.getValueAt(table.getSelectedRow(), 0) != null)
			    				value = isField ? ((Field)data.data[1]).get(data.data[2]) : Array.get(data.data[1], 
			    					(int)data.data[2]);
			    			if(isField)
			    				((Field)data.data[1]).setAccessible(accessible);
			    		}catch(Exception e)
			    		{
			    			showErrorDialog("Cannot get field value.", e);
			    		}
			    		JPanel panel = new JPanel(new GridBagLayout());
			    		GridBagConstraints panelC = new GridBagConstraints();
			    		panelC.insets = new Insets(10, 10, 0, 0);
			    		panelC.anchor = GridBagConstraints.NORTHWEST;
			    		panelC.fill = GridBagConstraints.BOTH;
			    		panelC.gridy = 2;
			    		panelC.weightx = 1;
			    		newFrame.add(panel, panelC);
			    		JCheckBox nullBox = new JCheckBox("Null");
			    		JButton setValue = new JButton("Set Value");
			    		if(table.getValueAt(table.getSelectedRow(), 0) == null)
			    			setValue.setEnabled(false);
			    		if(type == boolean.class || Enum.class.isAssignableFrom(type))
			    		{
			    			GridBagConstraints textC = new GridBagConstraints();
			    			textC.anchor = GridBagConstraints.NORTHWEST;
			    			textC.insets = new Insets(3, 0, 10, 0);
			    			panel.add(new JLabel(type == boolean.class ? "Boolean:   "  : "Enum:   "), textC);
			    			JComboBox<String> comboBox = new JComboBox<>();
			    			List<Field> enumFields = new ArrayList<>();
			    			if(type == boolean.class)
			    			{
			    				comboBox.addItem("true");
			    				comboBox.addItem("false");
			    				if(value != null && value.equals(true))
			    					comboBox.setSelectedIndex(0);
			    				else
			    					comboBox.setSelectedIndex(1);
			    				nullBox.setEnabled(false);
			    			}else
			    			{
			    				for(Field f : type.getDeclaredFields())
			    					if(f.getType() == type && Modifier.isStatic(f.getModifiers()))
			    						enumFields.add(f);
			    				for(Field f : enumFields)
			    					comboBox.addItem(f.getName());
			    				if(table.getValueAt(table.getSelectedRow(), 0) == null)
			    				{
			    					nullBox.setEnabled(false);
			    					comboBox.setEnabled(false);
			    				}else if(value == null)
			    				{
			    					nullBox.setSelected(true);
			    					comboBox.setEnabled(false);
			    				}else
			    				{
			    					comboBox.setSelectedItem(((Enum<?>)value).name());
			    					if(comboBox.getItemCount() == 0)
			    						comboBox.addItem(((Enum<?>)value).name());
			    				}
			    			}
			    			GridBagConstraints valueC = new GridBagConstraints();
				    		valueC.anchor = GridBagConstraints.NORTHWEST;
				    		valueC.gridx = 1;
				    		valueC.weightx = 1;
				    		panel.add(comboBox, valueC);
				    		nullBox.addActionListener(a -> {
				    			if(nullBox.isSelected())
				    				comboBox.setEnabled(false);
				    			else
				    				comboBox.setEnabled(true);
				    		});
				    		setValue.addActionListener(a -> {
				    			try
								{
									if(nullBox.isSelected())
									{
										if(isField)
										{
											if(!Enum.class.isAssignableFrom(type))
												throw new IllegalStateException(type.getName());
											boolean accessible = false;
											int modifier = 0;
											accessible = ((Field)data.data[1]).isAccessible();
											modifier = ((Field)data.data[1]).getModifiers();
											((Field)data.data[1]).setAccessible(true);
											Field modifiersField = Field.class.getDeclaredField("modifiers");
											modifiersField.setAccessible(true);
											modifiersField.setInt(((Field)data.data[1]), 
												((Field)data.data[1]).getModifiers() & ~Modifier.FINAL);
											((Field)data.data[1]).set(data.data[2], null);
											for(int i = 0; i < fieldsTable.getRowCount(); i++)
						    				{
						    					Data data1 = (Data)fieldsTable.getModel().getValueAt(i, 2);
						    					if(data1 == data)
						    					{
						    						fieldsTable.getModel().setValueAt(null, i, 3);
						    						data.data[3] = ((Field)data.data[1]).get(data.data[2]) == null;
						    						break;
						    					}
						    				}
											((Field)data.data[1]).setAccessible(accessible);
						    				modifiersField.setInt(((Field)data.data[1]), modifier);
						    				modifiersField.setAccessible(false);
										}else
										{
											if(!Enum.class.isAssignableFrom(type))
												throw new IllegalStateException(type.getName());
											Array.set(data.data[1], (int)data.data[2], null);
											for(int i = 0; i < fieldsTable.getRowCount(); i++)
						    				{
						    					Data data1 = (Data)fieldsTable.getModel().getValueAt(i, 2);
						    					if(data1 == data)
						    					{
						    						fieldsTable.getModel().setValueAt(null, i, 3);
						    						data.data[3] = true;
						    						break;
						    					}
						    				}
										}
									}else
									{
										if(isField)
										{
											boolean accessible = false;
											int modifier = 0;
											accessible = ((Field)data.data[1]).isAccessible();
											modifier = ((Field)data.data[1]).getModifiers();
											((Field)data.data[1]).setAccessible(true);
											Field modifiersField = Field.class.getDeclaredField("modifiers");
											modifiersField.setAccessible(true);
											modifiersField.setInt(((Field)data.data[1]), 
												((Field)data.data[1]).getModifiers() & ~Modifier.FINAL);
							    			try
							    			{
							    				if(type == boolean.class)
							    				{
							    					boolean prim = comboBox.getSelectedIndex() == 0;
								    				((Field)data.data[1]).setBoolean(data.data[2], prim);
							    				}else if(Enum.class.isAssignableFrom(type))
							    				{
							    					if(comboBox.getSelectedIndex() < enumFields.size() && comboBox.getSelectedIndex() != -1)
							    					{
							    						boolean accessible1 = false;
														int modifier1 = 0;
														accessible1 = enumFields.get(comboBox.getSelectedIndex()).isAccessible();
														modifier1 = enumFields.get(comboBox.getSelectedIndex()).getModifiers();
														enumFields.get(comboBox.getSelectedIndex()).setAccessible(true);
														modifiersField.setInt(enumFields.get(comboBox.getSelectedIndex()), 
															enumFields.get(comboBox.getSelectedIndex()).getModifiers() & ~Modifier.FINAL);
							    						((Field)data.data[1]).set(data.data[2], enumFields.get(comboBox.getSelectedIndex()).get(null));
							    						enumFields.get(comboBox.getSelectedIndex()).setAccessible(accessible1);
									    				modifiersField.setInt(enumFields.get(comboBox.getSelectedIndex()), modifier1);
							    					}else
							    						((Field)data.data[1]).set(data.data[2], null);
							    				}else
							    					throw new IllegalStateException(type.getName());
							    			}finally
							    			{
							    				for(int i = 0; i < fieldsTable.getRowCount(); i++)
							    				{
							    					Data data1 = (Data)fieldsTable.getModel().getValueAt(i, 2);
							    					if(data1 == data)
							    					{
							    						fieldsTable.getModel().setValueAt(((Field)data.data[1]).get(
							    							data.data[2]), i, 3);
							    						data.data[3] = ((Field)data.data[1]).get(data.data[2]) == null;
							    						break;
							    					}
							    				}
							    				((Field)data.data[1]).setAccessible(accessible);
							    				modifiersField.setInt(((Field)data.data[1]), modifier);
							    				modifiersField.setAccessible(false);
							    			}
										}else
										{
											if(type == boolean.class)
						    				{
												boolean prim = comboBox.getSelectedIndex() == 0;
												Array.setBoolean(data.data[1], (int)data.data[2], prim);
						    				}else if(Enum.class.isAssignableFrom(type))
						    				{
						    					if(comboBox.getSelectedIndex() < enumFields.size() && comboBox.getSelectedIndex() != -1)
						    					{
													Field modifiersField = Field.class.getDeclaredField("modifiers");
													modifiersField.setAccessible(true);
						    						boolean accessible1 = false;
													int modifier1 = 0;
													accessible1 = enumFields.get(comboBox.getSelectedIndex()).isAccessible();
													modifier1 = enumFields.get(comboBox.getSelectedIndex()).getModifiers();
													enumFields.get(comboBox.getSelectedIndex()).setAccessible(true);
													modifiersField.setInt(enumFields.get(comboBox.getSelectedIndex()), 
														enumFields.get(comboBox.getSelectedIndex()).getModifiers() & ~Modifier.FINAL);
													Array.set(data.data[1], (int)data.data[2], enumFields.get(comboBox.getSelectedIndex()).get(null));
						    						enumFields.get(comboBox.getSelectedIndex()).setAccessible(accessible1);
								    				modifiersField.setInt(enumFields.get(comboBox.getSelectedIndex()), modifier1);
								    				modifiersField.setAccessible(false);
						    					}else
						    						Array.set(data.data[1], (int)data.data[2], null);
						    				}else
						    					throw new IllegalStateException(type.getName());
											for(int i = 0; i < fieldsTable.getRowCount(); i++)
						    				{
						    					Data data1 = (Data)fieldsTable.getModel().getValueAt(i, 2);
						    					if(data1 == data)
						    					{
						    						fieldsTable.getModel().setValueAt(Array.get(data.data[1], 
						    							(int)data.data[2]), i, 3);
						    						data.data[3] = Array.get(data.data[1], (int)data.data[2]) == null;
						    						break;
						    					}
						    				}
										}
									}
								}catch(Exception e)
								{
									showErrorDialog("Error setting field.", e);
								}
								newFrame.dispose();
				    		});
			    		}else if(type.isArray())
			    		{
			    			JLabel text = new JLabel("Value:   ");
			    			GridBagConstraints textC = new GridBagConstraints();
			    			textC.anchor = GridBagConstraints.NORTHWEST;
			    			textC.insets = new Insets(3, 0, 10, 0);
			    			panel.add(text, textC);
			    			ListButton arrButton = new ListButton("Edit Array", value == null ? 
			    				Array.newInstance(type.getComponentType(), 0) : value, type.getComponentType(), panel);
			    			GridBagConstraints arrButtonC = new GridBagConstraints();
			    			arrButtonC.anchor = GridBagConstraints.NORTHWEST;
			    			arrButtonC.gridx = 1;
			    			arrButtonC.weightx = 1;
				    		panel.add(arrButton, arrButtonC);
				    		
				    		if(table.getValueAt(table.getSelectedRow(), 0) == null)
		    				{
		    					nullBox.setEnabled(false);
		    					arrButton.setEnabled(false);
		    				}else if(value == null)
				    		{
				    			nullBox.setSelected(true);
				    			arrButton.setEnabled(false);
				    		}
				    		Class<?> compType = getRealComponentType(type);
			    			if(!String.class.isAssignableFrom(compType) && !compType.isPrimitive()
			    				&& !Enum.class.isAssignableFrom(compType))
			    			{
			    				nullBox.setEnabled(false);
			    				arrButton.setEnabled(false);
			    				setValue.setEnabled(false);
			    			}
				    		nullBox.addActionListener(a -> {
				    			if(nullBox.isSelected())
									arrButton.setEnabled(false);
								else
									arrButton.setEnabled(true);
				    		});
				    		setValue.addActionListener(a -> {
				    			try
								{
									if(nullBox.isSelected())
									{
										if(isField)
										{
											boolean accessible = false;
											int modifier = 0;
											accessible = ((Field)data.data[1]).isAccessible();
											modifier = ((Field)data.data[1]).getModifiers();
											((Field)data.data[1]).setAccessible(true);
											Field modifiersField = Field.class.getDeclaredField("modifiers");
											modifiersField.setAccessible(true);
											modifiersField.setInt(((Field)data.data[1]), 
												((Field)data.data[1]).getModifiers() & ~Modifier.FINAL);
											((Field)data.data[1]).set(data.data[2], null);
											for(int i = 0; i < fieldsTable.getRowCount(); i++)
						    				{
						    					Data data1 = (Data)fieldsTable.getModel().getValueAt(i, 2);
						    					if(data1 == data)
						    					{
						    						fieldsTable.getModel().setValueAt(null, i, 3);
						    						data.data[3] = ((Field)data.data[1]).get(data.data[2]) == null;
						    						break;
						    					}
						    				}
											((Field)data.data[1]).setAccessible(accessible);
						    				modifiersField.setInt(((Field)data.data[1]), modifier);
						    				modifiersField.setAccessible(false);
										}else
										{
											Array.set(data.data[1], (int)data.data[2], null);
											for(int i = 0; i < fieldsTable.getRowCount(); i++)
						    				{
						    					Data data1 = (Data)fieldsTable.getModel().getValueAt(i, 2);
						    					if(data1 == data)
						    					{
						    						fieldsTable.getModel().setValueAt(null, i, 3);
						    						data.data[3] = true;
						    						break;
						    					}
						    				}
										}
									}else
									{
										if(isField)
										{
											boolean accessible = false;
											int modifier = 0;
											accessible = ((Field)data.data[1]).isAccessible();
											modifier = ((Field)data.data[1]).getModifiers();
											((Field)data.data[1]).setAccessible(true);
											Field modifiersField = Field.class.getDeclaredField("modifiers");
											modifiersField.setAccessible(true);
											modifiersField.setInt(((Field)data.data[1]), 
												((Field)data.data[1]).getModifiers() & ~Modifier.FINAL);
							    			try
							    			{
							    				((Field)data.data[1]).set(data.data[2], arrButton.array);
							    			}finally
							    			{
							    				for(int i = 0; i < fieldsTable.getRowCount(); i++)
							    				{
							    					Data data1 = (Data)fieldsTable.getModel().getValueAt(i, 2);
							    					if(data1 == data)
							    					{
							    						Object valueAt = ((Field)data.data[1]).get(data.data[2]);
							    						if(valueAt != null)
							    							valueAt = getArrayName(((Field)data.data[1]).get(data.data[2]).getClass());
							    						fieldsTable.getModel().setValueAt(valueAt, i, 3);
							    						data.data[3] = ((Field)data.data[1]).get(data.data[2]) == null;
							    						break;
							    					}
							    				}
							    				((Field)data.data[1]).setAccessible(accessible);
							    				modifiersField.setInt(((Field)data.data[1]), modifier);
							    				modifiersField.setAccessible(false);
							    			}
										}else
										{
											Array.set(data.data[1], (int)data.data[2], arrButton.array);
											for(int i = 0; i < fieldsTable.getRowCount(); i++)
						    				{
						    					Data data1 = (Data)fieldsTable.getModel().getValueAt(i, 2);
						    					if(data1 == data)
						    					{
						    						Object valueAt = ((Field)data.data[1]).get(data.data[2]);
						    						if(valueAt != null)
						    							valueAt = getArrayName(((Field)data.data[1]).get(data.data[2]).getClass());
						    						fieldsTable.getModel().setValueAt(valueAt, i, 3);
						    						data.data[3] = Array.get(data.data[1], (int)data.data[2]) == null;
						    						break;
						    					}
						    				}
										}
									}
								}catch(Exception e)
								{
									showErrorDialog("Error setting field.", e);
								}
								newFrame.dispose();
				    		});
			    		}else
			    		{
			    			JLabel text = new JLabel("Value:   ");
			    			GridBagConstraints textC = new GridBagConstraints();
			    			textC.anchor = GridBagConstraints.NORTHWEST;
			    			textC.insets = new Insets(3, 0, 10, 0);
			    			panel.add(text, textC);
			    			JTextField textField = new JTextField(20);
			    			GridBagConstraints valueC = new GridBagConstraints();
				    		valueC.anchor = GridBagConstraints.NORTHWEST;
				    		valueC.gridx = 1;
				    		valueC.weightx = 1;
				    		panel.add(textField, valueC);
				    		
				    		if(table.getValueAt(table.getSelectedRow(), 0) == null)
		    				{
		    					nullBox.setEnabled(false);
		    					textField.setEnabled(false);
		    				}else if(value == null)
				    		{
				    			nullBox.setSelected(true);
				    			textField.setEnabled(false);
				    			if(!String.class.isAssignableFrom(type) && !type.isPrimitive())
				    			{
				    				setValue.setEnabled(false);
				    				nullBox.setEnabled(false);
				    			}
				    		}else if(String.class.isAssignableFrom(type))
				    			textField.setText(value.toString());
				    		else if(type.isPrimitive())
				    		{
				    			textField.setText(value.toString());
				    			nullBox.setEnabled(false);
				    		}else
				    		{
				    			nullBox.setEnabled(false);
				    			textField.setEnabled(false);
				    			setValue.setEnabled(false);
				    			text.setEnabled(false);
				    		}
				    		nullBox.addActionListener(a -> {
				    			if(nullBox.isSelected())
									textField.setEnabled(false);
								else
									textField.setEnabled(true);
				    		});
				    		setValue.addActionListener(a -> {
				    			try
								{
									if(nullBox.isSelected())
									{
										if(isField)
										{
											if(!String.class.isAssignableFrom(type))
												throw new IllegalStateException(type.getName());
											boolean accessible = false;
											int modifier = 0;
											accessible = ((Field)data.data[1]).isAccessible();
											modifier = ((Field)data.data[1]).getModifiers();
											((Field)data.data[1]).setAccessible(true);
											Field modifiersField = Field.class.getDeclaredField("modifiers");
											modifiersField.setAccessible(true);
											modifiersField.setInt(((Field)data.data[1]), 
												((Field)data.data[1]).getModifiers() & ~Modifier.FINAL);
											((Field)data.data[1]).set(data.data[2], null);
											for(int i = 0; i < fieldsTable.getRowCount(); i++)
						    				{
						    					Data data1 = (Data)fieldsTable.getModel().getValueAt(i, 2);
						    					if(data1 == data)
						    					{
						    						fieldsTable.getModel().setValueAt(null, i, 3);
						    						data.data[3] = ((Field)data.data[1]).get(data.data[2]) == null;
						    						break;
						    					}
						    				}
											((Field)data.data[1]).setAccessible(accessible);
						    				modifiersField.setInt(((Field)data.data[1]), modifier);
						    				modifiersField.setAccessible(false);
										}else
										{
											if(!String.class.isAssignableFrom(type))
												throw new IllegalStateException(type.getName());
											Array.set(data.data[1], (int)data.data[2], null);
											for(int i = 0; i < fieldsTable.getRowCount(); i++)
						    				{
						    					Data data1 = (Data)fieldsTable.getModel().getValueAt(i, 2);
						    					if(data1 == data)
						    					{
						    						fieldsTable.getModel().setValueAt(null, i, 3);
						    						data.data[3] = true;
						    						break;
						    					}
						    				}
										}
									}else
									{
										if(isField)
										{
											boolean accessible = false;
											int modifier = 0;
											accessible = ((Field)data.data[1]).isAccessible();
											modifier = ((Field)data.data[1]).getModifiers();
											((Field)data.data[1]).setAccessible(true);
											Field modifiersField = Field.class.getDeclaredField("modifiers");
											modifiersField.setAccessible(true);
											modifiersField.setInt(((Field)data.data[1]), 
												((Field)data.data[1]).getModifiers() & ~Modifier.FINAL);
							    			try
							    			{
								    			if(type == byte.class)
								    			{
								    				byte prim = Byte.parseByte(textField.getText());
								    				((Field)data.data[1]).setByte(data.data[2], prim);
								    			}else if(type == short.class)
								    			{
								    				short prim = Short.parseShort(textField.getText());
								    				((Field)data.data[1]).setShort(data.data[2], prim);
								    			}else if(type == int.class)
								    			{
								    				int prim = Integer.parseInt(textField.getText());
								    				((Field)data.data[1]).setInt(data.data[2], prim);
								    			}else if(type == long.class)
								    			{
								    				long prim = Long.parseLong(textField.getText());
								    				((Field)data.data[1]).setLong(data.data[2], prim);
								    			}else if(type == char.class)
								    			{
								    				if(textField.getText().length() == 1)
								    					throw new IllegalArgumentException("Length of char must be 1");
								    				((Field)data.data[1]).setChar(data.data[2], textField.getText().charAt(0));
								    			}else if(type == float.class)
								    			{
								    				float prim = Float.parseFloat(textField.getText());
								    				((Field)data.data[1]).setFloat(data.data[2], prim);
								    			}else if(type == double.class)
								    			{
								    				double prim = Double.parseDouble(textField.getText());
								    				((Field)data.data[1]).setDouble(data.data[2], prim);
								    			}else if(String.class.isAssignableFrom(type))
								    				((Field)data.data[1]).set(data.data[2], textField.getText());
								    			else
								    				throw new IllegalStateException(type.getName());
							    			}finally
							    			{
							    				for(int i = 0; i < fieldsTable.getRowCount(); i++)
							    				{
							    					Data data1 = (Data)fieldsTable.getModel().getValueAt(i, 2);
							    					if(data1 == data)
							    					{
							    						fieldsTable.getModel().setValueAt(((Field)data.data[1]).get(
							    							data.data[2]), i, 3);
							    						data.data[3] = ((Field)data.data[1]).get(data.data[2]) == null;
							    						break;
							    					}
							    				}
							    				((Field)data.data[1]).setAccessible(accessible);
							    				modifiersField.setInt(((Field)data.data[1]), modifier);
							    				modifiersField.setAccessible(false);
							    			}
										}else
										{
											if(type == byte.class)
							    			{
							    				byte prim = Byte.parseByte(textField.getText());
							    				Array.setByte(data.data[1], (int)data.data[2], prim);
							    			}else if(type == short.class)
							    			{
							    				short prim = Short.parseShort(textField.getText());
							    				Array.setShort(data.data[1], (int)data.data[2], prim);
							    			}else if(type == int.class)
							    			{
							    				int prim = Integer.parseInt(textField.getText());
							    				Array.setInt(data.data[1], (int)data.data[2], prim);
							    			}else if(type == long.class)
							    			{
							    				long prim = Long.parseLong(textField.getText());
							    				Array.setLong(data.data[1], (int)data.data[2], prim);
							    			}else if(type == char.class)
							    			{
							    				if(textField.getText().length() == 1)
							    					throw new IllegalArgumentException("Length of char must be 1");
							    				Array.setChar(data.data[1], (int)data.data[2], textField.getText().charAt(0));
							    			}else if(type == float.class)
							    			{
							    				float prim = Float.parseFloat(textField.getText());
							    				Array.setFloat(data.data[1], (int)data.data[2], prim);
							    			}else if(type == double.class)
							    			{
							    				double prim = Double.parseDouble(textField.getText());
							    				Array.setDouble(data.data[1], (int)data.data[2], prim);
							    			}else if(String.class.isAssignableFrom(type))
							    				Array.set(data.data[1], (int)data.data[2], textField.getText());
							    			else
							    				throw new IllegalStateException(type.getName());
											for(int i = 0; i < fieldsTable.getRowCount(); i++)
						    				{
						    					Data data1 = (Data)fieldsTable.getModel().getValueAt(i, 2);
						    					if(data1 == data)
						    					{
						    						fieldsTable.getModel().setValueAt(Array.get(data.data[1], 
						    							(int)data.data[2]), i, 3);
						    						data.data[3] = Array.get(data.data[1], (int)data.data[2]) == null;
						    						break;
						    					}
						    				}
										}
									}
								}catch(Exception e)
								{
									showErrorDialog("Error setting field.", e);
								}
								newFrame.dispose();
				    		});
			    		}
			    		GridBagConstraints nullBoxC = new GridBagConstraints();
			    		nullBoxC.insets = new Insets(0, 10, 0, 0);
			    		nullBoxC.anchor = GridBagConstraints.NORTHWEST;
			    		nullBoxC.gridy = 3;
			    		newFrame.add(nullBox, nullBoxC);
			    		
			    		GridBagConstraints setValueC = new GridBagConstraints();
			    		setValueC.insets = new Insets(0, 0, 20, 0);
			    		setValueC.anchor = GridBagConstraints.SOUTH;
			    		setValueC.weighty = 1;
			    		setValueC.gridy = 4;
			    		newFrame.add(setValue, setValueC);
			    		newFrame.setAlwaysOnTop(isOnTop);
			    		newFrame.setVisible(true);
			        }
			    }
			});
			TableColumnModel colMod = fieldsTable.getTableHeader().getColumnModel();
			TableColumn tabCol0 = colMod.getColumn(0);
			tabCol0.setCellRenderer(new DefaultTableCellRenderer()
			{
				JLabel lbl = new JLabel();
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
				{
					if(isSelected)
						lbl.setBackground(table.getSelectionBackground());
					else
						lbl.setBackground(table.getBackground());
					lbl.setIcon((ImageIcon)value);
					lbl.setOpaque(true);
					return lbl;
				}
			});
			tabCol0.setHeaderValue("");
			tabCol0.setMinWidth(25);
			tabCol0.setMaxWidth(25);
			TableColumn tabCol1 = colMod.getColumn(1);
			tabCol1.setCellRenderer(new DefaultTableCellRenderer()
			{
				JLabel lbl = new JLabel();
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
				{
					if(isSelected)
						lbl.setBackground(table.getSelectionBackground());
					else
						lbl.setBackground(table.getBackground());
					lbl.setIcon((ImageIcon)value);
					lbl.setOpaque(true);
					return lbl;
				}
			});
			tabCol1.setHeaderValue("");
			tabCol1.setMinWidth(25);
			tabCol1.setMaxWidth(25);
			TableColumn tabCol2 = colMod.getColumn(2);
			tabCol2.setHeaderValue("name");
			tabCol2.setMinWidth(35);
			TableColumn tabCol3 = colMod.getColumn(3);
			tabCol3.setHeaderValue("value");
			tabCol3.setMinWidth(35);
			fieldsTable.setShowHorizontalLines(false);
			fieldsTable.setGridColor(Color.LIGHT_GRAY);
			fieldsTable.setRowHeight(25);
			jField.add(new JScrollPane(fieldsTable), BorderLayout.CENTER);
			fieldMethodsTab.addTab("Methods", jMethod);
			fieldMethodsTab.addTab("Fields", jField);
			fieldMethodsTab.setFont(new Font("Tahoma", Font.BOLD, 15));
			fieldMethodsTab.setOpaque(true);
			fieldMethodsTab.setBackground(new Color(105, 105, 105));
			GridBagConstraints fieldMethodsTabC = new GridBagConstraints();
			fieldMethodsTabC.fill = GridBagConstraints.BOTH;
			fieldMethodsTabC.anchor = GridBagConstraints.NORTHWEST;
			fieldMethodsTabC.weightx = 1;
			fieldMethodsTabC.weighty = 1;
			propertiesPanel.add(fieldMethodsTab, fieldMethodsTabC);
			propertiesPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
			propertiesPanel.setMinimumSize(new Dimension(0, 0));
			
			JSplitPane splitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainPanel, propertiesPanel);
			splitPane1.setDividerLocation(0.65);
			splitPane1.setResizeWeight(0.65);
			splitPane1.setContinuousLayout(true);
			splitPane1.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			splitPane1.setUI(new BasicSplitPaneUI()
			{
			    @Override
		        public BasicSplitPaneDivider createDefaultDivider() 
		        {
		            return new BasicSplitPaneDivider(this) 
		            {
		                @Override
		                public void setBorder(Border b) 
		                {}
		            };
		        }
		    });
			splitPane1.setBorder(null);
			splitPane1.addPropertyChangeListener(new PropertyChangeListener() 
			{
				@Override
				public void propertyChange(PropertyChangeEvent changeEvent)
				{
					String propertyName = changeEvent.getPropertyName();
					if(propertyName.equals(JSplitPane.DIVIDER_LOCATION_PROPERTY))
						splitPane1.setResizeWeight(((int)changeEvent.getNewValue()) / 
							(double)(splitPane1.getWidth() - splitPane1.getDividerSize()));
				}
			});
			
			breakpointPanel = new JPanel(new BorderLayout());
			breakpointPanel.setPreferredSize(breakpointPanel.getPreferredSize());
			JLabel breakpointsLabel = new JLabel("  Breakpoints");
			breakpointsLabel.setPreferredSize(new Dimension(breakpointsLabel.getPreferredSize().width, breakpointsLabel.getPreferredSize().height + 6));
			breakpointsLabel.setFont(new Font("Tahoma", Font.BOLD, 15));
			breakpointsLabel.setOpaque(true);
			breakpointsLabel.setBackground(new Color(105, 105, 105));
			breakpointsLabel.setForeground(Color.WHITE);
			breakpointPanel.add(breakpointsLabel, BorderLayout.NORTH);
			breakpointsTable = new MyTable(0, 6, 2);
			breakpointsTable.setShowHorizontalLines(false);
			breakpointsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			breakpointsTable.addMouseListener(new MouseAdapter()
			{
				@Override
			    public void mousePressed(MouseEvent mouseEvent)
				{
			        JTable table = (JTable)mouseEvent.getSource();
			        if(mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1 && table.getSelectedColumn() == 5)
			        {
			        	Object[] values = (Object[])((Data)table.getValueAt(table.getSelectedRow(), 5)).data[0];
			        	if(values == null)
			        		values = new Object[0];
			        	AtomicBoolean bool = (AtomicBoolean)table.getValueAt(table.getSelectedRow(), 4);
			        	JPanel panel = new JPanel();
						panel.setBounds(100, 100, 300, 400);
						panel.setLayout(new BorderLayout());
						
						JTable jtable = new JTable()
						{
							@Override
							public boolean isCellEditable(int row, int column)
							{
								return false;
							}
						};
						jtable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
						jtable.getTableHeader().setReorderingAllowed(false);
						DefaultTableModel lm = new DefaultTableModel();
						lm.addColumn("#");
						lm.addColumn("Type");
						lm.addColumn("Value");
						int i = 0;
						for(Object o : values)
						{
							String type = o != null ? getArrayName(o.getClass()) : "";
							lm.addRow(new Object[]{i, type, o});
							i++;
						}
						jtable.setModel(lm);
						jtable.getColumn("Value").setCellRenderer(new DefaultTableCellRenderer()
						{
							@Override
							public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
								boolean hasFocus, int row, int column)
							{
								if(value != null && value.getClass().isArray())
									value = getArrayName(value.getClass());
								Component cell = super.getTableCellRendererComponent(table, value, isSelected,
						            hasFocus, row, column);
								if(!cell.getBackground().equals(table.getSelectionBackground()))
									cell.setBackground(Color.WHITE);
								if(column == 2 && value == null && !isSelected)
									cell.setBackground(Color.RED);
								return cell;
							}
						});
						
						panel.add(new JScrollPane(jtable), BorderLayout.CENTER);
						if(JOptionPane.showOptionDialog(null, panel, "View Array",
					        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[] {"Ok", "Pass"}, null) == 1)
						{
							bool.set(true);
							int newRow = -1;
							for(int r = 0; r < table.getRowCount(); r++)
								if(table.getValueAt(r, 4) == bool)
								{
									newRow = r;
									break;
								}
							((DefaultTableModel)table.getModel()).fireTableCellUpdated(newRow, 4);
						}
			        }
				}
			});
			TableColumnModel colModB = breakpointsTable.getTableHeader().getColumnModel();
			TableColumn tabColB0 = colModB.getColumn(0);
			tabColB0.setHeaderValue("ID");
			TableColumn tabColB1 = colModB.getColumn(1);
			tabColB1.setHeaderValue("Time");
			tabColB1.setCellRenderer(new DefaultTableCellRenderer()
			{
				private SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
				
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
				{
					Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					((JLabel)c).setText(format.format(new Date((long)value)));
					return c;
				}
			});
			TableColumn tabColB2 = colModB.getColumn(2);
			tabColB2.setHeaderValue("Time Difference");
			tabColB2.setCellRenderer(new DefaultTableCellRenderer()
			{
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
				{
					Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					long diff = (long)value;
					String diffMS = String.valueOf(diff % 1000);
					String diffSeconds = String.valueOf(diff / 1000 % 60);  
					String diffMinutes = String.valueOf(diff / 60000 % 60);
					String diffHours = String.valueOf(diff / 3600000);
					if(diffSeconds.length() == 1)
						diffSeconds = "0" + diffSeconds;
					if(diffMinutes.length() == 1)
						diffMinutes = "0" + diffMinutes;
					if(diffHours.length() == 1)
						diffHours = "0" + diffHours;
					while(diffMS.length() != 4)
						diffMS = "0" + diffMS;
					((JLabel)c).setText(diffHours + ":" + diffMinutes + ":" + diffSeconds + " " + diffMS);
					return c;
				}
			});
			TableColumn tabColB3 = colModB.getColumn(3);
			tabColB3.setHeaderValue("Mode");
			TableColumn tabColB4 = colModB.getColumn(4);
			tabColB4.setHeaderValue("Pass");
			TableColumn tabColB5 = colModB.getColumn(5);
			tabColB5.setHeaderValue("View");
			tabColB5.setCellRenderer(new TableButtonRenderer());
			breakpointsTable.setGridColor(Color.LIGHT_GRAY);
			breakpointsTable.setRowHeight(25);
			breakpointPanel.add(new JScrollPane(breakpointsTable), BorderLayout.CENTER);
			JPanel bottomBreakpointPanel = new JPanel(new GridBagLayout());
			passBox = new JCheckBox("Pass", true);
			GridBagConstraints passBoxC = new GridBagConstraints();
			passBoxC.anchor = GridBagConstraints.EAST;
			passBoxC.weightx = 1;
			passBoxC.insets = new Insets(0, 0, 0, 10);
			bottomBreakpointPanel.add(passBox, passBoxC);
			JButton clearButton = new JButton("Pass and Delete Selected Breakpoints");
			clearButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					for(int index : breakpointsTable.getSelectedRows())
						((AtomicBoolean)breakpointsTable.getValueAt(index, 4)).set(true);
					int[] indices = Arrays.copyOf(breakpointsTable.getSelectedRows(), breakpointsTable.getSelectedRows().length);
					Arrays.sort(indices);
					int left = 0;
				    int right = indices.length - 1;
				    while(left < right)
				    {
				    	int temp = indices[left];
				    	indices[left] = indices[right];
				    	indices[right] = temp;
				        left++;
				        right--;
				    }
				    for(int i : indices)
				    	((DefaultTableModel)breakpointsTable.getModel()).removeRow(i);
				}
			});
			GridBagConstraints clearButtonC = new GridBagConstraints();
			clearButtonC.anchor = GridBagConstraints.EAST;
			clearButtonC.gridx = 1;
			clearButtonC.insets = new Insets(0, 0, 0, 10);
			bottomBreakpointPanel.add(clearButton, clearButtonC);
			bottomBreakpointPanel.setMinimumSize(new Dimension(0, 0));
			breakpointPanel.add(bottomBreakpointPanel, BorderLayout.SOUTH);
			breakpointPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
			breakpointPanel.setMinimumSize(new Dimension(0, 0));
			panel.add(breakpointPanel);
			
			JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane1, breakpointPanel);
			splitPane2.setDividerLocation(0.6);
			splitPane2.setResizeWeight(0.6);
			splitPane2.setContinuousLayout(true);
			splitPane2.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			splitPane2.setUI(new BasicSplitPaneUI()
			{
		        @Override
		        public BasicSplitPaneDivider createDefaultDivider() 
		        {
		            return new BasicSplitPaneDivider(this) 
		            {
		                @Override
		                public void setBorder(Border b) 
		                {}
		            };
		        }
		    });
			splitPane2.setBorder(null);
			splitPane2.addPropertyChangeListener(new PropertyChangeListener() 
			{
				@Override
				public void propertyChange(PropertyChangeEvent changeEvent)
				{
					String propertyName = changeEvent.getPropertyName();
					if(propertyName.equals(JSplitPane.DIVIDER_LOCATION_PROPERTY))
						splitPane2.setResizeWeight(((int)changeEvent.getNewValue()) / 
							(double)(splitPane2.getHeight() - splitPane2.getDividerSize()));
				}
			});
			GridBagConstraints splitPane2C = new GridBagConstraints();
			splitPane2C.anchor = GridBagConstraints.NORTHWEST;
			splitPane2C.fill = GridBagConstraints.BOTH;
			splitPane2C.weightx = 1;
			splitPane2C.weighty = 1;
			splitPane2C.insets = new Insets(3, 3, 3, 3);
			panel.add(splitPane2, splitPane2C);
			refreshTree(textField.getText());
			init = true;
			return panel;
		}
		
		private void updateFilter(HideableTreeNode node, String filterText, boolean clear)
		{
			if(clear && node != tree.getModel().getRoot())
			{
				for(int i = 0; i < ((HideableTreeNode)tree.getModel().getRoot()).getChildCountFilterless(); i++)
				{
					HideableTreeNode child = (HideableTreeNode)((HideableTreeNode)tree.getModel().getRoot()).getChildAtFilterless(i);
					child.filtered = false;
					updateFilter(child, filterText, false);
				}
			}
			for(int i = 0; i < node.getChildCountFilterless(); i++)
			{
				HideableTreeNode child = (HideableTreeNode)node.getChildAtFilterless(i);
				String name = (String)child.getUserObject();
				if(name.contains(filterText))
					child.filtered = false;
				else
					child.filtered = true;
				updateFilter(child, filterText, false);
			}
			((DefaultTreeModel)tree.getModel()).reload();
		}
		
		private void refreshTree(String filter)
		{
			expanded.clear();
			specificSearch = null;
			deleteChildren((HideableTreeNode)tree.getModel().getRoot());
			for(Field f : clazz.getDeclaredFields())
			{
				String name = f.getName()  + " (" + getArrayName(f.getType()) + ")";
				Object inst = null;
				if(Modifier.isStatic(f.getModifiers()) || instance != null)
					try
					{
						boolean old = f.isAccessible();
						f.setAccessible(true);
						if((f.getType().isPrimitive() || String.class.isAssignableFrom(f.getType()) || Enum.class.isAssignableFrom(f.getType()))
							&& f.get(instance) != null)
							name += " = " + f.get(instance);
						inst = f.get(instance);
						f.setAccessible(old);
					}catch(Exception e)
					{
						System.out.println("[DEBUG] ERROR GETTING FIELD VALUE (" + f.getName() + " @ " + clazz.getName() + ")");
					}
				FieldTreeNode node = new FieldTreeNode(name, f, inst, !f.getType().isPrimitive());
				top.add(node);
				if(!((String)node.getUserObject()).contains(filter))
					node.filtered = true;
			}
			if(showAllFields)
			{
				Class<?> clazz1 = clazz.getSuperclass();
				while(clazz1 != null)
				{
					for(Field f : clazz1.getDeclaredFields())
					{
						String name = f.getName()  + " (" + getArrayName(f.getType()) + ")";
						Object inst = null;
						if(!Modifier.isStatic(f.getModifiers()) || instance != null)
							try
							{
								boolean old = f.isAccessible();
								f.setAccessible(true);
								if((f.getType().isPrimitive() || String.class.isAssignableFrom(f.getType()) || Enum.class.isAssignableFrom(f.getType()))
									&& f.get(instance) != null)
									name += " = " + f.get(instance);
								inst = f.get(instance);
								f.setAccessible(old);
							}catch(Exception e)
							{
								System.out.println("[DEBUG] ERROR GETTING FIELD VALUE (" + f.getName() + " @ " + clazz1.getName() + ")");
							}
						FieldTreeNode node = new FieldTreeNode(name, f, inst, !f.getType().isPrimitive());
						top.add(node);
						if(!((String)node.getUserObject()).contains(filter))
							node.filtered = true;
					}
					clazz1 = clazz1.getSuperclass();
				}
			}
			((DefaultTreeModel)tree.getModel()).reload();
			tree.collapsePath(tree.getPathForRow(0));
			tableClazz = clazz;
			tableValue = instance;
			reloadFields(tableClazz, tableClazz);
			reloadMethods(tableClazz, tableClazz);
		}
		
		private void reloadFields(Class<?> clazz, Class<?> owner)
		{
			DefaultTableModel model = (DefaultTableModel)fieldsTable.getModel();
			for(int i = model.getRowCount() - 1; i >= 0; i--)
				model.removeRow(i);
			fieldsTable.setAutoCreateRowSorter(false);
			fieldsTable.setRowSorter(null);
			if(clazz.isArray())
			{
				if(tableValue != null)
					for(int i = 0; i < Array.getLength(tableValue); i++)
					{
						Object o = Array.get(tableValue, i);
						if(clazz.getComponentType().isPrimitive() || String.class.isAssignableFrom(clazz.getComponentType())
							|| Enum.class.isAssignableFrom(clazz.getComponentType()))
							model.addRow(new Object[] {editable, publicField, new Data(new Object[] {owner, tableValue, i, o == null, clazz}), o});
						else if(getRealComponentType(clazz.getComponentType()).isPrimitive() 
							|| String.class.isAssignableFrom(getRealComponentType(clazz.getComponentType())) 
							|| Enum.class.isAssignableFrom(getRealComponentType(clazz.getComponentType())))
							model.addRow(new Object[] {editable, publicField, new Data(new Object[] {owner, tableValue, i, o == null, clazz}), null});
						else
							model.addRow(new Object[] {null, publicField, new Data(new Object[] {owner, tableValue, i, o == null, clazz}), null});
					}
				return;
			}
			for(Field f : clazz.getDeclaredFields())
			{
				String inst = null;
				ImageIcon icon = null;
				boolean nullValue = false;
				if(Modifier.isStatic(f.getModifiers()) || tableValue != null)
					try
					{
						boolean old = f.isAccessible();
						f.setAccessible(true);
						Object value = f.get(tableValue);
						if(value == null)
							nullValue = true;
						if(getRealComponentType(f.getType()).isPrimitive() || String.class.isAssignableFrom(getRealComponentType(f.getType())) 
							|| Enum.class.isAssignableFrom(getRealComponentType(f.getType())))
						{
							icon = editable;
							if(value != null && (f.getType().isPrimitive() 
								|| String.class.isAssignableFrom(f.getType()) || Enum.class.isAssignableFrom(f.getType())))
								inst = String.valueOf(value);
							else if(value != null)
								inst = getArrayName(value.getClass());
						}
						f.setAccessible(old);
					}catch(Exception e)
					{
						System.out.println("[DEBUG] ERROR GETTING FIELD VALUE (" + f.getName() + " @ " + clazz.getName() + ")");
					}
				ImageIcon fieldIcon = null;
				if(Modifier.isPublic(f.getModifiers()))
					fieldIcon = publicField;
				else if (Modifier.isPrivate(f.getModifiers()))
					fieldIcon = privateField;
				else if (Modifier.isProtected(f.getModifiers()))
					fieldIcon = protectedField;
				else
					fieldIcon = defaultField;
				if(Modifier.isTransient(f.getModifiers()))
					fieldIcon = combineAccess(fieldIcon, transientCo, 2);
				boolean placedFinal = false;
				boolean placedVolatile = false;
				boolean placedStatic = false;
				if(Modifier.isFinal(f.getModifiers()))
				{
					fieldIcon = combineAccess(fieldIcon, finalCo, 0);
					placedFinal = true;
				}else if(Modifier.isVolatile(f.getModifiers()))
				{
					fieldIcon = combineAccess(fieldIcon, volatileCo, 0);
					placedVolatile = true;
				}else if(Modifier.isStatic(f.getModifiers()))
				{
					fieldIcon = combineAccess(fieldIcon, staticCo, 0);
					placedStatic = true;
				}
				if(Modifier.isFinal(f.getModifiers()) && !placedFinal)
					fieldIcon = combineAccess(fieldIcon, finalCo, 1);
				else if(Modifier.isVolatile(f.getModifiers()) && !placedVolatile)
					fieldIcon = combineAccess(fieldIcon, volatileCo, 1);
				else if(Modifier.isStatic(f.getModifiers()) && !placedStatic)
					fieldIcon = combineAccess(fieldIcon, staticCo, 1);
				model.addRow(new Object[] {icon, fieldIcon, new Data(new Object[] {owner, f, tableValue, nullValue}), inst});
			}
			if(showAllFields)
			{
				clazz = clazz.getSuperclass();
				while(clazz != null)
				{
					for(Field f : clazz.getDeclaredFields())
					{
						String inst = null;
						ImageIcon icon = null;
						boolean nullValue = false;
						if(Modifier.isStatic(f.getModifiers()) || tableValue != null)
							try
							{
								boolean old = f.isAccessible();
								f.setAccessible(true);
								Object value = f.get(tableValue);
								if(value == null)
									nullValue = true;
								if(getRealComponentType(f.getType()).isPrimitive() || String.class.isAssignableFrom(getRealComponentType(f.getType())) 
									|| Enum.class.isAssignableFrom(getRealComponentType(f.getType())))
								{
									icon = editable;
									if(value != null && (f.getType().isPrimitive() 
										|| String.class.isAssignableFrom(f.getType()) || Enum.class.isAssignableFrom(f.getType())))
										inst = String.valueOf(value);
									else if(value != null)
										inst = getArrayName(value.getClass());
								}
								f.setAccessible(old);
							}catch(Exception e)
							{
								System.out.println("[DEBUG] ERROR GETTING FIELD VALUE (" + f.getName() + " @ " + clazz.getName() + ")");
							}
						ImageIcon fieldIcon = null;
						if(Modifier.isPublic(f.getModifiers()))
							fieldIcon = publicField;
						else if (Modifier.isPrivate(f.getModifiers()))
							fieldIcon = privateField;
						else if (Modifier.isProtected(f.getModifiers()))
							fieldIcon = protectedField;
						else
							fieldIcon = defaultField;
						if(Modifier.isTransient(f.getModifiers()))
							fieldIcon = combineAccess(fieldIcon, transientCo, 2);
						boolean placedFinal = false;
						boolean placedVolatile = false;
						boolean placedStatic = false;
						if(Modifier.isFinal(f.getModifiers()))
						{
							fieldIcon = combineAccess(fieldIcon, finalCo, 0);
							placedFinal = true;
						}else if(Modifier.isVolatile(f.getModifiers()))
						{
							fieldIcon = combineAccess(fieldIcon, volatileCo, 0);
							placedVolatile = true;
						}else if(Modifier.isStatic(f.getModifiers()))
						{
							fieldIcon = combineAccess(fieldIcon, staticCo, 0);
							placedStatic = true;
						}
						if(Modifier.isFinal(f.getModifiers()) && !placedFinal)
							fieldIcon = combineAccess(fieldIcon, finalCo, 1);
						else if(Modifier.isVolatile(f.getModifiers()) && !placedVolatile)
							fieldIcon = combineAccess(fieldIcon, volatileCo, 1);
						else if(Modifier.isStatic(f.getModifiers()) && !placedStatic)
							fieldIcon = combineAccess(fieldIcon, staticCo, 1);
						model.addRow(new Object[] {fieldIcon, icon, new Data(new Object[] {owner, f, tableValue, nullValue}), inst});
					}
					clazz = clazz.getSuperclass();
				}
			}
			fieldsTable.setAutoCreateRowSorter(true);
		}
		
		private void reloadMethods(Class<?> clazz, Class<?> owner)
		{
			DefaultTableModel model = (DefaultTableModel)methodsTable.getModel();
			for(int i = model.getRowCount() - 1; i >= 0; i--)
				model.removeRow(i);
			methodsTable.setAutoCreateRowSorter(false);
			methodsTable.setRowSorter(null);
			for(Method m : clazz.getDeclaredMethods())
			{
				boolean cannotRun = false;
				if(!Modifier.isStatic(m.getModifiers()) && tableValue == null)
					cannotRun = true;
				if(!cannotRun)
					for(Class<?> arg : m.getParameterTypes())
						if(!getRealComponentType(arg).isPrimitive() && !String.class.isAssignableFrom(getRealComponentType(arg)) 
							&& !Enum.class.isAssignableFrom(getRealComponentType(arg)))
						{
							cannotRun = true;
							break;
						}
				ImageIcon methodIcon = null;
				if(Modifier.isPublic(m.getModifiers()))
					methodIcon = publicMethod;
				else if (Modifier.isPrivate(m.getModifiers()))
					methodIcon = privateMethod;
				else if (Modifier.isProtected(m.getModifiers()))
					methodIcon = protectedMethod;
				else
					methodIcon = defaultMethod;
				if(Modifier.isSynchronized(m.getModifiers()))
					methodIcon = combineAccess(methodIcon, synchronizedCo, 2);
				boolean placedAbstract = false;
				boolean placedFinal = false;
				boolean placedStatic = false;
				boolean placedNative = false;
				boolean placedDefault = false;
				if(Modifier.isAbstract(m.getModifiers()))
				{
					methodIcon = combineAccess(methodIcon, abstractCo, 0);
					placedAbstract = true;
				}else if(Modifier.isFinal(m.getModifiers()))
				{
					methodIcon = combineAccess(methodIcon, finalCo, 0);
					placedFinal = true;
				}else if(Modifier.isStatic(m.getModifiers()))
				{
					methodIcon = combineAccess(methodIcon, staticCo, 0);
					placedStatic = true;
				}else if(Modifier.isNative(m.getModifiers()))
				{
					methodIcon = combineAccess(methodIcon, nativeCo, 0);
					placedNative = true;
				}else if(m.isDefault())
				{
					methodIcon = combineAccess(methodIcon, defaultCo, 0);
					placedDefault = true;
				}
				if(Modifier.isAbstract(m.getModifiers()) && !placedAbstract)
					methodIcon = combineAccess(methodIcon, abstractCo, 1);
				else if(Modifier.isFinal(m.getModifiers()) && !placedFinal)
					methodIcon = combineAccess(methodIcon, finalCo, 1);
				else if(Modifier.isStatic(m.getModifiers()) && !placedStatic)
					methodIcon = combineAccess(methodIcon, staticCo, 1);
				else if(Modifier.isNative(m.getModifiers()) && !placedNative)
					methodIcon = combineAccess(methodIcon, nativeCo, 1);
				else if(m.isDefault() && !placedDefault)
					methodIcon = combineAccess(methodIcon, defaultCo, 1);
				model.addRow(new Object[] {cannotRun ? null : runnable, methodIcon, new Data(new Object[] {owner, m, tableValue})});
			}
			if(showAllMethods)
			{
				clazz = clazz.getSuperclass();
				while(clazz != null)
				{
					for(Method m : clazz.getDeclaredMethods())
					{
						boolean cannotRun = false;
						if(!Modifier.isStatic(m.getModifiers()) && tableValue == null)
							cannotRun = true;
						if(!cannotRun)
							for(Class<?> arg : m.getParameterTypes())
								if(!getRealComponentType(arg).isPrimitive() && !String.class.isAssignableFrom(getRealComponentType(arg)) 
									&& !Enum.class.isAssignableFrom(getRealComponentType(arg)))
								{
									cannotRun = true;
									break;
								}
						ImageIcon methodIcon = null;
						if(Modifier.isPublic(m.getModifiers()))
							methodIcon = publicMethod;
						else if (Modifier.isPrivate(m.getModifiers()))
							methodIcon = privateMethod;
						else if (Modifier.isProtected(m.getModifiers()))
							methodIcon = protectedMethod;
						else
							methodIcon = defaultMethod;
						if(Modifier.isSynchronized(m.getModifiers()))
							methodIcon = combineAccess(methodIcon, synchronizedCo, 2);
						boolean placedAbstract = false;
						boolean placedFinal = false;
						boolean placedStatic = false;
						boolean placedNative = false;
						boolean placedDefault = false;
						if(Modifier.isAbstract(m.getModifiers()))
						{
							methodIcon = combineAccess(methodIcon, abstractCo, 0);
							placedAbstract = true;
						}else if(Modifier.isFinal(m.getModifiers()))
						{
							methodIcon = combineAccess(methodIcon, finalCo, 0);
							placedFinal = true;
						}else if(Modifier.isStatic(m.getModifiers()))
						{
							methodIcon = combineAccess(methodIcon, staticCo, 0);
							placedStatic = true;
						}else if(Modifier.isNative(m.getModifiers()))
						{
							methodIcon = combineAccess(methodIcon, nativeCo, 0);
							placedNative = true;
						}else if(m.isDefault())
						{
							methodIcon = combineAccess(methodIcon, defaultCo, 0);
							placedDefault = true;
						}
						if(Modifier.isAbstract(m.getModifiers()) && !placedAbstract)
							methodIcon = combineAccess(methodIcon, abstractCo, 1);
						else if(Modifier.isFinal(m.getModifiers()) && !placedFinal)
							methodIcon = combineAccess(methodIcon, finalCo, 1);
						else if(Modifier.isStatic(m.getModifiers()) && !placedStatic)
							methodIcon = combineAccess(methodIcon, staticCo, 1);
						else if(Modifier.isNative(m.getModifiers()) && !placedNative)
							methodIcon = combineAccess(methodIcon, nativeCo, 1);
						else if(m.isDefault() && !placedDefault)
							methodIcon = combineAccess(methodIcon, defaultCo, 1);
						model.addRow(new Object[] {cannotRun ? null : runnable, methodIcon, new Data(new Object[] {owner, m, tableValue})});
					}
					clazz = clazz.getSuperclass();
				}
			}
			methodsTable.setAutoCreateRowSorter(true);
		}

		public void deleteChildren(HideableTreeNode node)
		{
		    for(int i = 0; i < node.getChildCountFilterless(); i++)
		        deleteChildren((HideableTreeNode)node.getChildAtFilterless(i));
		    node.removeAllChildren();
		}
		
		private ImageIcon combineAccess(ImageIcon icon1, ImageIcon icon2, int mode) 
		{
			Image img1 = icon1.getImage();
			Image img2 = icon2.getImage();

			int w = icon1.getIconWidth();
			int h = icon1.getIconHeight();
			BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = image.createGraphics();
			g2.drawImage(img1, 0, 0, null);
			switch(mode)
			{
				case 0:
					g2.drawImage(img2, w / 4, h / -4, null);
					break;
				case 1:
					g2.drawImage(img2, w / -4, h / -4, null);
					break;
				case 2:
					g2.drawImage(img2, w / 4, h / 4, null);
					break;
			}
			g2.dispose();
			
			return new ImageIcon(image);
		}
		
		private class MyTable extends JTable
		{
			private int tablemode;
			
			public MyTable(int numRows, int numColumns, int tablemode)
			{
				super(numRows, numColumns);
				this.tablemode = tablemode;
			}

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
			{
				Component returnComp = super.prepareRenderer(renderer, row, column);
				if(!returnComp.getBackground().equals(getSelectionBackground()))
					returnComp.setBackground(row % 2 == 0 ? UIManager.getColor("Panel.background") : Color.WHITE);
				if(column == 3 && tablemode == 1 && ((Data)getValueAt(row, 2)).data[3].equals(true) && selectionModel.getLeadSelectionIndex() != row)
					returnComp.setBackground(Color.RED);
				if(column == 4 && tablemode == 2 && ((AtomicBoolean)getValueAt(row, 4)).get() == true && selectionModel.getLeadSelectionIndex() != row)
					returnComp.setBackground(Color.GREEN);
				return returnComp;
			}
			
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		}
	}

	private class HideableTreeNode extends DefaultMutableTreeNode
	{
		public boolean filtered;
		
		public HideableTreeNode(String name, boolean primitive)
		{
			super(name, primitive);
		}
		
		public TreeNode getChildAtFilterless(int index)
		{
			return super.getChildAt(index);
		}
		
		@Override
		public TreeNode getChildAt(int index)
		{
			if(children == null)
				throw new ArrayIndexOutOfBoundsException("node has no children");
		    int realIndex = -1;
		    int visibleIndex = -1;
		    Enumeration<?> e = children.elements();
		    while(e.hasMoreElements())
		    {
		    	HideableTreeNode node = (HideableTreeNode)e.nextElement();
		    	if(!node.filtered)
		    		visibleIndex++;
		    	realIndex++;
		    	if(visibleIndex == index)
		    		return (TreeNode)children.elementAt(realIndex);
		    }
		    throw new ArrayIndexOutOfBoundsException("index unmatched");
		}
		
		public int getChildCountFilterless()
		{
			return super.getChildCount();
		}
		
		@Override
		public int getChildCount() 
		{
			if(children == null)
				return 0;
			int count = 0;
			Enumeration<?> e = children.elements();
			while(e.hasMoreElements())
			{
				HideableTreeNode node = (HideableTreeNode)e.nextElement();
				if(!node.filtered)
					count++;
			}
			return count;
		}
		
		@Override
		public void removeAllChildren()
		{
			for(int i = getChildCountFilterless() - 1; i >= 0; i--)
			{
				HideableTreeNode node = (HideableTreeNode)getChildAtFilterless(i);
				children.removeElementAt(i);
				node.setParent(null);
			}
		}
		
		@Override
		public void add(MutableTreeNode newChild)
		{
			if(newChild != null && newChild.getParent() == this)
				insert(newChild, getChildCountFilterless() - 1);
			else
				insert(newChild, getChildCountFilterless());
		}
	}
	
	private class FieldTreeNode extends HideableTreeNode
	{
		public Class<?> type;
		public Class<?> owner;
		public Field field;
		public Object value;
		private boolean loaded;
		
		public FieldTreeNode(String name, Field field, Object value, boolean primitive)
		{
			super(name, primitive);
			this.field = field;
			this.value = value;
			if(field.getType().isArray())
				owner = field.getDeclaringClass();
			else
				owner = field.getType();
		}
		
		public FieldTreeNode(String name, Class<?> type, Class<?> owner, Object value, boolean primitive)
		{
			super(name, primitive);
			this.type = type;
			this.value = value;
			this.owner = owner;
		}
		
		public void loadChildren(String filter)
		{
			if(!loaded)
			{
				loaded = true;
				Class<?> clazz;
				if(field != null)
					clazz = field.getType();
				else
					clazz = type;
				if(clazz.isArray())
				{
					if(value != null)
						for(int i = 0; i < Array.getLength(value); i++)
						{
							Object o = Array.get(value, i);
							String name = "(" + getArrayName(clazz.getComponentType()) + ")";
							if((clazz.getComponentType().isPrimitive() 
								|| String.class.isAssignableFrom(clazz.getComponentType()) || Enum.class.isAssignableFrom(clazz.getComponentType()))
								&& o != null)
								name += " = " + o;
							FieldTreeNode node = new FieldTreeNode(name, clazz.getComponentType(), 
								owner, o, !clazz.getComponentType().isPrimitive());
							add(node);
							if(!((String)node.getUserObject()).contains(filter))
								node.filtered = true;
						}
					return;
				}
				for(Field f : clazz.getDeclaredFields())
				{
					String name = f.getName()  + " (" + getArrayName(f.getType()) + ")";
					Object inst = null;
					if(Modifier.isStatic(f.getModifiers()) || value != null)
						try
						{
							boolean old = f.isAccessible();
							f.setAccessible(true);
							if((f.getType().isPrimitive() || String.class.isAssignableFrom(f.getType()) || Enum.class.isAssignableFrom(f.getType()))
								&& f.get(value) != null)
								name += " = " + f.get(value);
							inst = f.get(value);
							f.setAccessible(old);
						}catch(Exception e)
						{
							System.out.println("[DEBUG] ERROR GETTING FIELD VALUE (" + f.getName() + " @ " + clazz.getName() + ")");
						}
					FieldTreeNode node = new FieldTreeNode(name, f, inst, !f.getType().isPrimitive());
					add(node);
					if(!((String)node.getUserObject()).contains(filter))
						node.filtered = true;
				}
				if(showAllFields)
				{
					clazz = clazz.getSuperclass();
					while(clazz != null)
					{
						for(Field f : clazz.getDeclaredFields())
						{
							String name = f.getName()  + " (" + getArrayName(f.getType()) + ")";
							Object inst = null;
							if(Modifier.isStatic(f.getModifiers()) || value != null)
								try
								{
									boolean old = f.isAccessible();
									f.setAccessible(true);
									if((f.getType().isPrimitive() || String.class.isAssignableFrom(f.getType()) || Enum.class.isAssignableFrom(f.getType()))
										&& f.get(value) != null)
										name += " = " + f.get(value);
									inst = f.get(value);
									f.setAccessible(old);
								}catch(Exception e)
								{
									System.out.println("[DEBUG] ERROR GETTING FIELD VALUE (" + f.getName() + " @ " + clazz.getName() + ")");
								}
							FieldTreeNode node = new FieldTreeNode(name, f, inst, !f.getType().isPrimitive());
							add(node);
							if(!((String)node.getUserObject()).contains(filter))
								node.filtered = true;
						}
						clazz = clazz.getSuperclass();
					}
				}
			}
		}
	}
	
	private class Data
	{
		public Object[] data;
		
		public Data(Object[] data)
		{
			this.data = data;
		}
		
		@Override
		public String toString()
		{
			if(data[1] instanceof Field)
				return ((Field)data[1]).getName();
			else if(data[1] instanceof Method)
				return ((Method)data[1]).getName();
			return data[1].toString();
		}
	}
	
	private class ListButton extends JButton
	{
		public Object array;

		public ListButton(String text, Object array, Class<?> type, JPanel parent)
		{
			super(text);
			this.array = array;
			addActionListener(a -> {
				List<Object> values = new ArrayList<>();
				for(int i = 0; i < Array.getLength(this.array); i++)
				{
					Object o = Array.get(this.array, i);
					values.add(o);
				}
				JPanel panel = new JPanel();
				panel.setBounds(100, 100, 300, 400);
				panel.setLayout(new BorderLayout());
				
				JTable jtable = new JTable()
				{
					@Override
					public boolean isCellEditable(int row, int column)
					{
						return false;
					}
				};
				jtable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
				jtable.getTableHeader().setReorderingAllowed(false);
				DefaultTableModel lm = new DefaultTableModel();
				lm.addColumn("#");
				lm.addColumn("Value");
				int i = 0;
				for(Object o : values)
				{
					lm.addRow(new Object[]{i, o});
					i++;
				}
				jtable.setModel(lm);
				jtable.getColumn("Value").setCellRenderer(new DefaultTableCellRenderer()
				{
					@Override
					public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
						boolean hasFocus, int row, int column)
					{
						if(value != null && value.getClass().isArray())
							value = getArrayName(value.getClass());
						Component cell = super.getTableCellRendererComponent(table, value, isSelected,
				            hasFocus, row, column);
						if(!cell.getBackground().equals(table.getSelectionBackground()))
							cell.setBackground(Color.WHITE);
						if(column == 1 && value == null && !isSelected)
							cell.setBackground(Color.RED);
						return cell;
					}
				});
				
				panel.add(new JScrollPane(jtable), BorderLayout.CENTER);
				JPanel actions = new JPanel();
				actions.setLayout(new GridLayout(1, 4));
				JButton add = new JButton("Add");
				add.addActionListener(a2 -> {
					Object o = null;
					try
					{
						o = editValueWindow(null, type, true);
					}catch(Exception e)
					{
						showErrorDialog("Exception while editing list value.", e);
					}
					int row = jtable.getSelectedRow();
					if(row != -1)
					{
						lm.insertRow(row, new Object[]{-1, o});
						values.add(row, o);
						recalcIndex(lm);
					}else
					{
						lm.addRow(new Object[]{lm.getRowCount(), o});
						values.add(o);
					}
					jtable.repaint();
				});
				actions.add(add);
				JButton remove = new JButton("Remove");
				remove.addActionListener(a2 -> {
					int[] selectedRows = jtable.getSelectedRows();
					if(selectedRows.length > 0)
					{
						for(int j = selectedRows.length - 1; j >= 0; j--)
						{
							lm.removeRow(selectedRows[j]);
							values.remove(selectedRows[j]);
						}
						recalcIndex(lm);
						jtable.repaint();
					}
				});
				actions.add(remove);
				JButton edit = new JButton("Edit");
				edit.addActionListener(a2 -> {
					int row = jtable.getSelectedRow();
					if(row == -1)
						return;
					Object o = values.get(row);
					try
					{
						o = editValueWindow(o, type, false);
					}catch(Exception e)
					{
						showErrorDialog("Exception while editing list value.", e);
					}
					values.add(row, o);
					values.remove(row + 1);
					lm.insertRow(row, new Object[]{row, o});
					lm.removeRow(row + 1);
					jtable.repaint();
				});
				actions.add(edit);
				
				panel.add(actions, BorderLayout.PAGE_END);
				if(JOptionPane.showConfirmDialog(parent, panel, "Edit Array",
			        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION)
				{
					this.array = Array.newInstance(type, values.size());
					for(int i1 = 0; i1 < values.size(); i1++)
					{
						Object o = values.get(i1);
						Array.set(this.array, i1, o);
					}
				}
			});
		}

		private void recalcIndex(DefaultTableModel lm)
		{
			for(int i = 0; i < lm.getRowCount(); i++)
				lm.setValueAt(i, i, 0);
		}
		
		private Object editValueWindow(Object o, Class<?> type, boolean isAdd)
		{
    		JPanel panel = new JPanel(new GridBagLayout());
    		GridBagConstraints panelC = new GridBagConstraints();
    		panelC.insets = new Insets(10, 10, 0, 0);
    		panelC.anchor = GridBagConstraints.NORTHWEST;
    		panelC.fill = GridBagConstraints.BOTH;
    		panelC.gridy = 2;
    		panelC.weightx = 1;
    		JCheckBox nullBox = new JCheckBox("Null");
    		JButton setValue = new JButton("Set Value");
    		if(type == boolean.class || Enum.class.isAssignableFrom(type))
    		{
    			GridBagConstraints textC = new GridBagConstraints();
    			textC.anchor = GridBagConstraints.NORTHWEST;
    			textC.insets = new Insets(3, 0, 10, 0);
    			panel.add(new JLabel(type == boolean.class ? "Boolean:   "  : "Enum:   "), textC);
    			JComboBox<String> comboBox = new JComboBox<>();
    			List<Field> enumFields = new ArrayList<>();
    			if(type == boolean.class)
    			{
    				comboBox.addItem("true");
    				comboBox.addItem("false");
    				if(o != null && o.equals(true))
    					comboBox.setSelectedIndex(0);
    				else
    					comboBox.setSelectedIndex(1);
    				nullBox.setEnabled(false);
    			}else
    			{
    				for(Field f : type.getDeclaredFields())
    					if(f.getType() == type && Modifier.isStatic(f.getModifiers()))
    						enumFields.add(f);
    				for(Field f : enumFields)
    					comboBox.addItem(f.getName());
    				if(o == null)
    				{
    					nullBox.setSelected(true);
    					comboBox.setEnabled(false);
    				}else
    				{
    					comboBox.setSelectedItem(((Enum<?>)o).name());
    					if(comboBox.getItemCount() == 0)
    						comboBox.addItem(((Enum<?>)o).name());
    				}
    			}
    			GridBagConstraints valueC = new GridBagConstraints();
	    		valueC.anchor = GridBagConstraints.NORTHWEST;
	    		valueC.gridx = 1;
	    		valueC.weightx = 1;
	    		panel.add(comboBox, valueC);
	    		nullBox.addActionListener(a -> {
	    			if(nullBox.isSelected())
	    				comboBox.setEnabled(false);
	    			else
	    				comboBox.setEnabled(true);
	    		});
    		}else if(type.isArray())
    		{
    			JLabel text = new JLabel("Value:   ");
    			GridBagConstraints textC = new GridBagConstraints();
    			textC.anchor = GridBagConstraints.NORTHWEST;
    			textC.insets = new Insets(3, 0, 10, 0);
    			panel.add(text, textC);
    			ListButton arrButton = new ListButton("Edit Array", o == null ? Array.newInstance(type.getComponentType(),
    				0) : o, type.getComponentType(), panel);
    			GridBagConstraints arrButtonC = new GridBagConstraints();
    			arrButtonC.anchor = GridBagConstraints.NORTHWEST;
    			arrButtonC.gridx = 1;
    			arrButtonC.weightx = 1;
	    		panel.add(arrButton, arrButtonC);
	    		
	    		if(o == null)
	    		{
	    			nullBox.setSelected(true);
	    			arrButton.setEnabled(false);
	    		}
	    		Class<?> compType = getRealComponentType(type);
    			if(!String.class.isAssignableFrom(compType) && !compType.isPrimitive()
    				&& !Enum.class.isAssignableFrom(compType))
    			{
    				nullBox.setEnabled(false);
    				arrButton.setEnabled(false);
    				setValue.setEnabled(false);
    			}
	    		nullBox.addActionListener(a -> {
	    			if(nullBox.isSelected())
						arrButton.setEnabled(false);
					else
						arrButton.setEnabled(true);
	    		});
    		}else
    		{
    			JLabel text = new JLabel("Value:   ");
    			GridBagConstraints textC = new GridBagConstraints();
    			textC.anchor = GridBagConstraints.NORTHWEST;
    			textC.insets = new Insets(3, 0, 10, 0);
    			panel.add(text, textC);
    			JTextField textField = new JTextField(20);
    			GridBagConstraints valueC = new GridBagConstraints();
	    		valueC.anchor = GridBagConstraints.NORTHWEST;
	    		valueC.gridx = 1;
	    		valueC.weightx = 1;
	    		panel.add(textField, valueC);
	    		
	    		if(o == null)
	    		{
	    			nullBox.setSelected(true);
	    			textField.setEnabled(false);
	    			if(!String.class.isAssignableFrom(type) && !type.isPrimitive())
	    			{
	    				setValue.setEnabled(false);
	    				nullBox.setEnabled(false);
	    			}
	    		}else if(String.class.isAssignableFrom(type))
	    			textField.setText(o.toString());
	    		else if(type.isPrimitive())
	    		{
	    			textField.setText(o.toString());
	    			nullBox.setEnabled(false);
	    		}else
	    		{
	    			nullBox.setEnabled(false);
	    			textField.setEnabled(false);
	    			setValue.setEnabled(false);
	    			text.setEnabled(false);
	    		}
	    		nullBox.addActionListener(a -> {
	    			if(nullBox.isSelected())
						textField.setEnabled(false);
					else
						textField.setEnabled(true);
	    		});
    		}
    		
    		GridBagConstraints nullBoxC = new GridBagConstraints();
    		nullBoxC.insets = new Insets(0, 10, 0, 0);
    		nullBoxC.anchor = GridBagConstraints.NORTHWEST;
    		nullBoxC.gridy = 2;
    		panel.add(nullBox, nullBoxC);
    		
    		if(JOptionPane.showConfirmDialog(null, panel, isAdd ? "Add " + getArrayName(type) : 
    			"Edit " + getArrayName(type),
		        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION)
			{
				JCheckBox box = (JCheckBox)panel.getComponent(2);
				if(box.isSelected())
					return null;
				else if(panel.getComponent(1) instanceof JTextField)
				{
					JTextField field = (JTextField)panel.getComponent(1);
					if(type == byte.class)
						return Byte.parseByte(field.getText());
	    			if(type == short.class)
	    				return Short.parseShort(field.getText());
	    			if(type == int.class)
	    				return Integer.parseInt(field.getText());
	    			if(type == long.class)
	    				return Long.parseLong(field.getText());
	    			if(type == char.class)
	    			{
	    				if(field.getText().length() == 1)
	    					throw new IllegalArgumentException("Length of char must be 1");
	    				return field.getText().charAt(0);
	    			}
	    			if(type == float.class)
	    				return Float.parseFloat(field.getText());
	    			if(type == double.class)
	    				return Double.parseDouble(field.getText());
	    			if(String.class.isAssignableFrom(type))
	    				return field.getText();
	    			throw new IllegalStateException(type.getName());
				}else if(panel.getComponent(1) instanceof JComboBox)
				{
					JComboBox<?> comboBox = (JComboBox<?>)panel.getComponent(1);
					if(type == boolean.class)
    					return comboBox.getSelectedIndex() == 0;
    				if(Enum.class.isAssignableFrom(type))
    				{
    					List<Field> enumFields = new ArrayList<>();
    					for(Field f : type.getDeclaredFields())
	    					if(f.getType() == type && Modifier.isStatic(f.getModifiers()))
	    						enumFields.add(f);
    					if(comboBox.getSelectedIndex() < enumFields.size() && comboBox.getSelectedIndex() != -1)
    					{
    						try
    						{
	    						Field modifiersField = Field.class.getDeclaredField("modifiers");
								modifiersField.setAccessible(true);
	    						boolean accessible = false;
								int modifier = 0;
								accessible = enumFields.get(comboBox.getSelectedIndex()).isAccessible();
								modifier = enumFields.get(comboBox.getSelectedIndex()).getModifiers();
								enumFields.get(comboBox.getSelectedIndex()).setAccessible(true);
								modifiersField.setInt(enumFields.get(comboBox.getSelectedIndex()), 
									enumFields.get(comboBox.getSelectedIndex()).getModifiers() & ~Modifier.FINAL);
								Object toReturn = enumFields.get(comboBox.getSelectedIndex()).get(null);
	    						enumFields.get(comboBox.getSelectedIndex()).setAccessible(accessible);
	    						modifiersField.setInt(enumFields.get(comboBox.getSelectedIndex()), modifier);
								modifiersField.setAccessible(false);
								return toReturn;
    						}catch(Exception ex)
    						{
    							showErrorDialog("Exception occurred while choosing Enum, using null instead", ex);
    							return null;
    						}
    					}else
    						return null;
    				}else
    					throw new IllegalStateException(type.getName());
				}else if(panel.getComponent(1) instanceof ListButton)
					return ((ListButton)panel.getComponent(1)).array;
				return o;
			}else
				return o;
		}
	}
	
	public class TableButtonRenderer extends JButton implements TableCellRenderer
	{
		public TableButtonRenderer()
		{
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
		{
			if(isSelected)
			{
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			}else
			{
				setForeground(table.getForeground());
				setBackground(UIManager.getColor("Button.background"));
			}
			setText(((Data)value).data[1].toString());
			return this;
		}
	}
}
