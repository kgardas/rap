/*******************************************************************************
 * Copyright (c) 2002, 2012 Innoopract Informationssysteme GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Innoopract Informationssysteme GmbH - initial API and implementation
 *    EclipseSource - ongoing development
 ******************************************************************************/

package org.eclipse.swt.events;

import static org.eclipse.rap.rwt.lifecycle.WidgetUtil.getId;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import junit.framework.TestCase;

import org.eclipse.rap.rwt.lifecycle.PhaseId;
import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;

public class ControlEvent_Test extends TestCase {

  private Display display;
  private Control control;

  @Override
  protected void setUp() throws Exception {
    Fixture.setUp();
    display = new Display();
    control = new Shell( display, SWT.NONE );
    Fixture.fakeNewRequest( display );
  }

  @Override
  protected void tearDown() throws Exception {
    Fixture.tearDown();
  }

  public void testResized() {
    ControlListener listener = mock( ControlListener.class );
    control.addControlListener( listener );

    Fixture.fakePhase( PhaseId.PROCESS_ACTION );
    control.setSize( 10, 20 );

    verify( listener, times( 1 ) ).controlResized( any( ControlEvent.class ) );
    verify( listener, times( 0 ) ).controlMoved( any( ControlEvent.class ) );
  }

  public void testResized_FromClient() {
    ControlListener listener = mock( ControlListener.class );
    control.addControlListener( listener );

    // this does not belong to all controls, only to those which allow
    // resize or move operations by the user (e.g. Shell)
    Fixture.fakeSetParameter( getId( control ), "bounds.width", Integer.valueOf( 50 ) );
    Fixture.fakeSetParameter( getId( control ), "bounds.height", Integer.valueOf( 100 ) );
    Fixture.readDataAndProcessAction( control );

    verify( listener, times( 1 ) ).controlResized( any( ControlEvent.class ) );
    verify( listener, times( 0 ) ).controlMoved( any( ControlEvent.class ) );
    assertEquals( new Point( 50, 100 ), control.getSize() );
  }

  public void testMoved() {
    ControlListener listener = mock( ControlListener.class );
    control.addControlListener( listener );

    Fixture.fakePhase( PhaseId.PROCESS_ACTION );
    control.setLocation( 30, 40 );

    verify( listener, times( 0 ) ).controlResized( any( ControlEvent.class ) );
    verify( listener, times( 1 ) ).controlMoved( any( ControlEvent.class ) );
  }

  public void testMoved_FromClient() {
    ControlListener listener = mock( ControlListener.class );
    control.addControlListener( listener );

    // this does not belong to all controls, only to those which allow
    // resize or move operations by the user (e.g. Shell)
    Fixture.fakeSetParameter( getId( control ), "bounds.x", Integer.valueOf( 150 ) );
    Fixture.fakeSetParameter( getId( control ), "bounds.y", Integer.valueOf( 200 ) );
    Fixture.readDataAndProcessAction( control );

    verify( listener, times( 0 ) ).controlResized( any( ControlEvent.class ) );
    verify( listener, times( 1 ) ).controlMoved( any( ControlEvent.class ) );
    assertEquals( new Point( 150, 200 ), control.getLocation() );
  }

}