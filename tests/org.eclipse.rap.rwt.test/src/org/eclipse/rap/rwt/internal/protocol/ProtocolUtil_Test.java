/*******************************************************************************
 * Copyright (c) 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.rap.rwt.internal.protocol;

import java.util.Arrays;

import org.eclipse.rap.rwt.graphics.Graphics;
import org.eclipse.rap.rwt.internal.protocol.ProtocolUtil;
import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.internal.graphics.FontUtil;
import org.eclipse.swt.widgets.Display;

import junit.framework.TestCase;


public class ProtocolUtil_Test extends TestCase {

  private Display display;

  @Override
  protected void setUp() throws Exception {
    Fixture.setUp();
    display = new Display();
  }

  @Override
  protected void tearDown() throws Exception {
    Fixture.tearDown();
  }

  public void testColorToArray() {
    Color red = display.getSystemColor( SWT.COLOR_RED );

    int[] array = ProtocolUtil.getColorAsArray( red, false );

    checkColorArray( 255, 0, 0, 255, array );
  }

  public void testColorToArray_RGB() {
    RGB red = new RGB( 255, 0, 0 );

    int[] array = ProtocolUtil.getColorAsArray( red, false );

    checkColorArray( 255, 0, 0, 255, array );
  }

  public void testColorToArray_Transparent() {
    Color red = display.getSystemColor( SWT.COLOR_RED );

    int[] array = ProtocolUtil.getColorAsArray( red, true );

    checkColorArray( 255, 0, 0, 0, array );
  }

  public void testColorToArray_Null() {
    assertNull( ProtocolUtil.getColorAsArray( ( Color )null, false ) );
  }

  public void testFontAsArray() {
    Font font = new Font( display, "Arial", 22, SWT.NONE );

    Object[] array = ProtocolUtil.getFontAsArray( font );

    checkFontArray( new String[] { "Arial" }, 22, false, false, array );
  }

  public void testFontAsArray_FontData() {
    Font font = new Font( display, "Arial", 22, SWT.NONE );

    Object[] array = ProtocolUtil.getFontAsArray( FontUtil.getData( font ) );

    checkFontArray( new String[] { "Arial" }, 22, false, false, array );
  }

  public void testFontAsArray_Bold() {
    Font font = new Font( display, "Arial", 22, SWT.BOLD );

    Object[] array = ProtocolUtil.getFontAsArray( font );

    checkFontArray( new String[] { "Arial" }, 22, true, false, array );
  }

  public void testFontAsArray_Italic() {
    Font font = new Font( display, "Arial", 22, SWT.ITALIC );

    Object[] array = ProtocolUtil.getFontAsArray( font );

    checkFontArray( new String[] { "Arial" }, 22, false, true, array );
  }

  public void testFontAsArray_Null() {
    assertNull( ProtocolUtil.getFontAsArray( ( Font )null ) );
  }

  @SuppressWarnings("deprecation")
  public void testImageAsArray() {
    Image image = Graphics.getImage( Fixture.IMAGE_100x50 );

    Object[] array = ProtocolUtil.getImageAsArray( image );

    assertNotNull( array[ 0 ] );
    assertEquals( Integer.valueOf( 100 ), array[ 1 ] );
    assertEquals( Integer.valueOf( 50 ), array[ 2 ] );
  }

  public void testImageAsArray_Null() {
    assertNull( ProtocolUtil.getImageAsArray( null ) );
  }

  private void checkColorArray( int red, int green, int blue, int alpha, int[] array ) {
    assertEquals( red, array[ 0 ] );
    assertEquals( green, array[ 1 ] );
    assertEquals( blue, array[ 2 ] );
    assertEquals( alpha, array[ 3 ] );
  }

  private void checkFontArray( String[] names,
                               int size,
                               boolean bold,
                               boolean italic,
                               Object[] array )
  {
    Arrays.equals( names, ( String[] )array[ 0 ] );
    assertEquals( Integer.valueOf( size ), array[ 1 ] );
    assertEquals( Boolean.valueOf( bold ), array[ 2 ] );
    assertEquals( Boolean.valueOf( italic ), array[ 3 ] );
  }

  public void testIsClientMessageProcessed_No() {
    fakeNewJsonMessage();

    assertFalse( ProtocolUtil.isClientMessageProcessed() );
  }

  public void testIsClientMessageProcessed_Yes() {
    fakeNewJsonMessage();

    ProtocolUtil.getClientMessage();

    assertTrue( ProtocolUtil.isClientMessageProcessed() );
  }

  public void testGetClientMessage() {
    fakeNewJsonMessage();

    ClientMessage message = ProtocolUtil.getClientMessage();

    assertNotNull( message );
    assertTrue( message.getAllOperations( "w3" ).length > 0 );
  }

  public void testGetClientMessage_SameInstance() {
    fakeNewJsonMessage();

    ClientMessage message1 = ProtocolUtil.getClientMessage();
    ClientMessage message2 = ProtocolUtil.getClientMessage();

    assertSame( message1, message2 );
  }

  public void testReadHeaderPropertyValue() {
    fakeNewJsonMessage();

    assertEquals( "21", ProtocolUtil.readHeaderPropertyValue( "requestCounter" ) );
  }

  public void testReadHeaderPropertyValue_MissingProperty() {
    fakeNewJsonMessage();

    assertNull( ProtocolUtil.readHeaderPropertyValue( "abc" ) );
  }

  public void testReadProperyValue_MissingProperty() {
    fakeNewJsonMessage();

    assertNull( ProtocolUtil.readPropertyValue( "w3", "p0" ) );
  }

  public void testReadProperyValue_String() {
    fakeNewJsonMessage();

    assertEquals( "foo", ProtocolUtil.readPropertyValue( "w3", "p1" ) );
  }

  public void testReadProperyValue_Integer() {
    fakeNewJsonMessage();

    assertEquals( "123", ProtocolUtil.readPropertyValue( "w3", "p2" ) );
  }

  public void testReadProperyValue_Boolean() {
    fakeNewJsonMessage();

    assertEquals( "true", ProtocolUtil.readPropertyValue( "w3", "p3" ) );
  }

  public void testReadProperyValue_Null() {
    fakeNewJsonMessage();

    assertEquals( "null", ProtocolUtil.readPropertyValue( "w3", "p4" ) );
  }

  public void testReadPropertyValue_LastSetValue() {
    String json = "{ "
                + ClientMessage.PROP_HEADER + " : {},"
                + ClientMessage.PROP_OPERATIONS + " : ["
                + "[ \"set\", \"w3\", { \"p1\" : \"foo\" } ], "
                + "[ \"set\", \"w3\", { \"p1\" : \"bar\" } ] "
                + "] }";
    Fixture.fakeNewRequest( display );
    Fixture.fakeRequestParam( "message", json );

    assertEquals( "bar", ProtocolUtil.readPropertyValue( "w3", "p1" ) );
  }

  public void testReadEventPropertyValue_MissingProperty() {
    fakeNewJsonMessage();

    assertNull( ProtocolUtil.readEventPropertyValue( "w3", "widgetSelected", "item" ) );
  }

  public void testReadEventPropertyValue() {
    fakeNewJsonMessage();

    String value = ProtocolUtil.readEventPropertyValue( "w3", "widgetSelected", "detail" );
    assertEquals( "check", value );
  }

  public void testWasEventSend_Send() {
    fakeNewJsonMessage();

    assertTrue( ProtocolUtil.wasEventSent( "w3", "widgetSelected" ) );
  }

  public void testWasEventSend_NotSend() {
    fakeNewJsonMessage();

    assertFalse( ProtocolUtil.wasEventSent( "w3", "widgetDefaultSelected" ) );
  }

  //////////////////
  // Helping methods

  private void fakeNewJsonMessage() {
    String json = "{ "
                + ClientMessage.PROP_HEADER + " : { \"requestCounter\" : 21 },"
                + ClientMessage.PROP_OPERATIONS + " : ["
                + "[ \"set\", \"w3\", { \"p1\" : \"foo\", \"p2\" : 123 } ], "
                + "[ \"notify\", \"w3\", \"widgetSelected\", { \"detail\" : \"check\" } ], "
                + "[ \"set\", \"w3\", { \"p3\" : true, \"p4\" : null } ] "
                + "] }";
    Fixture.fakeNewRequest( display );
    Fixture.fakeRequestParam( "message", json );
  }
}
