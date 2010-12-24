/*
 *  soapUI, copyright (C) 2004-2010 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.security.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.Autoscroll;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;

import com.eviware.soapui.impl.wsdl.actions.testsuite.AddNewTestCaseAction;
import com.eviware.soapui.impl.wsdl.panels.support.ProgressBarTestCaseAdapter;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStep;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.support.TestSuiteListenerAdapter;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestSuiteListener;
import com.eviware.soapui.security.SecurityTest;
import com.eviware.soapui.security.check.AbstractSecurityCheck;
import com.eviware.soapui.security.check.SecurityCheck;
import com.eviware.soapui.security.support.ProgressBarSecurityTestAdapter;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.swing.ActionList;
import com.eviware.soapui.support.action.swing.ActionListBuilder;
import com.eviware.soapui.support.action.swing.ActionSupport;
import com.eviware.soapui.support.action.swing.SwingActionDelegate;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.swing.AutoscrollSupport;

/**
 * A panel showing a scrollable list of TestSteps in a SecurityTest.
 * 
 * @author dragica.soldo
 */

public class JSecurityTestTestStepList extends JPanel
{
	private Map<TestStep, TestStepListEnrtyPanel> panels = new HashMap<TestStep, TestStepListEnrtyPanel>();
	private final SecurityTest securityTest;
	private final TestSuiteListener testSuiteListener = new InternalTestSuiteListener();
	private TestStepListEnrtyPanel selectedTestStep;
	// private JInspectorPanel inspectorPanel;
	private JList securityChecksList;
	JSplitPane splitPane;
	JComponentInspector<JComponent> securityChecksInspector;
	private JComponent secCheckPanel;
	JPanel testStepListPanel;

	public JSecurityTestTestStepList( SecurityTest securityTest )
	{
		testStepListPanel = new JPanel( new BorderLayout() );
		this.securityTest = securityTest;
		testStepListPanel.setLayout( new BoxLayout( testStepListPanel, BoxLayout.Y_AXIS ) );

		for( int c = 0; c < securityTest.getTestCase().getTestStepCount(); c++ )
		{
			TestStepListEnrtyPanel testStepListEntryPanel = createTestStepListPanel( securityTest.getTestCase()
					.getTestStepAt( c ) );
			panels.put( securityTest.getTestCase().getTestStepAt( c ), testStepListEntryPanel );
			testStepListPanel.add( testStepListEntryPanel );
		}
		testStepListPanel.add( Box.createVerticalGlue() );

		secCheckPanel = buildSecurityChecksPanel();
		splitPane = UISupport
				.createVerticalSplit( new JScrollPane( testStepListPanel ), new JScrollPane( secCheckPanel ) );
		splitPane.setPreferredSize( new Dimension( 600, 400 ) );
		splitPane.setDividerLocation( 0.5 );
		splitPane.setResizeWeight( 0.6 );

		add( splitPane, BorderLayout.CENTER );
		securityTest.getTestCase().getTestSuite().addTestSuiteListener( testSuiteListener );

	}

	public SecurityCheck getCurrentSecurityCheck()
	{
		int ix = securityChecksList.getSelectedIndex();
		return ix == -1 ? null : securityTest.getTestStepSecurityCheckAt( selectedTestStep.getTestStep().getId(), ix );
	}

	// TODO see how to change the model for this to work...which class should
	// implement securable
	protected JPanel buildSecurityChecksPanel()
	{
		if( selectedTestStep != null && AbstractSecurityCheck.isSecurable( selectedTestStep.getTestStep() ) )
		{
			return new SecurityChecksPanel( selectedTestStep.getTestStep(), securityTest );
		}
		else
		{
			return new JPanel();
		}
	}

	protected JComponent buildSecurityChecksInspector()
	{
		JPanel p = new JPanel( new BorderLayout() );
		return p;
	}

	public void reset()
	{
		for( TestStepListEnrtyPanel testCasePanel : panels.values() )
		{
			testCasePanel.reset();
		}
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		securityTest.getTestCase().getTestSuite().addTestSuiteListener( testSuiteListener );
	}

	@Override
	public void removeNotify()
	{
		super.removeNotify();
		securityTest.getTestCase().getTestSuite().removeTestSuiteListener( testSuiteListener );
	}

	private final class InternalTestSuiteListener extends TestSuiteListenerAdapter
	{
		@Override
		public void testStepAdded( TestStep testStep, int index )
		{
			TestStepListEnrtyPanel testStepListEntry = createTestStepListPanel( testStep );
			panels.put( testStep, testStepListEntry );
			testStepListPanel.add( testStepListEntry, index );
			splitPane.remove( splitPane.getTopComponent() );
			splitPane.setTopComponent( new JScrollPane( testStepListPanel ) );
			revalidate();
			repaint();
		}

		@Override
		public void testStepRemoved( TestStep testStep, int index )
		{
			TestStepListEnrtyPanel testCaseListPanel = panels.get( testStep );
			if( testCaseListPanel != null )
			{
				remove( testCaseListPanel );
				TestStepListEnrtyPanel testStepListEntry = panels.remove( testStep );
				testStepListPanel.remove( testStepListEntry );
				splitPane.remove( splitPane.getTopComponent() );
				splitPane.setTopComponent( new JScrollPane( testStepListPanel ) );
				revalidate();
				repaint();
			}
		}

		@Override
		public void testStepMoved( TestStep testStep, int index, int offset )
		{
			TestStepListEnrtyPanel testStepListEntry = panels.get( testStep );
			if( testStepListEntry != null )
			{
				boolean hadFocus = testStepListEntry.hasFocus();

				testStepListPanel.remove( testStepListEntry );
				testStepListPanel.add( testStepListEntry, index + offset );
				splitPane.remove( splitPane.getTopComponent() );
				splitPane.setTopComponent( new JScrollPane( testStepListPanel ) );

				revalidate();
				repaint();

				if( hadFocus )
					testStepListEntry.requestFocus();
			}
		}
	}

	public final class TestStepListEnrtyPanel extends JPanel implements Autoscroll
	{
		private final WsdlTestStep testStep;
		private JProgressBar progressBar;
		private JLabel label;
		private ProgressBarSecurityTestAdapter progressBarAdapter;
		private TestCasePropertyChangeListener testCasePropertyChangeListener;
		private AutoscrollSupport autoscrollSupport;

		public TestStepListEnrtyPanel( WsdlTestStep testStep )
		{
			super( new BorderLayout() );

			setFocusable( true );

			this.testStep = testStep;
			autoscrollSupport = new AutoscrollSupport( this );

			progressBar = new JProgressBar()
			{
				protected void processMouseEvent( MouseEvent e )
				{
					if( e.getID() == MouseEvent.MOUSE_PRESSED || e.getID() == MouseEvent.MOUSE_RELEASED )
					{
						TestStepListEnrtyPanel.this.processMouseEvent( translateMouseEvent( e ) );
					}
				}

				protected void processMouseMotionEvent( MouseEvent e )
				{
					TestStepListEnrtyPanel.this.processMouseMotionEvent( translateMouseEvent( e ) );
				}

				/**
				 * Translates the given mouse event to the enclosing map panel's
				 * coordinate space.
				 */
				private MouseEvent translateMouseEvent( MouseEvent e )
				{
					return new MouseEvent( TestStepListEnrtyPanel.this, e.getID(), e.getWhen(), e.getModifiers(), e.getX()
							+ getX(), e.getY() + getY(), e.getClickCount(), e.isPopupTrigger(), e.getButton() );
				}
			};

			JPanel progressPanel = UISupport.createProgressBarPanel( progressBar, 5, false );

			progressBar.setMinimumSize( new Dimension( 0, 100 ) );
			progressBar.setBackground( Color.WHITE );
			progressBar.setInheritsPopupMenu( true );

			label = new JLabel( testStep.getLabel(), SwingConstants.LEFT );
			label.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
			label.setInheritsPopupMenu( true );
			label.setEnabled( !testStep.isDisabled() );

			add( progressPanel, BorderLayout.CENTER );
			add( label, BorderLayout.NORTH );

			testCasePropertyChangeListener = new TestCasePropertyChangeListener();

			// initPopup( testStep );

			addMouseListener( new MouseAdapter()
			{

				@Override
				public void mousePressed( MouseEvent e )
				{
					requestFocus();
					selectedTestStep = TestStepListEnrtyPanel.this;
					splitPane.remove( secCheckPanel );
					secCheckPanel = buildSecurityChecksPanel();
					secCheckPanel.revalidate();
					splitPane.setBottomComponent( secCheckPanel );
					splitPane.revalidate();
				}

				public void mouseClicked( MouseEvent e )
				{
					if( e.getClickCount() < 2 )
					{
						if( selectedTestStep != null )
							selectedTestStep.setSelected( false );

						setSelected( true );
						selectedTestStep = TestStepListEnrtyPanel.this;
					}
					else
					{
						UISupport.selectAndShow( TestStepListEnrtyPanel.this.testStep );
					}
				}
			} );

			addKeyListener( new TestCaseListPanelKeyHandler() );

			// init border
			setSelected( false );
		}

		public void reset()
		{
			progressBar.setValue( 0 );
			progressBar.setString( "" );
		}

		private void initPopup( WsdlTestStep testStep )
		{
			ActionList actions = ActionListBuilder.buildActions( testStep );
			actions.insertAction( SwingActionDelegate.createDelegate( AddNewTestCaseAction.SOAPUI_ACTION_ID, securityTest,
					null, null ), 0 );
			actions.insertAction( ActionSupport.SEPARATOR_ACTION, 1 );

			setComponentPopupMenu( ActionSupport.buildPopup( actions ) );
		}

		public void addNotify()
		{
			super.addNotify();
			testStep.addPropertyChangeListener( testCasePropertyChangeListener );
			// TODO check
			progressBarAdapter = new ProgressBarSecurityTestAdapter( progressBar, securityTest );
		}

		public void removeNotify()
		{
			super.removeNotify();
			if( progressBarAdapter != null )
			{
				testStep.removePropertyChangeListener( testCasePropertyChangeListener );
				progressBarAdapter.release();

				progressBarAdapter = null;
			}
		}

		public Dimension getMaximumSize()
		{
			Dimension size = super.getMaximumSize();
			size.height = 50;
			return size;
		}

		public void setSelected( boolean selected )
		{
			if( selected )
			{
				setBorder( BorderFactory.createLineBorder( Color.GRAY ) );
			}
			else
			{
				setBorder( BorderFactory.createLineBorder( Color.WHITE ) );
			}
		}

		public boolean isSelected()
		{
			return selectedTestStep != null && selectedTestStep.getTestStep() == testStep;
		}

		private final class TestCasePropertyChangeListener implements PropertyChangeListener
		{
			public void propertyChange( PropertyChangeEvent evt )
			{
				if( evt.getPropertyName().equals( TestCase.LABEL_PROPERTY ) )
				{
					label.setEnabled( !testStep.isDisabled() );
					label.setText( testStep.getLabel() );
				}
				else if( evt.getPropertyName().equals( TestCase.DISABLED_PROPERTY ) )
				{
					initPopup( testStep );
				}
			}
		}

		protected TestStep getTestStep()
		{
			return testStep;
		}

		public ModelItem getModelItem()
		{
			return testStep;
		}

		public void autoscroll( Point pt )
		{
			int ix = getIndexOf( this );
			if( pt.getY() < 12 && ix > 0 )
			{
				Rectangle bounds = JSecurityTestTestStepList.this.getComponent( ix - 1 ).getBounds();
				JSecurityTestTestStepList.this.scrollRectToVisible( bounds );
			}
			else if( pt.getY() > getHeight() - 12 && ix < securityTest.getTestCase().getTestStepCount() - 1 )
			{
				Rectangle bounds = JSecurityTestTestStepList.this.getComponent( ix + 1 ).getBounds();
				JSecurityTestTestStepList.this.scrollRectToVisible( bounds );
			}
		}

		public Insets getAutoscrollInsets()
		{
			return autoscrollSupport.getAutoscrollInsets();
		}

		private final class TestCaseListPanelKeyHandler extends KeyAdapter
		{
			public void keyPressed( KeyEvent e )
			{
				if( e.getKeyChar() == KeyEvent.VK_ENTER )
				{
					UISupport.selectAndShow( testStep );
					e.consume();
				}
				else
				{
					ActionList actions = ActionListBuilder.buildActions( testStep );
					if( actions != null )
						actions.dispatchKeyEvent( e );
				}
			}
		}
	}

	protected int getIndexOf( TestStepListEnrtyPanel panel )
	{
		return Arrays.asList( getComponents() ).indexOf( panel );
	}

	protected TestStepListEnrtyPanel createTestStepListPanel( TestStep testStep )
	{
		TestStepListEnrtyPanel testStepListPanel = new TestStepListEnrtyPanel( ( WsdlTestStep )testStep );

		return testStepListPanel;
	}

}
